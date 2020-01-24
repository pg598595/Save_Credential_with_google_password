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
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.CredentialRequestResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import kotlinx.android.synthetic.main.activity_sign_in.*


class SignInActivity : AppCompatActivity(), ResultCallback<CredentialRequestResult>,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {


    var googleApiClient: GoogleApiClient? = null

    private val TAG = "SignInActivity"
    private val RC_REQUEST = 11

    private var credentialRequest: CredentialRequest? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        credentialRequest = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .build()
        googleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .enableAutoManage(this, this)
            .addApi(Auth.CREDENTIALS_API)
            .build()


        btnSignIn.setOnClickListener {
            getDetails()
        }


    }


    override fun onStart() {
        super.onStart()
        Auth.CredentialsApi.request(googleApiClient, credentialRequest).setResultCallback(this)
    }

    override fun onResult(credentialRequestResult: CredentialRequestResult) {
        val status = credentialRequestResult.status
        if (status.isSuccess) {
            onCredentialSuccess(credentialRequestResult.credential)
        } else {
            if (status.statusCode === CommonStatusCodes.RESOLUTION_REQUIRED) {
                try {
                    status.startResolutionForResult(this, RC_REQUEST)
                } catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    private fun onCredentialSuccess(credential: Credential) {
        if (credential.getAccountType() == null) {
            val id = credential.getId()
            val username = credential.getName()
            val password = credential.getPassword()

            Log.d(TAG, "ID: $id, Username: $username, Password: $password")
            signInemail.setText(username)
            signInpassword.setText(password)

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val credential = data!!.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                onCredentialSuccess(credential!!)
            } else {
                Log.d(TAG, "Request failed")
            }
        }
    }

    fun getDetails() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onConnected(p0: Bundle?) {

        Log.d("TAG", "onConnected")

    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d("TAG", "onConnectionSuspended: " + p0);

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d("TAG", "onConnectionFailed: " + p0)


    }
}