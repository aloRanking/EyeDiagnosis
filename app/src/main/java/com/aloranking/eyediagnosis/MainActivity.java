package com.aloranking.eyediagnosis;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int CAMERA_REQUEST = 18;
    private static final int PICK_IMAGE = 100;
    private String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Button takePhoto;
    private Button uploadPhoto, analysePhoto;

    private ImageView imageView;
    private FloatingActionButton mShareFab;
    private FloatingActionButton mSaveFab;
    private FloatingActionButton mClearFab;


    private TextView mWelcomeText;
    private TextView mInfoText;

    private static final String FILE_PROVIDER_AUTHORITY = "com.aloranking.fileprovider";
    private String mTempPhotoPath;

    private Bitmap mResultsBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        takePhoto = findViewById(R.id.take_photo_btn);
        uploadPhoto = findViewById(R.id.upload_btn);
        analysePhoto = findViewById(R.id.analyse_btn);

        imageView = findViewById(R.id.imageView);
        mShareFab = findViewById(R.id.share_button);
        mSaveFab = findViewById(R.id.save_button);
        mClearFab = findViewById(R.id.clear_button);
        mWelcomeText = findViewById(R.id.welcome_text);



        mSaveFab.setVisibility(View.GONE);
        mShareFab.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);
        analysePhoto.setVisibility(View.GONE);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id==R.id.action_folder){

            /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()
                    + "/Emojify/");
            intent.setDataAndType(uri, "image");
            startActivity(Intent.createChooser(intent, "Open folder"))*/;

            getPhoto();
        }

        return super.onOptionsItemSelected(item);
    }

    public void getPhoto(){
        String bucketId = "";

        final String[] projection = new String[] {"DISTINCT " + MediaStore.Images.Media.BUCKET_DISPLAY_NAME + ", " + MediaStore.Images.Media.BUCKET_ID};
        final Cursor cur = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);

        while (cur != null && cur.moveToNext()) {
            final String bucketName = cur.getString((cur.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)));
            if (bucketName.equals("Emojify")) {
                bucketId = cur.getString((cur.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID)));
                break;
            }
        }
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        if (bucketId.length() > 0) {
            mediaUri = mediaUri.buildUpon()
                    .authority("media")
                    .appendQueryParameter("bucketId", bucketId)
                    .build();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, mediaUri);
        startActivity(intent);
    }

    public void takePhoto(View view) {
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            // Launch the camera if the permission exists
            launchCamera();
        }
    }

    public void uploadPhoto(View view) {

        if (EasyPermissions.hasPermissions(this, galleryPermissions)) {
            Intent gallery =
                    new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(gallery, PICK_IMAGE);

        } else {
            EasyPermissions.requestPermissions(this, "Access for storage",
                    101, galleryPermissions);
        }

    }

    private void launchCamera() {

       /* // Create the capture image intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
*/
        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();
                Log.d("Picture Path", mTempPhotoPath);

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Process the image and set it to the TextView
           processAndSetImage();
        } else if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();
             mTempPhotoPath = getPath( MainActivity.this, imageUri );
            Log.d("Picture Path", mTempPhotoPath);


            takePhoto.setVisibility(View.GONE);
            uploadPhoto.setVisibility(View.GONE);
            mWelcomeText.setVisibility(View.GONE);
            mInfoText.setVisibility(View.GONE);

            mSaveFab.setVisibility(View.VISIBLE);
            mShareFab.setVisibility(View.VISIBLE);
            mClearFab.setVisibility(View.VISIBLE);
            analysePhoto.setVisibility(View.VISIBLE);

            mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);
            imageView.setImageBitmap(mResultsBitmap);
            imageView.setVisibility(View.VISIBLE);
        }

            //Otherwise, delete the temporary image file_paths
            //BitmapUtils.deleteImageFile(this, mTempPhotoPath);

    }

    public static String getPath(Context context, Uri uri ) {
        String result = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver( ).query( uri, proj, null, null, null );
        if(cursor != null){
            if ( cursor.moveToFirst( ) ) {
                int column_index = cursor.getColumnIndexOrThrow( proj[0] );
                result = cursor.getString( column_index );
            }
            cursor.close( );
        }
        if(result == null) {
            result = "Not found";
        }
        return result;
    }

    private void processAndSetImage() {

        mWelcomeText.setVisibility(View.GONE);
        takePhoto.setVisibility(View.GONE);
        uploadPhoto.setVisibility(View.GONE);

        mSaveFab.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.VISIBLE);
        mClearFab.setVisibility(View.VISIBLE);


        analysePhoto.setVisibility(View.VISIBLE);


        mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);
        imageView.setImageBitmap(mResultsBitmap);

    }

    public void clearImage(View view) {
        mWelcomeText.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);
        imageView.setImageResource(0);
        analysePhoto.setVisibility(View.GONE);

        takePhoto.setVisibility(View.VISIBLE);
        uploadPhoto.setVisibility(View.VISIBLE);

        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
    }

    public void saveImage(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);

        mSaveFab.setEnabled(false);
    }

    public void shareImage(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);

        // Share the image
        BitmapUtils.shareImage(this, mTempPhotoPath);

        mShareFab.setEnabled(false);
    }
}
