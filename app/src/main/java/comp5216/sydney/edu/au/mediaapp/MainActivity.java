package comp5216.sydney.edu.au.mediaapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.content.FileProvider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import java.util.List;

public class MainActivity extends Activity {

    //request codes
    private static final int MY_PERMISSIONS_REQUEST_OPEN_CAMERA = 101;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHOTOS = 102;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_VIDEO = 103;
    private static final int MY_PERMISSIONS_REQUEST_READ_VIDEOS = 104;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 105;
    public final String APP_TAG = "MobileComputingTutorial";
    public String photoFileName = "photo.jpg";
    public String videoFileName = "video.mp4";
    public String audioFileName = "audio.3gp";
    MarshmallowPermission marshmallowPermission = new MarshmallowPermission(this);

    private File file;
    private final MediaRecorder recorder = null;
    private final MediaPlayer player = null;
    private StorageReference storageReference;  // Firebase Cloud Storage Reference

    private LocationManager locationManager;  // <-- ADDED: To manage location updates
    private String currentCity = "";  // <-- ADDED: Store the current city when taking photo or video

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Initialize Firebase Storage Reference
        storageReference = FirebaseStorage.getInstance().getReference();

        // Initialize LocationManager for fetching location
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    currentCity = addresses.get(0).getLocality(); // Get city name
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    public void onLoadPhotoClick(View view) {

        // Create intent for picking a photo from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Bring up gallery to select a photo
        startActivityForResult(intent, MY_PERMISSIONS_REQUEST_READ_PHOTOS);

    }

    public void onLoadVideoClick(View view) {

        // Create intent for picking a video from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        // Bring up gallery to select a video
        startActivityForResult(intent, MY_PERMISSIONS_REQUEST_READ_VIDEOS);

    }

    public void onTakePhotoClick(View v) {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()) {
            marshmallowPermission.requestPermissionForCamera();
        } else {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // set file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            photoFileName = "IMG_" + timeStamp + ".jpg";

            // Create a photo file reference
            Uri file_uri = getFileUri(photoFileName, 0);

            // Add extended data to the intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);

            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, MY_PERMISSIONS_REQUEST_OPEN_CAMERA);
            }
        }
    }

    public void onRecordVideoClick(View v) {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()) {
            marshmallowPermission.requestPermissionForCamera();
        } else {
            // create Intent to capture a video and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

            // set file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            videoFileName = "VIDEO_" + timeStamp + ".mp4";

            // Create a video file reference
            Uri file_uri = getFileUri(videoFileName, 1);

            // add extended data to the intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);

            // Start the video record intent to capture video
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_RECORD_VIDEO);
        }
    }

    // Returns the Uri for a photo/media stored on disk given the fileName
    public Uri getFileUri(String fileName) {
        // Get safe storage directory for photos
        File mediaStorageDir = new File(getExternalFilesDir(Environment.getExternalStorageDirectory().toString()), APP_TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(APP_TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        return Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator + fileName));
    }

    private void scanFile(String path) {

        MediaScannerConnection.scanFile(MainActivity.this,
                new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }

    // Returns the Uri for a photo/media stored on disk given the fileName and type
    public Uri getFileUri(String fileName, int type) {
        Uri fileUri = null;
        try {
            String typestr = "images"; //default to images type
            if (type == 1) {
                typestr = "videos";
            } else if (type != 0) {
                typestr = "audios";
            }

            File mediaStorageDir = new File(getExternalMediaDirs()[0], APP_TAG);

            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.d(APP_TAG, "failed to create directory");
            }

            file = new File(mediaStorageDir, fileName);

            if (Build.VERSION.SDK_INT >= 24) {
                fileUri = FileProvider.getUriForFile(this.getApplicationContext(), "comp5216.sydney.edu.au.mediaapp.fileProvider", file);
            } else {
                fileUri = Uri.fromFile(mediaStorageDir);
            }
        } catch (Exception ex) {
            Log.e("getFileUri", ex.getStackTrace().toString());
        }
        return fileUri;
    }

    private void uploadFileToFirebase(Uri fileUri, String folderName) {
        if (fileUri != null) {
            StorageReference fileRef = storageReference.child(currentCity + "/" + folderName + "/" + fileUri.getLastPathSegment());
            UploadTask uploadTask = fileRef.putFile(fileUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // File uploaded successfully
                Toast.makeText(MainActivity.this, "Upload successful", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(exception -> {
                // Handle unsuccessful uploads
                Toast.makeText(MainActivity.this, "Upload failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(APP_TAG, "Upload failed", exception);
            });
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        final VideoView mVideoView = findViewById(R.id.videoview);
        ImageView ivPreview = findViewById(R.id.photopreview);

        mVideoView.setVisibility(View.GONE);
        ivPreview.setVisibility(View.GONE);

        if (requestCode == MY_PERMISSIONS_REQUEST_OPEN_CAMERA) {
            if (resultCode == RESULT_OK) {
                // by this point we have the camera photo on disk
                Bitmap takenImage = BitmapFactory.decodeFile(file.getAbsolutePath());
                scanFile(file.getAbsolutePath());
                ivPreview.setImageBitmap(takenImage);
                ivPreview.setVisibility(View.VISIBLE);

                // Upload to Firebase
                uploadFileToFirebase(getFileUri(photoFileName, 1), "images");

            } else { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHOTOS) {
            if (resultCode == RESULT_OK) {
                Uri photoUri = data.getData();
                // Do something with the photo based on Uri
                Bitmap selectedImage;
                try {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);

                    // Load the selected image into a preview
                    ivPreview.setImageBitmap(selectedImage);
                    ivPreview.setVisibility(View.VISIBLE);
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // Upload to Firebase
                uploadFileToFirebase(data.getData(), "images");
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_VIDEOS) {
            if (resultCode == RESULT_OK) {
                Uri videoUri = data.getData();
                mVideoView.setVisibility(View.VISIBLE);
                mVideoView.setVideoURI(videoUri);
                mVideoView.requestFocus();
                mVideoView.setOnPreparedListener(new OnPreparedListener() {
                    // Close the progress bar and play the video
                    public void onPrepared(MediaPlayer mp) {
                        mVideoView.start();
                    }
                });

                // Upload to Firebase
                uploadFileToFirebase(getFileUri(videoFileName, 1), "videos");
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_VIDEO) {
            //if you are running on emulator remove the if statement
            if (resultCode != RESULT_OK) {
                Uri takenVideoUri = getFileUri(videoFileName, 1);
                mVideoView.setVisibility(View.VISIBLE);
                mVideoView.setVideoURI(takenVideoUri);
                mVideoView.requestFocus();
                mVideoView.setOnPreparedListener(new OnPreparedListener() {
                    // Close the progress bar and play the video
                    public void onPrepared(MediaPlayer mp) {
                        mVideoView.start();
                    }
                });

                // Upload to Firebase
                uploadFileToFirebase(getFileUri(videoFileName, 1), "videos");
            }
        }
    }

    // Unregister the location listener to conserve battery
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}
