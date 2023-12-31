package userInterface

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState

@Composable
fun valuesWindow() {

    val currentState = LocalStateInfo.current

    Window(
        onCloseRequest = {currentState.showValuesWindow = false}
        ,state = WindowState(width = 1200.dp, height = 800.dp)

    ) {

    }
}