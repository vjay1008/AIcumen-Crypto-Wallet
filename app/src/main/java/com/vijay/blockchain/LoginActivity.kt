package com.vijay.blockchain

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import com.kusu.linkedinlogin.Linkedin
import com.kusu.linkedinlogin.LinkedinLoginListener
import com.kusu.linkedinlogin.model.SocialUser

class LoginActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_linkedin_sign_in)

        webView = findViewById(R.id.linkedin_web_view)
        progressBar = findViewById(R.id.progress_bar)
        webView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE


        Linkedin.initialize(
            context = applicationContext,
            clientId = "81h3fis900yj9k",
            clientSecret = "3VbWr0Z5eq7PPJAA",
            redirectUri = "https://www.google.com",
            state = "RANDOM_STRING",
            scopes = listOf("r_liteprofile", "r_emailaddress", "w_member_social")
        )

        Linkedin.login(this, object : LinkedinLoginListener {
            override fun failedLinkedinLogin(error: String) {


                Toast.makeText(this@LoginActivity, error, Toast.LENGTH_SHORT).show()
                Log.i("failed", error)
                //todo failed functionality
            }

            override fun successLinkedInLogin(socialUser: SocialUser) {
                // todo success functionality
                Toast.makeText(this@LoginActivity, "success", Toast.LENGTH_SHORT).show()
                Log.i("success", "success")

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            }
        })

    }
}
