package com.vijay.blockchain

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.linkedin.platform.APIHelper
import com.linkedin.platform.LISessionManager
import com.linkedin.platform.errors.LIApiError
import com.linkedin.platform.errors.LIAuthError
import com.linkedin.platform.listeners.ApiListener
import com.linkedin.platform.listeners.ApiResponse
import com.linkedin.platform.listeners.AuthListener
import com.linkedin.platform.utils.Scope
import kotlinx.android.synthetic.main.activity_main.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.security.MessageDigest
import java.security.Provider
import java.security.Security


class MainActivity : AppCompatActivity() {

    private var web3: Web3j? = null
    //FIXME: Add your own password here
    private val password = "medium"
    private var walletPath: String? = null
    private var walletDir: File? = null


    lateinit var pDialog: ProgressDialog



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        walletPath = filesDir.absolutePath
        walletPath = "/data/data/com.vijay.blockchain/files/UTC--2020-02-26T05-31-32.9Z--aa4de3f81cdd34f27f229c2cb3bf644dfe1d3c7b.json" //onePlus
//        walletPath = "UTC--2020-02-24T11-39-31.2Z--60b6568204cb80d8d3c2689ec8749bb1234f5088.json"
//        walletPath = "/data/data/com.vijay.blockchain/files/UTC--2020-02-19T04-34-58.0Z--5c52c3c3aa6a2daca8dfa0ee7c04a4c0222629dd.json"

        walletDir = File(walletPath)

        DisplayProgressDialog()

        setupBouncyCastle()
        connectToEthNetwork()
        createWallet()
        getAddress()
//        sendTransaction()
        getBalance()
        generateHashKey()

        btn_send.setOnClickListener {
            sendTransaction()
        }

        linkedin_login_button.setOnClickListener {
            startActivity(Intent(this,LinkedinSignInActivity::class.java))

        }

    }


    //upon sign in button clicked
    fun signInWithLinkedIn(view: View) {

        //First check if user is already authenticated or not and session is valid or not
        if (!LISessionManager.getInstance(this).session.isValid) {

            //if not valid then start authentication
            LISessionManager.getInstance(applicationContext)
                .init(
                    this, buildScope() //pass the build scope here
                    , object : AuthListener {
                        override fun onAuthSuccess() { // Authentication was successful. You can now do
                            // other calls with the SDK.



                            fetchBasicProfileData()
                        }

                        override fun onAuthError(error: LIAuthError) { // Handle authentication errors
                            Log.e(
                                "auth ",
                                "Auth Error :$error"
                            )
                            Toast.makeText(
                                this@MainActivity,
                                error.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }, true
                ) //if TRUE then it will show dialog if

            // any device has no LinkedIn app installed to download app else won't show anything
        } else {
            Toast.makeText(this, "You are already authenticated.", Toast.LENGTH_SHORT).show()

            //if user is already authenticated fetch basic profile data for user
            fetchBasicProfileData()
        }
    }

    fun fetchBasicProfileData() {
        //In URL pass whatever data from user you want for more values check below link
        //LINK : https://developer.linkedin.com/docs/fields/basic-profile
        var url =
//            "https://api.linkedin.com/v2/me/~:(id,first-name,last-name,headline,public-profile-url,picture-url,email-address,picture-urls::(original))";
        "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id={81h3fis900yj9k}&redirect_uri=https%3A%2F%2Fwww.linked.com%2Fauth%2Flinkedin%2Fcallback&state=fooobar&scope=r_liteprofile%20r_emailaddress%20w_member_social"
        var apiHelper = APIHelper.getInstance(applicationContext)

        apiHelper.getRequest(this, url, object : ApiListener {
            override fun onApiError(LIApiError: LIApiError?) {
                // Error making GET request!
                Log.e("tag", "Fetch profile Error   :")
                Toast.makeText(
                    applicationContext,
                    "Failed to fetch basic profile data. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()

            }

            override fun onApiSuccess(apiResponse: ApiResponse) {
                // Success!
                Log.d(
                    "tag",
                    "API Res : " + apiResponse.responseDataAsString + "\n" + apiResponse.responseDataAsJson.toString()
                )
                Toast.makeText(
                    applicationContext,
                    "Successfully fetched LinkedIn profile data.",
                    Toast.LENGTH_SHORT
                ).show()

                updateUI(apiResponse)

            }

        })


/*
            LISessionManager.getInstance(getApplicationContext()).init(this, buildScope(), object : AuthListener {


                override fun onAuthSuccess(apiResponse: ApiResponse) {
                    Log.d(
                        "tag",
                        "API Res : " + apiResponse.getResponseDataAsString() + "\n" + apiResponse.getResponseDataAsJson().toString()
                    );
                    Toast.makeText(
                        applicationContext,
                        "Successfully fetched LinkedIn profile data.",
                        Toast.LENGTH_SHORT
                    ).show()

                    updateUI(apiResponse)
                }

                override fun onAuthError(error: LIAuthError?) {
                    // Error making GET request!
                    Log.e("tag", "Fetch profile Error   :")
                    Toast.makeText(
                        applicationContext,
                        "Failed to fetch basic profile data. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, true);
*/

    }


    fun updateUI(apiResponse: ApiResponse) {
        try {
            if (apiResponse != null) {
                var jsonObject = apiResponse.responseDataAsJson

                //display user basic details
                user_details_label.setText(
                    "Name : " + jsonObject.getString("firstName") + " " + jsonObject.getString(
                        "lastName"
                    ) + "\nHeadline : " + jsonObject.getString("headline") + "\nEmail Id : " + jsonObject.getString(
                        "emailAddress"
                    )
                );

/*
                //use the below string value to display small profile picture
                var smallPicture = jsonObject.getString("pictureUrl")

                //use the below json parsing for different profile pictures and big size images
                var pictureURLObject = jsonObject.getJSONObject("pictureUrls");
                if (pictureURLObject.getInt("_total") > 0) {
                    //get array of picture urls
                    var profilePictureURLArray = pictureURLObject.getJSONArray("values");
                    if (profilePictureURLArray != null && profilePictureURLArray.length() > 0) {
                        // get 1st image link and display using picasso
                        Picasso.with(this).load(profilePictureURLArray.getString(0))
                            .placeholder(R.mipmap.ic_launcher_round)
                            .error(R.mipmap.ic_launcher_round)
                            .into(userImageView);
                    }
                } else {
                    // if no big image is available then display small image using picasso
                    Picasso.with(this).load(smallPicture)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(userImageView);
                }
*/

                //show hide views
                linkedin_login_button.visibility = View.GONE
                logout_button.visibility = View.VISIBLE
                user_details_label.visibility = View.VISIBLE
                user_profile_image_view.visibility = View.VISIBLE
                share_button.visibility = View.VISIBLE
                open_my_profile_button.visibility = View.VISIBLE
                open_others_profile_button.visibility = View.VISIBLE

            }
        } catch (e: Exception) {
            e.stackTrace
        }
    }


    // Build the list of member permissions our LinkedIn session requires
    private fun buildScope(): Scope {
        return Scope.build(Scope.R_BASICPROFILE, Scope.R_EMAILADDRESS, Scope.W_SHARE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        LISessionManager.getInstance(applicationContext)
            .onActivityResult(this, requestCode, resultCode, data)
    }

    fun connectToEthNetwork() {
        pDialog.show()
        toastAsync("Connecting to Ethereum network...")
        web3 = Web3j.build(
            HttpService(
                "https://rinkeby.infura.io/v3/bf1746f7e8d54b1a8a9ab1a51f0a1678"
            )
        )
        try {

            val clientVersion = web3!!.web3ClientVersion().sendAsync().get()

            if (!clientVersion.hasError()) {
                toastAsync("Connected!")
                pDialog.cancel()
            } else {
                toastAsync(clientVersion.error.message)
                pDialog.cancel()
            }
        } catch (e: Exception) {
            toastAsync(e.message)
            pDialog.cancel()
        }
    }

    fun toastAsync(message: String?) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            Log.i("catch", message)
        }
    }

    fun getBalance() {
        pDialog.show()
        val balanceWei = web3!!.ethGetBalance(
            "0xF0f15Cedc719B5A55470877B0710d5c7816916b1",     //other wallet address with balance
//            "0xa76776755d9360124f11f4e912cec88c484ed6e6",//my wallet address
            DefaultBlockParameterName.LATEST
        ).sendAsync().get()
        println("balance in wei: $balanceWei")
        pDialog.cancel()

        val balanceInEther =
            Convert.fromWei(balanceWei.balance.toString(), Convert.Unit.ETHER)
        println("balance in ether: $balanceInEther")
        eth_balance_value.text = String.format(balanceInEther.toString())
    }

    fun sendTransaction() {
        DisplayProgressDialog()
        pDialog.show()
        try {
            val credentials =
                WalletUtils.loadCredentials(password, walletDir)
            val receipt = Transfer.sendFunds(
                web3,
                credentials,
                "0x31B98D14007bDEe637298086988A0bBd31184523",
                BigDecimal(1),
                Convert.Unit.ETHER
            ).sendAsync().get()
            toastAsync("Transaction complete: " + receipt.transactionHash)
//            pDialog.cancel()
            Log.i("sent", receipt.transactionHash)
        } catch (e: java.lang.Exception) {
            toastAsync(e.message)
            Log.i("sent error", e.message!!)
            pDialog.cancel()
        }
    }

    fun getAddress() {
        pDialog.show()
        try {
            val credentials =
                WalletUtils.loadCredentials(password, walletDir)
//            toastAsync("Your address is " + credentials.address)
            wallet_address_value.text = credentials.address.toString()
            Log.i("address ", credentials.address)
            pDialog.cancel()

        } catch (e: java.lang.Exception) {
            toastAsync(e.message)
            Log.i("address error", e.message!!)
            pDialog.cancel()


        }
    }

    fun createWallet() {
        pDialog.show()
        try {
            WalletUtils.generateNewWalletFile(password, walletDir)
            toastAsync("Wallet generated")
            pDialog.cancel()

        } catch (e: java.lang.Exception) {
            toastAsync(e.message)
            pDialog.cancel()

        }
    }

    private fun setupBouncyCastle() {
        val provider: Provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
            ?: // Web3j will set up the provider lazily when it's first used.
            return

        if (provider.javaClass.equals(BouncyCastleProvider::class.java)) { // BC with same package name, shouldn't happen in real life.
            return
        }
        // Android registers its own BC provider. As it might be outdated and might not include
// all needed ciphers, we substitute it with a known BC bundled in the app.
// Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
// of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    private fun generateHashKey() {

        try {
            val info = packageManager.getPackageInfo(
                "com.vijay.blockchain",
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d(
                    "KeyHash:",
                    Base64.encodeToString(md.digest(), Base64.DEFAULT)
                )
            }
        } catch (e: java.lang.Exception) {
            Log.e("TAG", e.message)
        }

    }

    fun DisplayProgressDialog() {

        pDialog = ProgressDialog(this@MainActivity)
        pDialog.setMessage("Fetching details..")
        pDialog.setCancelable(false)
        pDialog.show()
    }



}
