package com.oruba.niezbdnikturystyczny;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.oruba.niezbdnikturystyczny.UserClient;
import com.oruba.niezbdnikturystyczny.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;


import static android.text.TextUtils.isEmpty;

public class LoginActivity extends AppCompatActivity implements
        View.OnClickListener
{

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 99;

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;

    // widgets
    private EditText mEmail, mPassword;
    private ProgressBar mProgressBar;
    private CallbackManager mCallbackManager;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore mDb;
    private GoogleSignInClient mGoogleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mProgressBar = findViewById(R.id.progressBar);

        setupFirebaseAuth();
        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
        findViewById(R.id.link_register).setOnClickListener(this);
        findViewById(R.id.fb_login_button).setOnClickListener(this);
        findViewById(R.id.google_login_button).setOnClickListener(this);


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        hideSoftKeyboard();
    }




    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

    }

    private void hideDialog(){
        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
        ----------------------------- Firebase setup ---------------------------------
     */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    Toast.makeText(LoginActivity.this, "Zautoryzowano: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    //finish();

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
    }

    private void signIn(){
        //check if the fields are filled out
        if(!isEmpty(mEmail.getText().toString())
                && !isEmpty(mPassword.getText().toString())){
            Log.d(TAG, "onClick: attempting to authenticate.");
                showDialog();

                FirebaseAuth.getInstance().signInWithEmailAndPassword(mEmail.getText().toString(),
                        mPassword.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                hideDialog();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, "Autoryzacja nieudana", Toast.LENGTH_SHORT).show();
                        hideDialog();
                    }
                });
        }else{
            Toast.makeText(LoginActivity.this, "Nie wypełniłeś wszystkich pól.", Toast.LENGTH_SHORT).show();
        }
    }

    public void signWithFB() {
        // Initialize Facebook Login button
            mCallbackManager = CallbackManager.Factory.create();
            LoginButton loginButton = findViewById(R.id.fb_login_button);
            loginButton.setReadPermissions("email", "public_profile");
            loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {

                @Override
                public void onSuccess(LoginResult loginResult) {
                    Log.d(TAG, "facebook:onSuccess:" + loginResult);
                    handleFacebookAccessToken(loginResult.getAccessToken());
                }

                @Override
                public void onCancel() {
                    Log.d(TAG, "facebook:onCancel");
                    FirebaseAuth.getInstance().signOut();
                    LoginManager.getInstance().logOut();
                }

                @Override
                public void onError(FacebookException error) {
                    Log.d(TAG, "facebook:onError", error);
                    FirebaseAuth.getInstance().signOut();
                    LoginManager.getInstance().logOut();
                }
            });
    }
// ...
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);


            if(isNetworkAvailable()) {
                if (requestCode == RC_SIGN_IN) {
                    // The Task returned from this call is always completed, no need to attach
                    // a listener.
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        // Google Sign In failed, update UI appropriately
                        Log.w(TAG, "Google sign in failed", e);
                        // ...
                    }

                } else {
                    mCallbackManager.onActivityResult(requestCode, resultCode, data);
                }
            }
            else {
                Toast.makeText(LoginActivity.this, "Brak połączenia z Internetem.", Toast.LENGTH_SHORT).show();
            }

        }

    private void handleFacebookAccessToken(AccessToken token){
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            String email = firebaseUser.getEmail();
                            User user = new User();
                            user.setEmail(email);
                            user.setUsername(email.substring(0, email.indexOf("@")));
                            user.setUser_id(firebaseUser.getUid());


                            DocumentReference newUserRef = mDb
                                    .collection(getString(R.string.collection_users))
                                    .document(firebaseUser.getUid());

                            newUserRef.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if(task.isSuccessful()){
                                        setupFirebaseAuth();
                                    }else{
                                        View parentLayout = findViewById(android.R.id.content);
                                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                            Snackbar.make(parentLayout, "Email został już użyty do logowania inną metodą!", Snackbar.LENGTH_SHORT).show();
                                        }
                                        else {
                                            Snackbar.make(parentLayout, "Autoryzacja nieudana.", Snackbar.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(LoginActivity.this, "Email został już użyty do logowania inną metodą!",
                                        Toast.LENGTH_SHORT).show();
                                FirebaseAuth.getInstance().signOut();
                                LoginManager.getInstance().logOut();
                            }
                            else {
                                Toast.makeText(LoginActivity.this, "Autoryzacja nieudana.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            setupFirebaseAuth();
                        }

                        // ...
                    }
                });

    }

    @Override
    public void onClick(View view) {
        if(isNetworkAvailable()) {
            switch (view.getId()) {
                case R.id.link_register: {
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                    break;
                }

                case R.id.email_sign_in_button: {
                    signIn();
                    break;
                }
                case R.id.fb_login_button: {
                    signWithFB();
                    break;
                }
                case R.id.google_login_button: {
                    signWithGoogle();
                    break;
                }
            }
        }
        else {
            Toast.makeText(LoginActivity.this, "Brak połączenia z Internetem.", Toast.LENGTH_SHORT).show();
        }
    }

    private void signWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        showDialog();

            Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

            AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithCredential:success");
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                String email = firebaseUser.getEmail();
                                User user = new User();
                                user.setEmail(email);
                                user.setUsername(email.substring(0, email.indexOf("@")));
                                user.setUser_id(firebaseUser.getUid());


                                DocumentReference newUserRef = mDb
                                        .collection(getString(R.string.collection_users))
                                        .document(firebaseUser.getUid());
                                showDialog();

                                newUserRef.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> secondTtsk) {


                                        if(secondTtsk.isSuccessful()){
                                            setupFirebaseAuth();
                                            hideDialog();
                                        }else{
                                            View parentLayout = findViewById(android.R.id.content);
                                            if (secondTtsk.getException() instanceof FirebaseAuthUserCollisionException) {
                                                Snackbar.make(parentLayout, "Email został już użyty do logowania inną metodą!", Snackbar.LENGTH_SHORT).show();
                                            }
                                            else {
                                                Snackbar.make(parentLayout, "Autoryzacja nieudana.", Snackbar.LENGTH_SHORT).show();
                                            }
                                            hideDialog();
                                        }
                                    }
                                });
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithCredential:failure", task.getException());
                                View parentLayout = findViewById(android.R.id.content);
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(LoginActivity.this, "Email został już użyty do logowania inną metodą!",
                                            Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    Snackbar.make(parentLayout, "Autoryzacja nieudana.", Snackbar.LENGTH_SHORT).show();
                                }
                                setupFirebaseAuth();
                                hideDialog();
                            }

                            // ...
                        }
                    });
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}