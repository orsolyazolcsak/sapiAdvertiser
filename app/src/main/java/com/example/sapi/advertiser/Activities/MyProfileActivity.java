package com.example.sapi.advertiser.Activities;

import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.sapi.advertiser.Models.User;
import com.example.sapi.advertiser.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import static com.example.sapi.advertiser.Utils.Const.AD_UID_FIELD;
import static com.example.sapi.advertiser.Utils.Const.USERS_CHILD;

/**
 * My Profile Activity
 * Ebben az Activity-ben a felhasználó elérheti a saját adatait
 * név, email cím, telefonszám
 */
public class MyProfileActivity extends AppCompatActivity {
    private DatabaseReference mDatabaseUsers;
    private FirebaseAuth mAuth;

    private ImageView mMyImage;
    private TextView mMyFirstName;
    private TextView mMyLastName;
    private TextView mMyEmail;
    private TextView mMyPhoneNumber;
    private Button mChangeEmailBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);
        setTitle("My Profile");
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child(USERS_CHILD);
        mAuth = FirebaseAuth.getInstance();

        mMyImage = (ImageView) findViewById(R.id.my_image);
        mMyFirstName = (TextView) findViewById(R.id.my_first_name);
        mMyLastName = (TextView) findViewById(R.id.my_last_name);
        mMyEmail = (TextView) findViewById(R.id.my_email);
        mMyPhoneNumber = (TextView) findViewById(R.id.my_phone_number);
        mChangeEmailBtn = (Button) findViewById(R.id.change_email_btn);
        mChangeEmailBtn.setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        /**
         * Ha a felhasznáó be van jelentkezve
         */
        if (user != null) {

            String email = user.getEmail();
            mMyEmail.setText(email);

            /**
             * Ha a felhasználó email + jelszóval regisztrált, láthatóva teszem
             * az emailcím kicserélése gombot
             */

            List<? extends UserInfo> providerData = user.getProviderData();
            String providerId = providerData.get(1).getProviderId();

            if (providerId.equals("password")) {
                mChangeEmailBtn.setVisibility(View.VISIBLE);
            }

            String uid = user.getUid();
            /**
             * userRef - a user objektum az adatbázisból
             * Mikor megérkezik az adat az adatbázisból,
             * megjelenítem őket.
             */
            DatabaseReference userRef = mDatabaseUsers.child(uid);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User user = dataSnapshot.getValue(User.class);

                        mMyFirstName.setText(user.firstName);
                        mMyLastName.setText(user.lastName);
                        mMyPhoneNumber.setText(user.phonenumber);

                        Glide.with(getApplicationContext()).load(user.image).into(mMyImage);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }
}
