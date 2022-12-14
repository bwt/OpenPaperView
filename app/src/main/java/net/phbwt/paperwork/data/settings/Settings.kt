package net.phbwt.paperwork.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.phbwt.paperwork.helper.mapResultFlow
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.tls.HeldCertificate
import okhttp3.tls.decodeCertificatePem
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val BASE_URL = stringPreferencesKey("base_url")
private val SERVER_CA = stringPreferencesKey("server_ca")
private val CLIENT_PEM = stringPreferencesKey("client_pem")


@Singleton
class Settings @Inject constructor(
    @ApplicationContext private val ctxt: Context,
) {

    //region stored preferences

    val baseUrlStr: Flow<String> = ctxt.dataStore.data
        .map { it[BASE_URL] ?: "" }
        .distinctUntilChanged()

    val clientPemStr: Flow<String> = ctxt.dataStore.data
        .map { it[CLIENT_PEM] ?: "" }
        .distinctUntilChanged()

    val serverCaStr: Flow<String> = ctxt.dataStore.data
        .map { it[SERVER_CA] ?: "" }
        .distinctUntilChanged()

    //endregion

    //region derived shared objects (as Result)

    val baseUrl: Flow<Result<HttpUrl>> = baseUrlStr.map {
        runCatching { it.toHttpUrl() }
    }

    val contentBaseUrl = baseUrl.mapResultFlow { it.newBuilder().addPathSegment("papers").build() }

    // Request (without body) are immutable, so we can reuse it
    val dbRequest = baseUrl.mapResultFlow { baseUrl ->
        val dbUrl = baseUrl.newBuilder().addPathSegment("papers.sqlite").build()

        Request.Builder()
            .cacheControl(
// we want to use the cache (not to download the db unnecessarily)
// but also ignore max-age (to force a check on server)
                CacheControl.Builder()
                    .maxAge(0, TimeUnit.SECONDS)
                    .build()
            )
            .url(dbUrl)
            .build()
    }

    val clientPem: Flow<Result<HeldCertificate?>> = clientPemStr.map {
        runCatching { if (it.isNotBlank()) HeldCertificate.decode(it.trim()) else null }
    }

    val serverCa: Flow<Result<X509Certificate?>> = serverCaStr.map {
        runCatching { if (it.isNotBlank()) it.trim().decodeCertificatePem() else null }
    }

    //endregion

    // Coil image cache
    val imageCache = ctxt.cacheDir.resolve("image_cache")

    // OkHttp db cache
    val dbCache = ctxt.cacheDir.resolve("db_cache")

    // OkHttp settings checks cache
    val checksCache = ctxt.cacheDir.resolve("checks_cache")

    // Downloaded image and pdf
    // @see xml/file_provider_paths.xml
    val localPartsDir = ctxt.filesDir.resolve("local_files/parts")

    suspend fun updateBaseUrl(newVal: String) = update(BASE_URL, newVal)
    suspend fun updateClientPem(newVal: String) = update(CLIENT_PEM, newVal)
    suspend fun updateServerCa(newVal: String) = update(SERVER_CA, newVal)

    private suspend fun update(key: Preferences.Key<String>, newVal: String) =
        ctxt.dataStore.edit { settings ->
            settings[key] = newVal
        }

}
