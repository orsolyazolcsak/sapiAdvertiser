package com.example.sapi.advertiser.Activities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.example.sapi.advertiser.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.example.sapi.advertiser.Utils.Const.USERS_CHILD;
import static com.example.sapi.advertiser.Utils.Const.USER_IMG_FIELD;

/**
 * RegisterActivity
 * Ez az Activity kezeli a regisztrációt.
 * Email és jelszó segítségével regisztrál egy új felhasználót.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText mEmailField;
    private EditText mPasswordField;

    private Button mRegisterButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(USERS_CHILD);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mEmailField = (EditText) findViewById(R.id.emailField);
        mPasswordField = (EditText) findViewById(R.id.passwordField);
        mRegisterButton = (Button) findViewById(R.id.registerButton);

        mRegisterButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startRegister();
            }
        });

    }

    private void startRegister() {
        /**
         * Az email változóba beletesszük a beolvasott email címet
         * és a passwordba a jelszót
         */
        String email = mEmailField.getText().toString().trim();
        String password = mPasswordField.getText().toString().trim();
        /**
         * Ha egyik mező sem üres, regisztráljuk a felhasználót
         */
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            mProgressBar.setVisibility(View.VISIBLE);
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {

                        String userId = mAuth.getCurrentUser().getUid();

                        DatabaseReference user = mDatabase.child(userId);

                        user.child(USER_IMG_FIELD).setValue("default");

                        mProgressBar.setVisibility(View.INVISIBLE);
                        /**
                         * Beállítjuk, hogy ha BACK-et nyomunk ne hozzon vissza a regisztrációra
                         * és átirányítjuk a ListActivity-re
                         */
                        Intent listIntent = new Intent(RegisterActivity.this, ListActivity.class);
                        listIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(listIntent);
                    }
                }
            });
        }
    }
}
