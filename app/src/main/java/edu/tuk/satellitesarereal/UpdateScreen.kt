package edu.tuk.satellitesarereal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.tuk.satellitesarereal.ui.viewmodels.UpdateScreenViewModel


@Composable
fun UpdateScreen(updateScreenViewModel: UpdateScreenViewModel) {
    val urls by updateScreenViewModel.urls.observeAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        Modifier.padding(horizontal = 6.dp)
    ) {
        Text("Update Screen")
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("URL") }
            )
            Button(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 3.dp),
                onClick = {
                    updateScreenViewModel.onAddTleUrl(inputText)
                    inputText = ""
                }
            ) {
                Text(text = "Add url")
            }
        }
        Divider(Modifier.padding(vertical = 4.dp))
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
//                    Text(
//                        text = stringResource(id = R.string.remove_item_link),
//                        modifier = Modifier
//                            .align(alignment = Alignment.End)
//                            .padding(8.dp)
//                            .clickable { someViewModel.onRemoveTleUrl(it) }
//                            .border(width = 1.dp, Color.Blue, shape = RoundedCornerShape(3.dp))
//                            .padding(horizontal = 5.dp, vertical = 2.dp),
//                        color = Color.Blue
//                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

//@Composable
//fun PreviewUpdateScreen() {
//    Surface(Modifier.background(Color.White)) {
//        UpdateScreen()
//    }
//}