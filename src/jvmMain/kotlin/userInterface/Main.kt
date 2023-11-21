package userInterface

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

var pathToBondGraphFile: Path? = null

/* TODO
    1. Add .bdgh extension to file name.
    2. test saving partial bond graph
    3.delete results list when opening a new file
    4. put file name in menu bar
    5.zero out counts after clear
    6. null out pathToBondGraph after clear
    7. check for unsaved file when user request opening a file

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
        lateinit var  bondGraphData: String
        val state = remember { StateInfo() }
        val pathToDataFile = getDataFilePath()
        var afterSaveAction by remember {mutableStateOf< (@Composable () -> Unit)?>({println("default")})}


        //var xx by remember{ mutableStateOf(() -> unit = {})}




        CompositionLocalProvider(
            LocalStateInfo provides state

        ) {



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



                MenuBar {
                    Menu("File") {
                        Item("Open...", onClick = { open = true })
                        Item("Save", onClick = {save = true} )
                        Item("Save As", onClick = { saveAs = true })
                    }
                }



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
                            bondGraph.graphHasChanged= false
                            processAfterSaveAction = true
                        }
                    }
                }

                @Composable
                fun processSave(){
                    //save = false
                    println("processSave, pathToBondGraphFile = $pathToBondGraphFile")
                    if (pathToBondGraphFile == null){
                        processSaveAsDialog()
                        println("processSave saveAsDialog processed")
                    } else {
                        pathToBondGraphFile?.writeText(bondGraph.toSerializedStrings())
                        bondGraph.graphHasChanged = false
                        afterSaveAction?.invoke()
                        save = false
                    }
                }

                if (open) fileDialog("Test String", true) {
                    @Composable
                    open = false
                    println("fileDialog path = $it")
                    if (it != null) {
                        println("pathstring = ${it.pathString}  filename = ${it.fileName}}")
                        if (! pathToDataFile.exists()){
                            Files.createFile(pathToDataFile)
                        }
                        pathToDataFile.writeText(it.pathString)
                        bondGraphData = it.readText()
                        buildGraph = true
                        pathToBondGraphFile = it
                    }

                }

                if (saveAs) {
                    processSaveAsDialog()
                }

                if (save) {
                    println("calling processSave()")
                    processSave()
                }

                if (processAfterSaveAction){
                    println("processAfterSaveAction afterSaveAction= $afterSaveAction")
                    //afterSaveAction = {println("test")}
                    afterSaveAction?.invoke()
                    processAfterSaveAction = false
                }

                if (exit){
                    if (bondGraph.graphHasChanged){
                        afterSaveAction = {exitApplication()}
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

                if (startUp){
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
                    //state.clearGraph = false
                    //bondGraph.clear()
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

                if (state.showSaveFileDialog){

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

    mainWindow()
}

