@file:OptIn(ExperimentalTextApi::class)

package net.phbwt.paperwork.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import net.phbwt.paperwork.BuildConfig
import net.phbwt.paperwork.R
import net.phbwt.paperwork.ui.main.AppTransitions
import net.phbwt.paperwork.ui.main.Dest
import net.phbwt.paperwork.ui.main.WrappedScaffold
import net.phbwt.paperwork.ui.theme.AppTheme

@Destination(style = AppTransitions::class)
@Composable
fun AboutScreen(
    snackbarHostState: SnackbarHostState,
    onNavigationIcon: (Boolean) -> Unit,
    vm: AboutVM = hiltViewModel(),
) {
    val dbVersion = remember { vm.getDbVersion() }

    AboutContent(
        dbVersion,
        snackbarHostState,
        onNavigationIcon,
    )
}

@Composable
fun AboutContent(
    dbVersion: Int,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigationIcon: (Boolean) -> Unit = {},
) = WrappedScaffold(
    snackbarHostState,
    onNavigationIcon,
    Dest.About.labelRes,
    topLevel = true,
) { modifier ->
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {

        val appName = stringResource(R.string.app_name)

        Text(
            appName,
            modifier = Modifier.padding(8.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            "Version ${BuildConfig.VERSION_NAME}, database version $dbVersion",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            "Copyright 2024 Philippe Banwarth.",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )

        LinkedText(
            str = buildAnnotatedString {
                appendNormal("This program comes with absolutely no warranty. See the ")
                appendLink(
                    url = "https://www.gnu.org/licenses/gpl-3.0.html",
                    text = "GNU General Public License, version 3 or later",
                )
                appendNormal(" for details.")
            },
            modifier = Modifier.padding(8.dp),
        )

        LinkedText(
            str = buildAnnotatedString {
                appendNormal("$appName uses the following libraries, licensed under the ")
                appendLink(
                    url = "https://www.apache.org/licenses/LICENSE-2.0",
                    text = "Apache License, version 2.0",
                )
                appendNormal(" :")
            },
            modifier = Modifier.padding(8.dp),
        )

        LibRow(
            "Accompanist",
            "https://github.com/google/accompanist",
            "A collection of extension libraries for Jetpack Compose.",
        )

        LibRow(
            "Android Jetpack",
            "https://github.com/androidx/androidx",
            "Development environment for Android Jetpack extension libraries under the androidx namespace.",
        )

        LibRow(
            "Coil",
            "https://github.com/coil-kt/coil",
            "Image loading for Android and Compose Multiplatform.",
        )

        LibRow(
            "Compose Destinations",
            "https://github.com/raamcosta/compose-destinations",
            "Annotation processing library for type-safe Jetpack Compose navigation with no boilerplate.",
        )

        LibRow(
            "Dagger",
            "https://github.com/google/dagger",
            "A fast dependency injector for Android and Java.",
        )

        LibRow(
            "kotlin-coroutines-okhttp",
            "https://github.com/gildor/kotlin-coroutines-okhttp",
            "Kotlin Coroutines await() extension for OkHttp Call.",
        )

        LibRow(
            "kotlinx.collections.immutable",
            "https://github.com/Kotlin/kotlinx.collections.immutable",
            "Immutable persistent collections for Kotlin.",
        )

        LibRow(
            "OkHttp",
            "https://github.com/square/okhttp",
            "Square’s meticulous HTTP client for the JVM, Android, and GraalVM.",
        )

        // edge2edge : bottom
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}


@Composable
fun LibRow(name: String, url: String, desc: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(8.dp),
    ) {
        Text(
            name,
            modifier = Modifier
                .clickable { context.showUrl(url) },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        )
        Text(
            desc,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun LinkedText(
    str: AnnotatedString,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
) = ClickableText(
    str,
    modifier = modifier,
    style = MaterialTheme.typography.bodyMedium,
) {
    context.showUrl(str.getUrlAnnotations(it, it).firstOrNull()?.item?.url)
}


@Composable
fun AnnotatedString.Builder.appendLink(
    url: String,
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
) = withAnnotation(UrlAnnotation(url)) {
    withStyle(
        style = SpanStyle(
            color = color,
            textDecoration = TextDecoration.Underline,
        ),
    ) { append(text) }
}

@Composable
fun AnnotatedString.Builder.appendNormal(
    text: String,
    color: Color = LocalContentColor.current,
) = withStyle(style = SpanStyle(color = color)) { append(text) }


private fun Context.showUrl(url: String?) {
    if (url.isNullOrEmpty()) {
        Log.e(TAG, "No URL")
    } else {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}


private const val TAG = "AboutScreen"

//region preview

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        AboutContent(34)
    }
}

//endregion

