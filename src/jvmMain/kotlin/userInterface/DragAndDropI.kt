package userInterface

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
//import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import bondgraph.ElementTypes
import bondgraph.ElementTypes.*
import bondgraph.GraphElementDisplayData
import bondgraph.Bond
import kotlin.math.*
import bondgraph.BondGraph.Companion.getArrowOffsets
import bondgraph.BondGraph.Companion.getCausalOffsets
import bondgraph.BondGraph.Companion.offsetFromCenter
import bondgraph.BondGraph.Companion.getLabelOffset


internal val LocalStateInfo = compositionLocalOf { StateInfo() }
private var globalId = 0
private var count = 0

private var arrowId = 0
internal var isShifted = false
enum class Mode {BOND_MODE, ELEMENT_MODE }
enum class StrokeLocation{START, END, NO_STROKE}


/*
Data for drawing a bond, a line with a half arrow on one end and optional causal stroke,
a short line drawn at one end perpendicular to the arrow. The line is defined by starting
and ending Offsets.  THe arrow is drawn at the second offset.  THe causal stroke
might be at either end so we need to store that info.
*/
class ArrowData(var color:Color, var offsets: Pair<Offset, Offset>, var strokeLocation: StrokeLocation)

/*
Data for drawing the half arrow and causal stroke on a bond. To use this data
draw a lines form the end of the arrow to each offset.
 */
class ArrowOffsets(val oArrow: Offset, val oCausal1: Offset, val oCausal2: Offset)

/*
The following three composable functions draggable, dragTarget and dropTarget work together
to implement the dragging and dropping of elements onto the work space.  dropTarget function
also contains the code for drawing the bond arrows.  I found these functions in an article
on the internet.  They were very general purpose.  They have been specialized to make them
easier to use in this program and enhanced to provide functionality needed for this program.

They communicate with each other by accessing variables in an instance of CompositionalLocal.

draggable is basically a box that is capable of dragging any given composable over what ever
contents are displayed in the box.  At the least, the contents need to include at least one
dragTarget and dropTarget.

The contents to display are passed in as composable parameter.  In our case is almost all
of the ui minus the popup results window.

The composable to drag is provided by the dragTarget function, which in our case is just a
short Text().  The drag animation is accomplished by placing the composable in another box
that uses a .graphicsLayer modifier to translate its coordinates. A stream of coordinates is
provided by the dragTarget function.
 */

@Composable
fun draggable(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit  //Content to display
) {
    val state = LocalStateInfo.current

    Box(modifier = modifier.fillMaxSize())
    {
        content()
        if (state.isDragging) {
            // dragTarget function says to start dragging whatever it has placed
            // in the draggableComposable variable.
            var targetSize by remember {
                mutableStateOf(IntSize.Zero)
            }
            Box(modifier = Modifier
                .graphicsLayer {
                    val offset = (state.startPosition + state.dragOffset)
                    translationX = offset.x.minus(targetSize.width / 2)
                    translationY = offset.y.minus(targetSize.height / 2)
                }
                .onGloballyPositioned {
                    targetSize = it.size
                }
            ) {

                state.draggableComposable?.invoke()
            }
        }
    }
}

/*
The idea behind dragTarget is to pass in composable content, display it and
make it draggable. It does this by displaying the content in a box that uses a
.pointer modifier to look for drag events.  When the box sees a onDragStart event
it crates a another composable that we want to see dragged around. It stores
this new composable in the draggableComposable variable and sets isDragging variable
to true.  This notifies the draggable function that it is time to start positioning
the composable based on the position data that this function will provide by
processing onDrag events.

It also stores data in the dataToDrop variable that can be used by the dropTarget
function once dragging is complete.

Several things to note:
1. Calls to dragTarget must be place in the scope of a draggable function.
2. The content being dragged is not the content being displayed.  It is
   content created on the fly when an onDragStart event is processed. In
   our case it looks the same as our displayed content, a short String.
3. We only process drag events if we are in element mode, not while
   we are dragging bond arrows.

This is how to use this function. Say you have some content you want to be
draggable.  Instead of placing the content directly in a composable say a row
or a column, place a dragTarget there instead and then place your content in the
dragTarget.  Remember you dragTarget must also be in a draggable.  Usually
the row or column etc. will already be in a draggable.

This function has been enhanced to check of doubleTap events which are a
signal from the user to delete this element from the bond graph.
 */
@Composable
fun  dragTarget(
    modifier: Modifier,
    dataToDrop: ElementTypes,
    id: Int,
    content: @Composable (() -> Unit)
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var centerOffsetx by remember { mutableStateOf(0f) }
    var centerOffsety by remember { mutableStateOf(0f) }
    val currentState = LocalStateInfo.current


    val dragAndDropModifier = Modifier
        .onGloballyPositioned {
            it.boundsInWindow().let { rect ->
                // Grab this data for later use. It is the Offset of
                // the center of our string from the upper left corner
                // which is what is used for positioning. This works
                // because to box containing our string is wrapContent.
                centerOffsetx = rect.width/2f
                centerOffsety = rect.height/2f
            }
        }
        .pointerInput(Unit) {
            detectDragGestures(

                onDragStart = {
                    // Set things up for dragging. Save the id, centerOffsetx and centerOffsety for dropTarget to
                    // use later.  Set the text for this element to null, so it will disappear from the screen.
                    // We want it to look like the thing we are actually dragging is the thing the user clicked
                    // on.  Set up our draggable composable which is just a Box with Text in it.  Calculate the
                    // start position realative to the coordinates being used by the draggable function.
                    // Calculate centerPosition relative to coordinates of the work pane.  We need this because
                    // the dropTarget function will be redrawing the bonds attached to this element as we drag.

                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        globalId = id
                        bondGraph.getElement(id)?.displayData?.text  = AnnotatedString("")
                        currentState.dataToDrop = dataToDrop
                        currentState.isDragging = true
                        currentState.startPosition = currentPosition + it
                        currentState.draggableComposable = { elementContent((dataToDrop.toAnnotatedString())) }
                        currentState.centerOffsetx = centerOffsetx
                        currentState.centerOffsety = centerOffsety
                        currentState.centerPosition = currentState.startPosition - currentState.workPaneToWindowOffset
                    }
                    //
                }, onDrag = { change, dragAmount ->
                    // Add the distance we have been dragged to our positions. Update the
                    // bonds that are attached to this element with the new position so
                    // that they stay attached to the element as it is dragged.

                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        change.consume()
                        currentState.dragOffset += Offset(dragAmount.x, dragAmount.y)
                        currentState.centerPosition += Offset(dragAmount.x, dragAmount.y)
                        bondGraph.updateBondsForElement(id, currentState.centerPosition)
                    }


                }, onDragEnd = {
                    // set some flags to inform dropTarget function that dragging is completed.
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        currentState.needsBondUpdate = true
                        currentState.isDragging = false
                    }
                }, onDragCancel = {
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        currentState.dragOffset = Offset.Zero
                        currentState.isDragging = false
                    }
                }
            )
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    // User wants to delete this element.
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        if (currentState.mode == Mode.ELEMENT_MODE) {
                            bondGraph.removeElement(id)
                            currentState.needsElementUpdate = true
                        }
                    }
                }
            )
        }

    Box( contentAlignment = Alignment.Center
        ,modifier = modifier
            .onGloballyPositioned {
                currentPosition = it.localToWindow(Offset.Zero)
            }
            .then(dragAndDropModifier)
    ) {
        content()
    }
}

/*
The original main purpose of dropTarget was to handle things after a drag is
completed.  In the original program there were several dropTargets. Each would
check if the dragged composable was withing its layout bounds, and if it was
it would store and tally information stored in dataToDrop variable.

In this program there is only one dropTarget, the work pane where we are drawing
the bond graph.  We want the text currently being shown by the draggable composable
to remain on the screen where it was dropped. So we must either created a new element
and add it to the bond graph, or we must update the position information if an
existing element was moved.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalTextApi::class)
@Composable
fun  dropTarget(
    modifier: Modifier
) {
    val dragInfo = LocalStateInfo.current
    val startPosition = dragInfo.startPosition
    val dragOffset = dragInfo.dragOffset
    val textMeasurer = rememberTextMeasurer(50)
    var pointerOffset by remember {mutableStateOf(Offset(0f, 0f))}
    var pointerOrigin by remember { mutableStateOf(Offset(0f,0f))}
    var isBondDragging by remember {mutableStateOf(false)}
    var isBondDragEnded by remember { mutableStateOf(false) }
    var haveBondDragged by remember { mutableStateOf(false) }
    var bondId by remember { mutableStateOf(0) }
    var originId by remember { mutableStateOf(-1) }
    var destinationId by remember { mutableStateOf(-1) }

    fun findBond(x: Float, y: Float): Int {
        val epsilon = 5f
        val result = bondGraph.bondsMap.mapValues { (_,v) ->
            sqrt((v.offset1.x - x ).pow(2) + (v.offset1.y - y).pow(2)) +
                    sqrt((v.offset2.x - x ).pow(2) + (v.offset2.y - y).pow(2)) -
                    sqrt((v.offset1.x - v.offset2.x).pow(2) + (v.offset1.y - v.offset2.y).pow(2) )}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }
        return result?.key ?: -1
    }

    fun findElement(x: Float, y: Float, originId: Int ): Int {
        val epsilon = 50
        val result = bondGraph.getElementsMap().mapValues { (_,v) -> sqrt((v.displayData.centerLocation.x - x).pow(2) + (v.displayData.centerLocation.y - y).pow(2))}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }
        return if (result == null || result.key == originId) -1 else result.key
    }


    Box(modifier = modifier
        //.offset{IntOffset(-50,0)}
        .onGloballyPositioned {
            it.boundsInWindow().let { rect -> dragInfo.isCurrentDropTarget = rect.contains(startPosition + dragOffset)}
            dragInfo.workPaneToWindowOffset = it.localToWindow(Offset.Zero)
        }
        .pointerInput(Unit) {
            detectTapGestures (
                onTap ={
                    if (dragInfo.mode == Mode.BOND_MODE) {

                        if (bondId >= 0) {
                            if (isShifted){
                                val bond = bondGraph.getBond(bondId)
                                if (bond != null){
                                    if (bond.effortElement == null){
                                    bondGraph.setCasualElement(bondId, bond.element2)
                                    } else {
                                        bondGraph.setCasualElement(bondId, if (bond.effortElement == bond.element1) bond.element2 else bond.element1)
                                    }
                                }
                            }else {
                                val bond = bondGraph.getBond(bondId)
                                if (bond != null){
                                    bondGraph.setPowerElement(bondId, if (bond.powerToElement == bond.element1) bond.element2 else bond.element1)
                                }
                            }
                            isShifted = false
                            dragInfo.needsBondUpdate = true
                        }
                    }

                }
                , onDoubleTap = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        if (bondId >= 0) {
                            bondGraph.removeBond(bondId)
                            dragInfo.needsBondUpdate = true
                        }
                    }
                }
                , onPress = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        dragInfo.needsBondUpdate = true
                        bondId = findBond(it.x, it.y)
                    }
                }
            )

        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        originId = findElement(it.x, it.y, -1)
                        if (originId >= 0) {
                            pointerOrigin = bondGraph.getElementsMap()[originId]?.displayData?.centerLocation!!
                            pointerOffset = it
                            isBondDragging = true
                        }
                    }
                }
                , onDrag = { _, dragAmount ->
                    if (dragInfo.mode == Mode.BOND_MODE && originId >= 0) {
                        destinationId = findElement(pointerOffset.x, pointerOffset.y, originId)

                        if ( destinationId >= 0) {
                            val disData = bondGraph.getElement(destinationId)?.displayData
                            if (disData != null) {
                                pointerOffset = offsetFromCenter(disData.centerLocation, pointerOrigin, disData.width, disData.height )
                                dragInfo.arrowColor = Color.Red
                            }
                        } else {
                            pointerOffset += dragAmount
                            dragInfo.arrowColor = Color.Black
                        }
                        val displayData = bondGraph.getElementsMap()[originId]?.displayData
                        if (displayData != null)
                        pointerOrigin = offsetFromCenter(displayData.centerLocation, pointerOffset, displayData.width, displayData.height)
                    }
                }
                , onDragEnd = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        isBondDragging = false
                        isBondDragEnded = true
                    }
                }
                , onDragCancel = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        isBondDragging = false
                    }
                }

            )

        }

        .onSizeChanged {
           //pointerOffset = Offset(it.width / 2f, it.height / 2f)
        }

        .drawWithCache {
            onDrawBehind {
                // draw behind the content
                val drawArrowWithOffsets ={color: Color, start:  Offset, end: Offset, strokeLoc: StrokeLocation ->
                    drawLine(color = color, start, end, 1f)
                    drawLine(color = color, end, getArrowOffsets(start, end), 1f)
                    when (strokeLoc) {
                        StrokeLocation.START -> {
                            val offsets = getCausalOffsets(start, end)
                            drawLine(color = color, offsets.first, offsets.second)
                        }
                        StrokeLocation.END -> {
                            val offsets = getCausalOffsets(end, start)
                            drawLine(color = color ,offsets.first, offsets.second)
                        }
                        StrokeLocation.NO_STROKE -> {}
                    }

                }

                val drawArrowWithBond ={bond: Bond->
                    val color = Color.Black
                    drawLine(color = color, bond.offset1, bond.offset2, 1f)

                    if (bond.powerToElement == bond.element1) {
                        drawLine(color = color, bond.offset1, getArrowOffsets(bond.offset2, bond.offset1) )
                    } else {
                        drawLine(color = color, bond.offset2, getArrowOffsets(bond.offset1, bond.offset2) )
                    }

                    if (bond.effortElement != null){
                        if (bond.effortElement == bond.element2) {
                            val offsets = getCausalOffsets(bond.offset1, bond.offset2)
                            drawLine(color = color, offsets.first, offsets.second)
                        } else {
                            val offsets = getCausalOffsets(bond.offset2, bond.offset1)
                            drawLine(color = color, offsets.first, offsets.second)
                        }
                    }

                    val textLayoutResult = textMeasurer.measure(AnnotatedString(bond.displayId ))
                    val myLabelOffset = getLabelOffset(bond.offset1, bond.offset2, textLayoutResult.size.width, textLayoutResult.size.height)
                    drawText(text = bond.displayId, style = TextStyle(fontSize=MyConstants.labelFontsize), textMeasurer = textMeasurer, topLeft = myLabelOffset)
                }
                if (dragInfo.needsBondUpdate) {
                    bondGraph.bondsMap.values.forEach{drawArrowWithBond(it)}
                    dragInfo.needsBondUpdate = false
                }
                //bondGraph.bondsMap.values.forEach{drawArrowWithBond(it)}
                if (isBondDragging) {
                    drawArrowWithOffsets(dragInfo.arrowColor, pointerOrigin, pointerOffset, StrokeLocation.NO_STROKE)
                }
                if (isBondDragEnded){
                    val index = if (bondId >= 0) bondId else arrowId++
                    if (destinationId >= 0) {
                        bondGraph.addBond(index, originId, pointerOrigin, destinationId, pointerOffset, destinationId)
                    }
                    isBondDragEnded = false
                    haveBondDragged = true
                }

                bondGraph.bondsMap.values.forEach{drawArrowWithBond(it)}
            }
        }
    ) {
        println ("isDragging = ${dragInfo.isDragging}  isCurrentDropTarget = ${dragInfo.isCurrentDropTarget}  dragOffset = $dragOffset")
        if ( ! dragInfo.isDragging && ! dragInfo.isCurrentDropTarget && globalId < 1000) {
            val element = bondGraph.getElement(globalId)
            if (element != null) {
                element.displayData.text = element.elementType.toAnnotatedString()
                bondGraph.updateBondsForElement(globalId, element.displayData.centerLocation)
            }
        }

        if ( ! dragInfo.isDragging && dragInfo.isCurrentDropTarget && dragOffset != Offset.Zero) {

            println("workPane offset = ${dragInfo.workPaneToWindowOffset}")
            val centerLocation = startPosition + dragOffset - dragInfo.workPaneToWindowOffset
            val x =  centerLocation.x - dragInfo.centerOffsetx
            val y =  centerLocation.y - dragInfo.centerOffsety

            val id = if (globalId >= 1000) count++ else globalId

            bondGraph.addElement(id, dragInfo.dataToDrop, x, y, Offset(dragInfo.centerOffsetx, dragInfo.centerOffsety))
        }

        if ( ! dragInfo.isDragging) {
            dragInfo.dragOffset = Offset.Zero
        }

        if (dragInfo.needsElementUpdate) {
            bondGraph.getElementsMap().forEach { (k, v) -> key(k) { displayElement(v.displayData) }}
            dragInfo.needsElementUpdate = false
        }
        bondGraph.getElementsMap().forEach { (k, v) -> key(k) { displayElement(v.displayData) }}

    }
}

@Composable
fun elementContent(text:AnnotatedString){
    Box(
        modifier = Modifier
            .wrapContentSize()
    ) {
        Text(
            text = text,
            Modifier.align(Alignment.Center),
            fontSize = MyConstants.elementNameFontsize,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun displayElement(displayData: GraphElementDisplayData, size: Dp = 0.dp) {
    dragTarget(
        modifier = Modifier
            //.size(MyConstants.nodeBoxSize)
            //.wrapContentSize()
            .offset { IntOffset(displayData.x.toInt(), displayData.y.toInt())}
            .then( if (size > 0.dp) Modifier.size(30.dp ) else Modifier.wrapContentSize())
        ,ElementTypes.toEnum(displayData.text)
        ,displayData.id
    ) {

        elementContent(displayData.text)
    }
}

internal class StateInfo {
    var isDragging: Boolean by mutableStateOf(false)
    var isCurrentDropTarget: Boolean by mutableStateOf(false)
    var startPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var draggableComposable by mutableStateOf< (@Composable () -> Unit)?>(null)
    var needsElementUpdate by mutableStateOf(false)
    var needsBondUpdate by mutableStateOf(false)
    var centerPosition by mutableStateOf(Offset.Zero)
    var workPaneToWindowOffset by mutableStateOf(Offset.Zero)
    var centerOffsetx by mutableStateOf(0f)
    var centerOffsety by mutableStateOf(0f)
    var mode  by mutableStateOf(Mode.ELEMENT_MODE)    //var dataToDrop by mutableStateOf<Any?>(null)
    var showResults by mutableStateOf(false)
    var augment by mutableStateOf(false)
    var derive by mutableStateOf(false)
    var clearGraph by mutableStateOf(false)
    var arrowColor by mutableStateOf(Color.Black)
    var dataToDrop = INVALID

}
