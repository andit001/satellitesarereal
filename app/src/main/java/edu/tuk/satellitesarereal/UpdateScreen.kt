package edu.tuk.satellitesarereal

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.tuk.satellitesarereal.ui.viewmodels.UpdateScreenViewModel


@Composable
fun UpdateScreen(viewModel: UpdateScreenViewModel) {
    val urls by viewModel.urls.observeAsState()
    val fileList by viewModel.fileList.observeAsState()

    var inputText by remember { mutableStateOf("") }

    val scrollState = ScrollState(0)

    Column(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .verticalScroll(scrollState)
    ) {
        UrlInput(inputText, viewModel) {
            inputText = it
        }
        Spacer(Modifier.height(5.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { urls?.let { viewModel.onDownloadFiles(it) } }
        ) {
            Text(text = stringResource(R.string.download_files_button))
        }

        Spacer(Modifier.height(5.dp))

        urls?.let {
            if (it.isEmpty()) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.onLoadDefaultUrls() }
                ) {
                    Text(stringResource(R.string.load_default_sources_button))
                }
            }
        }

        UrlCardList(urls, viewModel)

        Spacer(Modifier.height(5.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { fileList?.forEach { viewModel.onDeleteFile(it) } }
        ) {
            Text(text = stringResource(R.string.delete_files_button))
        }

        fileList?.forEach { Text(it) }
    }
}

@Composable
private fun UrlCardList(
    urls: List<String>?,
    updateScreenViewModel: UpdateScreenViewModel,
) {
    urls?.forEach {
        Card(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.Gray),
            elevation = 5.dp
        ) {
            Column {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 12.dp)
                )
                OutlinedButton(
                    onClick = { updateScreenViewModel.onRemoveTleUrl(it) },
                    border = BorderStroke(1.dp, MaterialTheme.colors.primary),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 5.dp, end = 5.dp),
                ) {
                    Text(stringResource(R.string.remove_item_link))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun UrlInput(
    inputText: String,
    updateScreenViewModel: UpdateScreenViewModel,
    onInputTextUpdate: (value: String) -> Unit,
) {
    val context = LocalContext.current
    val invalidUrlGiven = stringResource(R.string.invalid_url_input)

    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { onInputTextUpdate(it) },
            label = { Text(stringResource(R.string.url_textfield)) }
        )
        Button(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 3.dp),
            onClick = {
                try {
                    updateScreenViewModel.onAddTleUrl(inputText)
                } catch (e: UpdateScreenViewModel.MalformedUrlException) {
                    Toast.makeText(
                        context,
                        invalidUrlGiven,
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                onInputTextUpdate("")
            }
        ) {
            Text(text = stringResource(R.string.add_url_button))
        }
    }
}

//@Composable
//fun PreviewUpdateScreen() {
//    Surface(Modifier.background(Color.White)) {
//        UpdateScreen()
//    }
//}