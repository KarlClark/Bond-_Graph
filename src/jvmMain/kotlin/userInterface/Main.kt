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
        @Composable
        fun runWindows() {

            val currentState = LocalStateInfo.current

            if (currentState.exit) {
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
    }
}

