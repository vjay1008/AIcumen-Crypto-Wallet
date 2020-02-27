package com.vijay.blockchain

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        walletPath =
            "/data/data/com.vijay.blockchain/files/UTC--2020-02-26T05-31-32.9Z--aa4de3f81cdd34f27f229c2cb3bf644dfe1d3c7b.json" //onePlus
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
//            startActivity(Intent(this, LinkedinSignInActivity::class.java))
            finish()

        }

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
        println("balance in wei: ${balanceWei.balance}")
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
