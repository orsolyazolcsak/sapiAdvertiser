package com.example.sapi.advertiser.Activities;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.example.sapi.advertiser.Models.Advertisment;
import com.example.sapi.advertiser.Models.User;
import com.example.sapi.advertiser.R;
import com.glide.slider.library.Animations.DescriptionAnimation;
import com.glide.slider.library.SliderLayout;
import com.glide.slider.library.SliderTypes.DefaultSliderView;
import com.glide.slider.library.SliderTypes.TextSliderView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.sapi.advertiser.Utils.Const.ADS_CHILD;
import static com.example.sapi.advertiser.Utils.Const.EXTRA_AD_ID;
import static com.example.sapi.advertiser.Utils.Const.EXTRA_AD_UID;
import static com.example.sapi.advertiser.Utils.Const.USERS_CHILD;

public class AdvertismentDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String mAd_key = null;
    private String mUser_key = null;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private GoogleMap mGoogleMap;

    @BindView(R.id.ad_detail_title)  TextView mAdDetailTitle;
    @BindView(R.id.ad_detail_description)  TextView mAdDetailDescription;
    @BindView(R.id.imageSlider) SliderLayout mSlider;
    @BindView(R.id.ad_detail_user_image) ImageView mUserImage;
    @BindView(R.id.ad_detail_user_name) TextView mUserName;
    @BindView(R.id.call) Button mCall;
    private Advertisment mAd;
    private DatabaseReference mAdRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertisment_detail);

        ButterKnife.bind(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        mAd_key = getIntent().getExtras().getString(EXTRA_AD_ID);
        mUser_key = getIntent().getExtras().getString(EXTRA_AD_UID);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mAdRef = mDatabase.child(ADS_CHILD).child(mAd_key);
        mAdRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mAd = dataSnapshot.getValue(Advertisment.class);

                    mAdDetailTitle.setText(mAd.title);
                    mAdDetailDescription.setText(mAd.description);

                    mGoogleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(mAd.locationLat, mAd.locationLng))
                            .title(mAd.location)) ;
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mAd.locationLat, mAd.locationLng),150));

                    initSlider(mAd.adImages);

                }
                else{
                    onBackPressed();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        DatabaseReference userRef = mDatabase.child(USERS_CHILD).child(mUser_key);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);

                    mUserName.setText(user.firstName);
                    mCall.setTag(user.phonenumber);

                    Glide.with(getApplicationContext()).load(user.image).into(mUserImage);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });




        mCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phone = (String)mCall.getTag();
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                startActivity(intent);

            }
        });

    }

    private void initSlider(List<String> images) {
        mSlider.removeAllSliders();
        for (String image : images) {
            TextSliderView textSliderView = new TextSliderView(this);

            textSliderView
                    .description("")
                    .image(image)
                    .setBitmapTransformation(new CenterCrop())
                ;

            mSlider.addSlider(textSliderView);
        }
        mSlider.setPresetTransformer(SliderLayout.Transformer.Default);
        mSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        mSlider.setCustomAnimation(new DescriptionAnimation());
        mSlider.setDuration(4000);
        mSlider.stopAutoCycle();

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getUid().equals(mUser_key)) {
            getMenuInflater().inflate(R.menu.advertisment_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.action_delete){
            mAdRef.removeValue();
            Intent listIntent = new Intent(AdvertismentDetailActivity.this, ListActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(listIntent);
        }
        if(item.getItemId()==R.id.action_edit){
            Intent editIntent = new Intent(AdvertismentDetailActivity.this, AdvertisementActivity.class);
            editIntent.putExtra(EXTRA_AD_ID, mAd_key);
            startActivity(editIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
