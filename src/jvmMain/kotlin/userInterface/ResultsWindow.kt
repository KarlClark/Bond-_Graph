package userInterface

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window


@Composable
fun showResults() {
    val currentState = LocalStateInfo.current
    currentState.showResultsWindow = true
    currentState.resultsWindowState.isMinimized = false
    currentState.resultsWindowOnTop = true
    currentState.resultsWindowOnTop = false
}

@Composable
fun resultsWindow() {
    var closeRequest by remember { mutableStateOf(false) }
    val results = remember{ bondGraph.results.resultsList}
    val scrollState = rememberScrollState()
    val currentState = LocalStateInfo.current

    //var valuesSetCopy by remember(bondGraph.valueSetWorkingCopy) { mutableStateOf( bondGraph.valueSetWorkingCopy) }
    Window(
        //onCloseRequest = {currentState.showValuesWindow = false}
        onCloseRequest = { closeRequest = true },
        state = currentState.resultsWindowState,
        alwaysOnTop = currentState.resultsWindowOnTop
    ) {
        Box(modifier = Modifier
            .padding(end = 12.dp)

        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)


            ) {
                results.forEach {
                    Text(
                        it, fontSize = MyConstants.resultsFontSize, modifier = Modifier
                            .padding(start = 10.dp, top = 5.dp)
                    )
                }
            }

            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(14.dp)
                    .fillMaxHeight()

                , adapter = rememberScrollbarAdapter(
                    scrollState = scrollState
                )
            )
        }
    }

    if (closeRequest){
        currentState.showResultsWindow = false
    }
}