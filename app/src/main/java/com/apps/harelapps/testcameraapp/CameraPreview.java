package com.apps.harelapps.testcameraapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class CameraPreview
        extends SurfaceView
        implements SurfaceHolder.Callback {

    // https://developer.android.com
    // learned about image formats, YuvImage , scale pictures and compress pictures.

    // https://github.com/alessandrofrancesconi/NiceCameraExample/blob/master/src/com/ale/nicecameraexample/CameraPreview.java
    // I followed this project for basic understanding about the Camera setup, Camera best size, preview best size, rotation etc .


    private final float ASPECT_RATIO_W = 4.0f;
    private final float ASPECT_RATIO_H = 3.0f;
    private final int PREVIEW_MAX_WIDTH = 640;
    private final int PICTURE_MAX_WIDTH = 1280;


    private List<View> colors;
    private List<TextView> colorsPrecent;
    private byte[] previewBuffer;
    private boolean bProcessing = false;
    private byte[] FrameData;
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    Handler mHandler = new Handler(Looper.getMainLooper());


    @SuppressWarnings("deprecation")
    public CameraPreview(Context context, List<View> colorArray, List<TextView> colorPrecentArray) {
        super(context);

        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.colors = colorArray;
        this.colorsPrecent = colorPrecentArray;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setupCamera();
        startCameraPreview(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (this.surfaceHolder.getSurface() == null) {
            return;
        }

        stopCameraPreview();

        try {
            setCameraCallback();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateCameraDisplayOrientation();
        startCameraPreview(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        float ratio = ASPECT_RATIO_H / ASPECT_RATIO_W;
        if (width > height * ratio) {
            width = (int) (height / ratio + .5);
        } else {
            height = (int) (width / ratio + .5);
        }

        setMeasuredDimension(width, height);
    }

    private void setupCamera() {

        MainActivity parent = (MainActivity) this.getContext();
        camera = parent.getCamera();

        if (camera == null) {
            return;
        }

        Camera.Parameters parameters = camera.getParameters();

        Size bestPreviewSize = getBestSize(parameters.getSupportedPreviewSizes(), PREVIEW_MAX_WIDTH);
        Size bestPictureSize = getBestSize(parameters.getSupportedPictureSizes(), PICTURE_MAX_WIDTH);

        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);

        parameters.setPreviewFormat(ImageFormat.NV21); // NV21 is the most supported format for preview frames
        parameters.setPictureFormat(ImageFormat.JPEG); // JPEG for full resolution images

        camera.setParameters(parameters);

        int prevWidth = camera.getParameters().getPreviewSize().width;
        int prevHeight = camera.getParameters().getPreviewSize().height;

        this.previewBuffer = new byte[prevWidth * prevHeight * ImageFormat.getBitsPerPixel(camera.getParameters().getPreviewFormat()) / 8];
    }

    private void setCameraCallback() {
        MainActivity parent = (MainActivity) this.getContext();
        Camera camera = parent.getCamera();

        camera.addCallbackBuffer(this.previewBuffer);
        camera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera cam) {
                if (!bProcessing) {
                    FrameData = previewBuffer;
                    mHandler.postDelayed(DoImageProcessing, 100);
                }
                cam.addCallbackBuffer(previewBuffer);
                //cam.setPreviewCallbackWithBuffer(null);
            }
        });
    }

    private Runnable DoImageProcessing = new Runnable() {
        public void run() {
            bProcessing = true;
            processFrame(FrameData, camera);
            bProcessing = false;
        }
    };

    private Size getBestSize(List<Size> sizes, int widthThreshold) {
        Size bestSize = null;

        for (Size currentSize : sizes) {
            boolean isDesiredRatio = ((currentSize.width / ASPECT_RATIO_W) == (currentSize.height / ASPECT_RATIO_H));
            boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
            boolean isInBounds = currentSize.width <= widthThreshold;

            if (isDesiredRatio && isInBounds && isBetterSize) {
                bestSize = currentSize;
            }
        }

        if (bestSize == null) {
            bestSize = sizes.get(0);
        }

        return bestSize;
    }

    private synchronized void startCameraPreview(SurfaceHolder holder) {
        MainActivity parent = (MainActivity) this.getContext();
        Camera camera = parent.getCamera();

        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }

    private synchronized void stopCameraPreview() {
        MainActivity parent = (MainActivity) this.getContext();
        Camera camera = parent.getCamera();

        try {
            camera.stopPreview();
        } catch (Exception e) {

        }
    }

    private void updateCameraDisplayOrientation() {
        MainActivity parent = (MainActivity) this.getContext();
        Camera camera = parent.getCamera();
        int cameraID = parent.getCameraID();

        if (camera == null) {
            return;
        }

        int result;
        Activity parentActivity = (Activity) this.getContext();

        int rotation = parentActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraID, info);

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(result);
    }

    private byte[] getScaledImage(byte[] originalImage, int newWidth, int newHeight) {

        final int COMPRESS_QUALITY = 0;

        Bitmap bitmapImage = (BitmapFactory.decodeByteArray(originalImage, 0, originalImage.length));

        Bitmap mutableBitmapImage = Bitmap.createScaledBitmap(bitmapImage, newWidth, newHeight, false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mutableBitmapImage.compress(Bitmap.CompressFormat.PNG, COMPRESS_QUALITY, outputStream);

        if (mutableBitmapImage != bitmapImage) {
            mutableBitmapImage.recycle();
        }

        bitmapImage.recycle();
        return outputStream.toByteArray();
    }

    private void processFrame(byte[] raw, Camera cam) {

        Camera.Parameters parameters = cam.getParameters();
        int frameHeight = parameters.getPreviewSize().height;
        int frameWidth = parameters.getPreviewSize().width;
        double ratio = (double) frameWidth / frameHeight;

        YuvImage yuv = new YuvImage(raw, ImageFormat.NV21, frameWidth, frameHeight, null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        yuv.compressToJpeg(new Rect(0, 0, frameWidth, frameHeight), 100, stream);
        byte[] jpegData = stream.toByteArray();
        byte[] arr = getScaledImage(jpegData, 100, (int) (100 * ratio));
        final Bitmap theBitmap = BitmapFactory.decodeByteArray(arr, 0, arr.length);
        int[] pixels = new int[theBitmap.getHeight() * theBitmap.getWidth()];
        theBitmap.getPixels(pixels, 0, theBitmap.getWidth(), 0, 0, theBitmap.getWidth(), theBitmap.getHeight());

        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < theBitmap.getHeight(); i++) {
            for (int j = 0; j < theBitmap.getWidth(); j++) {

                int c = theBitmap.getPixel(j, i);

                if (!map.containsKey(c)) {
                    map.put(c, 1);
                } else {
                    map.put(c, map.get(c) + 1);
                }
            }
        }

        Map.Entry<Integer, Integer>[] colors = getMap(map);

        initColors(colors);
        initPrecent(colors, theBitmap.getHeight() * theBitmap.getWidth());


    }

    private void initColors(Map.Entry<Integer, Integer>[] colorsArr) {
        for (int i = 0; i < colorsArr.length; i++) {
            Map.Entry<Integer, Integer> colorEntry = colorsArr[i];
            colors.get(i).setBackgroundColor(colorEntry.getKey());

        }
    }

    private void initPrecent(Map.Entry<Integer, Integer>[] colors, int length) {
        for (int i = 0; i < colors.length; i++) {
            colorsPrecent.get(i).setText(String.valueOf(Math.round(colors[i].getValue() * 100 / (double) length * 100)) + "%");
        }
    }


    public static Map.Entry<Integer, Integer>[] getMap(Map map) {
        Map.Entry<Integer, Integer>[] arr;
        Iterator it = map.entrySet().iterator();
        Map.Entry<Integer, Integer> largest1 = null, largest2 = null, largest3 = null, largest4 = null, largest5 = null;
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> pair = (Map.Entry) it.next();

            if (largest1 == null || pair.getValue() > largest1.getValue()) {
                largest5 = largest4;
                largest4 = largest3;
                largest3 = largest2;
                largest2 = largest1;
                largest1 = pair;
            } else if (largest2 == null || pair.getValue() > largest2.getValue()) {
                largest5 = largest4;
                largest4 = largest3;
                largest3 = largest2;
                largest2 = pair;
            } else if (largest3 == null || pair.getValue() > largest3.getValue()) {
                largest5 = largest4;
                largest4 = largest3;
                largest3 = pair;
            } else if (largest4 == null || pair.getValue() > largest4.getValue()) {
                largest5 = largest4;
                largest4 = pair;
            } else if (largest5 == null || pair.getValue() > largest5.getValue()) {
                largest5 = pair;
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        arr = new Map.Entry[]{largest1, largest2, largest3, largest4, largest5};
        return arr;
    }
}