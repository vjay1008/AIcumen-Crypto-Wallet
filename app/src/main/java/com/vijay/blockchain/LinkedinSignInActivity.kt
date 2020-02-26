package com.vijay.blockchain

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import com.vijay.blockchain.model.LinkedinToken
import com.vijay.blockchain.network.ProfileRestService
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class LinkedinSignInActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private var clientId: String? = null
    private var clientSecret: String? = null
    private var redirectUri: String? = null
    private var state: String? = null
    private var scopes: List<String>? = null
    private var isLogedIn = true

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_linkedin_sign_in)

        Linkedin.initialize(
            context = applicationContext,
            clientId = "81h3fis900yj9k",
            clientSecret = "3VbWr0Z5eq7PPJAA",
            redirectUri = "https://www.linkedin.com/auth/linkedin/callback",
            state = "RANDOM_STRING",
            scopes = listOf("PERMISSION_OPTIONS")
        )

        webView = findViewById(R.id.linkedin_web_view)
        progressBar = findViewById(R.id.progress_bar)
        webView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE

        clientId = getSharedPreferences(
            Constants.PREFS,
            Context.MODE_PRIVATE
        ).getString(Constants.CLIENT_ID, null)
        clientSecret =
            getSharedPreferences(
                Constants.PREFS,
                Context.MODE_PRIVATE
            ).getString(Constants.CLIENT_SECRET, null)
        redirectUri =
            getSharedPreferences(
                Constants.PREFS,
                Context.MODE_PRIVATE
            ).getString(Constants.REDIRECT_URI, null)
        state = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE).getString(
            Constants.STATE,
            null
        )
        scopes = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE).getStringSet(
            Constants.SCOPE,
            mutableSetOf("r_liteprofile")
        )?.toList()

        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val url = generateUrl()
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = Uri.parse(url)

                val error = uri.getQueryParameters(ERROR).getOrNull(0)
                if (error != null) {
                    finish()
                    Linkedin.linkedinLoginListener?.failedLinkedinLogin(
                        uri.getQueryParameters(ERROR_DESCRIPTION).getOrNull(
                            0
                        ) ?: ""
                    )
                    return false
                }

                val authCode = uri.getQueryParameters(CODE).getOrNull(0)
                if (authCode != null) {
                    getAccessToken(authCode)
                }

                return super.shouldOverrideUrlLoading(view, url)
            }

            @RequiresApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val error = request.url.getQueryParameters(ERROR).getOrNull(0)
                if (error != null) {
                    finish()
                    Linkedin.linkedinLoginListener?.failedLinkedinLogin(
                        request.url.getQueryParameters(ERROR_DESCRIPTION).getOrNull(
                            0
                        ) ?: ""
                    )
                    return false
                }

                val authCode = request.url.getQueryParameters(CODE).getOrNull(0)
                if (authCode != null) {
                    getAccessToken(authCode)
                }

                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        webView.loadUrl(url)
    }

    private fun generateUrl(): String {
        return Uri.parse(AUTHORIZATION_URL)
            .buildUpon()
            .appendQueryParameter(RESPONSE_TYPE, CODE)
            .appendQueryParameter(CLIENT_ID, this.clientId)
            .appendQueryParameter(REDIRECT_URI, this.redirectUri)
            .appendQueryParameter(STATE, this.state)
            .appendQueryParameter(SCOPE, getScopes()).build().toString()
    }

    private fun getScopes(): String {
        var scopeString = ""
        this.scopes?.let {
            for (scope in it) {
                scopeString += "$scope "
            }
        }
        return scopeString.dropLast(1)
    }

    fun getAccessToken(authCode: String) {
        progressBar.visibility = View.VISIBLE
        webView.visibility = View.GONE
        val thread = Thread(Runnable {
            try {
                val inputs = hashMapOf(
                    GRANT_TYPE to AUTHORIZATION_CODE,
                    CODE to authCode,
                    REDIRECT_URI to this.redirectUri,
                    CLIENT_ID to this.clientId,
                    CLIENT_SECRET to this.clientSecret
                )
                val inputsString = getDataString(inputs)

                val postData = inputsString.toByteArray(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlString = (ACCESS_TOKEN_URL)
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8"
                )
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                conn.instanceFollowRedirects = false
                conn.doOutput = true
                conn.doInput = true

                val os = DataOutputStream(conn.outputStream)
                os.writeBytes(getDataString(inputs))

                os.flush()
                os.close()

                val sb = StringBuilder()
                val rd: BufferedReader
                rd = try {
                    BufferedReader(InputStreamReader(conn.inputStream))
                } catch (e: FileNotFoundException) {
                    BufferedReader(InputStreamReader(conn.errorStream))
                }

                rd.use { reader ->
                    sb.append(reader.readLine())
                }

                val response = JSONObject(sb.toString())

                conn.disconnect()
                if (response.has("access_token")) {
                    finish()
                    if (isLogedIn) {
                        runOnUiThread {
                            val restService = Linkedin.linkedinLoginListener?.let {
                                ProfileRestService(
                                    LinkedinToken(
                                        response.getString("access_token"),
                                        response.getLong("expires_in")
                                    ),
                                    it
                                )
                            }
                            restService?.getLinkedInProfile()
                        }
                        isLogedIn = false
                    }
                }
//                else {
//                    finish()
//                    runOnUiThread {
//                        Linkedin.linkedinLoginListener?.failedLinkedinLogin(response.getString("error_description"))
//                    }
//                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })

        thread.start()
    }

    @Throws(UnsupportedEncodingException::class)
    private fun getDataString(params: HashMap<String, String?>): String {
        val result = StringBuilder()
        var first = true
        for (entry in params.entries) {
            if (first)
                first = false
            else
                result.append("&")
            result.append(URLEncoder.encode(entry.key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(entry.value, "UTF-8"))
        }
        return result.toString()
    }

    companion object {
        private const val AUTHORIZATION_URL = "https://www.linkedin.com/oauth/v2/authorization"
        private const val ACCESS_TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken"
        private const val USER_LITE_PROFILE_URL =
            "https://api.linkedin.com/v2/me?projection=(id,firstName,lastName,profilePicture(displayImage~:playableStreams))"
        private const val USER_EMAIL_URL =
            "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))"
        private const val USER_POST_URL = "https://api.linkedin.com/v2/ugcPosts"
        private const val RESPONSE_TYPE = "response_type"
        private const val CLIENT_ID = "client_id"
        private const val REDIRECT_URI = "redirect_uri"
        private const val STATE = "state"
        private const val SCOPE = "scope"
        private const val CODE = "code"
        private const val ERROR = "error"
        private const val ERROR_DESCRIPTION = "error_description"
        private const val GRANT_TYPE = "grant_type"
        private const val AUTHORIZATION_CODE = "authorization_code"
        private const val CLIENT_SECRET = "client_secret"
    }
}