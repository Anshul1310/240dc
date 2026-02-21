package com.anshul.a240dc;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.util.Range;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class MainActivity extends AppCompatActivity {

    private String selectedFps = "120";
    private String selectedIso = "400";
    private String selectedShutter = "1/240";

    private boolean supportsHighSpeedVideo(int targetFps) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            // Loop through all cameras on the device (e.g., back, front, ultrawide)
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);

                // We usually only care about the main Back camera for Pro Video
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue; // Skip front cameras
                }

                // 1. Check if High Speed Video is a supported capability
                int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                boolean supportsHighSpeedMode = false;

                if (capabilities != null) {
                    for (int capability : capabilities) {
                        if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) {
                            supportsHighSpeedMode = true;
                            break;
                        }
                    }
                }

                // 2. If high speed is supported, check if the max FPS hits our target (120 or 240)
                if (supportsHighSpeedMode) {
                    StreamConfigurationMap map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        // Get the allowed high-speed frame rate ranges from the hardware
                        Range<Integer>[] fpsRanges = map.getHighSpeedVideoFpsRanges();

                        for (Range<Integer> range : fpsRanges) {
                            if (range.getUpper() >= targetFps) {
                                return true; // Success! The camera supports the target FPS.
                            }
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // If the loop finishes without returning true, the device doesn't support it
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide status bar for full-screen camera experience
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (!supportsHighSpeedVideo(120)) {
            // DEVICE NOT SUPPORTED: Redirect to the fallback page
            Intent intent = new Intent(this, UnsupportedDeviceActivity.class);
            startActivity(intent);
            finish(); // Close this activity so the user can't press 'back' into it
            return; // Stop executing the rest of onCreate
        }
        setContentView(R.layout.activity_main);

        setupFpsToggle();
        setupIsoDial();
        setupShutterDial();

        // Record Button Click
        findViewById(R.id.btn_record).setOnClickListener(v -> {
            String metadata = "Recording at " + selectedFps + " FPS | ISO " + selectedIso + " | Speed " + selectedShutter;
            Toast.makeText(MainActivity.this, metadata, Toast.LENGTH_SHORT).show();
            // Start your Camera2 recording logic here
        });
    }

    private void setupFpsToggle() {
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggle_fps);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_120fps) {
                    selectedFps = "120";
                } else if (checkedId == R.id.btn_240fps) {
                    selectedFps = "240";
                }
            }
        });
    }

    private void setupIsoDial() {
        ChipGroup chipGroupIso = findViewById(R.id.chipGroup_iso);
        String[] isoValues = {"100", "200", "400", "800", "1600", "3200", "6400"};

        for (String iso : isoValues) {
            Chip chip = createProChip(iso);
            chipGroupIso.addView(chip);
            if (iso.equals(selectedIso)) {
                chip.setChecked(true);
            }
        }

        chipGroupIso.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = findViewById(checkedIds.get(0));
                selectedIso = chip.getText().toString();
            }
        });
    }

    private void setupShutterDial() {
        ChipGroup chipGroupShutter = findViewById(R.id.chipGroup_shutter);
        String[] shutterValues = {"1/60", "1/120", "1/240", "1/480", "1/1000", "1/2000", "1/4000"};

        for (String speed : shutterValues) {
            Chip chip = createProChip(speed);
            chipGroupShutter.addView(chip);
            if (speed.equals(selectedShutter)) {
                chip.setChecked(true);
            }
        }

        chipGroupShutter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = findViewById(checkedIds.get(0));
                selectedShutter = chip.getText().toString();
            }
        });
    }

    // Helper method to create a sleek Samsung-style chip
    private Chip createProChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(false); // Hide the default checkmark

        // Custom styling: Black background, white text unselected, yellow text selected
        chip.setChipBackgroundColorResource(android.R.color.transparent);

        // Create color state lists for text and borders
        int colorUnselected = ContextCompat.getColor(this, R.color.white);
        int colorSelected = ContextCompat.getColor(this, R.color.pro_accent_yellow);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked }, // checked
                new int[] { -android.R.attr.state_checked } // unchecked
        };

        int[] colors = new int[] { colorSelected, colorUnselected };
        ColorStateList colorStateList = new ColorStateList(states, colors);

        chip.setTextColor(colorStateList);
        chip.setChipStrokeColor(colorStateList);
        chip.setChipStrokeWidth(2f);

        return chip;
    }
}