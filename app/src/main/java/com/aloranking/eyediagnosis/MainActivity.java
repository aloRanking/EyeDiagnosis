package com.aloranking.eyediagnosis;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int CAMERA_REQUEST = 18;
    private static final int PICK_IMAGE = 100;
    private String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Button takePhoto;
    private Button uploadPhoto, analysePhoto;
    private boolean isPhotoSaved;

    private ImageView imageView;
    private FloatingActionButton mShareFab;
    private FloatingActionButton mSaveFab;
    private FloatingActionButton mClearFab;


    private TextView mWelcomeText;
    private TextView mInfoText;

    private static final String FILE_PROVIDER_AUTHORITY = "com.aloranking.fileprovider";
    private String mTempPhotoPath;
    private static String path2;

    private Bitmap mResultsBitmap, mUploadBitmap;

    private  Bitmap bmp, bmpimg1, bmpimg2;
    private static String descriptorType;
    private static int min_dist = 10;
    private static int min_matches = 750;
    private static int descriptor = DescriptorExtractor.BRISK;
    private static long startTime, endTime;
    private static String matchText,typeOfDisease;
    private static String path1;
    private int imageSelectionType = 0;

    ArrayList<Mat> histImages= new ArrayList<>();
    ArrayList<Bitmap> bmpImages= new ArrayList<>();
    List<Integer> list = new ArrayList<>();
    ArrayList<Bitmap> bitmapImages = new ArrayList<>();
    ArrayList<String> assestString = new ArrayList<>();
    private Context context;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("TAG", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = MainActivity.this;

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


        run();

       /* if (list.size()==0){
        addImagesToList();
        }*/


        final AssetManager mgr = getAssets();
        displayFiles(mgr, "imgs", context);

    }

    public void run() {
        if (descriptor == DescriptorExtractor.BRIEF)
            descriptorType = "BRIEF";
        else if (descriptor == DescriptorExtractor.BRISK)
            descriptorType = "BRISK";
        else if (descriptor == DescriptorExtractor.FREAK)
            descriptorType = "FREAK";
        else if (descriptor == DescriptorExtractor.ORB)
            descriptorType = "ORB";
        else if (descriptor == DescriptorExtractor.SIFT)
            descriptorType = "SIFT";
        else if (descriptor == DescriptorExtractor.SURF)
            descriptorType = "SURF";
        System.out.println(descriptorType);
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        min_dist = newIntent.getExtras().getInt("min_dist");
        descriptor = newIntent.getExtras().getInt("descriptor");
        min_matches = newIntent.getExtras().getInt("min_matches");
        run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this,
                mLoaderCallback);
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
            Intent call = new Intent(MainActivity.this, Settings.class);
            call.putExtra("descriptor", descriptor);
            call.putExtra("min_dist", min_dist);
            call.putExtra("min_matches", min_matches);
            call.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            call.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(call);
        }
        if (id == R.id.action_folder) {

            /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()
                    + "/Emojify/");
            intent.setDataAndType(uri, "image");
            startActivity(Intent.createChooser(intent, "Open folder"))*/
            ;

            getPhoto();
        }

        return super.onOptionsItemSelected(item);
    }





    public void getPhoto() {
        String bucketId = "";

        final String[] projection = new String[]{"DISTINCT " + MediaStore.Images.Media.BUCKET_DISPLAY_NAME + ", " + MediaStore.Images.Media.BUCKET_ID};
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

        cur.close();

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
            path2 = getPath(MainActivity.this, imageUri);
            path1 = "from the dataset";
            Log.d("Picture Path", path2);


            takePhoto.setVisibility(View.GONE);
            uploadPhoto.setVisibility(View.GONE);
            mWelcomeText.setVisibility(View.GONE);


            mSaveFab.setVisibility(View.VISIBLE);
            mShareFab.setVisibility(View.VISIBLE);
            mClearFab.setVisibility(View.VISIBLE);
            analysePhoto.setVisibility(View.VISIBLE);
            mShareFab.setEnabled(false);

            mUploadBitmap = BitmapUtils.resamplePic(this, path2);
            // mUploadBitmap = BitmapFactory.de

            imageView.setImageBitmap(mUploadBitmap);
            imageView.setVisibility(View.VISIBLE);
            imageSelectionType = 2;

        }

        //Otherwise, delete the temporary image file_paths
        //BitmapUtils.deleteImageFile(this, mTempPhotoPath);

    }

    public static String getPath(Context context, Uri uri) {
        String result = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(proj[0]);
                result = cursor.getString(column_index);
            }
            cursor.close();
        }
        if (result == null) {
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
        mShareFab.setEnabled(false);


        mUploadBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);
        imageView.setImageBitmap(mUploadBitmap);
        imageSelectionType = 1;



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
        mSaveFab.setEnabled(true);
        mShareFab.setEnabled(true);

        // BitmapUtils.deleteImageFile(this, mTempPhotoPath);


        Toast.makeText(this, "Picture Cleared", Toast.LENGTH_SHORT)
                .show();


       /* if (isPhotoSaved) {

        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete picture?")
                    .setIcon(R.drawable.ic_warning_black_24dp)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            mWelcomeText.setVisibility(View.VISIBLE);
                            mShareFab.setVisibility(View.GONE);
                            mSaveFab.setVisibility(View.GONE);
                            mClearFab.setVisibility(View.GONE);
                            imageView.setImageResource(0);
                            analysePhoto.setVisibility(View.GONE);

                            takePhoto.setVisibility(View.VISIBLE);
                            uploadPhoto.setVisibility(View.VISIBLE);

                            // BitmapUtils.deleteImageFile(this, mTempPhotoPath);


                            Toast.makeText(getApplicationContext(), "Picture Deleted", Toast.LENGTH_SHORT)
                                    .show();

                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.cancel();

                        }
                    })
                    .show();
        }*/


    }

    public void saveImage(View view) {
        // Delete the temporary image file
        //BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        isPhotoSaved = true;

        new AlertDialog.Builder(this)
                .setTitle("Save picture?")
                //.setMessage("Are you sure you want to delete this picture?")
                //.setIcon(R.drawable.ic_warning_black_24dp)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        BitmapUtils.saveImage(getApplicationContext(), mUploadBitmap);

                        mSaveFab.setEnabled(false);


                        //Log.d("DairyHome", "Diary deleted");
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.cancel();
                        //Log.d("DairyHome", "Aborting...");
                    }
                })
                .show();

        // Save the image

    }

    public void shareImage(View view) {
        // Delete the temporary image file
        //BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        //BitmapUtils.saveImage(this, mResultsBitmap);


        if (imageSelectionType == 1) {
            // Share the image
            BitmapUtils.shareImage(this, mTempPhotoPath);
        }else if (imageSelectionType == 2){
            BitmapUtils.shareImage(this, path2);
        }

        mShareFab.setEnabled(false);

    }

    void displayFiles (AssetManager mgr, String path, Context context) {
        try {
            String list[] = mgr.list(path);
            if (list != null)
                for (int i=0; i<list.length; ++i){


                    InputStream ims = context.getAssets().open("imgs/" + list[i]);
                    Bitmap bitmap = BitmapFactory.decodeStream(ims);
                    Bitmap bitResize = Bitmap.createScaledBitmap(bitmap, 200, 200,false);
                    bitmapImages.add(bitResize);

                 /*bitmapImages = BitmapFactory.decodeFile(list[i])*/
                  //  assestString = BitmapFactory.decodeFile(list[i]);


                    Log.v("Assets:", path +"/"+ list[i]);

                }
        } catch (IOException e) {
            Log.v("List error:", "can't list" + path);
        }

    }

    public void loadDataFromAssset(){
        /*try {
            // get input stream
            InputStream ims = getAssets().open("avatar.jpg");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            mImage.setImageDrawable(d);
        }
        catch(IOException ex) {
            return;
        }

        try {
            Class res = R.drawable.class;
            Field field = res.getField("drawableName");
            int drawableId = field.getInt(null);
        }
        catch (Exception e) {
            Log.e("MyTag", "Failure to get drawable id.", e);
        }*/
    }

    public void addImagesToList(){
        /*list.add(R.drawable.healthy1);
        list.add(R.drawable.healthy2);
        list.add(R.drawable.healthy3);
        list.add(R.drawable.healthy4);
        list.add(R.drawable.healthy5);
        list.add(R.drawable.healthy6);
        list.add(R.drawable.healthy7);
        list.add(R.drawable.healthy8);
        list.add(R.drawable.healthy9);
        list.add(R.drawable.healthy10);
        list.add(R.drawable.healthy11);
        list.add(R.drawable.healthy12);
        list.add(R.drawable.healthy13);
        list.add(R.drawable.healthy14);
        list.add(R.drawable.healthy15);
        list.add(R.drawable.healthy16);
        list.add(R.drawable.healthy17);
        list.add(R.drawable.healthy18);
        list.add(R.drawable.healthy19);
        //list.add(R.drawable.healthy20);
        //list.add(R.drawable.healthy21);
        list.add(R.drawable.healthy22);
        list.add(R.drawable.healthy23);
        //list.add(R.drawable.healthy24);
        list.add(R.drawable.healthy25);
        list.add(R.drawable.healthy26);
        list.add(R.drawable.healthy27);
        list.add(R.drawable.healthy28);
        list.add(R.drawable.healthy29);
        list.add(R.drawable.healthy30);*/
        /*list.add(R.drawable.healthy31);
        list.add(R.drawable.healthy32);
        list.add(R.drawable.healthy33);
        list.add(R.drawable.healthy34);
        list.add(R.drawable.healthy35);
        list.add(R.drawable.healthy36);
        list.add(R.drawable.healthy37);
        list.add(R.drawable.healthy38);
        list.add(R.drawable.healthy39);
        list.add(R.drawable.healthy40);
        list.add(R.drawable.healthy41);
        list.add(R.drawable.healthy42);
        list.add(R.drawable.healthy43);
        list.add(R.drawable.healthy44);
        list.add(R.drawable.healthy45);*/





        list.add(R.drawable.diabetic1);
        list.add(R.drawable.diabetic2);
        list.add(R.drawable.diabetic3);
        list.add(R.drawable.diabetic4);
        list.add(R.drawable.diabetic5);
       // list.add(R.drawable.diabetic6);
        list.add(R.drawable.diabetic7);
        list.add(R.drawable.diabetic8);
        list.add(R.drawable.diabetic9);
        list.add(R.drawable.diabetic10);
        /*list.add(R.drawable.diabetic11);
        list.add(R.drawable.diabetic12);
        list.add(R.drawable.diabetic13);
        list.add(R.drawable.diabetic14);
        list.add(R.drawable.diabetic15);
        list.add(R.drawable.diabetic16);
        list.add(R.drawable.diabetic17);
        list.add(R.drawable.diabetic18);
        list.add(R.drawable.diabetic19);
        list.add(R.drawable.diabetic20);*/
        /*list.add(R.drawable.diabetic21);
        list.add(R.drawable.diabetic22);
        list.add(R.drawable.diabetic23);
        list.add(R.drawable.diabetic24);
        list.add(R.drawable.diabetic25);
        list.add(R.drawable.diabetic26);
        list.add(R.drawable.diabetic27);
        list.add(R.drawable.diabetic28);
        list.add(R.drawable.diabetic29);
        list.add(R.drawable.diabetic30);
        list.add(R.drawable.diabetic31);
        list.add(R.drawable.diabetic32);
        list.add(R.drawable.diabetic33);
        list.add(R.drawable.diabetic34);
        list.add(R.drawable.diabetic35);
        list.add(R.drawable.diabetic36);
        list.add(R.drawable.diabetic37);
        list.add(R.drawable.diabetic38);
        list.add(R.drawable.diabetic39);
        list.add(R.drawable.diabetic40);
        list.add(R.drawable.diabetic41);
        list.add(R.drawable.diabetic42);
        list.add(R.drawable.diabetic43);
        list.add(R.drawable.diabetic44);
        list.add(R.drawable.diabetic45);*/




        /*list.add(R.drawable.glaucoma1);
        list.add(R.drawable.glaucoma2);
        list.add(R.drawable.glaucoma3);
        list.add(R.drawable.glaucoma4);
        list.add(R.drawable.glaucoma5);
        list.add(R.drawable.glaucoma6);
        list.add(R.drawable.glaucoma7);
        list.add(R.drawable.glaucoma8);
        list.add(R.drawable.glaucoma9);*/
        /*list.add(R.drawable.glaucoma10);
        list.add(R.drawable.glaucoma11);
        list.add(R.drawable.glaucoma12);
        list.add(R.drawable.glaucoma13);
        list.add(R.drawable.glaucoma14);
        list.add(R.drawable.glaucoma15);
        list.add(R.drawable.glaucoma16);
        list.add(R.drawable.glaucoma17);
        list.add(R.drawable.glaucoma18);
        list.add(R.drawable.glaucoma19);
        list.add(R.drawable.glaucoma20);*/
       /* list.add(R.drawable.glaucoma21);
        list.add(R.drawable.glaucoma22);
        list.add(R.drawable.glaucoma23);
        list.add(R.drawable.glaucoma24);
        list.add(R.drawable.glaucoma25);
        list.add(R.drawable.glaucoma26);
        list.add(R.drawable.glaucoma27);
        list.add(R.drawable.glaucoma28);
        list.add(R.drawable.glaucoma29);
        list.add(R.drawable.glaucoma30);
        list.add(R.drawable.glaucoma31);
        list.add(R.drawable.glaucoma32);
        list.add(R.drawable.glaucoma33);
        list.add(R.drawable.glaucoma34);
        list.add(R.drawable.glaucoma35);
        list.add(R.drawable.glaucoma36);
        list.add(R.drawable.glaucoma37);
        list.add(R.drawable.glaucoma38);
        list.add(R.drawable.glaucoma39);
        list.add(R.drawable.glaucoma40);
        list.add(R.drawable.glaucoma41);
        list.add(R.drawable.glaucoma42);
        list.add(R.drawable.glaucoma43);
        list.add(R.drawable.glaucoma44);
        list.add(R.drawable.glaucoma45);
        list.add(R.drawable.glaucoma46);*/


    }


    public void analyseImage(View view) {

        compareImages();



    }

    private void compareImages() {

        ProgressDialog pd;
        pd = new ProgressDialog(this);
        pd.setIndeterminate(true);
        pd.setCancelable(true);
        pd.setCanceledOnTouchOutside(false);
        pd.setMessage("Processing...");
        pd.show();

        int count = 0;
        if (histImages.size()==0) {


                for (int i = 0; i < bitmapImages.size(); i++) {

                    //Bitmap imgDrawable = BitmapFactory.decodeResource(getResources(), list.get(i));
                    Bitmap imgDrawable = bitmapImages.get(i);
                    Bitmap bitmap = Bitmap.createScaledBitmap(imgDrawable, 150, 150, true);
                    Mat img1 = new Mat();
                    Utils.bitmapToMat(bitmap, img1);
                    Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGBA2GRAY);
                    img1.convertTo(img1, CvType.CV_32F);
                    Mat hist1 = new Mat();
                    MatOfInt histSize = new MatOfInt(180);
                    MatOfInt channels = new MatOfInt(0);
                    ArrayList<Mat> bgr_planes1 = new ArrayList<Mat>();
                    Core.split(img1, bgr_planes1);
                    MatOfFloat histRanges = new MatOfFloat(0f, 180f);
                    boolean accumulate = false;
                    Imgproc.calcHist(bgr_planes1, channels, new Mat(), hist1, histSize, histRanges, accumulate);
                    Core.normalize(hist1, hist1, 0, hist1.rows(), Core.NORM_MINMAX, -1, new Mat());
                    img1.convertTo(img1, CvType.CV_32F);
                    hist1.convertTo(hist1, CvType.CV_32F);


                bmpImages.add(bitmap);
                Log.i("Bitmapimage", "bitmap images size is" + bmpImages.size());

                histImages.add(hist1);
                Log.i("DataImage", "data images size is" + histImages.size());
            }
        }



        bmpimg2 = Bitmap.createScaledBitmap(mUploadBitmap, 150, 150, true);
        Mat img2 = new Mat();
        Utils.bitmapToMat(bmpimg2, img2);
        Imgproc.cvtColor(img2, img2, Imgproc.COLOR_RGBA2GRAY);
        Mat hist2 = new Mat(); MatOfInt histSize = new MatOfInt(180);
        MatOfInt channels = new MatOfInt(0);
        ArrayList<Mat> bgr_planes2 = new ArrayList<Mat>();
        Core.split(img2, bgr_planes2);
        MatOfFloat histRanges = new MatOfFloat(0f, 180f);
        boolean accumulate = false;
        Imgproc.calcHist(bgr_planes2, channels, new Mat(), hist2, histSize, histRanges, accumulate);
        Core.normalize(hist2, hist2, 0, hist2.rows(), Core.NORM_MINMAX, -1, new Mat());
        img2.convertTo(img2, CvType.CV_32F);
        hist2.convertTo(hist2, CvType.CV_32F);


        for (int i =0; i<histImages.size(); i++){

            double compares = Imgproc.compareHist(histImages.get(i), hist2, Imgproc.CV_COMP_CHISQR);
            Log.d("EyeDiagnosis", "compare: " + compares);
            if (compares > 0 && compares < 200) {
                pd.cancel();
                bmpimg1 = bmpImages.get(i);
                Log.i("TAGS", "the value of i is "+ i);
                if (i<=44){
                    typeOfDisease = "Diabetis Retinopathy Detected";
                }else if (i>=45 && i<=89){
                    typeOfDisease = "Glaucoma Detected";
                }else {
                    typeOfDisease = " Healthy Eye";
                }
                Toast.makeText(MainActivity.this, "Image may be possible match, verifying", Toast.LENGTH_SHORT).show();
                new asyncTask(MainActivity.this).execute();

                Log.i("TAGA", "the value of i is "+ i);
                break;
            } else if (compares == 0) {
                 Toast.makeText(MainActivity.this, "Dataset matched", Toast.LENGTH_SHORT).show();
                 pd.cancel();
            } else {
                count++;
                if (count==list.size()){

                    Toast.makeText(MainActivity.this, "Match not found try another image", Toast.LENGTH_SHORT).show();
                    count=0;
                    pd.cancel();
                }
            }

            startTime = System.currentTimeMillis();




        }

    }


    public class asyncTask extends AsyncTask<Void, Void, Void> {
        private  Mat img1, img2, descriptors, dupDescriptors;
        private  FeatureDetector detector;
        private  DescriptorExtractor DescExtractor;
        private  DescriptorMatcher matcher;
        private  MatOfKeyPoint keypoints, dupKeypoints;
        private  MatOfDMatch matches, matches_final_mat;
        private  ProgressDialog pd;
        private  boolean isDuplicate = false;
        private MainActivity asyncTaskContext = null;
        private  Scalar RED = new Scalar(255, 0, 0);
        private  Scalar GREEN = new Scalar(0, 255, 0);

        private  Features2d features2d = null;

        public asyncTask(MainActivity context) {
            asyncTaskContext = context;
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(asyncTaskContext);
            pd.setIndeterminate(true);
            pd.setCancelable(true);
            pd.setCanceledOnTouchOutside(false);
            pd.setMessage("Processing...");
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // TODO Auto-generated method stub

            compare();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {


            try {
                Mat img3 = new Mat();
                MatOfByte drawnMatches = new MatOfByte();
                features2d.drawMatches(img1, keypoints, img2, dupKeypoints,
                        matches_final_mat, img3, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
                bmp = Bitmap.createBitmap(img3.cols(), img3.rows(),
                        Bitmap.Config.ARGB_8888);
                Imgproc.cvtColor(img3, img3, Imgproc.COLOR_BGR2RGB);
                Utils.matToBitmap(img3, bmp);
                List<DMatch> finalMatchesList = matches_final_mat.toList();
                final int matchesFound = finalMatchesList.size();
                endTime = System.currentTimeMillis();
                if (finalMatchesList.size() > min_matches)// dev discretion for
                // number of matches to
                // be found for an image
                // to be judged as
                // duplicate
                {
                    matchText = "Eye disease diagnosis ";
                           // + (endTime - startTime) + "ms";


                    isDuplicate = true;
                } else {
                    matchText = "Eye diagnosis ";
                            //+ (endTime - startTime) + "ms";
                    isDuplicate = false;
                }
                pd.dismiss();
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        asyncTaskContext);
                alertDialog.setTitle("Result");
                alertDialog.setCancelable(false);
                LayoutInflater factory = LayoutInflater.from(asyncTaskContext);
                final View view = factory.inflate(R.layout.result_view, null);
                ImageView matchedImages = (ImageView) view
                        .findViewById(R.id.finalImage);
                matchedImages.setImageBitmap(bmp);
                matchedImages.invalidate();
                /*final CheckBox shouldBeDuplicate = (CheckBox) view
                        .findViewById(R.id.checkBox);*/
                TextView message = (TextView) view.findViewById(R.id.message);
                TextView diseaseMssg = view.findViewById(R.id.disease_mssg);
                message.setText(matchText);
                diseaseMssg.setText(typeOfDisease);
                alertDialog.setView(view);
                /*shouldBeDuplicate
                        .setText("These images are actually duplicates.");*/
                alertDialog.setPositiveButton("Add to logs",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                File logs = new File(Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath()
                                        + "/EyeDiagnosis/Data Logs.txt");
                                FileWriter fw;
                                BufferedWriter bw;
                                try {
                                    fw = new FileWriter(logs, true);
                                    bw = new BufferedWriter(fw);
                                    bw.write("Algorithm used: "
                                            + descriptorType
                                            + "\nHamming distance: "
                                            + min_dist + "\nMinimum good matches: " + min_matches
                                            + "\nMatches found: " + matchesFound + "\nTime elapsed: " + (endTime - startTime) + "seconds\n" + path1
                                            + " was compared to " + path2
                                            + "\n"
                                            + "type of disease " + typeOfDisease
                                            + "\n" + "Is actual duplicate: "
                                            //+ shouldBeDuplicate.isChecked()
                                            + "\nRecognized as duplicate: "
                                            + isDuplicate + "\n");
                                    bw.close();
                                    Toast.makeText(
                                            asyncTaskContext,
                                            "Logs updated.\nLog location: "
                                                    + Environment
                                                    .getExternalStorageDirectory()
                                                    .getAbsolutePath()
                                                    + "/EyeDiagnosis/Data Logs.txt",
                                            Toast.LENGTH_LONG).show();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    // e.printStackTrace();
                                    try {
                                        File dir = new File(Environment
                                                .getExternalStorageDirectory()
                                                .getAbsolutePath()
                                                + "/EyeDiagnosis/");
                                        dir.mkdirs();
                                        logs.createNewFile();
                                        logs = new File(
                                                Environment
                                                        .getExternalStorageDirectory()
                                                        .getAbsolutePath()
                                                        + "/EyeDiagnosis/Data Logs.txt");
                                        fw = new FileWriter(logs, true);
                                        bw = new BufferedWriter(fw);
                                        bw.write("Algorithm used: "
                                                + descriptorType
                                                + "\nMinimum distance between keypoints: "
                                                + min_dist + "\n" + path1
                                                + " was compared to " + path2
                                                + "\n"
                                                + "type of disease " + typeOfDisease
                                                + "\n"
                                                + "Is actual duplicate: "
                                               // + shouldBeDuplicate.isChecked()
                                                + "\nRecognized as duplicate: "
                                                + isDuplicate + "\n");
                                        bw.close();
                                        Toast.makeText(
                                                asyncTaskContext,
                                                "Logs updated.\nLog location: "
                                                        + Environment
                                                        .getExternalStorageDirectory()
                                                        .getAbsolutePath()
                                                        + "/EyeDiagnosis/Data Logs.txt",
                                                Toast.LENGTH_LONG).show();
                                    } catch (IOException e1) {
                                        // TODO Auto-generated catch block
                                        e1.printStackTrace();
                                    }

                                }
                            }
                        });
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                alertDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(asyncTaskContext, e.toString(),
                        Toast.LENGTH_SHORT).show();

            }
        }




    void compare() {
        try {
            bmpimg1 = bmpimg1.copy(Bitmap.Config.ARGB_8888, true);
            bmpimg2 = bmpimg2.copy(Bitmap.Config.ARGB_8888, true);
            img1 = new Mat();
            img2 = new Mat();
            Utils.bitmapToMat(bmpimg1, img1);
            Utils.bitmapToMat(bmpimg2, img2);
            Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2RGB);
            Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGR2RGB);
            detector = FeatureDetector.create(FeatureDetector.PYRAMID_FAST);
            DescExtractor = DescriptorExtractor.create(descriptor);
            matcher = DescriptorMatcher
                    .create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            keypoints = new MatOfKeyPoint();
            dupKeypoints = new MatOfKeyPoint();
            descriptors = new Mat();
            dupDescriptors = new Mat();
            matches = new MatOfDMatch();
            detector.detect(img1, keypoints);
            Log.d("LOG!", "number of query Keypoints= " + keypoints.size());
            detector.detect(img2, dupKeypoints);
            Log.d("LOG!", "number of dup Keypoints= " + dupKeypoints.size());
            // Descript keypoints
            DescExtractor.compute(img1, keypoints, descriptors);
            DescExtractor.compute(img2, dupKeypoints, dupDescriptors);
            Log.d("LOG!", "number of descriptors= " + descriptors.size());
            Log.d("LOG!",
                    "number of dupDescriptors= " + dupDescriptors.size());
            // matching descriptors
            matcher.match(descriptors, dupDescriptors, matches);
            Log.d("LOG!", "Matches Size " + matches.size());
            // New method of finding best matches
            List<DMatch> matchesList = matches.toList();
            List<DMatch> matches_final = new ArrayList<DMatch>();
            for (int i = 0; i < matchesList.size(); i++) {
                if (matchesList.get(i).distance <= min_dist) {
                    matches_final.add(matches.toList().get(i));
                }
            }

            matches_final_mat = new MatOfDMatch();
            matches_final_mat.fromList(matches_final);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
}
