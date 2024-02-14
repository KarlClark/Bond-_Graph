package userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.rememberDialogState
import java.awt.FileDialog
import java.io.File
import java.nio.file.Path

@Composable
fun FrameWindowScope.fileDialog(
    title: String,
    isLoad: Boolean,
    onResult: (result: Path?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(window, "Choose a file", if (isLoad) LOAD else SAVE) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    if (file != null) {
                        if ( ! isLoad) {
                            val index = file.lastIndexOf(".")
                            if (index > 0) {
                                file = file.substring(0,index)
                            }
                            file = file  + ".bdgh"
                        }
                        onResult(File(directory).resolve(file).toPath())
                    } else {
                        onResult(null)
                    }
                }
            }
        }.apply {
            this.title = title
            if(! isLoad) {
                this.file = ".bdgh"
            }
        }
    },
    dispose = FileDialog::dispose
)

@Composable
fun oneButtonAlertDialog(message: String, buttonText: String, onClick: () -> Unit, onCloseRequest: () -> Unit) {

    val dialogState = rememberDialogState(size = DpSize(MyConstants.smallDialogWidth, MyConstants.smallDialogHeight))

    Dialog(visible = true
        , onCloseRequest = {onCloseRequest()}
        , state = dialogState

    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp)
            //.background(Color.Red)
            //,shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                //.background(Color.Blue)
                ,verticalArrangement = Arrangement.Center
                ,horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                )

                TextButton(onClick = onClick){
                    Text(buttonText)
                }
            }

        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun enterTextDialog(message: String, currentText: String, onSubmit: (text: String) -> Unit, onCloseRequest: () -> Unit) {

    var inputString by remember {mutableStateOf(currentText)}
    val dialogState = rememberDialogState(size = DpSize(MyConstants.smallDialogWidth, MyConstants.smallDialogHeight))
    val focusRequester = FocusRequester()

    Dialog(visible = true
        , onCloseRequest = {onCloseRequest()}
        , state = dialogState

    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp)
                //.background(Color.Red)
            //,shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    //.background(Color.Blue)
                ,verticalArrangement = Arrangement.Center
                ,horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                )

                BasicTextField(modifier = Modifier
                    .width(200.dp)
                    .onKeyEvent {
                        if (it.key == Key.Enter) {
                            onSubmit(inputString)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    , value = inputString
                    , onValueChange = {newtext ->
                        inputString = buildString {
                            newtext.forEach {
                                if ( ! (it == '\t' || it == '\n')) append (it)
                            }
                        }

                    }

                )
                Divider(
                    thickness = 1.dp,
                    color = Color.Black,
                    modifier = Modifier
                        .absolutePadding(right = MyConstants.valuesGeneralPadding)
                        .width(200.dp)
                )
            }

        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
@Composable
fun saveDialog(
    message:String,
    onSave:  () -> Unit,
    onSaveAs: () -> Unit,
    onDontSave: () -> Unit,
    onCancel: () -> Unit,
    onCloseRequest: () -> Unit,
) {

    val dialogState = rememberDialogState(size = DpSize(MyConstants.largeDialogWidth, MyConstants.largeDialogHeight))

    Dialog(

        visible = true
        ,onCloseRequest = {onCloseRequest()}
        , state = dialogState
    ) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onSave() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Save")
                    }

                    TextButton(
                        onClick = { onSaveAs() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Save As")
                    }

                    TextButton(
                        onClick = { onDontSave() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Don't Save")
                    }

                    TextButton(
                        onClick = { onCancel() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
