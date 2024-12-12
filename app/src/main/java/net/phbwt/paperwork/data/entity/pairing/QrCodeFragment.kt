package net.phbwt.paperwork.data.entity.pairing

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/**
 * Content of each QR code displayed by Paperwork.
 */
@Parcelize
data class QrCodeFragment(
    val index: Int,
    val count: Int,
    val content: ByteArray,
) : Parcelable

/**
 * Decoded content of the set of QR Codes
 */
data class QrCodeContent(
    val serverFingerprint: ByteArray,
    val secret: ByteArray,
    val port: Int,
    val serverAddresses: List<String>,
) {

    // IPv6 addresses in URLs must by quoted with square brackets
    fun toUrl(address: String) = if (address.contains(':')) "https://[${address}]:${port}/" else "https://${address}:${port}"


}
