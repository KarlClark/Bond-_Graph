package userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import bondgraph.BondGraph
import bondgraph.ElementTypes
import bondgraph.GraphElementDisplayData

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
    val graphBackground = Color(242, 215, 140)
    val resultsBackground = Color(206, 232, 246)
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

    val currentState = LocalStateInfo.current
    var bondModeColor by remember { mutableStateOf (Color.LightGray)}
    var nodeModeColor by remember { mutableStateOf( MyConstants.myGreen)}


    Box(Modifier.fillMaxHeight()
        .background(Color.Gray)
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
                    displayElement(GraphElementDisplayData(id++, it.toAnnotatedString(), 0f, 0f, 0f, 0f, Offset(0f, 0f)))
                    if (it == ElementTypes.ONE_JUNCTION || it == ElementTypes.MODULATED_TRANSFORMER) Divider(thickness = 2.dp, color = Color.Black)
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

                Button (colors = ButtonDefaults.buttonColors(backgroundColor = MyConstants.myGreen, contentColor = Color.White)
                    ,onClick = {
                    currentState.augment = true
                }


                ){
                    Text("Augment")
                }
                Button (colors = ButtonDefaults.buttonColors(backgroundColor = MyConstants.myGreen, contentColor = Color.White)
                    ,onClick = {
                        currentState.derive = true
                    }){
                    Text("Derive")

                }
                if (currentState.augment){
                    currentState.augment = false
                    bondGraph.augment()
                }

                if (currentState.derive){
                    currentState.derive = false
                    bondGraph.derive()
                }
            }
        }
    }
}

@Composable
fun bottomBar() {

    val currentState = LocalStateInfo.current

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
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            currentState.showResults = !currentState.showResults
                        }
                    )
                 }
        )
        Text("Clear"
            , fontSize = MyConstants.bottomBarFontSize
            , color = Color.White
            , modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 5.dp )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            currentState.clearGraph = true
                        }
                    )
                }
        )
        if (currentState.clearGraph) {
            currentState.clearGraph = false
            bondGraph.clear()
        }
    }
}

@Composable
fun windowBox() {

    //val currentState = LocalDragTargetInfo.current
    val state = remember { StateInfo() }

     CompositionLocalProvider(
         LocalStateInfo provides state
     ) {

         Column(
             modifier = Modifier
                 .fillMaxSize()
                 .background(Color.Yellow)
         ) {

             draggable(
                 Modifier
                     .background(color = Color.Gray)
                     .fillMaxWidth()
             ) {
                 Row(
                     Modifier
                         //.fillMaxSize()
                         .background(color = Color.Red)
                 ) {
                     textColumn()
                     dropTarget(
                         modifier = Modifier.background(color = MyConstants.graphBackground)
                             .fillMaxSize()
                     )
                 }
             }
             if (state.showResults) {
                 Column(
                     modifier = Modifier
                         .fillMaxWidth()
                         .requiredHeight(800.dp)
                         .background(MyConstants.resultsBackground)
                 ) {

                     Row(
                         modifier = Modifier
                             .background(Color.Gray)
                             .requiredHeight(30.dp)
                             .fillMaxWidth()
                             .weight(.1f, true)
                     ) {
                         Image(
                             painter = painterResource("baseline_minimize_white_48.png"),
                             contentDescription = "",
                             contentScale = ContentScale.Inside,
                             alignment = CenterEnd,
                             modifier = Modifier
                                 .fillMaxSize()
                                 .offset { IntOffset(0, -10) }
                                 .padding(horizontal = 10.dp)
                                 .clickable { state.showResults = false }

                         )
                     }
                     Column(
                         modifier = Modifier
                             .weight(3.5f, true)

                     ) {
                         bondGraph.resultsList.forEach {
                             Text(
                                 it, fontSize = MyConstants.resultsFontSize, modifier = Modifier
                                     .padding(start = 10.dp, top = 5.dp)
                             )
                         }
                     }
                 }
             }
             bottomBar()


         }
     }

}

@Composable
fun App() {
    windowBox()
}
