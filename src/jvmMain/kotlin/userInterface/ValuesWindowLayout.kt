package userInterface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import bondgraph.*

@Composable
fun setsBar (){

    val currentState = LocalStateInfo.current

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
                    .clickable { currentState.newSet = true; println("newSet clicked") }
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterVertically)
            )
        }

        Divider(
            thickness = 1.dp, color = Color.Black
        )
    }
}

@Composable
fun setColumn () {

    Column(
        modifier = Modifier
            .background(Color.DarkGray)
            .width(MyConstants.setColumnWidth)
            .fillMaxHeight()


    ) {

        if (bondGraph.valuesSetsMap.isEmpty()) {
            val id = bondGraph.getNextValueSetId()
            bondGraph.valuesSetsMap[id] = ValuesSet(id, "No Values Set")
        }

        setsBar()

        LazyColumn(
            modifier = Modifier
                .padding(MyConstants.valuesGeneralPadding)
                .background(Color.DarkGray)
            , verticalArrangement = Arrangement.spacedBy(MyConstants.valuesGeneralPadding)
        ) {
            bondGraph.valuesSetsMap.values.forEach {
                item { setItem(it) }
            }
        }
    }
}

@Composable
fun setItem(valuesSet: ValuesSet) {

    val currentState = LocalStateInfo.current

    Box(modifier = Modifier
        .border(BorderStroke(width = 1.dp, color = Color.Black))
        .background(color = if (currentState.selectedSetId == valuesSet.id) MyConstants.setSelectedColor else MyConstants.setDefaultColor)
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

                    Text("Save",
                        fontSize = MyConstants.valuesBarFontSizeSmall,
                        color = MyConstants.valuesBarsTextColor,
                        modifier = Modifier
                            .clickable { save = true }
                            .padding(horizontal = 14.dp)
                    )

                    Text("Save As",
                        fontSize = MyConstants.valuesBarFontSizeSmall,
                        color = MyConstants.valuesBarsTextColor,
                        modifier = Modifier
                            .clickable { saveAs = true }
                            .padding(horizontal = 12.dp)
                    )

                    Text("Delete",
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
fun valuesColumn() {

    val currentState = LocalStateInfo.current

    Column(modifier = Modifier
        .background(Color.Cyan)
        .width(MyConstants.valuesColumnWidth)
        .fillMaxHeight()
    ) {

        valuesBar()
        LazyColumn(modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
        ) {
            if (currentState.newSet) {
                var eList: List<Element>

                bondGraph.getElementList()
                    .filter{it is Capacitor}
                    .sortedBy{it.displayId.toString()}
                    .forEach{item{ onePortItem(it) }}

                bondGraph.getElementList()
                    .filter{it is Inertia}
                    .sortedBy{it.displayId.toString()}
                    .forEach{item{ onePortItem(it) }}

                bondGraph.getElementList()
                    .filter{it is Resistor}
                    .sortedBy{it.displayId.toString()}
                    .forEach{item{ onePortItem(it) }}


            }

        }
    }
}
@Composable
fun onePortItem(element: Element){

    var valueInput by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    Row(modifier = Modifier
        , horizontalArrangement = Arrangement.spacedBy(6.dp)

    ) {
        Column (modifier = Modifier
            .padding(top = 6.dp)
            , horizontalAlignment =  Alignment.CenterHorizontally

        ) {
            Text ("Name", modifier = Modifier
                , textAlign = TextAlign.Center
                , fontSize = MyConstants.valuesFontSize)
            Text(element.displayId)
        }

        Column (modifier = Modifier
            .padding(top = 6.dp)
            ,horizontalAlignment =  Alignment.CenterHorizontally

        ) {
            Text ("Value"
                , textAlign = TextAlign.Center
                , fontSize = MyConstants.valuesFontSize)
            BasicTextField(modifier = Modifier
                .width(MyConstants.valueColumnWidth)
                , value = valueInput
                , onValueChange = {newText -> valueInput = newText}
            )
            Divider( thickness = 1.dp, color = Color.Black, modifier = Modifier.width(MyConstants.valueColumnWidth))
        }

        Column (modifier = Modifier
            .padding(top = 6.dp)
            , horizontalAlignment =  Alignment.CenterHorizontally

        ) {
            Text ("Units"
                , fontSize = MyConstants.valuesFontSize)

            BasicTextField(modifier = Modifier
                .width(MyConstants.unitsColumnWidth)
                , value = unitsInput
                , onValueChange = {newText -> unitsInput = newText}
            )
            Divider( thickness = 1.dp, color = Color.Black, modifier = Modifier.width(MyConstants.unitsColumnWidth))
        }

        Column (modifier = Modifier
            .padding(top = 6.dp)
            , horizontalAlignment =  Alignment.CenterHorizontally

        ) {
            Text ("Description"
                , textAlign = TextAlign.Center
                , fontSize = MyConstants.valuesFontSize)

            BasicTextField(modifier = Modifier
                .fillMaxWidth()
                , value = descriptionInput
                , onValueChange = {newText -> descriptionInput = newText}
            )
            Divider( thickness = 1.dp, color = Color.Black)
        }


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