package net.phbwt.paperwork.ui.pagelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController


@Composable
fun PageListScreen(
    navController: NavController,
    documentId: Int,
    vm: PageListVM = hiltViewModel(),
) {
    val document by vm.loadDocument(documentId).collectAsStateWithLifecycle(null)

    val selectedIndex by remember { mutableIntStateOf(0) }

    val pdfFile = vm.getPdfLocalPath(document)

    // ensure fillMaxSize, even when the content is not ready
    // to avoid spurious automatic scaleIn
    // cf https://issuetracker.google.com/issues/295536728
    Box(modifier = Modifier.fillMaxSize()) {
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

private const val TAG = "PageListScreen"