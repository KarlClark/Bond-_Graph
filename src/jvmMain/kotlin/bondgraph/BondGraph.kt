package bondgraph

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import bondgraph.ElementTypes.*
import userInterface.LocalStateInfo
import userInterface.MyConstants
import userInterface.bondGraph
import kotlin.math.*


class BadGraphException(message: String) : Exception(message)

/*
An enum  for the different elements used in a bond graph, with the
capability to convert enumm value to an AnnotatedString and from
an AnnotatedString back to the enum value.
 */
enum class ElementTypes {
    ZERO_JUNCTION{
         override fun toAnnotatedString () = _0
     },
    ONE_JUNCTION{
        override fun toAnnotatedString() = _1
    },
    CAPACITOR{
        override fun toAnnotatedString() = C
    },
    RESISTOR{
        override fun toAnnotatedString() = R
    },
    INERTIA{
        override fun toAnnotatedString() = I
    },
    TRANSFORMER{
        override fun toAnnotatedString() = TF
    },
    GYRATOR{
        override fun toAnnotatedString() = GY
    },
    MODULATED_TRANSFORMER{
        override fun toAnnotatedString()  = MTF
    },
    SOURCE_OF_EFFORT{
        override fun toAnnotatedString() = Se
    },
    SOURCE_OF_FLOW{
        override fun toAnnotatedString() = Sf
    },
    INVALID {
        override fun toAnnotatedString() = AnnotatedString("INVALID")
    };

    abstract fun toAnnotatedString(): AnnotatedString
/*
We use an AnnotatedString so that the 'e' and 'f' in the 'Se' and 'Sf' elements
can be subscripts.
 */
    companion object {

        val style = SpanStyle(fontSize = MyConstants.elementNameFontsize, fontFamily = FontFamily.Serif)
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
                GY-> GYRATOR
                MTF-> MODULATED_TRANSFORMER
                Se -> SOURCE_OF_EFFORT
                Sf -> SOURCE_OF_FLOW
                else -> INVALID
            }
        }
    }
}

/*
The data needed to display a representation of the element on the screen.  The id, text and location are
pretty obvious, the width and height are the size of the text, and the centerLocation is the location of
the center of text.  This information is needed for drawing bonds to the element.  Every instance of
Element contains an ElementDisplayData instance as one of its properties.
 */
class ElementDisplayData (val id: Int, var text: AnnotatedString, var location: Offset, val width: Float, val height: Float, var centerLocation: Offset)

/*
The data class for a bond.  Contains the elements attached to each end of the bond and the Offsets of those elements on
the screen.  The powerToElement indicates which element the arrow points to, and should match either element 1 or
element 2.  THe effortElement indicates which element has the causal stroke and should match element 1 or element 2.
 */
class Bond(val id: Int, val element1: Element, var offset1: Offset, val element2: Element, var offset2: Offset, var powerToElement: Element?){
    var displayId: String = ""
    var effortElement: Element? = null


}
class BondGraph(var name: String) {

    /*
    Drawing bonds is complicated.  A bond is a line with a half arrow on one end, and a causal stroke
    on one end.  The half arrow and causal stroke can be on the same end or opposite ends.  The causal
    stroke is a short line perpendicular to the bond. A bond can lie at any orientation on the drawing
    surface pointing in any direction.

    Doing anything graphically requires x and y coordinates stored in an instance of the Offset class.
    For example the drawLine function requires two Offsets one for each endpoint.  So to draw the
    half arrow from one of the endpoints you must calculate another Offset that is a little bit
    short of the endpoint and a little off to the side of the bond.

    This companion object contains functions for calculating the various Offsets require for drawing
    a bond. Most of these functions have several things in common.

    1. The first two parameters are Offsets that can be used to determine the direction of the line.
       Sometimes they are also used as the starting point of a line.

    2. Most of these functions use the atan function to get the angle of the bond in the plane.  The
       Kotlin (and Java) documentation simply say that the atan function returns a value between
       -pi/2 and pi/2.  In my unit circle way of thinking atan should return a value between 0 and 2*pi
       or maybe between -pi and pi if you don't want obtuse angles.  On reflection if you are dealing
       with right triangles, then the maximum magnitude of an angle would be pi/2. If the sides of the
       triangle are oriented along the x and y axes, then the triangle could have four orientations.
       In two of the orientations the ratio of the sides would be positive and in and in the other two
       the ratio would be negative, and you would get angles between -pi//2 and pi/2. All this makes
       our job a little tricky. Rather than taking two Offsets and trying to decide which of the four
       cases applies, and then applying the appropriate one of four formulas, I use a variable to store
       a sign value either -1 or 1 which is used in one formula to add or subtract terms as needed. I
       admit that in most cases how to set the sign value was done by experiment.
     */
    companion object {

        /*
        Given two endpoint Offsets calculate a 3rd Offset so that a line drawn from the
        2nd endpoint to the 3rd Offset will form a half arrow.
         */
        fun getArrowOffsets(startOffset: Offset, endOffset: Offset): Offset{
            val arrowAngle = .7f
            val arrowLength = 15f
            val xLength = endOffset.x - startOffset.x
            val yLength = endOffset.y - startOffset.y
            val angle = atan(yLength/xLength)
            val sign = if (xLength < 0) 1f else -1f
            return Offset((endOffset.x + sign*(arrowLength * cos(angle - sign * arrowAngle).toFloat())),
                        endOffset.y + sign*(arrowLength * sin(angle - sign * arrowAngle).toFloat()))
        }

        /*
        Given two endpoint Offsets calculate two more Offsets so that drawing a line between them will
        create a short line perpendicular to the bond at the second endpoint.
         */
        fun getCausalOffsets(startOffset: Offset, endOffset: Offset): Pair<Offset, Offset> {
            val strokeLength = 7f
            val xLength = endOffset.x - startOffset.x
            val yLength = endOffset.y - startOffset.y
            val angle = atan(yLength/xLength)
            val sign = if (xLength < 0) 1f else -1f

            val off1 = Offset((endOffset.x + sign*(strokeLength * cos(angle + sign * 3.14/2f).toFloat())),
                            endOffset.y + sign*(strokeLength * sin(angle + sign * 3.14/2f).toFloat()))

            val off2= Offset((endOffset.x + sign*(strokeLength * cos(angle - sign * 3.14/2f).toFloat())) ,
                           endOffset.y + sign*(strokeLength * sin(angle - sign * 3.14/2f).toFloat()))

            return Pair(off1, off2)
        }

        /*
        Given two endpoint Offsets and the width and height of a string, calculate and Offset for
        positioning the text midway between the endpoints and off to the side of a line between them.
        The Offset must specify the position of the upper left corner of the text.
         */
        fun getLabelOffset (startOffset: Offset, endOffset: Offset, width: Int, height: Int): Offset{

            //val length = sqrt((endOffset.x - startOffset.x).pow(2) + (endOffset.y - startOffset.y).pow(2))
            val length = 15f
            val xLength = endOffset.x - startOffset.x
            val yLength = endOffset.y - startOffset.y
            val angle = atan(yLength/xLength)
            val middleX = startOffset.x + xLength/2f
            val middleY = startOffset.y + yLength/2f
            val sign = if (xLength < 0) 1f else -1f
            return Offset((middleX - width/2 + sign*(length * cos(angle - sign * 3.14/2f).toFloat())),
                        middleY - height/2 + sign*(length * sin(angle - sign * 3.14/2f).toFloat()))
        }


        /*
        Given two Offsets and the width and height of string located at the first Offset,
        calculate an Offset that is along the line defined by the input Offsets and a little
        ways away from the edge of the text.  This is used to determine where to end a bond
        so that it doesn't touch the text.
         */
        fun offsetFromCenter(offset1: Offset, offset2: Offset, width: Float, height: Float):Offset {
            val l = max(width, height)/2 + 5f
            val d = sqrt((offset1.x - offset2.x ).pow(2) + (offset1.y - offset2.y).pow(2))
            return Offset((offset1.x - (l * (offset1.x - offset2.x)/d)), offset1.y - (l * (offset1.y - offset2.y)/d))
        }
    }

    private val elementsMap = linkedMapOf<Int, Element>() // map of element ids mapped to their elements
    val bondsMap = mutableStateMapOf<Int, Bond>() // Map of bond ids mapped to their bonds.
    val resultsList = mutableStateListOf<String>() // List of error or results that we want to display.

    fun addElement(id: Int, elementType: ElementTypes, location: Offset, centerOffset: Offset): Unit {
        if (elementsMap.contains(id)){
            // Existing element was dragged so update position data. When dragging, the
            // display data was set to null so it has to be reset also.
            elementsMap[id]?.displayData?.text = elementType.toAnnotatedString()
            elementsMap[id]?.displayData?.location = location
            elementsMap[id]?.displayData?.centerLocation = centerOffset
        } else {
            // This is a new element. Figure out what subclass of element it is, invoke its constructor and add it
            // to the elementsMap.
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
                    ElementDisplayData(
                        id,
                        elementType.toAnnotatedString(),
                        location,
                        (centerOffset.x - location.x) * 2f,
                        (centerOffset.y - location.y) * 2f,
                        centerOffset)
                )
            }
        }
    }

    fun getElementsMap():Map<Int, Element> = elementsMap

    fun getElement(id: Int): Element? {
        return elementsMap[id]
    }

    // Check to see if the point (x,y) is close to an element that is not the originId element.  We start
    // dragging out a new bond for some element (originId).  We want to know if we are getting close to
    // another element.  So similar to above
    // 1. map the elements to their distance from the point.
    // 2. filter to see if any of the distances are within epsilon.
    // 3. Take the closest one if more than one.
    // 4. The origin doesn't count.
    fun findElement(x: Float, y: Float, originId: Int ): Int {
        val epsilon = 50
        val result = bondGraph.getElementsMap().mapValues { (_,v) ->
            sqrt((v.displayData.centerLocation.x - x).pow(2) + (v.displayData.centerLocation.y - y).pow(2))}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }
        return if (result == null || result.key == originId) -1 else result.key
    }

    fun removeElement (id: Int) {
        elementsMap[id]?.getBondList()?.forEach{
            it.element1.removeBond(it.id)
            it.element2.removeBond(it.id)
            bondsMap.remove(it.id)
        }
        elementsMap.remove(id)
        removeBondAugmentation()
    }





    @Composable
    fun clear(){
        val state = LocalStateInfo.current
        elementsMap.clear()
        bondsMap.clear()
        state.needsElementUpdate = true
    }

    fun addBond(id: Int, elementId1: Int, offset1: Offset, elementId2: Int, offset2: Offset, powerToElementId: Int) {
        //val labelOffset = getLabelOffset(offset1, offse
        val element1 = elementsMap[elementId1]
        val element2 = elementsMap[elementId2]
        if (element1 != null && element2 != null) {
            val bond = Bond(id, element1, offset1, element2, offset2, elementsMap[powerToElementId])
            bondsMap[id] = bond
            elementsMap[elementId1]?.addBond(bond)
            elementsMap[elementId2]?.addBond(bond)
            removeBondAugmentation()
        }
    }

    fun getBond(id: Int): Bond? {
        return bondsMap[id]
    }

    // This function searches the bonds to see if the point (x,y) lies on any of them. Basically if
    // we have a line from point p1 to point p3, we want to know if point px lies on the line.  To
    // check this we use the idea that the distance for p1 to px + the distance from  p2 to px must
    // equal the distance form p1 to p2,  d1x + d2x = d12.  To account for floating point error and
    // to allow for clicking near the line, we check  -epsilon < d1x + d2x - d12 < epsilon where
    // epsilon is a margin for error determined by experiment. The distance between two points is
    // sqrt( (x1 x2) ** 2 + (y1 - y2) ** 2). So the steps are
    // 1. map all the bonds to their d1x + d2x - d12 value
    // 2. filter for the value being between - and + epsilon
    // 3. choose the one with the smallest value if there is more than one.
    fun findBond(x: Float, y: Float): Int {
        val epsilon = 5f
        val result = bondGraph.bondsMap
            .mapValues { (_,v) ->
                sqrt((v.offset1.x - x ).pow(2) + (v.offset1.y - y).pow(2)) +
                        sqrt((v.offset2.x - x ).pow(2) + (v.offset2.y - y).pow(2)) -
                        sqrt((v.offset1.x - v.offset2.x).pow(2) + (v.offset1.y - v.offset2.y).pow(2) )}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }
        return result?.key ?: -1
    }


    fun removeBond(id: Int){
        elementsMap[bondsMap[id]?.element1?.id]?.removeBond(id)
        elementsMap[bondsMap[id]?.element2?.id]?.removeBond(id)
        bondsMap.remove(id)
        removeBondAugmentation()
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
                bondsMap[id]?.effortElement = element
            }
        }
    }

    fun updateBondsForElement(elementId: Int, newCenter: Offset)  {
        val width = elementsMap[elementId]?.displayData?.width
        val height = elementsMap[elementId]?.displayData?.height
        val bondsList = elementsMap[elementId]?.getBondList()
        if (width != null && height != null &&  ! bondsList.isNullOrEmpty()) {
            for (bond in bondsList){
                if (bond.element1.id == elementId) {
                    val stableCenter = bond.element2.displayData.centerLocation
                    val stableWidth = bond.element2.displayData.width
                    val stableHeight = bond.element2.displayData.height
                   /* bond.offset2 = offsetFromCenter(stableCenter, newCenter, width, height)
                    bond.offset1 = offsetFromCenter(newCenter, stableCenter, stableWidth, stableHeight)*/
                    bond.offset2 = offsetFromCenter(stableCenter, newCenter, stableWidth, stableHeight)
                    bond.offset1 = offsetFromCenter(newCenter, stableCenter, width, height)
                } else {
                    val stableCenter = bond.element1.displayData.centerLocation
                    val stableWidth = bond.element1.displayData.width
                    val stableHeight = bond.element1.displayData.height
                   /* bond.offset1 = offsetFromCenter(stableCenter, newCenter, width, height)
                    bond.offset2 = offsetFromCenter(newCenter, stableCenter, stableWidth, stableHeight)*/
                    bond.offset1 = offsetFromCenter(stableCenter, newCenter, stableWidth, stableHeight)
                    bond.offset2 = offsetFromCenter(newCenter, stableCenter, width, height)
                }
            }
        }
    }

    fun causalityComplete () = bondsMap.all{it.value.effortElement != null}

    fun getUnassignedStorageElements() = elementsMap.values.filter{ v  -> (v.elementType == CAPACITOR || v.elementType == INERTIA) && v.getBondList()[0].effortElement == null}

    fun getUnassignedResistors() = elementsMap.values.filter{ v -> v.elementType == RESISTOR  && v.getBondList()[0].effortElement == null}

    fun getIndepdentStorageelements()  = elementsMap.values.filter { v -> (v.elementType == CAPACITOR && v.getBondList()[0].effortElement != v) ||
            (v.elementType == INERTIA && v.getBondList()[0].effortElement == v)}

    fun removeBondAugmentation() {
        bondsMap.values.forEach { it.effortElement = null
        it.displayId = ""
        }
    }
   @Composable
    fun augment() {

       val state = LocalStateInfo.current

       removeBondAugmentation()

       try{

           if(bondsMap.isEmpty()){
               throw BadGraphException("Error: graph has no bonds")
           }
           // Remove any previous augmentation

           // Assign number labels to the bonds
           var cnt = 1;
           bondsMap.values.forEach {it.displayId = cnt++.toString() }
           // Get a list of all sources
           val sourcesMap = elementsMap.filter { it.value.elementType == SOURCE_OF_FLOW || it.value.elementType == SOURCE_OF_EFFORT }
           val sources = ArrayList(sourcesMap.values)
           if (sources.isEmpty()) {
               throw BadGraphException("Error: graph has no sources.")
           }
           // Starting with one of the sources, count all the elements reachable from that point. If this count doesn't
           // equal the number of elements in the whole graph, then there are elements that are not connected to the graph.
           val element1 = sources[0]?.getBondList()?.get(0)?.element1
           val element2 = sources[0]?.getBondList()?.get(0)?.element2
           if (element1 != null && element2 != null) {
               val count = if (element1 == sources[0]) element2.countElements(element1, 1) else element1.countElements(element2, 1)
               if (count < elementsMap.size){
                   throw BadGraphException("Error: graph has disconnected parts.")
               }
           }

           // Create a name for each element based on its type
           //and the bond number or numbers its attached to.
           elementsMap.forEach { it.value.creatDisplayId() }

           // Assign causality starting from the sources
           sources.forEach{it.assignCausality()}

           // While causality is incomplete and there are still
           // I and C elements with unassigned causality, use
           // them to continue assigning causality.
           var done = causalityComplete()
           while ( ! done ){

               if (! done){
                   val elementList = getUnassignedStorageElements()
                   if (elementList.isNotEmpty()){
                       elementList[0].assignCausality()
                       done = causalityComplete()
                   } else {
                       done = true
                   }
               }
           }

           // If causality is stile incomplete then continue
           // using R elements.
           done = causalityComplete()
           while ( ! done ){
               if (! done){
                   val elementList = getUnassignedResistors()
                   if (elementList.isNotEmpty()){
                       elementList[0].assignCausality()
                       done = causalityComplete()
                   } else {
                       done = true
                   }
               }
           }

       }catch(e: BadGraphException ) {
           resultsList.clear()
           resultsList.add(e.message.toString())
           state.showResults = true
       }

       }

    @Composable
    fun derive(){

        val state = LocalStateInfo.current

        try {

            resultsList.clear()

            if (! causalityComplete()) throw BadGraphException("Error: Graph is not completely augmented")

            val elementsList = getIndepdentStorageelements()
            if (elementsList.isEmpty()) throw BadGraphException("Error: There are no independent capacitors or resistors.")

            for (element in elementsList ) {
                resultsList.add(element.deriveEquation())
            }
            state.showResults = true

        }catch(e: BadGraphException ) {
            resultsList.clear()
            resultsList.add(e.message.toString())
            state.showResults = true
        }
    }
}

