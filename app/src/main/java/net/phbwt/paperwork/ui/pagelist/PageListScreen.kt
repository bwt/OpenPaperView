package net.phbwt.paperwork.ui.pagelist

import androidx.compose.runtime.*
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

private const val TAG = "PageListScreen"