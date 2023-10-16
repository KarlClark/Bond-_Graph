import androidx.compose.ui.input.key.Key.Companion.Window
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.draganddrop.isShifted
import userInterface.App

fun main() = application {
    Window(title = "lkdfjlafk", onCloseRequest = ::exitApplication
        ,onKeyEvent = {
            println("Key Event,  isShiftPressed = ${it.isShiftPressed}")
            if (it.isShiftPressed) {
                isShifted = true
            }
            false
        }) {
        App()
    }
}
