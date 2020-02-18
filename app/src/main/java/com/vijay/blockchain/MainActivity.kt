package com.vijay.blockchain

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.security.Provider
import java.security.Security


class MainActivity : AppCompatActivity() {

    private var web3: Web3j? = null
    //FIXME: Add your own password here
    private val password = "medium"
    private var walletPath: String? = null
    private var walletDir: File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        walletPath = filesDir.absolutePath
        walletPath = "/data/data/com.vijay.blockchain/files/UTC--2020-02-18T09-20-12.8Z--a76776755d9360124f11f4e912cec88c484ed6e6.json"
        walletDir = File(walletPath)

        setupBouncyCastle()
        connectToEthNetwork()
        createWallet()
        getAddress()
        sendTransaction()
        getBalance()
    }

    fun connectToEthNetwork() {
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
            } else {
                toastAsync(clientVersion.error.message)
            }
        } catch (e: Exception) {
            toastAsync(e.message)
        }
    }

    fun toastAsync(message: String?) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            Log.i("catch", message)
        }
    }

    fun getBalance() {
        val balanceWei = web3!!.ethGetBalance(
//            "0xF0f15Cedc719B5A55470877B0710d5c7816916b1",
            "0xa76776755d9360124f11f4e912cec88c484ed6e6",
            DefaultBlockParameterName.LATEST
        ).sendAsync().get()
        println("balance in wei: $balanceWei")

        val balanceInEther =
            Convert.fromWei(balanceWei.balance.toString(), Convert.Unit.ETHER)
        println("balance in ether: $balanceInEther")
    }

    fun sendTransaction() {
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
            Log.i("sent", receipt.transactionHash)
        } catch (e: java.lang.Exception) {
            toastAsync(e.message)
            Log.i("sent error", e.message)
        }
    }

    fun getAddress() {
        try {
        val credentials =
            WalletUtils.loadCredentials(password, walletDir)
        toastAsync("Your address is " + credentials.address)
        Log.i("address ", credentials.address)

        } catch (e: java.lang.Exception) {
            toastAsync(e.message)
            Log.i("address error", e.message)

        }
    }

    fun createWallet() {
        try {
            WalletUtils.generateNewWalletFile(password, walletDir)
            toastAsync("Wallet generated")

        } catch (e: java.lang.Exception) {
            toastAsync(e.message)
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


}
