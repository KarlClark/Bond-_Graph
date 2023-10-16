import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.draganddrop.*

val bondGraphMap = linkedMapOf<String,Color>("0" to Color.Blue, "1" to Color.Magenta, "C" to Color.Yellow, "I" to Color.Cyan)
val graphNodenames = arrayListOf<String>("0", "1", "C", "I", "R", "TF", "GY", "MTF")
val graphNodeDisplayDataMap    = linkedMapOf<Int, GraphNodeDisplayData>()
var textColor by mutableStateOf(Color.Black)
object MyConstants {
    val nodeFontsize: TextUnit =20.sp
    val nodeBoxSize: Dp = 45.dp

}

class GraphNodeDisplayData (val id: Int, var text: String, val x: Float, val y: Float)
fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: (Modifier.() -> Modifier)? = null,
): Modifier {
    return if (condition) {
        then(ifTrue(Modifier))
    } else if (ifFalse != null) {
        then(ifFalse(Modifier))
    } else {
        this
    }
}

@Composable
fun textColumn() {

    val currentState = LocalDragTargetInfo.current

    Box(Modifier.fillMaxHeight()
        .background(Color.LightGray)
    ) {
        Column(
            Modifier
                .padding(5.dp)
                .fillMaxHeight().width(60.dp).fillMaxWidth()

            ,horizontalAlignment = Alignment.CenterHorizontally
            ,verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically)
            //,verticalArrangement = Arrangement.Center
        )
        {

            Text ("Bond\nMode", color = textColor,
                modifier=Modifier
                    .padding(bottom =20.dp)
                    .pointerInput(Unit){detectTapGestures (
                        onTap ={
                            if (currentState.mode == Mode.NODE_MODE) {
                                textColor = Color.Blue
                                currentState.mode = Mode.BOND_MODE
                            } else{
                                textColor = Color.Black
                                currentState.mode = Mode.NODE_MODE
                            }

                        }
                    )
                    }
            )
            var id = 1000
            for (entry in graphNodenames) {
                displayNode(GraphNodeDisplayData(id++, entry, 0f, 0f))
            }
        }
    }
}



@Composable
fun windowBox() {
    Draggable (Modifier
        //.background(color = Color.Blue)
        .fillMaxSize()){
        Row(Modifier
            .fillMaxSize()
            .background(color = Color.Red)
        ) {
            textColumn()
            DropTarget(modifier=Modifier.background(color = Color.Yellow)
                .fillMaxSize()
            )
        }
    }
}

@Composable
fun App() {
    windowBox()
}

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
