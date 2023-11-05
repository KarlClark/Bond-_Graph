import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import userInterface.isShifted
import userInterface.app

fun main() = application {
    Window(title = "lkdfjlafk"
        ,onCloseRequest = ::exitApplication
        , state = WindowState(width=1200.dp, height = 800.dp)
        ,onKeyEvent = {
            println("Key Event,  isShiftPressed = ${it.isShiftPressed}")
            if (it.isShiftPressed) {
                isShifted = true
            }
            false
        }) {

        app()
    }
}
