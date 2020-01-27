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
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.auth.api.credentials.IdentityProviders
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig
import com.google.android.gms.auth.api.credentials.HintRequest
import android.text.TextUtils
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.auth.api.credentials.CredentialRequestResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.credentials.CredentialRequest
import android.util.Patterns
import androidx.annotation.Nullable


class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, ResultCallback<CredentialRequestResult> {

    private var mGoogleApiClient: GoogleApiClient? = null
    var mCredentialsApiClient: CredentialsClient?=null
    var mCredentialRequest: CredentialRequest?=null
    var isResolving: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpGoogleApiClient()

        //needed for Android Oreo to show dialog (forceEnableSaveDialog)

        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()


        mCredentialsApiClient = Credentials.getClient(this, options)
        createCredentialRequest()


        btnSignUp.setOnClickListener{
            val email = email.text.toString()
            val password = password.text.toString()
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || !Patterns.EMAIL_ADDRESS.matcher(
                    email
                ).matches()
            )
                Toast.makeText(applicationContext, "Please enter valid email and password", Toast.LENGTH_SHORT).show()

            else {
                if(cbrememberMe.isChecked){
                    val credential = Credential.Builder(email)
                        .setPassword(password)
                        .build()

                    saveCredentials(credential)
                }
                else{
                   gotoHomePage()
                }

            }
        }




    }

    fun setUpGoogleApiClient() {


        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addApi(Auth.CREDENTIALS_API)
            .enableAutoManage(this, this)
            .build()
    }

    fun createCredentialRequest() {
        mCredentialRequest = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .setAccountTypes(IdentityProviders.GOOGLE)
            .build()
    }

    fun requestCredentials() {
        Auth.CredentialsApi.request(mGoogleApiClient, mCredentialRequest).setResultCallback(this)
    }


    private fun onCredentialRetrieved(credential: Credential) {
        val accountType = credential.accountType
        if (accountType == null) {

            // Sign in with information from the Credential.

            gotoHomePage()

        } else if (accountType == IdentityProviders.GOOGLE) {


            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

            val signInClient = GoogleSignIn.getClient(this, gso)
            val task = signInClient.silentSignIn()

            task.addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    setLoginFields(task.result!!.email, null)
                } else {
                    Toast.makeText(applicationContext, "Unable to do a google sign in", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }


    fun gotoHomePage() {
        val intent = Intent(this@MainActivity,HomeActivity::class.java);

        intent.putExtra("isRemember", cbrememberMe.isChecked)

        startActivity(intent)

        finish()
    }






    override fun onConnected(@Nullable bundle: Bundle?) {
        Log.d(TAG, "onConnected")
        requestCredentials()

    }

    override fun onConnectionSuspended(i: Int) {

    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    override fun onDestroy() {
        mGoogleApiClient!!.disconnect()
        super.onDestroy()
    }

    override fun onResult(credentialRequestResult: CredentialRequestResult) {

        val status = credentialRequestResult.status
        if (status.isSuccess) {
            onCredentialRetrieved(credentialRequestResult.credential)
        } else {
            if (status.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                try {
                    isResolving = true
                    status.startResolutionForResult(this, RC_READ)
                } catch (e: IntentSender.SendIntentException) {
                    Log.d(TAG, e.toString())
                }

            } else {

                showHintDialog()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "onActivityResult")
        if (requestCode == RC_READ) {
            if (resultCode == Activity.RESULT_OK) {
                val credential = data!!.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                onCredentialRetrieved(credential!!)
            } else {
                Log.d(TAG, "Request failed")
            }
            isResolving = false
        }

        if (requestCode == RC_HINT) {
            if (resultCode == Activity.RESULT_OK) {
                val credential = data!!.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                setLoginFields(credential!!.id, "")
            } else {
                Toast.makeText(applicationContext, "Hint dialog closed", Toast.LENGTH_SHORT).show()

            }
        }

        if (requestCode == RC_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "SAVE: OK")
                gotoHomePage()
                Toast.makeText(applicationContext, "Credentials saved", Toast.LENGTH_SHORT).show()

            }
        }


    }

    fun setLoginFields(emailfield: String?, passwordfiled: String?) {
        if (!TextUtils.isEmpty(emailfield))
            email.setText(emailfield)

        if (!TextUtils.isEmpty(passwordfiled))
            password.setText(passwordfiled)
    }

    fun showHintDialog() {
        val hintRequest = HintRequest.Builder()
            .setHintPickerConfig(
                CredentialPickerConfig.Builder()
                    .setShowCancelButton(true)
                    .build()
            )
            .setEmailAddressIdentifierSupported(true)
            .setAccountTypes(IdentityProviders.GOOGLE, IdentityProviders.FACEBOOK)
            .build()

        val intent = mCredentialsApiClient?.getHintPickerIntent(hintRequest)
        try {
            startIntentSenderForResult(intent?.intentSender, RC_HINT, null, 0, 0, 0)
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Could not start hint picker Intent", e)
        }

    }

    fun saveCredentials(credential: Credential) {


        mCredentialsApiClient?.save(credential)?.addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "SAVE: OK")
                Toast.makeText(applicationContext, "Credentials saved", Toast.LENGTH_SHORT).show()
                gotoHomePage()
                return@OnCompleteListener
            }

            val e = task.exception
            if (e is ResolvableApiException) {

                // Try to resolve the save request. This will prompt the user if
                // the credential is new.

                val rae = e as ResolvableApiException?
                try {
                    rae!!.startResolutionForResult(this@MainActivity, RC_SAVE)
                } catch (f: IntentSender.SendIntentException) {
                    // Could not resolve the request

                    Log.e(TAG, "Failed to send resolution.", f)
                    Toast.makeText(applicationContext, "Saved failed", Toast.LENGTH_SHORT).show()

                }

            } else {
                // Request has no resolution

                Toast.makeText(applicationContext, "Saved failed", Toast.LENGTH_SHORT).show()

            }
        })

    }

    companion object {
        val TAG = "MainActivity"
        private val RC_READ = 3
        private val RC_SAVE = 1
        private val RC_HINT = 2
    }
}