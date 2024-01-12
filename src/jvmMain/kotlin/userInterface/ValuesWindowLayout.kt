package userInterface

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import bondgraph.*
@Composable
fun dropDownSelectionBox (items: ArrayList<String> = arrayListOf(), startIndex: Int = 0, width: Dp = 85.dp, color: Color = Color.White, action: (i: Int) -> Unit){

    var isExpanded by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(startIndex) }
    val downArrow = painterResource("arrow-down.png")
    val upArrow = painterResource("arrow-up.png")
    val indicies = arrayListOf<Int>()
    val expandIconSize = 10.dp
    for (index in 0 until items.size) {
        indicies.add(index)
    }

    indicies.forEach { println (it) }

    Column (modifier = Modifier
        //.fillMaxWidth()

    ) {

        Box(modifier = Modifier
            .background(color)
            //.padding(MyConstants.valuesGeneralPadding)
            .border(width = 1.dp, color = Color.Black)

        ) {
            Row(
                modifier = Modifier
                    .width(width)
                    .background(color)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                , verticalAlignment = Alignment.CenterVertically

            ) {
                Text(
                    items[currentIndex], modifier = Modifier
                        .weight(1f)
                )

                Image(  // minimize icon
                    painter = if (isExpanded) upArrow else downArrow,
                    contentDescription = "",
                    contentScale = ContentScale.Inside,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .width(expandIconSize)
                        .height(expandIconSize)
                        .clickable { isExpanded = !isExpanded }
                )
            }
        }

        if (isExpanded) {
            Box (modifier = Modifier
                //.padding(horizontal = 6.dp)

            ){
                Popup {
                    Column(
                        modifier = Modifier
                            .background(color)
                            .border(width = 1.dp, color = Color.Black)
                            .padding(horizontal = 6.dp)
                        , verticalArrangement = Arrangement.spacedBy(3.dp)

                    ) {
                        Spacer(modifier = Modifier.height(3.dp))

                        for (index in 0 until items.size) {
                            Text(items[index], modifier = Modifier
                                //.padding(vertical = 2.dp)
                                .clickable {
                                    currentIndex = indicies[index]
                                    action(currentIndex)
                                    isExpanded = false
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}
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
    val operations = arrayListOf("multiply", "divide")
    var operationsIndex by remember { mutableStateOf(0) }
    val bondList = element.getBondList()
    val idPair = if (bondList[0].displayId < bondList[1].displayId) Pair(bondList[0].displayId, bondList[1].displayId) else Pair (bondList[1].displayId, bondList[0].displayId)
    val powerVars = arrayListOf("effort - " + idPair.first, "effort - " + idPair.second, "flow - " + idPair.first, "flow - "  + idPair.second)
    var powwerVarsIndex by remember { mutableStateOf(0) }
    val indexMap = if (element is Transformer)
        mapOf<Int, Int>(0 to 1, 1 to 0, 2 to 3, 3 to 2 ) else mapOf(0 to 3, 1 to 2, 2 to 1, 3 to 0)

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
                Row (modifier = Modifier
                    .background(Color.White)
                    .padding(MyConstants.valuesGeneralPadding)
                    .fillMaxWidth()
                    //,verticalAlignment = Alignment.Bottom
                    , horizontalArrangement = Arrangement.spacedBy(6.dp)

                ){

                    dropDownSelectionBox(operations) {
                        println ("index = $it, choice = ${operations[it]}")
                        operationsIndex = it
                    }

                    dropDownSelectionBox (powerVars) {
                        println("index = $it, choice = ${powerVars[it]}")
                        powwerVarsIndex = it
                    }

                    Text ("by", modifier = Modifier
                        .padding(top = 3.dp)
                    )

                    Column (

                    ){
                        BasicTextField(modifier = Modifier
                            .width(MyConstants.valueColumnWidth)
                            .padding(top = 3.dp)
                            .focusRequester(valueFocusRequester)
                            .onKeyEvent {
                                if (it.key == Key.Tab) {
                                    unitsFocusRequester.requestFocus()
                                }
                                if (it.key == Key.Enter) {
                                    nextItemFocusRequester.requestFocus()
                                }
                                true
                            }
                            , value = valueInput, onValueChange = { newText ->
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
                            modifier = Modifier
                                .width(MyConstants.valueColumnWidth)
                                .padding(bottom = 12.dp)
                        )
                    }

                    Text(" to get " + powerVars[indexMap[powwerVarsIndex]!!],
                        modifier = Modifier
                            .padding(top = 3.dp)
                    )
                }
                Row (

                ){
                    Text(
                        "Units", modifier = Modifier
                            .padding(horizontal = 6.dp), fontSize = MyConstants.valuesFontSize
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
                            modifier = Modifier
                                .width(MyConstants.unitsColumnWidth)
                                .padding(bottom = 12.dp)
                        )
                    }

                    Text(
                        "Description",
                        modifier = Modifier
                            .padding(horizontal = 6.dp),
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