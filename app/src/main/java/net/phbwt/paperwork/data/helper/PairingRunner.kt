package net.phbwt.paperwork.data.helper

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.entity.pairing.PairingConfig
import net.phbwt.paperwork.data.entity.pairing.QrCodeContent
import net.phbwt.paperwork.helper.desc
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.connection.RealCall
import okhttp3.tls.HandshakeCertificates
import okio.ByteString.Companion.toByteString
import ru.gildor.coroutines.okhttp.await
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSocket

/**
 * Pairing step 2
 * Try an HTTPS query to each server address found in the QR Code
 * until one works
 */
class PairingRunner(private val applicationContext: Context) {

    val pairingStatus = MutableStateFlow<PairingStatus?>(null)

    suspend fun runPairing(config: QrCodeContent): PairinResult? = withContext(Dispatchers.IO) {
        pairingStatus.value = Ongoing(getString(R.string.settings_pairing_starting))

        // try each one of the addresses
        for (address in config.serverAddresses) {
            pairingStatus.value = Ongoing(getString(R.string.settings_pairing_trying, address))
            try {
                delay(1000)

                val result = pairAddress(config, address)

                if (result != null) {
                    pairingStatus.value = Succeeded(getString(R.string.settings_pairing_success, address))
                    return@withContext result
                }
            } catch (ex: Exception) {
                coroutineContext.ensureActive()
                Log.i(TAG, "Pairing with ${address} failed ${ex.desc()}")
                delay(500)
            }
        }

        // none of the addresses worked
        pairingStatus.value = Failed(getString(R.string.settings_pairing_failed))
        return@withContext null
    }

    fun pairingCanceled() {
        pairingStatus.value = null
    }

    private suspend fun pairAddress(qrCode: QrCodeContent, address: String): PairinResult? {

        Log.i(TAG, "Trying : '$address'")

        val expectedServerCertHash = qrCode.serverFingerprint.toByteString()

        val handshakeCertificates = HandshakeCertificates.Builder()
            .addPlatformTrustedCertificates()
            // we don't have the CA yet
            // so we trust any certificate
            // but we will check it's fingerprint
            .addInsecureHost(address)
            .build()

        val serverValidated = AtomicBoolean(false)

        val client = OkHttpClient.Builder()
            .callTimeout(9000, TimeUnit.MILLISECONDS)
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            .sslSocketFactory(handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager)
            .eventListener(object : EventListener() {

                // The server's certificate will not be available in the response
                // cf https://stackoverflow.com/questions/64531304/how-to-get-the-server-certificate-with-okhttp-response
                override fun connectionAcquired(call: Call, connection: Connection) {
                    val socket = (call as RealCall).connection?.socket() as? SSLSocket

                    // compare each server certificate with the expected one
                    val hashMatches = socket?.session?.peerCertificates?.any { cert ->
                        val hash = cert.encoded.toByteString().sha256()
                        Log.d(TAG, "expected ${expectedServerCertHash.hex()}, got : ${hash.hex()}")
                        hash == expectedServerCertHash
                    }

                    serverValidated.set(hashMatches ?: false)
                }
            }).build()

        val currentUrl = qrCode.toUrl(address)
        val pairingUrl = "$currentUrl/pair"
        val authorization = "${Base64.encodeToString(qrCode.secret, Base64.NO_WRAP)}"

        client.newCall(
            Request.Builder().url(pairingUrl).addHeader(
                "Authorization", "Basic $authorization"
            ).build()
        ).await().use { response ->
            if (!response.isSuccessful) {
                // We can connect but something is wrong
                // This should not really happen
                Log.w(TAG, "Response " + response.code)
                return null
            } else {

                if (!serverValidated.get()) {
                    // the server's certificate's fingerprint does not match
                    return null
                }

                val body = response.body?.source()?.readUtf8() ?: throw RuntimeException("Response without body")

                val config = Json.decodeFromString<PairingConfig>(body)

                Log.i(TAG, "Pairing succeeded")
                return PairinResult(currentUrl, config)
            }
        }
    }

    private fun getString(msgRes: Int, vararg args: String) = applicationContext.getString(msgRes, *args)

    sealed interface PairingStatus {
        val message: String
    }

    data class Ongoing(override val message: String) : PairingStatus
    data class Succeeded(override val message: String) : PairingStatus
    data class Failed(override val message: String) : PairingStatus

    data class PairinResult(
        val address: String,
        val config: PairingConfig,
    )
}

private const val TAG = "PairingRunner"

