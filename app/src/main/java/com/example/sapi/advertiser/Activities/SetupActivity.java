package com.example.sapi.advertiser.Activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.sapi.advertiser.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.lamudi.phonefield.PhoneEditText;
import com.mobsandgeeks.saripaar.AnnotationRule;
import com.mobsandgeeks.saripaar.annotation.Length;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;
import com.mobsandgeeks.saripaar.annotation.Pattern;
import com.mobsandgeeks.saripaar.annotation.ValidateUsing;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;
import static com.example.sapi.advertiser.Utils.Const.STORAGE_USR_IMAGES;
import static com.example.sapi.advertiser.Utils.Const.USERS_CHILD;
import static com.example.sapi.advertiser.Utils.Const.USER_IMG_FIELD;
import static com.example.sapi.advertiser.Utils.Const.USER_FIRSTNAME_FIELD;
import static com.example.sapi.advertiser.Utils.Const.USER_LASTNAME_FIELD;
import static com.example.sapi.advertiser.Utils.Const.USER_PHONENUMBER_FIELD;

/**
 * Setup Activity
 * Itt bekérjük a felhasználó adatait:
 * Vezetéknév, keresztnév, telefonszám, kép
 * Ezeket mind kötelező megadja
 */
public class SetupActivity extends AppCompatActivity {

    @BindView(R.id.setupImageViewBig)
    ImageView mUserImageView;
    @BindView(R.id.setupFirstName)
    EditText mFirstName;
    @BindView(R.id.setupLastName)
    EditText mLastName;
    @BindView(R.id.setupPhoneNumber)
    PhoneEditText mPhoneNumber;
    @BindView(R.id.setupImageLayout)
    RelativeLayout mBlurredImage;
    @BindView(R.id.setupFinishButton)
    TextView mSubmitButton;
    @BindView(R.id.progressBar)
    ProgressBar mProgress;

    private Uri mImageUri = null;

    private static final int GALLERY_REQUEST = 1;

    private DatabaseReference mDatabaseUsers;
    private FirebaseAuth mAuth;
    private StorageReference mStorageImage;

    /**
     * beolvassuk az adatokat
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        ButterKnife.bind(this);

        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child(USERS_CHILD);
        mAuth = FirebaseAuth.getInstance();
        mStorageImage = FirebaseStorage.getInstance().getReference().child(STORAGE_USR_IMAGES);
        if (mAuth.getCurrentUser() == null) {
            Intent loginIntent = new Intent(SetupActivity.this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(loginIntent);
        }
        mPhoneNumber.setDefaultCountry("RO");
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSetupAccount();
            }
        });
    }

    /**
     * elmentjük az adatokat adatbázisba
     */
    private void startSetupAccount() {
        if (ValidateFields()) {
            final String firstname = mFirstName.getText().toString().trim();
            final String lastname = mLastName.getText().toString().trim();
            final String phonenumber = mPhoneNumber.getPhoneNumber();

            final String userId = mAuth.getCurrentUser().getUid();
            mProgress.setVisibility(View.VISIBLE);

            StorageReference filepath = mStorageImage.child(mImageUri.getLastPathSegment());
            filepath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    String downloadUri = taskSnapshot.getDownloadUrl().toString();

                    mDatabaseUsers.child(userId).child(USER_FIRSTNAME_FIELD).setValue(firstname);
                    mDatabaseUsers.child(userId).child(USER_LASTNAME_FIELD).setValue(lastname);
                    mDatabaseUsers.child(userId).child(USER_PHONENUMBER_FIELD).setValue(phonenumber);
                    mDatabaseUsers.child(userId).child(USER_IMG_FIELD).setValue(downloadUri);

                    mProgress.setVisibility(View.GONE);

                    Intent listIntent = new Intent(SetupActivity.this, ListActivity.class);
                    listIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(listIntent);
                }
            });
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {

            Uri imageUri = data.getData();

            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setAspectRatio(1, 1)
                    .start(this);

        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mImageUri = result.getUri();

                Glide.with(this).load(mImageUri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(mUserImageView);


                Glide.with(this).load(mImageUri)
                        .apply(bitmapTransform(new BlurTransformation(10)))
                        .into(new SimpleTarget<Drawable>() {
                            @Override
                            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                                mBlurredImage.setBackground(resource);
                            }
                        });


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

    public void ImageClickHandler(View view) {

        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST);

    }

    private boolean ValidateFields() {
        if (TextUtils.isEmpty(mFirstName.getText().toString().trim())) {
            mFirstName.setError("First name is required!");
            mFirstName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(mLastName.getText().toString().trim())) {
            mLastName.setError("Last name is required!");
            mLastName.requestFocus();
            return false;
        }
        if (!mPhoneNumber.isValid()) {
            mPhoneNumber.setError("Phone number is required!");
            mPhoneNumber.requestFocus();
            return false;
        }
        if (mImageUri == null) {
            Toast.makeText(getApplicationContext(), "Profile image is required!", Toast.LENGTH_SHORT).show();
            mUserImageView.requestFocus();
            return false;
        }
        return true;
    }


}
