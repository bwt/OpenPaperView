package net.phbwt.paperwork.data.helper

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.entity.pairing.PairingConfig
import net.phbwt.paperwork.data.entity.pairing.QrCodeContent
import net.phbwt.paperwork.helper.desc
import net.phbwt.paperwork.ui.settingscheck.Check
import net.phbwt.paperwork.ui.settingscheck.Level
import net.phbwt.paperwork.ui.settingscheck.Msg
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okhttp3.internal.connection.RealCall
import okhttp3.tls.HandshakeCertificates
import okio.ByteString.Companion.toByteString
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSocket
import kotlin.coroutines.coroutineContext

/**
 * Pairing step 2
 * Try an HTTPS query to each server address found in the QR Code
 * until one works
 */
class PairingRunner(private val applicationContext: Context) {

    val logFlow = MutableStateFlow<ImmutableList<Check>>(persistentListOf())

    private var log = persistentListOf<Check>()

    suspend fun runPairing(config: QrCodeContent): PairingResult? = withContext(Dispatchers.IO) {

        log = log.clear()

        // try each one of the addresses
        for (address in config.serverAddresses) {
            val label = "$address:${config.port}"
            addItem(Msg(R.string.pairing_trying, label), Level.Title, null)
            try {
                delay(1000)

                val result = pairAddress(config, address)

                if (result != null) {
                    return@withContext result
                }
            } catch (ex: Exception) {
                coroutineContext.ensureActive()

                // The "Cancelled" message is handled elsewhere :
                // - It may take some time to get the exception (e.g. network)
                // - The job may have been restarted and we should ignore the previous job's exception

                addItem(Msg(R.string.pairing_failed), Level.Error, Msg(R.string.pairing_exception, ex.desc()))

                Log.i(TAG, "Pairing with ${address} failed ${ex.desc()}")
                delay(500)
            }
        }

        // none of the addresses worked
        addItem(Msg(R.string.pairing_all_failed), Level.Title, null)
        return@withContext null
    }

    fun jobWasCancelled() {
        addItem(Msg(R.string.pairing_cancelled), Level.Error, null)
    }

    private suspend fun pairAddress(qrCode: QrCodeContent, address: String): PairingResult? {

        Log.i(TAG, "Trying : '$address'")

        // very crude, but it is only a avoid spurious messages
        val isIPv4 = address.count { it == '.' } == 3 && address.matches("^[0-9.]+$".toRegex())
        val isIPv6 = address.count { it == ':' } > 0 && address.matches("^\\[?[a-fA-F0-9:.]+]?$".toRegex())
        val addressIsIp = isIPv4 || isIPv6

        if (addressIsIp) {
            addItem(Msg(R.string.pairing_ip_address), Level.Warn, Msg(R.string.pairing_ip_address_warning))
        }

        // DNS / mDNS resolution
        var ipAddress: InetAddress?
        try {
            ipAddress = InetAddress.getByName(address)

            coroutineContext.ensureActive()

            if (!addressIsIp) {
                addItem(Msg(R.string.pairing_resolved), Level.OK, Msg(R.string.pairing_resolved_as, ipAddress.hostAddress))
            }
        } catch (ex: UnknownHostException) {
            coroutineContext.ensureActive()

            ipAddress = null
            addItem(Msg(R.string.pairing_not_resolved), Level.Warn, Msg(R.string.pairing_dns_or_mdns_problem))
        }

        // ping
        if (ipAddress != null && Build.VERSION.SDK_INT > 26) {

            val ts = SystemClock.elapsedRealtime()
            val pinged = ipAddress.isReachable(5_000)
            val pingTime = SystemClock.elapsedRealtime() - ts

            if (pinged) {
                addItem(Msg(R.string.pairing_pinged), Level.OK, Msg(R.string.pairing_pinged_in, pingTime.toString()))
            } else {
                addItem(Msg(R.string.pairing_not_pinged), Level.Warn, Msg(R.string.pairing_no_ping_response))
            }
        }

        coroutineContext.ensureActive()

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

                    serverValidated.set(hashMatches == true)
                }
            }).build()

        val currentUrl = qrCode.toUrl(address)
        val pairingUrl = "$currentUrl/pair"
        val authorization = "${Base64.encodeToString(qrCode.secret, Base64.NO_WRAP)}"

        client.newCall(
            Request.Builder().url(pairingUrl).addHeader(
                "Authorization", "Basic $authorization"
            ).build()
        ).executeAsync().use { response ->
            return withContext(Dispatchers.IO) {
                when {
                    !response.isSuccessful -> {
                        // We can connect but something is wrong
                        // This should not really happen

                        addItem(Msg(R.string.pairing_failed), Level.Error, Msg(R.string.pairing_http_error, response.code))
                        null
                    }

                    !serverValidated.get() -> {
                        // the server's certificate's fingerprint does not match
                        addItem(Msg(R.string.pairing_failed), Level.Error, Msg(R.string.pairing_fingerprint_error))
                        null
                    }

                    else -> {
                        val body = response.body.source().readUtf8()

                        val config = Json.decodeFromString<PairingConfig>(body)

                        addItem(Msg(R.string.pairing_success), Level.OK, Msg(R.string.pairing_success_detail, address))
                        PairingResult(currentUrl, config)
                    }
                }
            }
        }
    }

    private fun addItem(item: Check) {
        log += item
        logFlow.update { log }
    }

    private fun addItem(
        desc: Msg,
        level: Level,
        msg: Msg?,
    ) = addItem(Check(desc, level, msg))

    data class PairingResult(
        val address: String,
        val config: PairingConfig,
    )
}

private const val TAG = "PairingRunner"

