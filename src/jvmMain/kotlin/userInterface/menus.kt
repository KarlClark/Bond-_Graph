package userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup

@Composable
fun dropDownSelectionBox (items: ArrayList<String> = arrayListOf(), title: String = "", startIndex: Int = 0, width: Dp = 85.dp
                          ,fontSize:TextUnit = 14.sp, titleBackgroundColor: Color = Color.White, spacing:Dp = 3.dp, dropDownBackgroundColor: Color = Color.White, action: (i: Int) -> Unit){

    var isExpanded by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(startIndex) }
    val downArrow = painterResource("arrow-down.png")
    val upArrow = painterResource("arrow-up.png")
    val indicies = arrayListOf<Int>()
    val expandIconSize = 10.dp
    for (index in 0 until items.size) {
        indicies.add(index)
    }


    Column (modifier = Modifier
        , verticalArrangement = Arrangement.Center

    ) {

        Box(modifier = Modifier
            .border(1.dp, color = if (title == "") Color.Black else titleBackgroundColor)
            .background(color = if (title == "")dropDownBackgroundColor else titleBackgroundColor)

        ) {
            Row(
                modifier = Modifier
                    .width(width)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    if (title == "") items[currentIndex] else title
                    , fontSize = fontSize
                    , modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded }
                )

                if (title == "") {
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
        }

        if (isExpanded) {
            Box (modifier = Modifier

            ){
                Popup {
                    Column(
                        modifier = Modifier
                            .border(width = 1.dp, color = Color.Black)
                            .background(color=dropDownBackgroundColor)
                            .padding(horizontal = 6.dp)
                        , verticalArrangement = Arrangement.spacedBy(spacing)

                    ) {
                        Spacer(modifier = Modifier.height(3.dp))

                        for (index in 0 until items.size) {
                            Text(items[index], modifier = Modifier
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
fun basicCheckbox(id: Int = 0, label: String = "", startSate: Boolean = false, fontSize: TextUnit, backgroundColor: Color, height: Dp= 30.dp, scale: Float = 0.7f, action: (id: Int, state: Boolean) -> Unit){
    val checkedState = remember { mutableStateOf(startSate) }
    Row(modifier = Modifier
        .background(color = backgroundColor)
        .height(height)
        , verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(modifier = Modifier
            .scale(scale)
            ,checked = checkedState.value
            ,onCheckedChange = {
                checkedState.value = it
                action(id, it)
            }
        )

        Text(label
            ,fontSize = fontSize
            , modifier = Modifier
                .padding(end = 12.dp)

        )
    }
}

@Composable
fun checkboxGroup(labels: ArrayList<String> = arrayListOf(), startStates: ArrayList<Boolean> = arrayListOf(), fontSize: TextUnit, backgroundColor: Color, itemHeight: Dp = 30.dp, spacing: Dp = 1.dp, action: (index: Int, state: Boolean) -> Unit) {
    var idx = 0
    var statex = false
    Column(modifier = Modifier
        .border(width = 1.dp, color = Color.Black)
        .background(color = backgroundColor)
        .padding(horizontal = 6.dp)
        //, verticalArrangement = Arrangement.spacedBy(30.dp)

    ){
        for (index in 0 until labels.size) {
            basicCheckbox(index, labels[index], startStates[index], fontSize, height = itemHeight, backgroundColor = backgroundColor) {id, state ->
               action(id, state)

            }
        }
    }
}

@Composable
fun dropDownCheckboxGroup (labels: ArrayList<String> = arrayListOf(), title: String = "", startIndex: Int = 0, width: Dp = 85.dp
                           , fontSize:TextUnit = 14.sp, titleBackgroundColor: Color = Color.White, itemHeight: Dp = 30.dp, spacing:Dp = 3.dp, dropDownBackgroundColor: Color = Color.White, action: (id: Int, state: Boolean) -> Unit){

    var isExpanded by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(startIndex) }
    val downArrow = painterResource("arrow-down.png")
    val upArrow = painterResource("arrow-up.png")
    val indicies = arrayListOf<Int>()
    val expandIconSize = 10.dp

    var states = remember { arrayListOf<Boolean>() }

    labels.forEach { states.add(false) }
    /*for (index in 0 until labels.size) {
        indicies.add(index)
    }*/


    Column (modifier = Modifier
        , verticalArrangement = Arrangement.Center

    ) {

        Box(modifier = Modifier
            .border(1.dp, color = if (title == "") Color.Black else titleBackgroundColor)
            .background(color = if (title == "")dropDownBackgroundColor else titleBackgroundColor)

        ) {
            Row(
                modifier = Modifier
                    .width(width)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    if (title == "") labels[currentIndex] else title
                    , fontSize = fontSize
                    , modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded }
                )

                if (title == "") {
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
        }

        if (isExpanded) {
            Box (modifier = Modifier


            ){
                Popup {
                    checkboxGroup(labels, states, fontSize, backgroundColor = dropDownBackgroundColor, itemHeight = itemHeight, spacing = spacing){id, state ->
                        states[id] = state
                        action(id, state)}
                }
            }
        }
    }
}
