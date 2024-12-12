package net.phbwt.paperwork.data.helper

import android.util.Log
import net.phbwt.paperwork.data.entity.pairing.QrCodeContent
import net.phbwt.paperwork.data.entity.pairing.QrCodeFragment

/**
 * Pairing step 1
 * Extract a fragment from one QR Code
 */
fun extractQrCodeFragment(from: ByteArray): Result<QrCodeFragment> = runCatching {

    if (from.size < FRAGMENT_HEADER_SIZE) {
        return Result.failure(QrCodeException("Too small"))
    }

    val (version, index, count) = from

    if (version > 0) {
        return Result.failure(QrCodeException("Unknown version $version"))
    }

    if (index >= count) {
        return Result.failure(QrCodeException("Inconsistent index $index / count $count"))
    }

    Log.d(TAG, "Decoded QR Code version=$version index=$index count$count")
    return Result.success(QrCodeFragment(index.toInt(), count.toInt(), from.sliceArray(FRAGMENT_HEADER_SIZE until from.size)))
}

/**
 * Extract the content from a set of QR Codes
 */
fun extractQrCodeContent(fragmentsBase: List<QrCodeFragment?>): Result<QrCodeContent> = runCatching {

    // a few consistency checks

    if (fragmentsBase.any { it == null }) {
        return Result.failure(QrCodeException("Missing fragments"))
    }

    val fragments = fragmentsBase.filterNotNull()

    if (fragments.isEmpty()) {
        return Result.failure(QrCodeException("No fragments"))
    }

    if (fragments.any { it.count != fragments.size }) {
        return Result.failure(QrCodeException("Inconsistent fragment count"))
    }

    fragments.forEachIndexed { index, fragment ->
        if (fragment.index != index) {
            return Result.failure(QrCodeException("Inconsistent fragment index"))
        }
    }

    val data = fragments.fold(byteArrayOf()) { acc, fragment -> acc + fragment.content }
    var currentIndex = 0

    fun read1(): Byte = data[currentIndex++]

    // read a 16 bit integer (network order)
    fun read2(): Int = (read1() * 256 + read1())

    fun readN(count: Int): ByteArray = if (count > data.size - currentIndex) {
        throw QrCodeException("Bad count $count, remaining ${data.size - currentIndex}")
    } else {
        ByteArray(count) { read1() }
    }

    fun indexOfNext0OrEnd(): Int {
        for (i in currentIndex until data.size) {
            if (data[i] == 0x00.toByte()) return i
        }
        return data.size
    }

    val serverFingerprint = readN(read2())
    val secret = readN(read2())
    val port = read2()

    val addresses = mutableListOf<String>()
    var next0: Int
    do {
        next0 = indexOfNext0OrEnd()
        addresses.add(readN(next0 - currentIndex).toString(Charsets.UTF_8))
        // skip the 0 byte
        currentIndex++
    } while (next0 < data.size)

    return Result.success(
        QrCodeContent(
            serverFingerprint = serverFingerprint,
            secret = secret,
            port = port,
            serverAddresses = addresses.distinct(),
        )
    )
}

class QrCodeException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

private const val FRAGMENT_HEADER_SIZE = 3

private const val TAG = "QrCodeExtractor"


