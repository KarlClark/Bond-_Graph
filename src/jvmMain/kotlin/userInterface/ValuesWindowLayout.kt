package userInterface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import bondgraph.ValuesSet

@Composable
fun valuesSetsBar (){

    var newSet by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .background(Color.Red)
        .fillMaxWidth()
    ) {

        Divider(
            thickness = 1.dp, color = Color.Black
        )

        Row(
            modifier = Modifier
                .height(MyConstants.valuesRowHeight)
                .background(MyConstants.valuesBarsColor)
                .fillMaxWidth()

        ) {

            Text(
                "Value Sets",
                fontSize = MyConstants.valuesBarFontSize,
                textAlign = TextAlign.Left,
                color = MyConstants.valuesBarsTextColor,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            )

            Text("+",
                fontSize = MyConstants.plusSignFontSize,
                textAlign = TextAlign.Center,
                color = MyConstants.valuesBarsTextColor,
                modifier = Modifier
                    .clickable { newSet = true; println("newSet clicked") }
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterVertically)
            )

            if (newSet) {
                newSet = false
                println("newSet")
            }
        }

        Divider(
            thickness = 1.dp, color = Color.Black
        )
    }
}
@Composable
fun valuesBar () {

    val currentState = LocalStateInfo.current

    var save by remember { mutableStateOf(false) }
    var saveAs by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }

    Column( modifier = Modifier
        .fillMaxWidth()

    ) {

        Divider(
            thickness = 1.dp, color = Color.Black
        )

        Row(
            modifier = Modifier
                .height(MyConstants.valuesRowHeight)
                .background(MyConstants.valuesBarsColor)
                .fillMaxWidth()

        ) {
            Column(

            ) {

                Text(
                    currentState.setName,
                    textAlign = TextAlign.Center,
                    fontSize = MyConstants.valuesBarFontSize,
                    color = MyConstants.valuesBarsTextColor,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Row(
                ) {

                    Text("Save"
                        //, textAlign = TextAlign.Left
                        ,
                        fontSize = MyConstants.valuesBarFontSizeSmall,
                        color = MyConstants.valuesBarsTextColor,
                        modifier = Modifier
                            .clickable { save = true }
                            .padding(horizontal = 14.dp)
                    )

                    Text("Save As"
                        //, textAlign = TextAlign.Right
                        ,
                        fontSize = MyConstants.valuesBarFontSizeSmall,
                        color = MyConstants.valuesBarsTextColor,
                        modifier = Modifier
                            .clickable { saveAs = true }
                            .padding(horizontal = 12.dp)
                    )

                    Text("Delete"
                        //, textAlign = TextAlign.Right
                        ,
                        fontSize = MyConstants.valuesBarFontSizeSmall,
                        color = MyConstants.valuesBarsTextColor,
                        modifier = Modifier
                            .clickable { saveAs = true }
                            .padding(horizontal = 12.dp)
                    )
                }

            }
        }

        Divider(
            thickness = 1.dp, color = Color.Black
        )
    }

    if (save){
        save = false
        println("save clicked")
    }

    if (saveAs) {
        saveAs = false
        println("saveAs clicked")
    }

    if (delete){
        delete = false
        println("delete clicked")
    }
}

@Composable
fun setColumn () {

    Column(
        modifier = Modifier
            .background(Color.DarkGray)
            .width(MyConstants.valuesSetColumnWidth)


    ) {

        if (bondGraph.valuesSetsMap.isEmpty()) {
            val id = bondGraph.getNextValueSetId()
            bondGraph.valuesSetsMap[id] = ValuesSet(id, "No Values Set")
        }
        val id = bondGraph.getNextValueSetId()
        bondGraph.valuesSetsMap[id] = ValuesSet(id, "Long long long description")

        valuesSetsBar()

        LazyColumn(
            modifier = Modifier
                .padding(MyConstants.valuesGeneralPadding)
                .background(Color.DarkGray)
                //.background(Color.Yellow)
            , verticalArrangement = Arrangement.spacedBy(MyConstants.valuesGeneralPadding)
        ) {
            bondGraph.valuesSetsMap.values.forEach {
                item { valuesSetItem(it) }
            }
        }
    }
}

@Composable
fun valuesColumn() {
    Column(modifier = Modifier
        .background(Color.Cyan)
        .width(MyConstants.valuesColumnWidth)
    ) {

        valuesBar()
        LazyColumn(modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()


        ) {
            item {
                Text("LazyCOlumn 2", modifier = Modifier.fillMaxWidth())
                Text("LazyCOlumn 2")
                Text("LazyCOlumn 2")
            }
        }
    }

}

@Composable
fun valuesSetItem(valuesSet: ValuesSet) {

    val currentState = LocalStateInfo.current

    Box(modifier = Modifier
        .border(BorderStroke(width = 1.dp, color = Color.Black))
        .background(color = if (currentState.selectedSetId == valuesSet.id) MyConstants.valuesSetSelectedColor else MyConstants.valuesSetDefaultColor)
        .fillMaxWidth()
        .clickable { println("${valuesSet.description} clicked") }


    ) {
        Text(valuesSet.description
            , modifier = Modifier
                .fillMaxWidth()
                .padding(MyConstants.valuesGeneralPadding)
        )
    }
}

@Composable
fun valuesWindow() {

    val currentState = LocalStateInfo.current

    Window(
        onCloseRequest = {currentState.showValuesWindow = false}
        ,state = rememberWindowState(width = Dp.Unspecified),
    ) {

        Box  {
            var size = IntSize(0,0)
            var width = 0

            Row (modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .background(Color.LightGray)

            ) {

                setColumn()

                Divider(
                      thickness = 1.dp
                    , color = Color.Black
                    , modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)

                )

                valuesColumn()
            }
        }
    }
}