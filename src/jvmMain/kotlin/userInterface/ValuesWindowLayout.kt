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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
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
                            .clickable { delete = true }
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
        .background(Color.DarkGray)
        .width(MyConstants.valuesColumnWidth)
        .fillMaxHeight()
    ) {

        valuesBar()
        LazyColumn(modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .padding(MyConstants.valuesGeneralPadding)
            , verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (currentState.newSet) {
                var eList = arrayListOf<Element>()
                var focusRequesterOriginal = FocusRequester()
                val focusRequesterList = arrayListOf<FocusRequester>()

                eList.addAll( bondGraph.getElementList()
                    .filter{it is Capacitor}
                    .sortedBy{it.displayId.toString()})


                eList.addAll(bondGraph.getElementList()
                    .filter{it is Inertia}
                    .sortedBy{it.displayId.toString()})


                eList.addAll(bondGraph.getElementList()
                    .filter{it is Resistor}
                    .sortedBy{it.displayId.toString()})

                eList.addAll(bondGraph.getElementList()
                    .filter{it is Transformer}
                    .sortedBy{it.displayId.toString()})

                eList.addAll(bondGraph.getElementList()
                    .filter{it is Gyrator}
                    .sortedBy{it.displayId.toString()})


                println("elist.size =${eList.size}")
                for (index in 0 until eList.size){
                    focusRequesterList.add(FocusRequester())
                }
                focusRequesterList.add(focusRequesterList[0])
                for (index in 0 until eList.size) {
                    println("element = ${eList[index].displayId} : ${eList[index]::class.simpleName}")
                    if (eList[index] is Capacitor || eList[index] is Inertia || eList[index] is Resistor) {
                        item {onePortItem(eList[index], focusRequesterList[index], focusRequesterList[index + 1], index == 0)}
                    } else {
                        item {twoPortItem(eList[index], focusRequesterList[index], focusRequesterList[index + 1])}
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun onePortItem(element: Element, valueFocusRequester: FocusRequester, nextItemFocusRequester: FocusRequester, initialFocus: Boolean){

    var valueInput by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    val unitsFocusRequester = FocusRequester()
    val descriptionFocusRequester = FocusRequester()
    Box (modifier = Modifier
        .background(Color.LightGray)

    ) {
        Row(
            modifier = Modifier
                .border(BorderStroke(width = 1.dp, Color.Black))
                .padding(12.dp)
                .background(Color.LightGray)
            , horizontalArrangement = Arrangement.spacedBy(6.dp)
            , verticalAlignment = Alignment.CenterVertically


        ) {

            Text(element.displayId
                , modifier = Modifier
                    .width(MyConstants.diplayNameWidth)
                    ,textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier
                    .background(MyConstants.myWhite)
                //.padding(MyConstants.valuesGeneralPadding)
                , horizontalArrangement = Arrangement.spacedBy(6.dp)

            ) {

                Column(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .absolutePadding(left = MyConstants.valuesGeneralPadding)
                    , horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    Text(
                        "Value",
                        modifier = Modifier
                            .padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        fontSize = MyConstants.valuesFontSize
                    )
                    BasicTextField(modifier = Modifier
                        .width(MyConstants.valueColumnWidth)
                        .focusRequester(valueFocusRequester)
                        .onKeyEvent {
                            if (it.key == Key.Tab) {
                                unitsFocusRequester.requestFocus()
                            }
                            if (it.key == Key.Enter) {
                                nextItemFocusRequester.requestFocus()
                            }
                            true
                        }, value = valueInput, onValueChange = { newText ->
                        var periodCount = 0
                        valueInput = buildString {
                            newText.forEach {
                                when {
                                    it == '.' -> {
                                        if (periodCount++ == 0) {
                                            append(it)
                                        }
                                    }

                                    it == '0' -> {
                                        if ((length == 1 && get(0) != '0') || length != 1) {
                                            append(it)
                                        }
                                    }

                                    it.isDigit() -> {
                                        if (length == 1 && get(0) == '0') {
                                            deleteAt(0)
                                        }
                                        append(it)
                                    }
                                }
                            }
                        }
                    }
                    )
                    Divider(
                        thickness = 1.dp,
                        color = Color.Black,
                        modifier = Modifier.width(MyConstants.valueColumnWidth).padding(bottom = 12.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    Text(
                        "Units", modifier = Modifier
                            .padding(vertical = 6.dp), fontSize = MyConstants.valuesFontSize
                    )

                    BasicTextField(modifier = Modifier
                        .width(MyConstants.unitsColumnWidth)
                        .focusRequester(unitsFocusRequester)
                        .onKeyEvent {
                            if (it.key == Key.Tab) {
                                descriptionFocusRequester.requestFocus()
                            }
                            if (it.key == Key.Enter) {
                                nextItemFocusRequester.requestFocus()
                            }
                            true
                        }, value = unitsInput, onValueChange = { newText ->
                        unitsInput = buildString {
                            newText.forEach {
                                if (!(it == '\t' || it == '\n')) append(it)
                            }
                        }
                    }
                    )
                    Divider(
                        thickness = 1.dp,
                        color = Color.Black,
                        modifier = Modifier.width(MyConstants.unitsColumnWidth)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    Text(
                        "Description",
                        modifier = Modifier
                            .padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        fontSize = MyConstants.valuesFontSize
                    )

                    BasicTextField(modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(descriptionFocusRequester)
                        .onKeyEvent {
                            if (it.key == Key.Tab) {
                                valueFocusRequester.requestFocus()
                            }
                            if (it.key == Key.Enter) {
                                nextItemFocusRequester.requestFocus()
                            }
                            true
                        }
                        , value = descriptionInput
                        , onValueChange = { newText ->
                            descriptionInput = buildString {
                                newText.forEach {
                                    if (!(it == '\t' || it == '\n')) append(it)
                                }
                            }
                    }
                    )
                    Divider(thickness = 1.dp, color = Color.Black, modifier = Modifier.absolutePadding(right = MyConstants.valuesGeneralPadding))
                }
            }


        }
        LaunchedEffect(Unit) {
            if (initialFocus) {
                valueFocusRequester.requestFocus()
            }
        }
    }
}
@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun twoPortItem(element: Element, valueFocusRequester: FocusRequester, nextItemFocusRequester: FocusRequester){
    var valueInput by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    val unitsFocusRequester = FocusRequester()
    val descriptionFocusRequester = FocusRequester()

    Box (modifier = Modifier
        .background(Color.LightGray)

    ) {
        Row(
            modifier = Modifier
                .border(BorderStroke(width = 1.dp, Color.Black))
                .padding(12.dp)
                .background(Color.LightGray)
            , horizontalArrangement = Arrangement.spacedBy(6.dp)
            , verticalAlignment = Alignment.CenterVertically


        ) {

            Text(element.displayId
                , modifier = Modifier
                    .width(MyConstants.diplayNameWidth)
                ,textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier
                    .background(MyConstants.myWhite)
                //.padding(MyConstants.valuesGeneralPadding)


            ) {
                Row (

                ){
                    Column (

                    ){
                        BasicTextField(modifier = Modifier
                            .width(MyConstants.valueColumnWidth)
                            .focusRequester(valueFocusRequester)
                            .onKeyEvent {
                                if (it.key == Key.Tab) {
                                    unitsFocusRequester.requestFocus()
                                }
                                if (it.key == Key.Enter) {
                                    nextItemFocusRequester.requestFocus()
                                }
                                true
                            }, value = valueInput, onValueChange = { newText ->
                            var periodCount = 0
                            valueInput = buildString {
                                newText.forEach {
                                    when {
                                        it == '.' -> {
                                            if (periodCount++ == 0) {
                                                append(it)
                                            }
                                        }

                                        it == '0' -> {
                                            if ((length == 1 && get(0) != '0') || length != 1) {
                                                append(it)
                                            }
                                        }

                                        it.isDigit() -> {
                                            if (length == 1 && get(0) == '0') {
                                                deleteAt(0)
                                            }
                                            append(it)
                                        }
                                    }
                                }
                            }
                        }
                        )
                        Divider(
                            thickness = 1.dp,
                            color = Color.Black,
                            modifier = Modifier.width(MyConstants.valueColumnWidth).padding(bottom = 12.dp)
                        )
                    }
                }
                Row (

                ){
                    Text(
                        "Units", modifier = Modifier
                            .padding(vertical = 6.dp), fontSize = MyConstants.valuesFontSize
                    )
                    Column (

                    ){
                        BasicTextField(modifier = Modifier
                            .width(MyConstants.unitsColumnWidth)
                            .focusRequester(unitsFocusRequester)
                            .onKeyEvent {
                                if (it.key == Key.Tab) {
                                    descriptionFocusRequester.requestFocus()
                                }
                                if (it.key == Key.Enter) {
                                    nextItemFocusRequester.requestFocus()
                                }
                                true
                            }, value = unitsInput, onValueChange = { newText ->
                            unitsInput = buildString {
                                newText.forEach {
                                    if (!(it == '\t' || it == '\n')) append(it)
                                }
                            }
                        }
                        )
                        Divider(
                            thickness = 1.dp,
                            color = Color.Black,
                            modifier = Modifier.width(MyConstants.unitsColumnWidth)
                        )
                    }

                    Text(
                        "Description",
                        modifier = Modifier
                            .padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        fontSize = MyConstants.valuesFontSize
                    )

                    Column (

                    ){
                        BasicTextField(modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(descriptionFocusRequester)
                            .onKeyEvent {
                                if (it.key == Key.Tab) {
                                    valueFocusRequester.requestFocus()
                                }
                                if (it.key == Key.Enter) {
                                    nextItemFocusRequester.requestFocus()
                                }
                                true
                            }
                            , value = descriptionInput
                            , onValueChange = { newText ->
                                descriptionInput = buildString {
                                    newText.forEach {
                                        if (!(it == '\t' || it == '\n')) append(it)
                                    }
                                }
                            }
                        )
                        Divider(thickness = 1.dp, color = Color.Black, modifier = Modifier.absolutePadding(right = MyConstants.valuesGeneralPadding))
                    }
                }
            }
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
                    , color = Color.White
                    , modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)

                )

                valuesColumn()
            }
        }
    }
}