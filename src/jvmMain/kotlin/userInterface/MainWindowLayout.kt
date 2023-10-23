package userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
    val subTextFontsize: TextUnit = 15.sp
    val labelFontsize: TextUnit = 15.sp
    val bottomBarFontSize = 15.sp
    val resultsFontSize = 15.sp
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
        .background(Color.Blue)
    ) {
        Column(
            Modifier
                .padding(2.dp)
                //.fillMaxHeight().width(80.dp).fillMaxWidth()
                .width(IntrinsicSize.Max)
                .fillMaxHeight()

            ,horizontalAlignment = Alignment.CenterHorizontally
            ,verticalArrangement = Arrangement.spacedBy(2.dp, alignment = Alignment.Top)
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

            Column (
                 horizontalAlignment = Alignment.CenterHorizontally
                ,verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically)
                ,modifier= Modifier
                    .background(Color.LightGray)
                    .padding(2.dp)
                    .fillMaxWidth()
                    //.fillMaxHeight().width(60.dp).fillMaxWidth()
                    .wrapContentHeight()


                //, verticalArrangement = Arrangement.Bottom

            ){
                var id = 1000
                enumValues<ElementTypes>().forEach {
                    displayElement(GraphElementDisplayData(id++, it.displayString(), 0f, 0f, 0f, 0f, Offset(0f, 0f)))
                }
            }

            Column (
                Modifier
                    .background(Color.White)
                    .padding(2.dp)
                    //.fillMaxHeight().width(60.dp).fillMaxWidth()
                    .wrapContentWidth()

                ,horizontalAlignment = Alignment.CenterHorizontally
                ,verticalArrangement = Arrangement.spacedBy(5.dp, alignment = Alignment.CenterVertically)
            ){

                Button (onClick = {
                    println ("Button click")
                    bondGraph.augment()
                }){
                    Text("Augment")
                }
                Button (onClick = {println ("Button click")}){
                    Text("Derive")
                }
            }

        }
    }
}

/*@Composable
fun textBox(text: String) {
    Box (modifier = Modifier
        //.fillMaxHeight()
        .background(Color.Cyan)
        .wrapContentSize()
    ) {
        Text(text = text
            //,textAlign = TextAlign.Center
            ,fontSize = MyConstants.bottomBarFontSize
            ,color = Color.Black
            , modifier = Modifier
                //.fillMaxHeight()
                //.wrapContentHeight(align = Alignment.CenterVertically)
                //.align(Alignment.Center)
        )
    }
}*/
@Composable
fun bottomBar() {

    val currentState = LocalDragTargetInfo.current

    Row(Modifier
        .height(60.dp)
        //.heightIn(60.dp, 60.dp)
        .requiredHeightIn(60.dp, 60.dp)
        .fillMaxWidth()
        .background(Color.DarkGray)
    ){

        Text("Results"
            , fontSize = MyConstants.bottomBarFontSize
            , color = Color.White
            , modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 5.dp )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            println("Results tap")
                            currentState.showResults = !currentState.showResults
                        }
                    )
                 }
        )
    }
}

@Composable
fun windowBox() {

    val currentState = LocalDragTargetInfo.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Magenta)
    ) {

        Draggable(
            Modifier
                .background(color = Color.Blue)
                .fillMaxWidth()
        ) {
            Row(
                Modifier
                    //.fillMaxSize()
                    .background(color = Color.Red)
            ) {
                textColumn()
                DropTarget(
                    modifier = Modifier.background(color = Color.Yellow)
                        .fillMaxSize()
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5F)
                .background(Color.Cyan)
                //.align(All)

        ) {
            Text(text = "jgfkj")
        }
        if (currentState.showResults) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(800.dp)
                    .background(Color.Cyan)
            ) {

                Row(
                    modifier = Modifier
                        .background(Color.Gray)
                        .requiredHeight(30.dp)
                        .fillMaxWidth()
                        .weight(.1f, true)
                ){}
                Column(modifier = Modifier
                    .weight(3f, true)

                ) {
                    bondGraph.resultsList.forEach {
                        Text(it
                            ,fontSize = MyConstants.resultsFontSize
                            , modifier = Modifier
                                .padding(start = 10.dp, top = 5.dp)
                        )
                    }
                }
            }
        }
        bottomBar()
    }

}

@Composable
fun App() {
    windowBox()
}
