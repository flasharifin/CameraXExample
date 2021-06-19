package com.example.cameraxexample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

/**
 * Created by Mochamad Arifin on 19/06/2021.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView camera, flash;
    private TextureView cameraView;
    private File imageFile;
    private ImageCapture imageCapture;
    private Boolean flashLamp = false;

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView  = findViewById(R.id.cameraView);
        camera      = findViewById(R.id.takePict);
        flash       = findViewById(R.id.flashCamera);

        camera.setOnClickListener(this);
        flash.setOnClickListener(this);

        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    public void startCamera() {
        CameraX.unbindAll();

        Rational aspectRatio = new Rational (cameraView.getWidth(), cameraView.getHeight());
        Size screen = new Size(cameraView.getWidth(), cameraView.getHeight());

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();

        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup) cameraView.getParent();
                parent.removeView(cameraView);
                parent.addView(cameraView, 0);
                // code for always render refreshed camera screen view
                cameraView.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.BACK).setFlashMode(FlashMode.OFF).build();

        imageCapture = new ImageCapture(imageCaptureConfig);

        CameraX.bindToLifecycle((LifecycleOwner) this, preview, imageCapture);
    }

    private void updateTransform() {

        Matrix mx = new Matrix();
        float w = cameraView.getMeasuredWidth();
        float h = cameraView.getMeasuredHeight();

        float cx = w/2f;
        float cy = h/2f;

        int rotationDgr;
        int rotation = (int) cameraView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }
        // for Old Mobile Version
        mx.postRotate((float)rotationDgr,cx,cy);

        // for Newer Mobile Version
        cameraView.setTransform(mx);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.takePict:
                try {
                    String state = Environment.getExternalStorageState();
                    File folder = null;
                    if (state.contains(Environment.MEDIA_MOUNTED)) {
                        folder = new File(Environment
                                .getExternalStorageDirectory() + "/Demo"); // Name Folder Path Directory save image
                    } else {
                        folder = new File(Environment
                                .getExternalStorageDirectory() + "/Demo"); // Name Folder Path Directory save image
                    }

                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        java.util.Date date = new java.util.Date();
                        imageFile = new File(folder.getAbsolutePath()
                                + File.separator
                                + new Timestamp(date.getTime()).toString()
                                + "Image.jpg");

                        imageFile.createNewFile();
                        imageCapture.takePicture(imageFile, new ImageCapture.OnImageSavedListener() {
                            @Override
                            public void onImageSaved(@NonNull File file) {
                                String msg = "Pic captured at " + file.getAbsolutePath();
                                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();

                            }
                            @Override
                            public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {

                                String msg = "Pic captured Failed at " + imageFile.getAbsolutePath();
                                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();

                                if(cause != null) {
                                    cause.printStackTrace();
                                }
                            }
                        });
                    }else {
                        Toast.makeText(getBaseContext(), "Image Not saved",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                }catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.flashCamera:
                if (flashLamp) {
                    flashLamp = false;
                    imageCapture.setFlashMode(FlashMode.OFF);
                    Toast.makeText(this, "Flash Disable", Toast.LENGTH_SHORT).show();
                }else {
                    flashLamp = true;
                    imageCapture.setFlashMode(FlashMode.ON);
                    Toast.makeText(this, "Flash Enable", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}