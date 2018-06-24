
package com.apps.harelapps.testcameraapp;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity
        extends Activity {

    public static RelativeLayout firstColor, secondColor, thirdColor, forthColor, fifthColor;

    FrameLayout preview;

    TextView firstColorText, secondColorText, thirdColorText, forthColorText, fifthColorText;

    private Camera camera;

    private int cameraID;

    private CameraPreview camPreview;

    private List<View> colorViewsArray;

    private List<TextView> colorPrecentArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LayoutInflater controlInflater = LayoutInflater.from(getApplicationContext());
        View viewControl = controlInflater.inflate(R.layout.custom, null);
        FrameLayout.LayoutParams layoutParamsControl = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        this.addContentView(viewControl, layoutParamsControl);
        initViews();

        colorViewsArray = new ArrayList<>();
        colorPrecentArray = new ArrayList<>();

        initColorAndPrecent();


        if (isCameraInstance()) {
            this.camPreview = new CameraPreview(this, colorViewsArray, colorPrecentArray);
        } else {
            this.finish();
        }

        preview.addView(this.camPreview);

        FrameLayout.LayoutParams previewLayout = (FrameLayout.LayoutParams) camPreview.getLayoutParams();
        previewLayout.width = LayoutParams.MATCH_PARENT;
        previewLayout.height = LayoutParams.MATCH_PARENT;
        this.camPreview.setLayoutParams(previewLayout);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCameraInstance()) {
            // TODO: camPreview.refresh...
        } else {

            this.finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCameraInstance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCameraInstance();
    }

    private boolean isCameraInstance() {
        if (this.camera != null) {


            return true;
        }

        if (this.cameraID < 0) {
            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, camInfo);

                if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    try {
                        this.camera = Camera.open(i);
                        this.cameraID = i;
                        return true;
                    } catch (RuntimeException e) {

                    }
                }
            }
        } else {
            try {
                this.camera = Camera.open(this.cameraID);
            } catch (RuntimeException e) {

            }
        }


        if (this.camera == null) {
            try {
                this.camera = Camera.open();
                this.cameraID = 0;
            } catch (RuntimeException e) {

                return false;
            }
        }

        return true;
    }

    private void releaseCameraInstance() {
        if (this.camera != null) {
            try {
                this.camera.stopPreview();
            } catch (Exception e) {

            }

            this.camera.setPreviewCallback(null);
            this.camera.release();
            this.camera = null;
            this.cameraID = -1;

        }
    }

    public Camera getCamera() {
        return this.camera;
    }

    public int getCameraID() {
        return this.cameraID;
    }

    public void initColorAndPrecent() {
        colorViewsArray.add(firstColor);
        colorViewsArray.add(secondColor);
        colorViewsArray.add(thirdColor);
        colorViewsArray.add(forthColor);
        colorViewsArray.add(fifthColor);

        colorPrecentArray.add(firstColorText);
        colorPrecentArray.add(secondColorText);
        colorPrecentArray.add(thirdColorText);
        colorPrecentArray.add(forthColorText);
        colorPrecentArray.add(fifthColorText);
    }

    public void initViews() {
        preview = findViewById(R.id.camera_preview);
        firstColor = findViewById(R.id.first_color);
        secondColor = findViewById(R.id.second_color);
        thirdColor = findViewById(R.id.third_color);
        forthColor = findViewById(R.id.forth_color);
        fifthColor = findViewById(R.id.fifth_color);

        firstColorText = findViewById(R.id.first_color_text);
        secondColorText = findViewById(R.id.second_color_text);
        thirdColorText = findViewById(R.id.third_color_text);
        forthColorText = findViewById(R.id.forth_color_text);
        fifthColorText = findViewById(R.id.fifth_color_text);
    }

}