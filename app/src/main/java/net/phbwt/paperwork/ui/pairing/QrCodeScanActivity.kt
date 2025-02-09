package net.phbwt.paperwork.ui.pairing

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.KeyEvent
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.os.postDelayed
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultMetadataType
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import net.phbwt.paperwork.R
import net.phbwt.paperwork.data.entity.pairing.QrCodeFragment
import net.phbwt.paperwork.data.helper.extractQrCodeContent
import net.phbwt.paperwork.data.helper.extractQrCodeFragment
import net.phbwt.paperwork.helper.desc

/**
 * Based on https://github.com/journeyapps/zxing-android-embedded/blob/master/sample/src/main/java/example/zxing/ContinuousCaptureActivity.java
 */
class QrCodeScanActivity : ComponentActivity() {
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var statusView: TextView
    private lateinit var beepManager: BeepManager

    private val handler = Handler(Looper.getMainLooper())

    // only to detect duplicate
    private var lastScan: String? = null

    private val qrCodes = mutableListOf<QrCodeFragment?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.qr_code_scan_activity)

        barcodeView = findViewById<DecoratedBarcodeView>(R.id.barcode_scanner)
        barcodeView.barcodeView.setDecoderFactory(
            DefaultDecoderFactory(
                listOf(BarcodeFormat.QR_CODE),
                mapOf(DecodeHintType.ALSO_INVERTED to true),
                null,
                0,
            )
        )

        barcodeView.decodeContinuous(callback)

        statusView = barcodeView.statusView

        beepManager = BeepManager(this)
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
        updateUi()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArray(QRCODES_KEY, qrCodes.toTypedArray())
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        qrCodes.clear()
        qrCodes.addAll(savedInstanceState.getParcelableArray(QRCODES_KEY)?.map { it as QrCodeFragment } ?: emptyList())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    private fun updateUi(errorMsg: String? = null) {
        if (errorMsg != null) {
            statusView.text = getString(R.string.pairing_bad_qrcode, errorMsg)
            statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            statusView.setTextColor(getColor(R.color.error))

            handler.postDelayed(1500) {
                // clear the error message
                updateUi()
            }
        } else if (qrCodes.isEmpty()) {
            statusView.setText(R.string.pairing_scan)
            statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            statusView.setTextColor(Color.WHITE)
        } else {
            val status = qrCodes.map {
                if (it != null) '\u2611' else '\u2610'
            }.joinToString(" ")

            statusView.text = status
            statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60f)
            statusView.setTextColor(Color.GREEN)
        }
    }

    private fun handleBarcode(result: BarcodeResult) {
        if (result.text == lastScan) {
            // duplicate
            return
        }

        lastScan = result.text

        beepManager.playBeepSoundAndVibrate()

        val segments = result.resultMetadata[ResultMetadataType.BYTE_SEGMENTS] as? List<ByteArray>

        if (segments.isNullOrEmpty()) {
            updateUi("Not a binary QR Code")
            return
        }

        val data = segments.fold(byteArrayOf()) { acc, seg -> acc + seg }

        val fragmentResult = extractQrCodeFragment(data)

        if (fragmentResult.isFailure) {
            updateUi(fragmentResult.exceptionOrNull()?.desc())
            return
        }

        val newFragment = fragmentResult.getOrNull()!!

        if (qrCodes.isNotEmpty() && qrCodes.size != newFragment.count) {
            updateUi("Fragment count mismatch : expected ${qrCodes.size}, got ${newFragment.count}")

            // restart
            qrCodes.clear()
            return
        }

        if (qrCodes.isEmpty()) {
            for (i in 0 until newFragment.count) {
                qrCodes.add(null)
            }
        }

        qrCodes[newFragment.index] = newFragment
        updateUi()

        if (qrCodes.all { it != null }) {
            val result = extractQrCodeContent(qrCodes)

            when {
                result.isFailure -> updateUi(result.exceptionOrNull()?.desc())
                result.isSuccess -> {
                    setResult(RESULT_OK, Intent().apply {
                        putQrCodeContent(result.getOrThrow())
                    })
                    finish()
                }
            }
        }
    }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) = handleBarcode(result)

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint?>?) {}
    }
}

private const val TAG = "QrCodeScanActivity"
private const val QRCODES_KEY = "opv_qrcodes"