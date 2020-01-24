package com.example.credentialdemo

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(),
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {


    override fun onConnected(p0: Bundle?) {

        Log.d("TAG", "onConnected")

    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d("TAG", "onConnectionSuspended: " + p0);

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d("TAG", "onConnectionFailed: " + p0)


    }

    var RC_SAVE = 123
    private var mGoogleApiClient: GoogleApiClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .enableAutoManage(this, 0, this)
            .addApi(Auth.CREDENTIALS_API)
            .build()
        btnSignUp.setOnClickListener {
            saveSignUp()
        }

        goToSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

    }


    // Important: only store passwords in this field.
    // Android autofill uses this value to complete
    // sign-in forms, so repurposing this field will
    // likely cause errors.

    fun saveSignUp() {
        val credential = Credential.Builder(email.text.toString())
            .setName(username.text.toString())
            .setPassword(password.text.toString())
            .build()
        requestSaveCredentials(credential)
    }

    private fun requestSaveCredentials(credential: Credential) {
        Auth.CredentialsApi.save(mGoogleApiClient, credential).setResultCallback(this@MainActivity)
    }

    override fun onResult(result: Status) {


        val status = result.getStatus()
        if (status.isSuccess()) {
            Toast.makeText(this, "Credentials saved successfully", Toast.LENGTH_LONG).show()
        } else {
            if (status.hasResolution()) {
                try {
                    status.startResolutionForResult(this, RC_SAVE)
                } catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Credentials saved successfully", Toast.LENGTH_LONG).show()
            } else {
                Log.d("TAG", "Save cancelled by user")
            }


        }
    }

}
