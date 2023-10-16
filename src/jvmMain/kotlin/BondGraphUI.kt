package com.example.draganddrop

import GraphNodeDisplayData
import MyConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import graphNodeDisplayDataMap
import textColor
import kotlin.math.*


internal val LocalDragTargetInfo = compositionLocalOf { DragTargetInfo() }
internal var globalId = 0
internal var count = 0

val offsetList = arrayListOf<Pair<Offset, Offset>>()
val offsetMap = hashMapOf<Int, LineData>()
var arrowId = 0
var isShifted = false
enum class Mode {BOND_MODE, NODE_MODE }
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
    dataToDrop: String,
    id: Int,
    content: @Composable (() -> Unit)
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var charOffsetx by remember { mutableStateOf(0f) }
    var charOffsety by remember { mutableStateOf(0f) }
    val currentState = LocalDragTargetInfo.current


    val nodeModeModifier = Modifier
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
                    if (currentState.mode == Mode.NODE_MODE) {
                        println("mode = ${currentState.mode}")

                        println("id= $id,  dataToDrop = $dataToDrop ")
                        println("DragTarget id = $id")
                        globalId = id
                        graphNodeDisplayDataMap[id]?.text = ""
                        currentState.dataToDrop = dataToDrop
                        currentState.isDragging = true
                        currentState.dragPosition = currentPosition + it
                        currentState.draggableComposable = { nodeContent((dataToDrop)) }
                        currentState.charOffsetx = charOffsetx
                        currentState.charOffsety = charOffsety
                    }
                    //
                }, onDrag = { change, dragAmount ->
                    if (currentState.mode == Mode.NODE_MODE) {
                        change.consumeAllChanges()

                        currentState.dragOffset += Offset(dragAmount.x, dragAmount.y)
                        //currentState.finalOffset += Offset(dragAmount.x, dragAmount.y)
                    }


                }, onDragEnd = {
                    if (currentState.mode == Mode.NODE_MODE) {
                        println("onDragEnd")
                        currentState.finalOffset = currentState.dragOffset
                        currentState.finalPosition = currentState.dragPosition
                        currentState.isDragging = false
                    }
                }, onDragCancel = {
                    if (currentState.mode == Mode.NODE_MODE) {
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
                    if (currentState.mode == Mode.NODE_MODE) {
                        if (currentState.mode == Mode.NODE_MODE) {
                            println("onDoubleTap,  id = $id, dataToDrop = $dataToDrop")
                            graphNodeDisplayDataMap.remove(id)
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
                        currentState.mode = Mode.NODE_MODE
                    }
                }
            )
        }

    Box( contentAlignment = Alignment.Center, modifier = modifier
        .onGloballyPositioned {
            currentPosition = it.localToWindow(Offset.Zero)

        }
        .then(nodeModeModifier)

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
    var choice by remember { mutableStateOf(0) }
    var needsUpdate by remember { mutableStateOf(false) }

    fun getChoice(x: Float, y: Float): Int {
        val epsilon = 5f
        val result = offsetMap.mapValues { (_,v) ->
            sqrt((v.offsets.first.x - x ).pow(2) + (v.offsets.first.y - y).pow(2)) +
                    sqrt((v.offsets.second.x - x ).pow(2) + (v.offsets.second.y - y).pow(2)) -
                    sqrt((v.offsets.first.x - v.offsets.second.x).pow(2) + (v.offsets.first.y - v.offsets.second.y).pow(2) )}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }
        return result?.key ?: -1
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

                        if (choice >= 0) {
                            //offsetMap[choice]?.color = Color.Black
                            val of = offsetMap[choice]?.offsets?.first
                            val os = offsetMap[choice]?.offsets?.second
                            val sl = offsetMap[choice]?.strokeLocation
                            if (os != null && of != null && sl != null) {
                                if (isShifted){
                                    println("Flip stroke")
                                    when (sl) {
                                        StrokeLocation.START -> offsetMap[choice] = LineData(Color.Black, Pair(of, os), StrokeLocation.END)
                                        StrokeLocation.END -> offsetMap[choice] =   LineData(Color.Black, Pair(of, os), StrokeLocation.START)
                                        StrokeLocation.NO_STROKE  -> offsetMap[choice] = LineData(Color.Black, Pair(of, os), StrokeLocation.END)
                                    }
                                }else {
                                    println("flip arrow")
                                    when (sl) {
                                        StrokeLocation.START -> offsetMap[choice] =
                                            LineData(Color.Black, Pair(os, of), StrokeLocation.END)

                                        StrokeLocation.END -> offsetMap[choice] =
                                            LineData(Color.Black, Pair(os, of), StrokeLocation.START)

                                        StrokeLocation.NO_STROKE -> offsetMap[choice] =
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
                        if (choice >= 0) {
                            offsetMap.remove(choice)
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
                        choice = getChoice(it.x, it.y)
                        if (choice >= 0) {
                            offsetMap[choice]?.color = Color.Red
                        }
                        println("choice = $choice")
                    }
                }
            )

        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        println("line on drag start")
                        val x = it.x
                        val y = it.y

                        if (choice >= 0){
                            val of = offsetMap[choice]?.offsets?.first!!
                            val ol = offsetMap[choice]?.offsets?.second!!
                            val xf = of.x
                            val yf = of.y
                            val xl = ol.x
                            val yl = ol.y
                            val df = sqrt((xf-x).pow(2) + (yf-y).pow(2))
                            val dl = sqrt((xl-x).pow(2) + (yl-y).pow(2))
                            pointerOrigin = if (df < dl) ol else of
                            offsetMap.remove(choice)
                            needsUpdate = true
                        } else{
                            pointerOrigin = it
                        }
                        println("choice = $choice")
                        pointerOffset = it
                        isDragging = true
                        println("Drag Start")

                        println("pointerOrgin = $pointerOrigin  pointerOffset = $pointerOffset")
                    }
                }
                , onDrag = { change, dragAmount ->
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        pointerOffset += dragAmount
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
                    val index = if (choice >= 0) choice else arrowId++
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
            println("charOffsetx = ${dragInfo.charOffsetx}  charOffsety = ${dragInfo.charOffsety}")

            val x = with(LocalDensity.current) { (finalOffset + finalPosition).x - xOffset - dragInfo.charOffsetx  }
            val y = with(LocalDensity.current) { (finalPosition + finalOffset).y - dragInfo.charOffsety}
            val id = if (globalId >= 1000) count++ else globalId

            graphNodeDisplayDataMap[id] = (GraphNodeDisplayData(id, dragInfo.dataToDrop, x, y))
            println("globalId = $globalId,  id = $id")
        }

        if (!dragInfo.isDragging) {
            val currentState = LocalDragTargetInfo.current
            currentState.dragOffset = Offset.Zero
            currentState.finalOffset = Offset.Zero
        }

        println("display ${graphNodeDisplayDataMap.size} nodes")

        if (dragInfo.needsUpdate) {
            graphNodeDisplayDataMap.forEach { (K, V) -> key(K) {displayNode(V)}}
            dragInfo.needsUpdate = false
        }
        graphNodeDisplayDataMap.forEach { (K, V) -> key(K) {displayNode(V)}}

    }
}

@Composable
fun nodeContent(text:String){
    Box(
        modifier = Modifier
            //.size(MyConstants.nodeBoxSize)
            .wrapContentSize()
        //choice.background(Color.LightGray)
    ) {
        Text(
            text = text,
            Modifier.align(Alignment.Center),
            fontSize = MyConstants.nodeFontsize,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun displayNode(graphNodeDisplayData: GraphNodeDisplayData) {
    println("id = ${graphNodeDisplayData.id}  text= ${graphNodeDisplayData.text}  x= ${graphNodeDisplayData.x}  y= ${graphNodeDisplayData.y}")
    DragTarget(
        modifier = Modifier
            //.size(MyConstants.nodeBoxSize)
            .wrapContentSize()
            .offset { IntOffset(graphNodeDisplayData.x.toInt(), graphNodeDisplayData.y.toInt())}
        ,graphNodeDisplayData.text
        ,graphNodeDisplayData.id
    ) {

        nodeContent(graphNodeDisplayData.text)
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
    var charOffsetx by mutableStateOf(0f)
    var charOffsety by mutableStateOf(0f)
    var mode  by mutableStateOf(Mode.NODE_MODE)    //var dataToDrop by mutableStateOf<Any?>(null)
    var dataToDrop: String = ""
    //var trigger by mutableStateOf(0)

}
