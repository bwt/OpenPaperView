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
@Parcelize
data class QrCodeContent(
    val serverFingerprint: ByteArray,
    val secret: ByteArray,
    val port: Int,
    val serverAddresses: List<String>,
) : Parcelable {

    // IPv6 addresses in URLs must by quoted with square brackets
    fun toUrl(address: String) = if (address.isUnquotedIPv6()) "https://[${address}]:${port}/" else "https://${address}:${port}"
}

private fun String.isUnquotedIPv6(): Boolean = this.contains(':') && !this.startsWith('[') && !this.endsWith(']')
