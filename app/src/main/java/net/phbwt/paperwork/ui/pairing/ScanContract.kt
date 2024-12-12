package net.phbwt.paperwork.ui.pairing

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import net.phbwt.paperwork.data.entity.pairing.QrCodeContent

class ScanContract : ActivityResultContract<Void?, QrCodeContent?>() {
    override fun createIntent(context: Context, input: Void?): Intent =
        Intent(context, QrCodeScanActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): QrCodeContent? = if (intent != null && resultCode == Activity.RESULT_OK) {
        QrCodeContent(
            serverFingerprint = intent.getByteArrayExtra(RESULT_SERVER_FINGERPRINT) ?: byteArrayOf(),
            secret = intent.getByteArrayExtra(RESULT_SECRET) ?: byteArrayOf(),
            port = intent.getIntExtra(RESULT_PORT, 0),
            serverAddresses = intent.getStringArrayExtra(RESULT_SERVER_ADDRESSES)?.toList() ?: emptyList(),
        )
    } else {
        null
    }
}

fun Intent.putQrCodeContent(content: QrCodeContent) {
    putExtra(RESULT_SERVER_FINGERPRINT, content.serverFingerprint)
    putExtra(RESULT_SECRET, content.secret)
    putExtra(RESULT_PORT, content.port)
    putExtra(RESULT_SERVER_ADDRESSES, content.serverAddresses.toTypedArray())
}

private const val RESULT_SERVER_ADDRESSES = "SERVER_ADDRESSES"
private const val RESULT_SECRET = "SECRET"
private const val RESULT_PORT = "PORT"
private const val RESULT_SERVER_FINGERPRINT = "SERVER_FINGERPRINT"
