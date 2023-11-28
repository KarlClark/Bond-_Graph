package bondgraph

import algebra.Equation
import algebra.Token
import algebra.solve
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import bondgraph.ElementTypes.*
import userInterface.LocalStateInfo
import kotlin.math.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString


class BadGraphException (message: String) : Exception(message)
class AlgebraException (message: String) : Exception(message)


class Results() {
    val resultsList = mutableListOf<AnnotatedString>()

    fun add(string: String) {
        resultsList.add(AnnotatedString( string))
    }

    fun add(string: AnnotatedString) {
        resultsList.add(string)
    }

    fun clear(){
        resultsList.clear()
        resultsList.clear()
    }

    @Composable
    fun forEachResult ( fn: @Composable (string: AnnotatedString) -> Unit ) {
        for (r  in resultsList) {
            fn(r)
        }
    }
}




/*
The data class for a bond.  Contains the elements attached to each end of the bond and the Offsets of those elements on
the screen.  The powerToElement indicates which element the arrow points to, and should match either element 1 or
element 2.  THe effortElement indicates which element has the causal stroke and should match element 1 or element 2.

 */
class Bond(val id: Int, val element1: Element, var offset1: @Contextual Offset, val element2: Element, var offset2: @Contextual Offset, var powerToElement: Element){
    var displayId: String = ""
    var effortElement: Element? = null
}
@Serializable
class BondSerializationData(val id: Int, val displayId: String, val elementId1: Int, val loc1x: Float, val loc1y: Float, val elementId2: Int, val locx2: Float, val locy2: Float, val powerToElementId: Int, val effortElementId: Int) {
    companion object {
        fun getData(bond: Bond): BondSerializationData {
            with(bond) {
                return BondSerializationData(
                    id,
                    displayId,
                    element1.id,
                    offset1.x,
                    offset1.y,
                    element2.id,
                    offset2.x,
                    offset2.y,
                    powerToElement.id,
                    effortElement?.id ?: -1
                )
            }
        }

        fun makeBond(data: BondSerializationData, elementsMap: Map<Int, Element>): Bond? {

            with(data) {
                val element1 = elementsMap[elementId1]
                val element2 = elementsMap[elementId2]
                val powerToElement = elementsMap[powerToElementId]
                if (element1 != null && element2 != null && powerToElement != null) {
                    val bond = Bond(
                        id, element1, Offset(loc1x, loc1y), element2, Offset(locx2, locy2), powerToElement
                    )
                    bond.displayId = displayId
                    if (effortElementId != -1) {
                        bond.effortElement = elementsMap[effortElementId]
                    }
                    return bond
                } else {
                    return null
                }
            }
        }
    }
}
@Serializable
class BondGraphSerializationData(val elementData: List<ElementSerializationData>, val bondData: List<BondSerializationData>)

@Serializable
class BondGraphSerializationData2(val elementData: List<ElementSerializationData>, val bondData: List<BondSerializationData>, val arbitrarilyAssignedResistorsIds: List<Int>)


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
            return Offset((endOffset.x + sign*(arrowLength * cos(angle - sign * arrowAngle))),
                        endOffset.y + sign*(arrowLength * sin(angle - sign * arrowAngle)))
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
            val d = (offset1 - offset2).getDistance()
            return Offset((offset1.x - (l * (offset1.x - offset2.x)/d)), offset1.y - (l * (offset1.y - offset2.y)/d))
        }
    }

    /*
    All the information for the bond graph is stored in two maps, one for the elements and one
    for the bonds.  These lists include both the bond graph info such as which elements are connected,
    the power directions, causal settings ect. and all the graphical info needed to draw the bond
    graph on the screen.  The two maps are linked in that an element store which bonds connect to it,
    and a bond stores which two elements it connects.  Clearly only one map is really required, but
    it is easier to process the bond graph from two maps rather than having to generate the info in
    one map from the other when needed. Having two maps is definitely better for storing the
    graphical data.  However, there have been a few bugs caused by failing to update both lists
    properly when adding or removing elements or bonds.

     */
    private var elementsMap = linkedMapOf<Int, Element>() // map of element ids mapped to their elements
    var bondsMap = mutableStateMapOf<Int, Bond>() // Map of bond ids mapped to their bonds.
    val arbitrarilyAssignedResistors = arrayListOf<Element>() // List of resistors that were assigned causality arbitrarily.
    val results = Results()

    var graphHasChanged = false
    var newElementId = 0
    var newBondId = 0



    fun toSerializedStrings(): String {


        val elementData = elementsMap.values.map{ElementSerializationData.getData(it)}
        val bondData = bondsMap.values.map{BondSerializationData.getData(it)}
        val arbitrarilyAssignedResistorsIds = arrayListOf<Int>()


        arbitrarilyAssignedResistors.forEach { arbitrarilyAssignedResistorsIds.add(it.id) }
        return Cbor.encodeToHexString( BondGraphSerializationData2(elementData, bondData, arbitrarilyAssignedResistorsIds))
        }

@Composable
    fun fromSerializedStrings(serializedString: String) {

        val state = LocalStateInfo.current
        val data:BondGraphSerializationData2 = Cbor.decodeFromHexString(serializedString)

        elementsMap.clear()
        bondsMap.clear()
        arbitrarilyAssignedResistors.clear()
        results.clear()

        for (elementDatum in data.elementData) {
            val element = ElementSerializationData.makeElement(this, elementDatum)
            key(element.id){elementsMap[element.id] = element}
        }

        for (bondDatum in data.bondData){
            val bond = BondSerializationData.makeBond(bondDatum, elementsMap)
            if (bond != null) {
                bondsMap[bond.id] = bond
                bond.element1.addBond(bond)
                bond.element2.addBond(bond)
            }
        }

        println("data.arbitrarilyAssignedResistorsIds.size = ${data.arbitrarilyAssignedResistorsIds.size}")
        data.arbitrarilyAssignedResistorsIds.forEach{ elementsMap.get(it)
            ?.let { it1 -> arbitrarilyAssignedResistors.add(it1) } }

        elementsMap.values.forEach{it.createDisplayId()}

        newElementId = elementsMap.values.maxOf{it.id} + 1
        newBondId = if (bondsMap.size > 0) bondsMap.values.maxOf{it.id} + 1 else 0
        results.clear()
        state.showResults = false
        state.needsElementUpdate = true
    }

    fun getNextElementId() = newElementId++
    fun getNextBondId() = newBondId++

    // Add or update an element in the bond graph.
    fun addElement(id: Int, elementType: ElementTypes, location: Offset, centerOffset: Offset) {
        graphHasChanged = true
        if (elementsMap.contains(id)){
            // Existing element was dragged so update position data. When dragging, the
            // display data was set to null, so it has to be reset also.
            elementsMap[id]?.displayData?.text = elementType.toAnnotatedString()
            elementsMap[id]?.displayData?.location = location
            elementsMap[id]?.displayData?.centerLocation = centerOffset
        } else {
            val elementClass = Element.getElementClass(elementType)

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

    fun getElementList(): List<Element> = ArrayList(elementsMap.values)

    fun getElement(id: Int): Element? {
        return elementsMap[id]
    }

    // Check to see if the point (x,y) is close to an element that is not the originId element.  We start
    // dragging out a new bond for some element (originId).  We want to know if we are getting close to
    // another element.  So
    // 1. map the elements to their distance from the point.
    // 2. filter to see if any of the distances are within epsilon.
    // 3. Take the closest one if more than one.
    // 4. The origin doesn't count.
    // 5. return -1 if we are not close to anything.

    fun findElement(offset: Offset, originId: Int): Int {
        val epsilon = 50
        val result = elementsMap
            .mapValues { (_,v) -> (v.displayData.centerLocation - offset).getDistance()}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }
        return if (result == null || result.key == originId) -1 else result.key
    }

    /*
    Remove an element from the bond graph. We must also remove any
    bonds attached to it, since we don't allow disconnected bonds.
    So we have to get a list of bonds attached to this element and
    then for each bond get the two elements attached to it and then
    delete each element's reference to that bond.  We do this for
    both elements even though one of the elements is the one we
    will be deleting, just because it is easier to code rather than
    checking each case to find which is the other element.
    Then we can remove bond from the bondsMap.  After processing all
    the bonds, we can remove the element.  Finally, we remove the
    augmentation from the bond graph since it is no longer valid.
     */
    fun removeElement (id: Int) {
        graphHasChanged = true
        elementsMap[id]?.getBondList()?.forEach{
            it.element1.removeBond(it.id)
            it.element2.removeBond(it.id)
            bondsMap.remove(it.id)
        }
        elementsMap.remove(id)
        removeBondAugmentation()
    }

    /*
    A function for iterating over the elementsMap and calling a function on
    each element. This way the outside world doesn't need direct access to
    the elementsMap.  I tried to do this with the bondsMap too, but calls
    to that function occur inside a Modifier, and attempts to make the
    function  @Composable caused the compiler to crash with a huge stack
    trace, an indication of the complexity of the @Composable methodology.
     */
    @Composable
    fun forEachElement ( fn: @Composable (element: Element) -> Unit ) {
        for (p  in elementsMap) {
           key(p.key) {fn(p.value)}
        }
    }

    // Clear everything from the bond graph and start over.
    @Composable
    fun clear(){
        graphHasChanged = false
        val state = LocalStateInfo.current
        elementsMap.clear()
        bondsMap.clear()
        newElementId = 0
        newBondId = 0
        state.needsElementUpdate = true
    }

    /*
    Use the provided information to create a bond and add it to the bond graph.  It needs to be added
    to the bondsMap and two each element it attaches to. Remove augmentation which is now invalid.
     */
    fun addBond(id: Int, elementId1: Int, offset1: Offset, elementId2: Int, offset2: Offset, powerToElementId: Int) {
        val element1 = elementsMap[elementId1]
        val element2 = elementsMap[elementId2]

        graphHasChanged = true

        if (element1 != null && element2 != null) {
            val powerToElement = if (element1.id == powerToElementId) element1 else element2
            val bond = Bond(id, element1, offset1, element2, offset2, powerToElement)
            bondsMap[id] = bond
            element1.addBond(bond)
            element2.addBond(bond)
            removeBondAugmentation()
        }
    }

    fun getBond(id: Int): Bond? {
        return bondsMap[id]
    }

    /*
    Used to see if the user has clicked near a bond.
    This function searches the bonds to see if the point (x,y) lies on any of them. Basically if
    we have a line from point p1 to point p2, we want to know if point px lies on the line.  To
    check this we use the idea that the distance for p1 to px + the distance from  p2 to px must
    equal the distance form p1 to p2,  d1x + d2x = d12.  To account for floating point error and
    to allow for clicking near the line, we check  -epsilon < d1x + d2x - d12 < epsilon where
    epsilon is a margin for error determined by experiment. So the steps are
    1. map all the bonds to their d1x + d2x - d12 value
    2. filter for the value being between - and + epsilon
    3. choose the one with the smallest value if there is more than one.
    4. return -1 if the click was not near a bond.
    */
    fun findBond(offset: Offset): Int {
        val epsilon = 5f
        val result = bondsMap
            .mapValues {(_, v) -> (v.offset1 - offset).getDistance() + (v.offset2 - offset).getDistance() - (v.offset1 - v.offset2).getDistance()}
            .filter { (_, v) -> -epsilon < v && v < epsilon }
            .minByOrNull { (_, value) -> value }
        return result?.key ?: -1
    }

    /*
    Remove a bond form the bond graph. First get each element attached
    to this bond and remove the element's references to the bond.
    Then remove the bond from the bondsMap.
     */
    fun removeBond(id: Int){
        graphHasChanged = true
        elementsMap[bondsMap[id]?.element1?.id]?.removeBond(id)
        elementsMap[bondsMap[id]?.element2?.id]?.removeBond(id)
        bondsMap.remove(id)
        removeBondAugmentation()
    }

    /*
    Set which element on the bond is the power element.  First make sure
    the element is one of the elements attached to the bond.
     */
    fun setPowerElement(id: Int, element: Element){
        graphHasChanged = true
        if (bondsMap[id] != null){
            if(bondsMap[id]?.element1 === element || bondsMap[id]?.element2 === element){
                bondsMap[id]?.powerToElement = element
            }
        }
    }

    /*
    Set which element on the bond is the causal or Effort element.  First make sure
    the element is one of the elements attached to the bond.
     */
    fun setCasualElement(id: Int, element: Element?) {
        graphHasChanged = true
        if (bondsMap[id] != null){
            if(bondsMap[id]?.element1 == element || bondsMap[id]?.element2 == element){
                bondsMap[id]?.effortElement = element
            }
        }
    }

    /*
    This function is used when the user is dragging an element that has bonds
    attached to it.  As the element moves we need to update the endpoint of the
    bond so that they stay close to the element. Both ends of the bond need to be
    updated so the bond stays aligned with the centers of both elements. We need
    to call offsetsFromCenter on both ends of ever bond attached to the element.
    TO do that we the width, height and center offset for both elements
    attached to the bond.  So get the width hand height of the element defined
    by elementId.  Then get a alist of all the bonds attached to that element.
    Then for each bond find the other element and get its width, height and
    center.  Then make the calls to set the bond offsets.

     */
    fun updateBondsForElement(elementId: Int, movingCenter: Offset)  {
        graphHasChanged = true
        val movingWidth = elementsMap[elementId]?.displayData?.width
        val movingHeight = elementsMap[elementId]?.displayData?.height
        val bondsList = elementsMap[elementId]?.getBondList()
        if (movingWidth != null && movingHeight != null &&  ! bondsList.isNullOrEmpty()) {
            for (bond in bondsList){
                if (bond.element1.id == elementId) {
                    val fixedCenter = bond.element2.displayData.centerLocation
                    val fixedWidth = bond.element2.displayData.width
                    val fixedHeight = bond.element2.displayData.height
                    bond.offset2 = offsetFromCenter(fixedCenter, movingCenter, fixedWidth, fixedHeight)
                    bond.offset1 = offsetFromCenter(movingCenter, fixedCenter, movingWidth, movingHeight)
                } else {
                    val fixedCenter = bond.element1.displayData.centerLocation
                    val fixedWidth = bond.element1.displayData.width
                    val fixedHeight = bond.element1.displayData.height
                    bond.offset1 = offsetFromCenter(fixedCenter, movingCenter, fixedWidth, fixedHeight)
                    bond.offset2 = offsetFromCenter(movingCenter, fixedCenter, movingWidth, movingHeight)
                }
            }
        }
    }

    // Iterate the bondsMap to see if every bond has an effortElement assigned.
    private fun causalityComplete () = bondsMap.all{it.value.effortElement != null}

    // iterate the elementsMap and make a list of storage elements (capacitors and inertias) that so not have an effort element assigned.
    private fun getUnassignedStorageElements() = elementsMap.values.filter{ v  -> (v.elementType == CAPACITOR || v.elementType == INERTIA) && v.getBondList()[0].effortElement == null}

    // iterate the elementsMap and make a list of resistors that so not have an effort element assigned.
    private fun getUnassignedResistors() = elementsMap.values.filter{ v -> v.elementType == RESISTOR  && v.getBondList()[0].effortElement == null}

    /*
    Search the elementsMap looking for storage elements (capacitors and inertias) that are in integral causality.
    That is capacitors that are setting the effort, and inertias that are setting the flow.
     */
    private fun getIndependentStorageElements()  = elementsMap.values.filter { v -> (v.elementType == CAPACITOR && v.getBondList()[0].effortElement != v) ||
            (v.elementType == INERTIA && v.getBondList()[0].effortElement == v)}

    /*
    We remove tha augmentation when the bond graph is changed, such as adding
    or deleting elements or bonds.  The previous augmentation would no longer
    be valid.
    */
    private fun removeBondAugmentation() {
        graphHasChanged = true
        bondsMap.values.forEach { it.effortElement = null
        it.displayId = ""
        }
    }

    /*
    The first step in augmenting a bond graph is to assign a unique number to each bond, typically
    from 1 to the number of bonds.  The numbers are used to distinguish different entities in the
    bond graph. For example the flow on bond 4 would be called f4.

    The second step is to assign causality. I can't thoroughly explain causality in a comment,
    this usually takes a couple of college lectures.  But it deals with the idea that an
    element in a bond graph can set either the effort or the flow on a bond but not both.
    For an easy example, a source of effort has only one causality. It sets the effort on a bond.
    The flow is then determined by what the bond connects to on the other end.  To see this,
    think of a battery, a source of effort.  It sets the voltage on a circuit, say 12 volts.
    If it maintains a steady 12 volts, it can't also set the current, the flow.  The rest of
    the circuit will determine how much current is drawn.

    Every type of element has rules about the causality of it bonds. Some like a source of effort
    have only one. Some allow certain combinations.  Some have preferred causality from a modeling
    point of view.  To assign causality start with a sourced of effort or flow and assign its
    required causality.  Then look at what it is connected to and see if that element's rules force
    the causality on any other bonds.  For example, a source of effort connected to a 1 junction
    (common flow junction) doesn't force anything.  But a source of effort connected to a
    0 junction (common effort junction) does a lot.  Since all bonds on a 0 junction have the same
    effort (by definition) only one can set that effort.  All the others must set the flow. So now
    you check what all those bonds are attached to and see if anything else is forced etc.
    There are more rules about what bonds to set and extend until every bond has been set.

    Also, during augmentation the bond graph will be checked for errors that would make it a
    nonfunctional graph.
     */
   @Composable
    fun augment() {
        graphHasChanged = true

       val state = LocalStateInfo.current

       // Remove any previous augmentation
       removeBondAugmentation()

       try{

           if(bondsMap.isEmpty()){
               throw BadGraphException("Error: graph has no bonds")
           }
           // Assign number labels to the bonds
           /*TODO: assign bond numbers considering their display location and the elements they
               attach to.  Try to get the numbers to flow across the graph in order. Elements
               like transformers should have consecutive numbers on their bonds.
          */
           var cnt = 1
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



           /*
           Assign causality starting from the sources.  Each element
           has a function for assigning causality based on its
           rules. So calling an element's assignCausality() function
           will start a chain of calls to other element's
           assignCausality() functions.
           */
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
                       bondsMap.values.forEach{println ("bond ${it.id} effort element= ${it.effortElement?.id}")}
                   } else {
                       done = true
                   }
               }
           }

           // If causality is still incomplete then continue
           // using R elements.
           done = causalityComplete()
           while ( ! done ){
               if (! done){
                   val elementList = getUnassignedResistors()
                   if (elementList.isNotEmpty()){
                       elementList[0].assignCausality()
                       arbitrarilyAssignedResistors.add(elementList[0])
                       done = causalityComplete()
                   } else {
                       done = true
                   }
               }
           }

       }catch(e: BadGraphException ) {
           results.clear()
           results.add(e.message.toString())
           state.showResults = true
       }

       }

    @Composable
    fun derive(){

        val state = LocalStateInfo.current


        try {

            results.clear()

            /*
          Create a name for each element based on its type
          and the numbers of the bonds it's attached to.
          Each element type has a function for creating
          its name from the numbers of the bonds it's
          attached to.
          */

            elementsMap.forEach { it.value.createDisplayId() }
            elementsMap.forEach{it.value.createTokens()}

            if (! causalityComplete()) throw BadGraphException("Error: Graph is not completely augmented")

            println("arbitrarilyAssignedResistors.size = ${arbitrarilyAssignedResistors.size }")
            if (arbitrarilyAssignedResistors.size > 0){
                val equationsList = arrayListOf<Equation>()
                val relativilySolvedList = arrayListOf<Equation>()
                arbitrarilyAssignedResistors.forEach { println("calling deriveEquation on ${it.displayId}"); equationsList.add(it.deriveEquation()) }
                results.add(equationsList[0].toAnnotatedString())
                equationsList.forEach { relativilySolvedList.add( solve(it.leftSide as Token, it)) }
                relativilySolvedList.forEach { results.add(it.toAnnotatedString()) }
                state.showResults = true
                return
            }

            val elementsList = getIndependentStorageElements()
            if (elementsList.isEmpty()) throw BadGraphException("Error: There are no independent capacitors or resistors.")

            for (element in elementsList ) {
                results.add(element.deriveEquation().toAnnotatedString())
            }
            state.showResults = true

        }catch(e: BadGraphException ) {
            results.clear()
            results.add(e.message.toString())
            state.showResults = true
        }
        catch (e: AlgebraException){
            results.clear()
            results.add(e.message.toString())
            state.showResults = true
        }
    }
}

