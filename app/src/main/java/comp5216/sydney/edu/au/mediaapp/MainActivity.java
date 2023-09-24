package comp5216.sydney.edu.au.mediaapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 10;
    private static final String[] CAMERA_STORAGE_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private PreviewView previewView;
    private ImageCapture imageCapture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.camera_preview);

        Button captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(v -> takePhoto());

        if (!hasCameraStoragePermissions()) {
            ActivityCompat.requestPermissions(this, CAMERA_STORAGE_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        } else {
            if (!hasLocationPermissions()) {
                ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            } else {
                initializeCamera();
            }
        }
    }

    private boolean hasCameraStoragePermissions() {
        for (String permission : CAMERA_STORAGE_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permission not granted: " + permission);
                return false;
            }
        }
        Log.d("MainActivity", "Camera and Storage permissions granted");
        return true;
    }

    private boolean hasLocationPermissions() {
        for (String permission : LOCATION_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (hasCameraStoragePermissions()) {
            if (hasLocationPermissions()) {
                initializeCamera();
            } else {
                ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            explainPermissionReason("Camera and storage permissions are required to capture and save photos.");
        }
    }

    private void explainPermissionReason(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void initializeCamera() {
        Log.d("MainActivity", "Initializing camera");

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                if (cameraProvider == null) {
                    Log.d("MainActivity", "cameraProvider is null");
                    Toast.makeText(MainActivity.this, "Camera provider is null.", Toast.LENGTH_LONG).show();
                    return;
                }

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                try {
                    if (!cameraProvider.hasCamera(cameraSelector)) {
                        Log.d("MainActivity", "No camera matching the camera selector.");
                        Toast.makeText(MainActivity.this, "No camera available.", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (CameraInfoUnavailableException e) {
                    Log.d("MainActivity", "Camera info unavailable: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Camera info unavailable.", Toast.LENGTH_LONG).show();
                    return;
                }

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();
                Log.d("MainActivity", "ImageCapture initialized");

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
                Log.d("MainActivity", "Camera has been bound to lifecycle");

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error initializing camera", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Error initializing camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        Log.d("MainActivity", "Inside takePhoto() method");

        if (imageCapture == null) {
            Toast.makeText(this, "Camera not initialized", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "ImageCapture is null");
            return;
        }

        File file = new File(getExternalFilesDir(null), "photo-" + System.currentTimeMillis() + ".jpg");
        if(file.exists()) {
            Log.d("MainActivity", "File already exists");
        } else {
            Log.d("MainActivity", "New file created at: " + file.getAbsolutePath());
        }

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        Log.d("MainActivity", "About to capture image");

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String msg = "Photo captured: " + outputFileResults.getSavedUri();
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Image saved successfully");
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getApplicationContext(), "Error capturing photo", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Image capture error: " + exception.getMessage(), exception);
            }
        });
    }

}
