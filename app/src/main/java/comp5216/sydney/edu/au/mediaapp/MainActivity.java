package comp5216.sydney.edu.au.mediaapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
        return hasPermissions(CAMERA_STORAGE_PERMISSIONS);
    }

    private boolean hasLocationPermissions() {
        return hasPermissions(LOCATION_PERMISSIONS);
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (hasCameraStoragePermissions()) {
                if (hasLocationPermissions()) {
                    initializeCamera();
                } else {
                    explainPermissionReason("Location permissions are required for XYZ functionality.");
                }
            } else {
                explainPermissionReason("Camera and storage permissions are required to capture and save photos.");
            }
        }
    }

    private void explainPermissionReason(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Optionally, you could also show a dialog with more detailed explanation
    }

    private void initializeCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                if (cameraProvider == null) {
                    Log.d("MainActivity", "cameraProvider is null");
                    return;
                }

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                imageCapture = new ImageCapture.Builder().build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);

                Log.d("MainActivity", "Camera has been initialized");

            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors here
                Toast.makeText(this, "Error initializing camera", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Error initializing camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        Log.d("MainActivity", "Inside takePhoto() method");
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("MainActivity", "About to capture image");

        // Generate a file to store the image
        File file = new File(getExternalFilesDir(null), "photo-" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String msg = "Photo captured: " + outputFileResults.getSavedUri();
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getApplicationContext(), "Error capturing photo", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Image capture error: " + exception.getMessage(), exception);
            }
        });
    }
}
