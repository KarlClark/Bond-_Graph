import androidx.compose.runtime.*
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import userInterface.isShifted
import userInterface.windowBox
import java.nio.file.Path
import androidx.compose.ui.window.AwtWindow
import bondgraph.BondGraphSerializationData
import bondgraph.BondSerializatonData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import userInterface.MyConstants
import userInterface.bondGraph
import java.awt.FileDialog
import java.io.File

val openDialog = DialogState<Path?>()
var testSer: String = ""

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


suspend fun open() {

        val path = openDialog.awaitResult()
        if (path != null) {
            println("$path")
        }
}

class DialogState<T> {
    private var onResult: CompletableDeferred<T>? by mutableStateOf(null)

    val isAwaiting get() = onResult != null

    suspend fun awaitResult(): T {
        onResult = CompletableDeferred()
        val result = onResult!!.await()
        onResult = null
        return result
    }

    fun onResult(result: T) = onResult!!.complete(result)


}
fun main() = application {
    @Composable
    fun mainWindow() {
        var showOpen by remember { mutableStateOf(false) }
        var showClose by remember { mutableStateOf(false) }
        var buildGraph by remember { mutableStateOf(false) }
        lateinit var  bondGrraphData: BondGraphSerializationData
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
                    Item("Open...", onClick = {showOpen = true})
                    Item("Save", onClick = {showClose = true})
                }
            }

            windowBox()
            if (showOpen)  fileDialog("Test String", true) {
                showOpen = false
                bondGrraphData = bondGraph.toSerializedStrings()

            }

            if (showClose)  fileDialog("Test String", false) {@Composable
                showClose = false
                println("$it")
                buildGraph = true
            }

            if (buildGraph){
                buildGraph = false
                bondGraph.fromSerializedStrings(bondGrraphData)
            }
        }
    }

    mainWindow()
}

