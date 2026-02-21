package com.anshul.a240dc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.*;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Range;
import android.view.Surface;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;
    private Button btnRecord;
    private boolean isRecording = false;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private MediaRecorder mediaRecorder;
    private CameraConstrainedHighSpeedCaptureSession captureSession;

    // Store the path globally so we can show it when recording stops
    private String currentVideoFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = findViewById(R.id.btnRecord);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        checkPermissions();

        btnRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean needsPermission = false;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needsPermission = true;
                break;
            }
        }

        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permissions required to record video.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        camera.close();
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        camera.close();
                    }
                }, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupMediaRecorder() {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            File videoDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            if (!videoDir.exists()) {
                videoDir.mkdirs();
            }

            File videoFile = new File(
                    videoDir,
                    "HighSpeed_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4"
            );

            // Save the path to the global variable
            currentVideoFilePath = videoFile.getAbsolutePath();
            mediaRecorder.setOutputFile(currentVideoFilePath);

            // Set for 1080p at 240fps
            mediaRecorder.setVideoEncodingBitRate(100_000_000);
            mediaRecorder.setVideoFrameRate(120);
            mediaRecorder.setVideoSize(1920, 1080);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        if (cameraDevice == null) return;
        setupMediaRecorder();

        try {
            Surface recorderSurface = mediaRecorder.getSurface();
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(recorderSurface);

            Range<Integer> fpsRange = new Range<>(120, 120);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);

            List<Surface> surfaces = Collections.singletonList(recorderSurface);

            cameraDevice.createConstrainedHighSpeedCaptureSession(
                    surfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = (CameraConstrainedHighSpeedCaptureSession) session;
                            try {
                                List<CaptureRequest> requestList = captureSession.createHighSpeedRequestList(captureRequestBuilder.build());
                                captureSession.setRepeatingBurst(requestList, null, null);
                                mediaRecorder.start();

                                runOnUiThread(() -> {
                                    isRecording = true;
                                    btnRecord.setText("STOP");
                                    btnRecord.setBackgroundColor(Color.DKGRAY);
                                    Toast.makeText(MainActivity.this, "Recording 240FPS...", Toast.LENGTH_SHORT).show();
                                });
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(MainActivity.this, "Failed to configure camera", Toast.LENGTH_SHORT).show();
                        }
                    },
                    null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.close();
            }
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
            }

            isRecording = false;
            btnRecord.setText("RECORD");
            btnRecord.setBackgroundColor(Color.RED);

            // Show the exact file path in the Toast
            Toast.makeText(this, "Saved: " + currentVideoFilePath, Toast.LENGTH_LONG).show();

            // Notify the Android gallery that a new video has been created
            MediaScannerConnection.scanFile(this,
                    new String[]{currentVideoFilePath}, null,
                    (path, uri) -> {
                        // The gallery now knows about the file
                    });

            openCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
    }
}