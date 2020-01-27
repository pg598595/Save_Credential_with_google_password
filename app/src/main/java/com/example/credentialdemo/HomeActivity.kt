package com.example.credentialdemo

import android.app.Activity
import android.app.AlertDialog
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
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity(), ResultCallback<CredentialRequestResult>,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    private var credentialRequest: CredentialRequest? = null

    var googleApiClient: GoogleApiClient? = null
    var isRememeber = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


         isRememeber= intent.getBooleanExtra("isRemember",false)


        googleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .enableAutoManage(this, this)
            .addApi(Auth.CREDENTIALS_API)
            .build()
        btnLogout.setOnClickListener {
            logOut()
        }

        btndeleteAccount.setOnClickListener {
            if(isRememeber){

                val dialog = AlertDialog.Builder(this)
                    .setMessage("This will sign you out of the account and delete it.")
                    .setPositiveButton("Continue") { dialog, which ->
                        dialog.dismiss()

                        requestCredentials()

                    }
                    .setNegativeButton("Cancel") { dialog, which -> dialog.dismiss() }
                    .create()

                dialog.show()

            }
            else{
                logOut()
            }

        }


    }

    private fun requestCredentials() {
        credentialRequest = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .build()

        Auth.CredentialsApi.request(googleApiClient, credentialRequest).setResultCallback(this)
    }

    fun logOut() {

        Auth.CredentialsApi.disableAutoSignIn(googleApiClient)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onResult(credentialRequestResult: CredentialRequestResult) {
        val status = credentialRequestResult.status
        if (status.isSuccess) {
            onCredentialSuccess(credentialRequestResult.credential)
        } else {
            if (status.hasResolution()) {
                try {
                    status.startResolutionForResult(this, RC_REQUEST)
                } catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show()
            }
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

    private fun onCredentialSuccess(credential: Credential) {
        if (credential.accountType == null) {
            Auth.CredentialsApi.delete(googleApiClient, credential).setResultCallback { status ->
                if (status.isSuccess) {
                    logOut()
                } else {
                    Toast.makeText(this@HomeActivity, "Account deletion failed", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        }
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

    companion object {

        private val TAG = "HomeActivity"
        private val RC_REQUEST = 12
    }
}