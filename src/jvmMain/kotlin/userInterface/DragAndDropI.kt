package com.example.draganddrop

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import bondgraph.Element
import bondgraph.Elements
import bondgraph.Elements.*
import bondgraph.GraphElementDisplayData
import userInterface.*
import kotlin.math.*


internal val LocalDragTargetInfo = compositionLocalOf { DragTargetInfo() }
internal var globalId = 0
internal var count = 0

val offsetList = arrayListOf<Pair<Offset, Offset>>()
val offsetMap = hashMapOf<Int, LineData>()
var arrowId = 0
var isShifted = false
enum class Mode {BOND_MODE, ELEMENT_MODE }
enum class StrokeLocation{START, END, NO_STROKE}


class LineData(var color:Color, var offsets: Pair<Offset, Offset>, var strokeLocation: StrokeLocation)
class ArrowOffsets(val oArrow: Offset, val oCausal1: Offset, val oCausal2: Offset)
fun getArrowOffsets(startOffset: Offset, endOffset: Offset): Offset{
    val arrowAngle = .7f
    val arrowLength = 15f
    val xLength = endOffset.x - startOffset.x
    val yLength = endOffset.y - startOffset.y
    val angle = atan(yLength/xLength)
    val sign = if (xLength < 0) 1f else -1f
    return Offset((endOffset.x + sign*(arrowLength * cos(angle - sign * arrowAngle).toFloat())) , endOffset.y + sign*(arrowLength * sin(angle - sign * arrowAngle).toFloat()))
}

fun getCausalOffsets(startOffset: Offset, endOffset: Offset): Pair<Offset, Offset> {
    val arrowAngle = .7f
    val arrowLength = 15f
    val xLength = endOffset.x - startOffset.x
    val yLength = endOffset.y - startOffset.y
    val angle = atan(yLength/xLength)
    val sign = if (xLength < 0) 1f else -1f
    val off1 = Offset((endOffset.x + sign*(arrowLength * cos(angle + sign * 3.14/2f).toFloat())) , endOffset.y + sign*(arrowLength * sin(angle + sign * 3.14/2f).toFloat()))
    val off2= Offset((endOffset.x + sign*(arrowLength * cos(angle - sign * 3.14/2f).toFloat())) , endOffset.y + sign*(arrowLength * sin(angle - sign * 3.14/2f).toFloat()))
    return Pair(off1, off2)
}

@Composable
fun Draggable(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    val state = remember { DragTargetInfo() }

    CompositionLocalProvider(
        LocalDragTargetInfo provides state
    ) {
        Box(modifier = modifier.fillMaxSize())
        {
            content()
            if (state.isDragging) {
                var targetSize by remember {
                    mutableStateOf(IntSize.Zero)
                }
                Box(modifier = Modifier
                    .graphicsLayer {
                        val offset = (state.dragPosition + state.dragOffset)
                        scaleX = 1.0f
                        scaleY = 1.0f
                        alpha = if (targetSize == IntSize.Zero) 0f else .9f
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
}

@Composable
fun  DragTarget(
    modifier: Modifier,
    dataToDrop: Elements,
    id: Int,
    content: @Composable (() -> Unit)
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var charOffsetx by remember { mutableStateOf(0f) }
    var charOffsety by remember { mutableStateOf(0f) }
    val currentState = LocalDragTargetInfo.current


    val elementModeModifier = Modifier
        .onGloballyPositioned {
            it.boundsInWindow().let { rect ->
                println("width=  ${rect.width}, height=  ${rect.height}")
                charOffsetx = rect.width/2f
                charOffsety = rect.height/2f
            }
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        println("mode = ${currentState.mode}")

                        println("id= $id,  dataToDrop = $dataToDrop ")
                        println("DragTarget id = $id")
                        globalId = id
                        bondGraph.getElementsMap()[id]?.displayData?.text  = ""
                        currentState.dataToDrop = dataToDrop
                        currentState.isDragging = true
                        currentState.dragPosition = currentPosition + it
                        currentState.draggableComposable = { elementContent((dataToDrop.displayString())) }
                        currentState.centerOffsetx = charOffsetx
                        currentState.centerOffsety = charOffsety
                    }
                    //
                }, onDrag = { change, dragAmount ->
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        change.consumeAllChanges()

                        currentState.dragOffset += Offset(dragAmount.x, dragAmount.y)
                        //currentState.finalOffset += Offset(dragAmount.x, dragAmount.y)
                    }


                }, onDragEnd = {
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        println("onDragEnd")
                        currentState.finalOffset = currentState.dragOffset
                        currentState.finalPosition = currentState.dragPosition
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
                /*onPress = {
                    if (id >= 1000) {
                        textColor = Color.Black
                        mode = Mode.NODE_MODE
                    }
                }*/
                onDoubleTap = {
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        if (currentState.mode == Mode.ELEMENT_MODE) {
                            println("onDoubleTap,  id = $id, dataToDrop = $dataToDrop")
                            bondGraph.remove(id)
                            currentState.needsUpdate = true
                        }
                    }
                }
            )
        }

    val bondModeModifier = Modifier
        .pointerInput(Unit){

            detectTapGestures(
                onPress = {
                    println("BOND_MODE id= $id")
                    if (id >= 1000) {
                        textColor = Color.Black
                        currentState.mode = Mode.ELEMENT_MODE
                    }
                }
            )
        }

    Box( contentAlignment = Alignment.Center, modifier = modifier
        .onGloballyPositioned {
            currentPosition = it.localToWindow(Offset.Zero)

        }
        .then(elementModeModifier)

    ) {



        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun  DropTarget(
    modifier: Modifier
) {


    val dragInfo = LocalDragTargetInfo.current
    val dragPosition = dragInfo.dragPosition
    val dragOffset = dragInfo.dragOffset
    val finalPosition = dragInfo.finalPosition
    val finalOffset = dragInfo.finalOffset
    var xOffset by remember{ mutableStateOf(0f) }

    var pointerOffset by remember {mutableStateOf(Offset(0f, 0f))}
    var pointerOrigin by remember { mutableStateOf(Offset(0f,0f))}
    var isDragging by remember {mutableStateOf(false)}
    var isDragEnded by remember { mutableStateOf(false) }
    var haveDragged by remember { mutableStateOf(false) }
    var bond by remember { mutableStateOf(0) }
    var needsUpdate by remember { mutableStateOf(false) }
    var elementId by remember { mutableStateOf(-1) }
    //var displayData: GraphElementDisplayData? by remember(GraphElementDisplayData(-1,"", 0f, 0f, 0f, 0f, Offset(0f, 0f)))
    var elementCenter by remember { mutableStateOf(Offset(0f, 0f)) }

    fun findBond(x: Float, y: Float): Int {
        val epsilon = 5f
        val result = offsetMap.mapValues { (_,v) ->
            sqrt((v.offsets.first.x - x ).pow(2) + (v.offsets.first.y - y).pow(2)) +
                    sqrt((v.offsets.second.x - x ).pow(2) + (v.offsets.second.y - y).pow(2)) -
                    sqrt((v.offsets.first.x - v.offsets.second.x).pow(2) + (v.offsets.first.y - v.offsets.second.y).pow(2) )}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }
        return result?.key ?: -1
    }

    fun findElement(x: Float, y: Float, originElement: Element?): Int {
        val epsilon = 50
        val result = bondGraph.getElementsMap().mapValues { (_,v) -> sqrt((v.displayData.centerLocation.x - x).pow(2) + (v.displayData.centerLocation.y - y).pow(2))}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }

        return if (result == null || result == originElement) -1 else result.key
    }

    fun offsetFromCenter(offset1: Offset, offset2: Offset, width: Float, height: Float):Offset {
        val l = (width + height)/2f + 5f
        val d = sqrt((offset1.x - offset2.x ).pow(2) + (offset1.y - offset2.y).pow(2))
        Offset(11f, 1f)
        return Offset((offset1.x - (l * (offset1.x - offset2.x)/d)), offset1.y - (l * (offset1.y - offset2.y)/d))

    }

    Box(modifier = modifier
        //.offset{IntOffset(-50,0)}
        .onGloballyPositioned {
            it.boundsInWindow().let { rect ->
                dragInfo.isCurrentDropTarget = rect.contains(dragPosition + dragOffset)
                xOffset = rect.left
            }
        }
        .pointerInput(Unit) {
            detectTapGestures (
                onTap ={
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        println("Tap at $it  isShifted = $isShifted")

                        if (bond >= 0) {
                            //offsetMap[choice]?.color = Color.Black
                            val of = offsetMap[bond]?.offsets?.first
                            val os = offsetMap[bond]?.offsets?.second
                            val sl = offsetMap[bond]?.strokeLocation
                            if (os != null && of != null && sl != null) {
                                if (isShifted){
                                    println("Flip stroke")
                                    when (sl) {
                                        StrokeLocation.START -> offsetMap[bond] = LineData(Color.Black, Pair(of, os), StrokeLocation.END)
                                        StrokeLocation.END -> offsetMap[bond] =   LineData(Color.Black, Pair(of, os), StrokeLocation.START)
                                        StrokeLocation.NO_STROKE  -> offsetMap[bond] = LineData(Color.Black, Pair(of, os), StrokeLocation.END)
                                    }
                                }else {
                                    println("flip arrow")
                                    when (sl) {
                                        StrokeLocation.START -> offsetMap[bond] =
                                            LineData(Color.Black, Pair(os, of), StrokeLocation.END)

                                        StrokeLocation.END -> offsetMap[bond] =
                                            LineData(Color.Black, Pair(os, of), StrokeLocation.START)

                                        StrokeLocation.NO_STROKE -> offsetMap[bond] =
                                            LineData(Color.Black, Pair(os, of), StrokeLocation.NO_STROKE)
                                    }
                                }
                            }
                            isShifted = false
                            needsUpdate = true
                        }
                    }

                }
                , onDoubleTap = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        println("doubleTap at $it")
                        if (bond >= 0) {
                            offsetMap.remove(bond)
                            needsUpdate = true
                        }
                    }
                }
                , onPress = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        println("onPress")
                        needsUpdate = true
                        val x = it.x
                        val y = it.y
                        bond = findBond(it.x, it.y)
                        if (bond >= 0) {
                            offsetMap[bond]?.color = Color.Red
                        }
                        println("choice = $bond")
                    }
                }
            )

        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        val x = it.x
                        val y = it.y
                        elementId = findElement(it.x, it.y, null)
                        if (elementId >= 0) {
                           //val displayData = bondGraph.getElementsMap()[elementId]?.displayData
                            //bondGraph.getGraphElementsDisplayDataMap().forEach{(k,v) -> println("id= ${k}  centerLocation= ${v.centerLocation}")}
                            pointerOrigin = bondGraph.getElementsMap()[elementId]?.displayData?.centerLocation!!
                            pointerOffset = it
                            isDragging = true
                        }
                        println("Drag Start")
                    }
                }
                , onDrag = { change, dragAmount ->
                    if (dragInfo.mode == Mode.BOND_MODE && elementId >= 0) {
                        pointerOffset += dragAmount
                        val displayData = bondGraph.getElementsMap()[elementId]?.displayData
                        if (displayData != null)
                        pointerOrigin = offsetFromCenter(displayData.centerLocation, pointerOffset, displayData!!.width, displayData!!.height)
                    }
                }
                , onDragEnd = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        isDragging = false
                        isDragEnded = true
                    }
                }
                , onDragCancel = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        isDragging = false
                    }
                }

            )

        }

        .onSizeChanged {
            pointerOffset = Offset(it.width / 2f, it.height / 2f)
        }

        .drawWithCache {
            onDrawBehind {
                // draw behind the content
                val drawArrow ={color: Color, start:  Offset, end: Offset, strokeLoc: StrokeLocation->
                    drawLine(color = color, start, end, 1f)
                    drawLine(color = color, end, getArrowOffsets(start, end), 1f)
                    when (strokeLoc) {
                        StrokeLocation.START -> {
                            val offsets = getCausalOffsets(start, end)
                            drawLine(color = color, offsets.first, offsets.second)
                        }
                        StrokeLocation.END -> {
                            val offsets = getCausalOffsets(end, start)
                            drawLine(color = color, offsets.first, offsets.second)
                        }
                        StrokeLocation.NO_STROKE -> {}
                    }
                }
                println("draw")
                if (needsUpdate) {
                    println("Updateing")
                    offsetMap.values.forEach {println(it);drawArrow(it.color, it.offsets.first, it.offsets.second, it.strokeLocation)}
                    needsUpdate = false
                }
                if (isDragging) {
                    println("dragging")
                    drawArrow(Color.Red, pointerOrigin, pointerOffset, StrokeLocation.NO_STROKE)
                }
                if (isDragEnded){
                    offsetList.add(Pair(pointerOrigin, pointerOffset))
                    val index = if (bond >= 0) bond else arrowId++
                    offsetMap[index] = LineData(Color.Black,Pair(pointerOrigin, pointerOffset), StrokeLocation.NO_STROKE)
                    isDragEnded = false
                    haveDragged = true
                }

                offsetMap.values.forEach {drawArrow(it.color, it.offsets.first, it.offsets.second, it.strokeLocation)}
            }
        }
    ) {

        if (!dragInfo.isDragging && dragInfo.isCurrentDropTarget && dragOffset != Offset.Zero) {

            println("Here 3 isCurrentDropTarget = ${dragInfo.isCurrentDropTarget}")
            println("offset x = ${dragOffset.x}  offset y = ${dragOffset.y}")
            println("position x = ${dragPosition.x}  position y = ${dragPosition.y}")
            println("final offset x = ${finalOffset.x}   final offset y = ${finalOffset.y}")
            println("final position x = ${finalPosition.x}  finalPosition y = ${finalPosition.y}")
            println("xOffset = $xOffset ")
            println("charOffsetx = ${dragInfo.centerOffsetx}  charOffsety = ${dragInfo.centerOffsety}")

            val x = with(LocalDensity.current) { (finalOffset + finalPosition).x - xOffset - dragInfo.centerOffsetx  }
            val y = with(LocalDensity.current) { (finalPosition + finalOffset).y - dragInfo.centerOffsety}
            val id = if (globalId >= 1000) count++ else globalId

            bondGraph.add(id, dragInfo.dataToDrop, x, y, Offset(dragInfo.centerOffsetx, dragInfo.centerOffsety))
            println("globalId = $globalId,  id = $id")
        }

        if (!dragInfo.isDragging) {
            val currentState = LocalDragTargetInfo.current
            currentState.dragOffset = Offset.Zero
            currentState.finalOffset = Offset.Zero
        }

        //println("display ${bondGraph.getGraphElementsDisplayDataMap().size} elements")

        if (dragInfo.needsUpdate) {
            bondGraph.getElementsMap().forEach { (K, V) -> key(K) {displayElement(V.displayData)}}
            dragInfo.needsUpdate = false
        }
        bondGraph.getElementsMap().forEach { (K, V) -> key(K) {displayElement(V.displayData)}}

    }
}

@Composable
fun elementContent(text:String){
    Box(
        modifier = Modifier
            //.size(MyConstants.nodeBoxSize)
            .wrapContentSize()
        //choice.background(Color.LightGray)
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
fun displayElement(displayData: GraphElementDisplayData) {
    //println("id = ${graphElementDisplayData.id}  text= ${graphElementDisplayData.text}  x= ${graphElementDisplayData.x}  y= ${graphElementDisplayData.y}")
    DragTarget(
        modifier = Modifier
            //.size(MyConstants.nodeBoxSize)
            .wrapContentSize()
            .offset { IntOffset(displayData.x.toInt(), displayData.y.toInt())}
        ,Elements.toEnum(displayData.text)
        ,displayData.id
    ) {

        elementContent(displayData.text)
    }
}
internal class DragTargetInfo {
    var isDragging: Boolean by mutableStateOf(false)
    var isCurrentDropTarget: Boolean by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var finalOffset  by mutableStateOf (Offset.Zero)
    var finalPosition  by mutableStateOf (Offset.Zero)
    var draggableComposable by mutableStateOf< (@Composable () -> Unit)?>(null)
    var needsUpdate by mutableStateOf(false)
    var centerOffsetx by mutableStateOf(0f)
    var centerOffsety by mutableStateOf(0f)
    var mode  by mutableStateOf(Mode.ELEMENT_MODE)    //var dataToDrop by mutableStateOf<Any?>(null)
    var dataToDrop = INVALID
    //var trigger by mutableStateOf(0)

}
