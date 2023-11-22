package userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.nio.file.Path
import kotlinx.serialization.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

var pathToBondGraphFile: Path? = null

/* TODO
    - rename count variable and move it and newBondId into BondGraph Class
    - grey out elements in sidebar when in bond mode
    - change color of elements when dragging.

 */
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



@OptIn(ExperimentalSerializationApi::class)
fun main() = application {
    @Composable
    fun mainWindow() {
        var open by remember { mutableStateOf(false) }
        var saveAs by remember { mutableStateOf(false) }
        var save by remember { mutableStateOf(false) }
        var exit by remember { mutableStateOf(false) }
        var processAfterSaveAction by remember { mutableStateOf(false) }
        var buildGraph by remember { mutableStateOf(false) }
        var startUp by remember { mutableStateOf(true) }
        var expandMenu by remember { mutableStateOf(false) }
        lateinit var  bondGraphData: String
        val state = remember { StateInfo() }
        val pathToDataFile = getDataFilePath()
        var afterSaveAction by remember {mutableStateOf< (@Composable () -> Unit)?>({println("default")})}


        //var xx by remember{ mutableStateOf(() -> unit = {})}


        CompositionLocalProvider(
            LocalStateInfo provides state

        ) {

            @Composable
            fun myMenuBar() {

                val currentState = LocalStateInfo.current

                Row(
                    Modifier
                        .height(35.dp)
                        .requiredHeightIn(35.dp, 35.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray)
                ) {


                    Column (Modifier
                        .fillMaxHeight()
                        , verticalArrangement = Arrangement.Center
                        //.fillMaxWidth()
                    )

                    {

                        Text ("Files"
                            , fontSize = MyConstants.menuBarFontSize
                            , textAlign = TextAlign.Center
                            , modifier = Modifier
                                .clickable { expandMenu = true }
                                .padding(horizontal = 12.dp)


                        )

                        DropdownMenu(expanded = expandMenu, onDismissRequest = { println("onDismissRequest"); expandMenu = false }) {
                            DropdownMenuItem(onClick = { open = true; expandMenu = false}) { Text("Open", fontSize = MyConstants.menuItemFontSize) }
                            DropdownMenuItem(onClick = { save = true; expandMenu = false }) { Text("Save", fontSize = MyConstants.menuItemFontSize) }
                            DropdownMenuItem(onClick = { saveAs = true; expandMenu = false }) { Text("Save As", fontSize = MyConstants.menuItemFontSize) }
                        }
                    }

                    Text(text = pathToBondGraphFile?.toString() ?: ""
                        , textAlign = TextAlign.Center
                        , fontSize = MyConstants.menuBarFontSize
                        , color = Color.Blue
                        , modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .fillMaxWidth()

                    )
                }
            }



            Window(
                /*onCloseRequest = ::exitApplication*/ onCloseRequest = {exit = true}, state = WindowState(width = 1200.dp, height = 800.dp)
                //, icon = (painterResource("one_by_one_pixel.jpg"))

                , onKeyEvent = {
                    println("Key Event,  isShiftPressed = ${it.isShiftPressed}")
                    if (it.isShiftPressed) {
                        isShifted = true
                    }
                    false
                }) {


                /*Column {
                    Text("Test Text")

                    MenuBar (){
                        Menu("File") {
                            Item("Open...", onClick = { open = true })
                            Item("Save", onClick = { save = true })
                            Item("Save As", onClick = { saveAs = true })
                        }

                        //Text(pathToBondGraphFile?.toString() ?: "")
                    }


                }*/

                Column {

                    myMenuBar()

                    windowBox()

                    @Composable
                    fun processSaveAsDialog() {
                        fileDialog("Test String", false) {
                            println("fileDialog selected path = $it")
                            saveAs = false
                            save = false
                            if (it != null) {
                                println("pathstring = ${it.pathString}  filename = ${it.fileName}}")
                                if (!pathToDataFile.exists()) {
                                    Files.createFile(pathToDataFile)
                                }
                                pathToDataFile.writeText(it.pathString)
                                it.writeText(bondGraph.toSerializedStrings())
                                pathToBondGraphFile = it
                                bondGraph.graphHasChanged = false
                                processAfterSaveAction = true
                            }
                        }
                    }

                    @Composable
                    fun processSave() {
                        //save = false
                        println("processSave, pathToBondGraphFile = $pathToBondGraphFile")
                        if (pathToBondGraphFile == null) {
                            processSaveAsDialog()
                            println("processSave saveAsDialog processed")
                        } else {
                            pathToBondGraphFile?.writeText(bondGraph.toSerializedStrings())
                            bondGraph.graphHasChanged = false
                            afterSaveAction?.invoke()
                            save = false
                        }
                    }

                    if (open) {
                        if (bondGraph.graphHasChanged) {
                            afterSaveAction = { open = true }
                            state.showSaveFileDialog = true
                            open = false
                        } else {
                            fileDialog("Test String", true) {
                                @Composable
                                open = false
                                println("fileDialog path = $it")
                                if (it != null) {
                                    println("pathstring = ${it.pathString}  filename = ${it.fileName}}")
                                    if (!pathToDataFile.exists()) {
                                        Files.createFile(pathToDataFile)
                                    }
                                    pathToDataFile.writeText(it.pathString)
                                    bondGraphData = it.readText()
                                    buildGraph = true
                                    pathToBondGraphFile = it
                                }

                            }
                        }
                    }

                    if (saveAs) {
                        processSaveAsDialog()
                    }

                    if (save) {
                        println("calling processSave()")
                        processSave()
                    }

                    if (processAfterSaveAction) {
                        println("processAfterSaveAction afterSaveAction= $afterSaveAction")
                        //afterSaveAction = {println("test")}
                        afterSaveAction?.invoke()
                        processAfterSaveAction = false
                    }

                    if (exit) {
                        if (bondGraph.graphHasChanged) {
                            afterSaveAction = { exitApplication() }
                            state.showSaveFileDialog = true;
                            exit = false
                        } else {
                            exitApplication()
                        }
                    }

                    if (buildGraph) {
                        buildGraph = false
                        bondGraph.fromSerializedStrings(bondGraphData)
                    }

                    if (startUp) {
                        startUp = false
                        if (pathToDataFile.exists()) {
                            val pathString = pathToDataFile.readText()
                            val path = Paths.get(pathString)
                            val data = path.readText()
                            pathToBondGraphFile = path
                            bondGraph.fromSerializedStrings(data)
                            println("startUp, assigning graphHasChanged to false")
                            bondGraph.graphHasChanged = false
                        }
                    }

                    if (state.clearGraph) { // Clear the work area of the current bond graph drawing.
                        println("if clearGraph  graphHasChanged = ${bondGraph.graphHasChanged}")
                        if (bondGraph.graphHasChanged) {
                            afterSaveAction = {
                                println("afteSaveAction")
                                bondGraph.clear()
                                pathToBondGraphFile = null
                            }
                            state.showSaveFileDialog = true

                            state.clearGraph = false
                        } else {
                            bondGraph.clear()
                            pathToBondGraphFile = null
                            state.clearGraph = false
                        }
                    }

                    if (state.showSaveFileDialog) {

                        saveFileDialog(
                            onSave = {
                                println("save")
                                save = true
                                state.showSaveFileDialog = false
                            },
                            onDontSave = {
                                println("Don't Save")
                                bondGraph.graphHasChanged = false
                                state.showSaveFileDialog = false
                                processAfterSaveAction = true
                            },
                            onCancel = {
                                println("Cancel")
                                state.clearGraph = false
                                state.showSaveFileDialog = false
                            },
                            onCloseRequest = {
                                println("Close Request")
                                state.clearGraph = false
                                state.showSaveFileDialog = false
                            }
                        )
                    }

                    /* val pathname = System.getProperties().getProperty("user.home")
                println("${pathname}")*/

                }
            }
        }
    }

    mainWindow()
}

