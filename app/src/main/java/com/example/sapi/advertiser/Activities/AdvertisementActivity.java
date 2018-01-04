package com.example.sapi.advertiser.Activities;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.sapi.advertiser.Models.Advertisment;
import com.example.sapi.advertiser.Models.User;
import com.example.sapi.advertiser.R;
import com.example.sapi.advertiser.Utils.Const;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.sapi.advertiser.Utils.Const.*;

/**
 * Activity a hirdetések kezeléséhez.
 *
 * Az aktivity az új hirdetések létrehozásáért és a meglévő hirdetések szerkesztéséért felelős.
 */
public class AdvertisementActivity extends AppCompatActivity {
    /**
     * Tag a loggoláshoz
     */
    private static final String TAG = "AdvertisementActivity";

    @BindView(R.id.edit_title)  EditText etTitle;
    @BindView(R.id.edit_description)  EditText etDescription;
    @BindView(R.id.button_location)  Button btnLocation;
    @BindView(R.id.progressBar)  ProgressBar pgbSaving;
    @BindView(R.id.linlayout_gallery) LinearLayout llGallery;
    @BindView(R.id.imageSelect) ImageButton imbGallery;
    @BindView(R.id.horizontal_scroll) HorizontalScrollView horizontal_scroll;

    private List<Uri> mImageUriList = new ArrayList<>();

    private boolean mImagesChanged = false;

    private StorageReference mStorage;
    private DatabaseReference mDatabaseAds;
    private DatabaseReference mDatabaseUsers;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;

    private String mAdId;
    private Advertisment mAd = new Advertisment();

    private boolean mIsNewAd;


    private DatabaseReference mAdRef;
    private DatabaseReference mAdUpload;
    private DatabaseReference mAdImagesUpload;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_advertisement);

        ButterKnife.bind(this);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabaseAds = FirebaseDatabase.getInstance().getReference().child(ADS_CHILD);
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child(USERS_CHILD).child(mCurrentUser.getUid());

        readDisplayStateValues(savedInstanceState);
    }


    @OnClick(R.id.imageSelect)
     void startGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(galleryIntent, RC_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GALLERY && resultCode == RESULT_OK) {
            mImageUriList = new ArrayList<>();
            if (data.getData() != null) {
                Uri mImageUri = data.getData();
                mImageUriList.add(mImageUri);
            } else {
                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    int limit = mClipData.getItemCount() < 10 ? mClipData.getItemCount() : 10;
                    for (int i = 0; i < limit; i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        mImageUriList.add(uri);
                    }
                }
            }
            mImagesChanged = true;
            displayImages();
        }
        if (requestCode == RC_PLACE_PICKER && resultCode == Activity.RESULT_OK) {

            Place place = PlacePicker.getPlace(this, data);
            mAd.location = place.getAddress().toString();
            mAd.locationLat= place.getLatLng().latitude;
            mAd.locationLng = place.getLatLng().longitude;

            displayLocation();
        }
    }

    private void displayAd() {
        etDescription.setText(mAd.description);
        etTitle.setText(mAd.title);

        displayImages();
        displayLocation();
    }

    private void displayImages() {
        llGallery.setVisibility(View.VISIBLE);
        imbGallery.setVisibility(View.GONE);

        llGallery.removeAllViews();
        for (Uri uri : mImageUriList) {
            ImageView imageView = new ImageView(this);
            imageView.setPadding(20, 20, 20, 20);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(getApplicationContext()).load(uri).into(imageView);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startGallery();
                }
            });
            llGallery.addView(imageView);
        }
        horizontal_scroll.setVisibility(View.VISIBLE);
        imbGallery.setVisibility(View.GONE);
    }

    private void displayLocation() {
        btnLocation.setText(mAd.location);
    }

    private void startSavingAdvertisment() {

        final String title = etTitle.getText().toString().trim();
        final String description = etDescription.getText().toString().trim();

        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(description) && !mImageUriList.isEmpty() && !TextUtils.isEmpty(mAd.location)) {
            pgbSaving.setVisibility(View.VISIBLE);
            if(mIsNewAd) {
                 mAdUpload = mDatabaseAds.push();
            }
            else{
                mAdUpload = mAdRef;
            }
            mAd.title=title;
            mAd.description=description;


            if(mImagesChanged) {
                mAd.adImages.clear();
                uploadImages();
            }
            else {
                uploadAdvertisment();
            }

        }
    }

    private void uploadAdvertisment() {
        Map<String, Object> adValues = mAd.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/"+ mAdUpload.getKey(), adValues);

        mDatabaseAds.updateChildren(childUpdates);

        startActivity(new Intent(AdvertisementActivity.this, ListActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private void uploadImages() {
        final Collection<Task<?>> tasks = new ArrayList<>();

        for (Uri mImageUri : mImageUriList) {
            StorageReference filepath = mStorage.child(STORAGE_AD_IMAGES).child(UUID.randomUUID().toString());
            tasks.add(filepath.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            mAd.adImages.add(downloadUrl.toString());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            FirebaseCrash.logcat(Log.ERROR, TAG, "Failed to upload image to storage.");
                            FirebaseCrash.report(e);
                            Toast.makeText(getApplicationContext(), "Failed to upload image to storage.", Toast.LENGTH_LONG).show();
                        }
                    })
            );
        }

        Tasks.whenAll(tasks).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mAd.image = mAd.adImages.get(0);
                uploadAdvertisment();
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Failed to upload image to storage task.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @OnClick(R.id.button_location)
     void startPlacePicker() {
        try {
            PlacePicker.IntentBuilder intentBuilder =
                    new PlacePicker.IntentBuilder();

            Intent intent = intentBuilder.build(AdvertisementActivity.this);

            startActivityForResult(intent, RC_PLACE_PICKER);

        } catch (GooglePlayServicesRepairableException
                | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_edit_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            startSavingAdvertisment();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("PlaceName", mAd.location);
        outState.putDouble("PlaceLat", mAd.locationLat);
        outState.putDouble("PlaceLng", mAd.locationLng);

        ArrayList<String> uriStrings = new ArrayList<>();
        for (Uri uri : mImageUriList) {
            uriStrings.add(uri.toString());
        }

        outState.putStringArrayList("Images", uriStrings);
    }


    private void readDisplayStateValues(Bundle savedInstanceState) {
        Intent intent = getIntent();

        mAdId = intent.getStringExtra(Const.EXTRA_AD_ID);

        mIsNewAd = mAdId == null;

        mDatabaseUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                mAd.userImage = user.image;
                mAd.uid = dataSnapshot.getKey();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        if (savedInstanceState != null) {
            //TODO: make advertisment implement parcelable
            mAd.location = savedInstanceState.getString("PlaceName");
            mAd.locationLat = savedInstanceState.getDouble("PlaceLat");
            mAd.locationLng = savedInstanceState.getDouble("PlaceLng");

            ArrayList<String> uriStrings = savedInstanceState.getStringArrayList("Images");
            for (String uriString : uriStrings) {
                mImageUriList.add(Uri.parse(uriString));
            }
            displayImages();
            displayLocation();
        } else {
            if (!mIsNewAd) {
                mAdRef = mDatabaseAds.child(mAdId);
                mAdRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            mAd = dataSnapshot.getValue(Advertisment.class);
                            for(String image : mAd.adImages){
                                mImageUriList.add(Uri.parse(image));
                            }
                            displayAd();
                        } else {
                            Toast.makeText(getApplicationContext(), "The advertisment was deleted.", Toast.LENGTH_LONG).show();
                            onBackPressed();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(getApplicationContext(), "Some error occured.", Toast.LENGTH_LONG).show();
                        onBackPressed();
                    }
                });
            }
        }
    }
}
