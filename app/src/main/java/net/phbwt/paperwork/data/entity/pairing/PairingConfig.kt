package net.phbwt.paperwork.data.entity.pairing

import kotlinx.serialization.Serializable

/**
 * Content of the HTTP response from the server.
 * JSON encoded
 */
@Serializable
data class PairingConfig(
    val client: ConfigClient,
    val server: ConfigServer,
)

@Serializable
data class ConfigClient(
    val certificate: String,
    val key: String,
)

@Serializable
data class ConfigServer(
    val certificate: String,
)

