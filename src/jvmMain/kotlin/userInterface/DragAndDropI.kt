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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import bondgraph.ElementTypes
import bondgraph.ElementTypes.*
import bondgraph.GraphElementDisplayData
import bondgraph.Bond
import userInterface.*
import kotlin.math.*
import bondgraph.BondGraph.Companion.getArrowOffsets
import bondgraph.BondGraph.Companion.getCausalOffsets
import bondgraph.BondGraph.Companion.offsetFromCenter
import bondgraph.BondGraph.Companion.getLabelOffset


internal val LocalDragTargetInfo = compositionLocalOf { DragTargetInfo() }
internal var globalId = 0
internal var count = 0

var arrowId = 0
var isShifted = false
enum class Mode {BOND_MODE, ELEMENT_MODE }
enum class StrokeLocation{START, END, NO_STROKE}


class LineData(var color:Color, var offsets: Pair<Offset, Offset>, var strokeLocation: StrokeLocation)
class ArrowOffsets(val oArrow: Offset, val oCausal1: Offset, val oCausal2: Offset)

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
    dataToDrop: ElementTypes,
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
                //println("width=  ${rect.width}, height=  ${rect.height}")
                charOffsetx = rect.width/2f
                charOffsety = rect.height/2f
            }
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        //println("mode = ${currentState.mode}")

                        //println("id= $id,  dataToDrop = $dataToDrop ")
                        //println("DragTarget id = $id")
                        globalId = id
                        bondGraph.getElementsMap()[id]?.displayData?.text  = AnnotatedString("")
                        currentState.dataToDrop = dataToDrop
                        currentState.isDragging = true
                        currentState.dragPosition = currentPosition + it
                        currentState.draggableComposable = { elementContent((dataToDrop.displayString())) }
                        currentState.centerOffsetx = charOffsetx
                        currentState.centerOffsety = charOffsety
                        val cp =bondGraph.getElement(id)?.displayData?.centerLocation
                          if (cp != null){
                              currentState.centerPosition = cp
                          }
                    }
                    //
                }, onDrag = { change, dragAmount ->
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        change.consumeAllChanges()

                        currentState.dragOffset += Offset(dragAmount.x, dragAmount.y)
                        currentState.centerPosition += Offset(dragAmount.x, dragAmount.y)
                        bondGraph.updateBondsForElement(id, currentState.centerPosition)
                        //currentState.finalOffset += Offset(dragAmount.x, dragAmount.y)
                    }


                }, onDragEnd = {
                    if (currentState.mode == Mode.ELEMENT_MODE) {
                        //println("onDragEnd")
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
                            //println("onDoubleTap,  id = $id, dataToDrop = $dataToDrop")
                            bondGraph.removeElement(id)
                            currentState.needsElementUpdate = true
                        }
                    }
                }
            )
        }

    val bondModeModifier = Modifier
        .pointerInput(Unit){

            detectTapGestures(
                onPress = {
                   //println("BOND_MODE id= $id")
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalTextApi::class)
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
    val textMeasurer = rememberTextMeasurer(50)
    val style = TextStyle(fontSize = MyConstants.labelFontsize)


    var pointerOffset by remember {mutableStateOf(Offset(0f, 0f))}
    var pointerOrigin by remember { mutableStateOf(Offset(0f,0f))}
    var isDragging by remember {mutableStateOf(false)}
    var isDragEnded by remember { mutableStateOf(false) }
    var haveDragged by remember { mutableStateOf(false) }
    var bondId by remember { mutableStateOf(0) }
    //var needsUpdate by remember { mutableStateOf(false) }
    var originId by remember { mutableStateOf(-1) }
    var destinationId by remember { mutableStateOf(-1) }
    //var displayData: GraphElementDisplayData? by remember(GraphElementDisplayData(-1,"", 0f, 0f, 0f, 0f, Offset(0f, 0f)))
    var elementCenter by remember { mutableStateOf(Offset(0f, 0f)) }

    fun updateBonds(){
        dragInfo.needsBondUpdate =true
    }
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
            it.boundsInWindow().let { rect ->
                dragInfo.isCurrentDropTarget = rect.contains(dragPosition + dragOffset)
                xOffset = rect.left
            }
        }
        .pointerInput(Unit) {
            detectTapGestures (
                onTap ={
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        //println("Tap at $it  isShifted = $isShifted")

                        if (bondId >= 0) {
                            if (isShifted){
                                //println("Flip stroke")
                                val bond = bondGraph.getBond(bondId)
                                if (bond != null){
                                    if (bond.casualToElement == null){
                                    bondGraph.setCasualElement(bondId, bond.element2)
                                    } else {
                                        bondGraph.setCasualElement(bondId, if (bond.casualToElement == bond.element1) bond.element2 else bond.element1)
                                    }
                                }
                            }else {
                                //("flip arrow")
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
                        //println("doubleTap at $it")
                        if (bondId >= 0) {
                            bondGraph.removeBond(bondId)
                            dragInfo.needsBondUpdate = true
                        }
                    }
                }
                , onPress = {
                    if (dragInfo.mode == Mode.BOND_MODE) {
                        //println("onPress")
                        dragInfo.needsBondUpdate = true
                        val x = it.x
                        val y = it.y
                        bondId = findBond(it.x, it.y)
                        //println("choice = $bondId")
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
                        originId = findElement(it.x, it.y, -1)
                        if (originId >= 0) {
                           //val displayData = bondGraph.getElementsMap()[elementId]?.displayData
                            //bondGraph.getGraphElementsDisplayDataMap().forEach{(k,v) -> println("id= ${k}  centerLocation= ${v.centerLocation}")}
                            pointerOrigin = bondGraph.getElementsMap()[originId]?.displayData?.centerLocation!!
                            pointerOffset = it
                            isDragging = true
                        }
                        //println("Drag Start")
                    }
                }
                , onDrag = { change, dragAmount ->
                    if (dragInfo.mode == Mode.BOND_MODE && originId >= 0) {
                        destinationId = findElement(pointerOffset.x, pointerOffset.y, originId)

                        if ( destinationId >= 0) {
                            val disData = bondGraph.getElement(destinationId)?.displayData
                            if (disData != null) {
                                pointerOffset = offsetFromCenter(disData.centerLocation, pointerOrigin, disData.width, disData.height )
                            }
                        } else {
                            pointerOffset += dragAmount
                        }
                        val displayData = bondGraph.getElementsMap()[originId]?.displayData
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
                val drawArrowWithOffsets ={color: Color, start:  Offset, end: Offset, strokeLoc: StrokeLocation->
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

                val drawArrowWithBond ={bond: Bond->
                    val color = Color.Black
                    drawLine(color = color, bond.offset1, bond.offset2, 1f)

                    if (bond.powerToElement == bond.element1) {
                        drawLine(color = color, bond.offset1, getArrowOffsets(bond.offset2, bond.offset1) )
                    } else {
                        drawLine(color = color, bond.offset2, getArrowOffsets(bond.offset1, bond.offset2) )
                    }

                    if (bond.casualToElement != null){
                        if (bond.casualToElement == bond.element1) {
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
                println("draw, needBondUpdate = ${dragInfo.needsBondUpdate}")
                if (dragInfo.needsBondUpdate) {
                    println("Updateing")
                    bondGraph.bondsMap.values.forEach{drawArrowWithBond(it)}
                    dragInfo.needsBondUpdate = false
                }
                //bondGraph.bondsMap.values.forEach{drawArrowWithBond(it)}
                if (isDragging) {
                    //println("dragging")
                    drawArrowWithOffsets(Color.Red, pointerOrigin, pointerOffset, StrokeLocation.NO_STROKE)
                }
                if (isDragEnded){
                    //println("bondId = $bondId  arrowId = $arrowId")
                    val index = if (bondId >= 0) bondId else arrowId++
                    //println("bondId= $bondId arrowId = $arrowId")
                    //println("originId = $originId  destinationId = $destinationId")
                    if (destinationId >= 0) {
                        bondGraph.addBond(index, originId, pointerOrigin, destinationId, pointerOffset, destinationId)
                    }
                    isDragEnded = false
                    haveDragged = true
                }

                bondGraph.bondsMap.values.forEach{drawArrowWithBond(it)}
            }
        }
    ) {

        if (!dragInfo.isDragging && dragInfo.isCurrentDropTarget && dragOffset != Offset.Zero) {

            //println("Here 3 isCurrentDropTarget = ${dragInfo.isCurrentDropTarget}")
            //println("offset x = ${dragOffset.x}  offset y = ${dragOffset.y}")
            //println("position x = ${dragPosition.x}  position y = ${dragPosition.y}")
            //println("final offset x = ${finalOffset.x}   final offset y = ${finalOffset.y}")
            //println("final position x = ${finalPosition.x}  finalPosition y = ${finalPosition.y}")
            //println("xOffset = $xOffset ")
            //println("charOffsetx = ${dragInfo.centerOffsetx}  charOffsety = ${dragInfo.centerOffsety}")

            val x = with(LocalDensity.current) { (finalOffset + finalPosition).x - xOffset - dragInfo.centerOffsetx  }
            val y = with(LocalDensity.current) { (finalPosition + finalOffset).y - dragInfo.centerOffsety}
            val id = if (globalId >= 1000) count++ else globalId

            bondGraph.addElement(id, dragInfo.dataToDrop, x, y, Offset(dragInfo.centerOffsetx, dragInfo.centerOffsety))
            //println("globalId = $globalId,  id = $id")
        }

        if (!dragInfo.isDragging) {
            val currentState = LocalDragTargetInfo.current
            currentState.dragOffset = Offset.Zero
            currentState.finalOffset = Offset.Zero
        }

        //println("display ${bondGraph.getGraphElementsDisplayDataMap().size} elements")

        if (dragInfo.needsElementUpdate) {
            bondGraph.getElementsMap().forEach { (K, V) -> key(K) {displayElement(V.displayData)}}
            dragInfo.needsElementUpdate = false
        }
        bondGraph.getElementsMap().forEach { (K, V) -> key(K) {displayElement(V.displayData)}}

    }
}

@Composable
fun elementContent(text:AnnotatedString){
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
        ,ElementTypes.toEnum(displayData.text)
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
    var needsElementUpdate by mutableStateOf(false)
    var needsBondUpdate by mutableStateOf(false)
    //var bondId by mutableStateOf("")
    var centerPosition by mutableStateOf(Offset.Zero)
    var centerOffsetx by mutableStateOf(0f)
    var centerOffsety by mutableStateOf(0f)
    var mode  by mutableStateOf(Mode.ELEMENT_MODE)    //var dataToDrop by mutableStateOf<Any?>(null)
    //var bondList: () -> SnapshotStateList<String> = {mutableStateListOf<String>()}

    var dataToDrop = INVALID
    //var trigger by mutableStateOf(0)

}
