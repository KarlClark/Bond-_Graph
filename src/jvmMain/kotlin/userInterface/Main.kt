package userInterface

import algebra.testCases
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
val runTestCases = true
val runTestCases_2 = false

/* TODO
    - grey out elements in sidebar when in bond mode
    - dialog to prevent saving a blank bond graph
    - simplify a bond graph

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
    val path =  Paths.get(pathString)
    return path
}



//@OptIn(ExperimentalSerializationApi::class)
fun main() = application {
    val state = remember { StateInfo() }
    CompositionLocalProvider(LocalStateInfo provides state) {

        val currentState = LocalStateInfo.current
        var showDialog by remember { mutableStateOf(false) }
        var save by remember {mutableStateOf(false)}
        var saveAs by remember { mutableStateOf(false) }
        var dontSave by remember { mutableStateOf(false) }

        @Composable
        fun runWindows() {



            if (currentState.exit) {
                println("exit function valuesSetHasChanged = ${bondGraph.valuesSetHasChanged}")
                if (bondGraph.valuesSetHasChanged){
                    currentState.exit = false
                    showDialog = true
                } else
                    if (bondGraph.graphHasChanged) {

                        currentState.afterSaveAction = { exitApplication() }
                        state.showSaveFileDialog = true;
                        currentState.exit = false
                    } else {
                    exitApplication()
                }
            }

            mainWindow()
        }

        runWindows()

        if (state.showValuesWindow) {
            println("displa values window")
            valuesWindow()
        }

        if(state.showResultsWindow){
            println("show results window")
            currentState.resultsWindowState.isMinimized = false


            resultsWindow()
            currentState.resultsWindowOnTop = true
            currentState.resultsWindowOnTop = false
        }

        if (showDialog) {
            saveDialog(message = "Save this Values Set?"
                ,onSave = {
                    save = true
                    showDialog = false
                }
                , onSaveAs = {
                    saveAs = true
                    showDialog = false
                }
                , onDontSave = {
                    dontSave = true
                    showDialog = false
                }
                , onCancel = {
                    showDialog = false
                }
                , onCloseRequest = {
                    showDialog = false
                }
            )
        }

        if (save){
            save = false
            saveFunction {
                println("finish action")
                currentState.exit = true
            }
        }

        if (saveAs) {
            saveAsFunction {
                saveAs = false
                currentState.exit = true
            }
        }

        if (dontSave) {
            dontSave = false
            bondGraph.valuesSetHasChanged = false
            currentState.exit = true
        }
    }
}

