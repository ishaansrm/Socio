package com.ishaangarg.socio;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

    final static String TAG = "LOGIN ACTIVITY";

    private CallbackManager callbackManager;

    int googleLogin = 0;
    SignInButton gBtn;
    LoginButton fbLoginButton;
    TextView logOut, name, emailID;
    ImageView profilePic;

    ProgressDialog progress;

    String personName, personPhoto, email, fbID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(toolbar);

        gBtn = (SignInButton) findViewById(R.id.sign_in_button);
        fbLoginButton = (LoginButton) findViewById(R.id.fb_login_button);

        name = (TextView) findViewById(R.id.name);
        emailID = (TextView) findViewById(R.id.email);
        profilePic = (ImageView) findViewById(R.id.profile_pic);

        if (!isOnline()) {
            notOnline();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();


        gBtn.setOnClickListener(this);

        //FB STUFF
        callbackManager = CallbackManager.Factory.create();
        fbLoginButton.setHeight(50);

        fbLoginButton.setReadPermissions(Arrays.asList("public_profile, email"));

        fbLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                progress = ProgressDialog.show(MainActivity.this, "Loading",
                        "Shouldn't take long", true);
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(
                                    JSONObject object,
                                    GraphResponse response) {
                                // Application code
                                Log.v("LoginActivity", response.toString());
                                JSONObject jo = response.getJSONObject();
                                try {
                                    personName = jo.get("name").toString();
                                    email = jo.get("email").toString();
                                    fbID = jo.get("id").toString();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                progress.dismiss();
                                loggedIn();
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {
                Log.d(TAG, "FB LOGIN ERROR: " + e);
            }

        });
    }

    public void notOnline() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("No Connection")
                .setMessage("Please check your internet connection")
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isOnline()) {
                            dialog.cancel();
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (isOnline()) {
                            dialog.cancel();
                        } else
                            notOnline();
                    }
                })
                .show();


    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account,
        // grant permissions or resolve an error in order to sign in. Refer to the javadoc for
        // ConnectionResult to see possible error codes.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                progress.dismiss();
                Log.d(TAG, "Could not resolve the connection result");
                //showErrorDialog(connectionResult);
            }
        } else {
            // Show the signed-out UI
            try {
                progress.dismiss();
            }
            catch (Exception e){
                Log.d(TAG, "no dialog to dismiss, must be first run");
            }
            Log.d(TAG, "blah");
            //showSignedOutUI();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        if (googleLogin == 0) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        } else {
            if (requestCode == RC_SIGN_IN) {
                // If the error resolution was not successful we should not resolve further.
                if (resultCode != RESULT_OK) {
                    mShouldResolve = false;
                }
                mIsResolving = false;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.
        Log.d(TAG, "onConnected:" + bundle);
        googleLogin = 1;
        mShouldResolve = false;
        if(progress!=null)
            progress.dismiss();

        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            personName = currentPerson.getDisplayName();
            Log.d(TAG, "NAME: " + personName);
            personPhoto = currentPerson.getImage().getUrl();
            Log.d(TAG, "NAME: " + personPhoto);
            personPhoto = personPhoto.substring(0,
                    personPhoto.length() - 2)
                    + 230;

            try {
                email = Plus.AccountApi.getAccountName(mGoogleApiClient);
                Log.d(TAG, "EMAIL: " + email);
            } catch (Exception e) {
                Log.d(TAG, "Couldn't get email ID");
            }
        } else Log.d(TAG, "WHOOOOPSSIIEEEE");
        loggedIn();
    }

    public void loggedIn() {
        gBtn.setVisibility(View.GONE);
        fbLoginButton.setVisibility(View.GONE);
        logOut = (TextView) findViewById(R.id.sign_out_button);
        logOut.setVisibility(View.VISIBLE);

        name.setText(personName);
        Log.d(TAG, "name hai: " + personName);
        emailID.setText(email);
        emailID.setVisibility(View.VISIBLE);
        name.setVisibility(View.VISIBLE);
        if (personPhoto != null)
            Picasso.with(this).load(personPhoto)
                    .resize(200, 200).placeholder(R.drawable.ic_action_account_circle).into(profilePic);
        else if (fbID != null)
            Picasso.with(this).load("https://graph.facebook.com/" + fbID + "/picture?type=large")
                    .resize(200, 200).placeholder(R.drawable.ic_action_account_circle).into(profilePic);
        profilePic.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_in_button) {
            googleLogin = 1;
            onSignInClicked();
            progress = ProgressDialog.show(this, "Loading",
                    "Shouldn't take long", true);
        }
        if (v.getId() == R.id.sign_out_button) {

            if (mGoogleApiClient.isConnected()) {
                googleLogin = 0;

                /*Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                mGoogleApiClient.disconnect();*/

                Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                mGoogleApiClient.disconnect();

            } else {
                LoginManager.getInstance().logOut();
            }

            gBtn.setVisibility(View.VISIBLE);
            fbLoginButton.setVisibility(View.VISIBLE);
            logOut.setVisibility(View.GONE);
            name.setVisibility(View.GONE);
            profilePic.setVisibility(View.GONE);
            emailID.setVisibility(View.GONE);
        }
    }

    private void onSignInClicked() {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        mGoogleApiClient.connect();

        // Show a message to the user that we are signing in.
        Log.d(TAG, "Signing in...");
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (AccessToken.getCurrentAccessToken() != null) {

            progress = ProgressDialog.show(this, "Loading",
                    "Shouldn't take long", true);

            GraphRequest request = GraphRequest.newMeRequest(
                    AccessToken.getCurrentAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(
                                JSONObject object,
                                GraphResponse response) {
                            // Application code
                            Log.v("LoginActivity", response.toString());
                            JSONObject jo = response.getJSONObject();
                            try {
                                personName = jo.get("name").toString();
                                email = jo.get("email").toString();
                                fbID = jo.get("id").toString();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            progress.dismiss();
                            loggedIn();
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "id,name,email");
            request.setParameters(parameters);
            request.executeAsync();
        }
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
}
