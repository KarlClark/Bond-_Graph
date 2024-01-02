package userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import kotlin.math.max

@Composable
fun valuesWindow() {

    val currentState = LocalStateInfo.current

    Window(
        onCloseRequest = {currentState.showValuesWindow = false}
        ,state = rememberWindowState(width = Dp.Unspecified),

        //,state = WindowState(width = 1200.dp, height = 800.dp)


    ) {

        Box (modifier = Modifier


        ) {
            var size = IntSize(0,0)
            var width = 0
            Row (modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .background(Color.LightGray)

            ) {
                LazyColumn (

                ) {
                    item {
                        Text("LazyCOlumn 1")
                    }
                }

                LazyColumn (

                ) {
                    item {
                        Text("LazyCOlumn 2")
                        Text("LazyCOlumn 2")
                        Text("LazyCOlumn 2")
                    }
                }

            }

        }

    }
}