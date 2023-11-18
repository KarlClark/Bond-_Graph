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
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun getDataFilePath(): Path{
    val separator = System.getProperty("file.separator")
    //val pathString = System.getenv("LocalAppData") + separator +"Bond_Graph" + separator + "filename.txt"
    val directory = System.getenv("LocalAppData") + separator + "Bond_Graph"
    val directoryPath = Paths.get(directory)
    if (! directoryPath.exists()){
        Files.createDirectories(directoryPath)
    }
    val pathString = directory + separator + "filename.txt"
    println("$pathString")
    val path =  Paths.get(pathString)
    println("path = $path")
    return path
}
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
        var startUp by remember { mutableStateOf(true) }
        lateinit var  bondGraphData: String
        val state = remember { StateInfo() }
        val dataFilePath = getDataFilePath()

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
                        println("pathstring = ${it.pathString}  filename = ${it.fileName}}")
                        if (! dataFilePath.exists()){
                            Files.createFile(dataFilePath)
                        }
                        dataFilePath.writeText(it.pathString)
                        bondGraphData = it.readText()
                        buildGraph = true
                    }

                }

                if (showSave) fileDialog("Test String", false) {
                    println("$it")
                    showSave = false
                    if (it != null) {
                        println("pathstring = ${it.pathString}  filename = ${it.fileName}}")
                        if (! dataFilePath.exists()){
                            Files.createFile(dataFilePath)
                        }
                        dataFilePath.writeText(it.pathString)
                        it.writeText(bondGraph.toSerializedStrings())
                    }
                }

                if (buildGraph) {
                    buildGraph = false
                    bondGraph.fromSerializedStrings(bondGraphData)
                }

                if (startUp){
                    startUp = false
                    if (dataFilePath.exists()) {
                        val pathString = dataFilePath.readText()
                        val path = Paths.get(pathString)
                        val data = path.readText()
                        bondGraph.fromSerializedStrings(data)
                    }
                }
               /* val pathname = System.getProperties().getProperty("user.home")
                println("${pathname}")*/

            }
        }
    }

    mainWindow()
}

