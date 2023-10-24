package bondgraph

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import bondgraph.ElementTypes.*
import com.example.draganddrop.DragTargetInfo
import com.example.draganddrop.LocalDragTargetInfo
import userInterface.MyConstants
import kotlin.math.*


class BadGraphException(message: String) : Exception(message)
enum class ElementTypes {
    ZERO_JUNCTION{
         override fun displayString () = _0
     },
    ONE_JUNCTION{
        override fun displayString() = _1
    },
    CAPACITOR{
        override fun displayString() = C
    },
    RESISTOR{
        override fun displayString() = R
    },
    INERTIA{
        override fun displayString() = I
    },
    TRANSFORMER{
        override fun displayString() = TF
    },
    GYRATOR{
        override fun displayString() = GY
    },
    MODULATED_TRANSFORMER{
        override fun displayString()  = MTF
    },
    SOURCE_OF_EFFORT{
        override fun displayString() = Se
    },
    SOURCE_OF_FLOW{
        override fun displayString() = Sf
    },

    INVALID {
        override fun displayString() = AnnotatedString("")
    };

    abstract fun displayString(): AnnotatedString

    companion object {

        val style = SpanStyle(fontSize = MyConstants.elementNameFontsize)
        val subStyle = SpanStyle(fontSize = MyConstants.subTextFontsize)
        val _0 = AnnotatedString("0", style)
        val _1 = AnnotatedString("1", style)
        val C = AnnotatedString("C", style)
        val R = AnnotatedString("R", style)
        val I = AnnotatedString("I", style)
        val TF = AnnotatedString("TF", style)
        val GY = AnnotatedString("GY", style)
        val MTF = AnnotatedString("MTF", style)
        val Se = buildAnnotatedString {
            pushStyle(style)
            append ("S")
            pushStyle(subStyle)
            append("e")
            toAnnotatedString()
        }

        val Sf = buildAnnotatedString {
            pushStyle(style)
            append ("S")
            pushStyle(subStyle)
            append("f")
            toAnnotatedString()
        }
        fun toEnum(value: AnnotatedString): ElementTypes {
            return when (value) {
                _0 -> ZERO_JUNCTION
                _1 -> ONE_JUNCTION
                C -> CAPACITOR
                R -> RESISTOR
                I -> INERTIA
                TF -> TRANSFORMER
                GY -> GYRATOR
                MTF -> MODULATED_TRANSFORMER
                Se -> SOURCE_OF_EFFORT
                Sf -> SOURCE_OF_FLOW
                else -> INVALID
            }
        }
    }
}

class GraphElementDisplayData (val id: Int, var text: AnnotatedString, var x: Float, var y: Float, val width: Float, val height: Float, var centerLocation: Offset)

class Bond(val id: Int, val element1: Element?, var offset1: Offset, val element2: Element?, var offset2: Offset, var powerToElement: Element?){
    var displayId: String = ""
    var casualToElement: Element? = null


}
class BondGraph(var name: String) {
    companion object {
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
            val strokeLength = 15f
            val xLength = endOffset.x - startOffset.x
            val yLength = endOffset.y - startOffset.y
            val angle = atan(yLength/xLength)
            val sign = if (xLength < 0) 1f else -1f
            val off1 = Offset((endOffset.x + sign*(strokeLength * cos(angle + sign * 3.14/2f).toFloat())) , endOffset.y + sign*(strokeLength * sin(angle + sign * 3.14/2f).toFloat()))
            val off2= Offset((endOffset.x + sign*(strokeLength * cos(angle - sign * 3.14/2f).toFloat())) , endOffset.y + sign*(strokeLength * sin(angle - sign * 3.14/2f).toFloat()))
            return Pair(off1, off2)
        }

        fun getLabelOffset (startOffset: Offset, endOffset: Offset, width: Int, height: Int): Offset{

            //val length = sqrt((endOffset.x - startOffset.x).pow(2) + (endOffset.y - startOffset.y).pow(2))
            val length = 15f
            val xLength = endOffset.x - startOffset.x
            val yLength = endOffset.y - startOffset.y
            val angle = atan(yLength/xLength)
            val middleX = startOffset.x + xLength/2f
            val middleY = startOffset.y + yLength/2f
            val sign = if (xLength < 0) 1f else -1f
            //println("$xLength  $yLength  $angle")
            val off1 = Offset((middleX + sign*(length * cos(angle + sign * 3.14/2f).toFloat())) , middleY + sign*(length * sin(angle + sign * 3.14/2f).toFloat()))
            val off2 = Offset((middleX - width/2 + sign*(length * cos(angle - sign * 3.14/2f).toFloat())) , middleY - height/2 + sign*(length * sin(angle - sign * 3.14/2f).toFloat()))
            return off2
        }


        fun offsetFromCenter(offset1: Offset, offset2: Offset, width: Float, height: Float):Offset {
            val l = (width + height)/2f + 5f
            val d = sqrt((offset1.x - offset2.x ).pow(2) + (offset1.y - offset2.y).pow(2))
            Offset(11f, 1f)
            return Offset((offset1.x - (l * (offset1.x - offset2.x)/d)), offset1.y - (l * (offset1.y - offset2.y)/d))
        }
    }

    val elementsMap = linkedMapOf<Int, Element>()
    val bondsMap = mutableStateMapOf<Int, Bond>()
    val resultsList = mutableStateListOf<String>()

    fun addElement(id: Int, elementType: ElementTypes, x: Float, y: Float, centerOffset: Offset): Unit {
        if (elementsMap.contains(id)){
            elementsMap[id]?.displayData?.text = elementType.displayString()
            elementsMap[id]?.displayData?.x = x
            elementsMap[id]?.displayData?.y = y
            elementsMap[id]?.displayData?.centerLocation = Offset(x + centerOffset.x, y + centerOffset.y)
        } else {
            val elementClass = when (elementType) {
                ZERO_JUNCTION -> ::ZeroJunction
                ONE_JUNCTION -> ::OneJunction
                CAPACITOR -> ::Capacitor
                RESISTOR -> ::Resistor
                INERTIA -> ::Inertia
                TRANSFORMER -> ::Transformer
                GYRATOR -> ::Gyrator
                MODULATED_TRANSFORMER -> ::ModulatedTransformer
                SOURCE_OF_EFFORT -> ::SourceOfEffort
                SOURCE_OF_FLOW -> :: SourceOfFlow
                INVALID -> null
            }
            if (elementClass != null) {
                elementsMap[id] = elementClass.invoke(
                    this,
                    id,
                    elementType,
                    GraphElementDisplayData(
                        id,
                        elementType.displayString(),
                        x,
                        y,
                        centerOffset.x * 2f,
                        centerOffset.y * 2f,
                        Offset(x + centerOffset.x, y + centerOffset.y)
                    )
                )
            }
        }
    }

    fun getElementsMap():Map<Int, Element> = elementsMap

    fun getElement(id: Int): Element? {
        return elementsMap[id]
    }

    fun removeElement (id: Int) {
        elementsMap[id]?.getBondList()?.forEach{bondsMap.remove(it.id) }
        elementsMap.remove(id)
    }

    fun addBond(id: Int, elementId1: Int, offset1: Offset, elementId2: Int, offset2: Offset, powerToElementId: Int) {
        //val labelOffset = getLabelOffset(offset1, offse
        val bond = Bond(id, elementsMap[elementId1], offset1, elementsMap[elementId2], offset2, elementsMap[powerToElementId])
        bondsMap[id] = bond
        elementsMap[elementId1]?.addBond(bond)
        elementsMap[elementId2]?.addBond(bond)
    }

    fun getBond(id: Int): Bond? {
        return bondsMap[id]
    }

    fun removeBond(id: Int){
        elementsMap[bondsMap[id]?.element1?.id]?.removeBond(id)
        elementsMap[bondsMap[id]?.element2?.id]?.removeBond(id)
        bondsMap.remove(id)
    }

    fun elementRemoveBond(bond: Bond?){
        if (bond != null) {
            bondsMap.remove(bond.id)
        }
    }
    fun setPowerElement(id: Int, element: Element?){
        if (bondsMap[id] != null){
            if(bondsMap[id]?.element1 == element || bondsMap[id]?.element2 == element){
                bondsMap[id]?.powerToElement = element
            }
        }
    }
    fun setCasualElement(id: Int, element: Element?) {
        if (bondsMap[id] != null){
            if(bondsMap[id]?.element1 == element || bondsMap[id]?.element2 == element){
                bondsMap[id]?.casualToElement = element
            }
        }
    }

    fun updateBondsForElement(elementId: Int, newCenter: Offset)  {
        val width = elementsMap[elementId]?.displayData?.width
        val height = elementsMap[elementId]?.displayData?.height
        val bondsList = elementsMap[elementId]?.getBondList()
        if (width != null && height != null &&  ! bondsList.isNullOrEmpty()) {
            for (bond in bondsList){
                if (bond.element1?.id == elementId) {
                    val stableCenter = bond.element2?.displayData?.centerLocation
                    val stableWidth = bond.element2?.displayData?.width
                    val stableHeight = bond.element2?.displayData?.height
                    if (stableCenter != null && stableWidth != null && stableHeight != null) {
                        bond.offset2 = offsetFromCenter(stableCenter, newCenter, width, height)
                        bond.offset1 = offsetFromCenter(newCenter, stableCenter, stableWidth, stableHeight)
                    }
                } else {
                    val stableCenter = bond.element1?.displayData?.centerLocation
                    val stableWidth = bond.element1?.displayData?.width
                    val stableHeight = bond.element1?.displayData?.height
                    if (stableCenter != null && stableWidth != null && stableHeight != null) {
                        bond.offset1 = offsetFromCenter(stableCenter, newCenter, width, height)
                        bond.offset2 = offsetFromCenter(newCenter, stableCenter, stableWidth, stableHeight)
                    }
                }
            }
        }
    }

   @Composable
    fun augment() {
        println("augment called")

       val state = LocalDragTargetInfo.current
       var cnt = 1;
       bondsMap.values.forEach { val bond= it; it.displayId = cnt++.toString(); bondsMap[bond.id] = bond }

       elementsMap.forEach { (_, V) -> println("${V.id}, ${V.element} ") }
       try{
           val sources = elementsMap.filter { it.value.element == SOURCE_OF_FLOW || it.value.element == SOURCE_OF_EFFORT }
           if (sources.isEmpty()) {
               throw BadGraphException("Error: graph has no sources.")
           }

       }catch(e: BadGraphException ) {
           println("caught error")
           resultsList.clear()
           resultsList.add(e.message.toString())
           state.showResults = true
       }

       }
   //}
}