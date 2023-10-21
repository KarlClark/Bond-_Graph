package userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import bondgraph.BondGraph
import bondgraph.ElementTypes
import bondgraph.GraphElementDisplayData
import com.example.draganddrop.*

//val graphElementTypeNames = arrayListOf<String>("0", "1", "C", "I", "R", "TF", "GY", "MTF")
val bondGraph = BondGraph("test graph")

//val graphElementDisplayDataMap    = linkedMapOf<Int, GraphElementDisplayData>()
var textColor by mutableStateOf(Color.Black)
object MyConstants {
    val elementNameFontsize: TextUnit =20.sp
    val labelFontsize: TextUnit = 15.sp
    val myGreen = Color(10, 140, 10)
}


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
    var bondModeColor by remember { mutableStateOf (Color.LightGray)}
    var nodeModeColor by remember { mutableStateOf( MyConstants.myGreen)}
    Box(Modifier.fillMaxHeight()
        .background(Color.LightGray)
    ) {
        Column(
            Modifier
                .padding(2.dp)
                .fillMaxHeight().width(60.dp).fillMaxWidth()

            ,horizontalAlignment = Alignment.CenterHorizontally
            ,verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically)
            //,verticalArrangement = Arrangement.Center
        )
        {

            Column (Modifier
                .background(Color.White)
                .fillMaxWidth()
                ,horizontalAlignment = Alignment.CenterHorizontally
                ,verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically)

            ){

                Text("Bond\nMode", color = bondModeColor, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top= 10.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (currentState.mode == Mode.ELEMENT_MODE && bondGraph.getElementsMap().size >= 2) {
                                        bondModeColor = MyConstants.myGreen
                                        nodeModeColor = Color.LightGray
                                        currentState.mode = Mode.BOND_MODE
                                    }
                                }
                            )

                        }
                )

                Text("Node\nMode", color = nodeModeColor, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (currentState.mode == Mode.BOND_MODE) {
                                        nodeModeColor = MyConstants.myGreen
                                        bondModeColor = Color.LightGray
                                        currentState.mode = Mode.ELEMENT_MODE
                                    }
                                }
                            )
                        }
                )
            }

            var id = 1000
            enumValues<ElementTypes>().forEach {
                displayElement(GraphElementDisplayData(id++, it.displayString(), 0f, 0f, 0f,0f, Offset(0f, 0f)))
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
