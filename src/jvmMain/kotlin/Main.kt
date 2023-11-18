import androidx.compose.runtime.*
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import java.nio.file.Path
import androidx.compose.ui.window.AwtWindow
import kotlinx.serialization.*
import userInterface.*
import userInterface.LocalStateInfo
import userInterface.StateInfo
import userInterface.isShifted
import java.awt.FileDialog
import java.io.File
import kotlin.io.path.readText
import kotlin.io.path.writeText


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
                        onResult(File(directory).resolve(file).toPath())
                    } else {
                        onResult(null)
                    }
                }
            }
        }.apply {
            this.title = title
        }
    },
    dispose = FileDialog::dispose
)

@OptIn(ExperimentalSerializationApi::class)
fun main() = application {
    @Composable
    fun mainWindow() {
        var showOpen by remember { mutableStateOf(false) }
        var showSave by remember { mutableStateOf(false) }
        var buildGraph by remember { mutableStateOf(false) }
        lateinit var  bondGraphData: String
        val state = remember { StateInfo() }


        CompositionLocalProvider(
            LocalStateInfo provides state

        ) {
            Window(
                onCloseRequest = ::exitApplication, state = WindowState(width = 1200.dp, height = 800.dp)
                //, icon = (painterResource("one_by_one_pixel.jpg"))

                , onKeyEvent = {
                    println("Key Event,  isShiftPressed = ${it.isShiftPressed}")
                    if (it.isShiftPressed) {
                        isShifted = true
                    }
                    false
                }) {

                MenuBar {
                    Menu("File") {
                        Item("Open...", onClick = { showOpen = true })
                        Item("Save", onClick = { showSave = true })
                    }
                }

                windowBox()
                if (showOpen) fileDialog("Test String", true) {
                    @Composable
                    showOpen = false
                    println("$it")
                    if (it != null) {
                        bondGraphData = it.readText()
                        buildGraph = true
                    }

                }

                if (showSave) fileDialog("Test String", false) {
                    println("$it")
                    showSave = false
                    if (it != null) {
                        it.writeText(bondGraph.toSerializedStrings())
                    }
                }

                if (buildGraph) {
                    buildGraph = false
                    bondGraph.fromSerializedStrings(bondGraphData)
                }
            }
        }
    }

    mainWindow()
}

