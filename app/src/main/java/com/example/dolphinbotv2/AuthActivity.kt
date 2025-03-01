package com.example.dolphinbotv2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var buttonGoogle: LinearLayout

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("SignIn", "Google sign in failed", e)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        init()
    }

    private fun init() {
        initView()
        initListeners()
    }

    private fun initView() {
        buttonGoogle = findViewById(R.id.buttonPanelGoogle)
    }

    private fun initListeners() {
        buttonGoogle.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("SignOut", "User signed out")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SignIn", "signInWithCredential:success")
                    showHome()
                } else {
                    Log.w("SignIn", "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun showHome() {
        val user = auth.currentUser
        val fullName = user?.displayName ?: "Usuario"
        val firstNamefirstName = fullName.split(" ").firstOrNull()?.uppercase() ?: "Usuario"
        val userPhotoUrl = user?.photoUrl?.toString()  ?: ""

        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("USER_NAME", firstNamefirstName)
            putExtra("USER_PHOTO", userPhotoUrl)
        }
        startActivity(homeIntent)
    }
}
