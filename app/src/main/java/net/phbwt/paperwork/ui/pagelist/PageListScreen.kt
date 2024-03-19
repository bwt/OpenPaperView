package net.phbwt.paperwork.ui.pagelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import net.phbwt.paperwork.ui.main.AppTransitions
import net.phbwt.paperwork.ui.main.EmptyScaffold

data class PageListScreenArgs(
    val documentId: Int,
)

@Destination(
    navArgsDelegate = PageListScreenArgs::class,
    style = AppTransitions::class,
)
@Composable
fun PageListScreen(
    snackbarHostState: SnackbarHostState,
    onNavigationIcon: (Boolean) -> Unit,
    vm: PageListVM = hiltViewModel(),
) {
    val document by vm.document.collectAsStateWithLifecycle()

    val selectedIndex by remember { mutableIntStateOf(0) }

    val pdfFile = vm.getPdfLocalPath(document)

    // Scaffold is only needed for the snackbar
    EmptyScaffold(
        snackbarHostState,
    ) { innerPadding ->

        // ensure fillMaxSize, even when the content is not ready
        // to avoid spurious automatic scaleIn
        // cf https://issuetracker.google.com/issues/295536728
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when {
                document == null -> {
                    // nothing
                }

                document!!.isImagesDoc -> {
                    PageListContentImages(document, selectedIndex)
                }

                pdfFile != null && pdfFile.exists() -> {
                    PageListContentPdf(pdfFile)
                }

                else -> {
                    throw IllegalStateException("Should not happen : exists : ${pdfFile?.exists()} '${document}'")
                }
            }
        }
    }
}

private const val TAG = "PageListScreen"