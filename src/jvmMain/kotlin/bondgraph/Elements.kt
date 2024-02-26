package bondgraph

import algebra.*
import algebra.Number
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import userInterface.MyConstants
import bondgraph.PowerVar.*
import bondgraph.Operation.*

/*
An enum  for the different elements used in a bond graph, with the
capability to convert enum value to an AnnotatedString and from
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
    INVALID_TYPE {
        override fun toAnnotatedString() = INVALID
    };

    abstract fun toAnnotatedString(): AnnotatedString
    /*
    We use an AnnotatedString so that the 'e' and 'f' in the 'Se' and 'Sf' elements
    can be subscripts.
     */
    companion object {

        private val style = SpanStyle(fontSize = MyConstants.elementNameFontSize, fontFamily = FontFamily.Serif)
        private val subStyle = SpanStyle(fontSize = MyConstants.subTextFontSize)
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
        val INVALID = AnnotatedString("INVALID", style)

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
                else -> INVALID_TYPE
            }
        }

        fun toEnum(value: String): ElementTypes {
            return when (value) {
                "0" -> ZERO_JUNCTION
                "1" -> ONE_JUNCTION
                "C" -> CAPACITOR
                "R'" -> RESISTOR
                "I" -> INERTIA
                "TF" -> TRANSFORMER
                "GY" -> GYRATOR
                "MTF" -> MODULATED_TRANSFORMER
                "Se" -> SOURCE_OF_EFFORT
                "Sf" -> SOURCE_OF_FLOW
                else -> INVALID_TYPE
            }
        }
    }
}

enum class PowerVar {
    EFFORT {
        override fun toString() = "effort"
   }
    ,FLOW {
        override fun toString() = "flow"
    }
    ,UNKNOWN {
        override fun toString() = "unknown"
    };

    companion object{
        fun toEnum(string: String): PowerVar {
            when  (string){
                "effort" -> return EFFORT
                "flow" -> return FLOW
                else -> return UNKNOWN
            }
        }
    }
}

enum class Operation {
    MULTIPLY {
        override fun toString() = "multiply"
    }
    ,DIVIDE {
        override fun toString() = "divide"
    }
    ,UNKNOWN {
        override fun toString() = "unknown"
    };

    companion object{
        fun toEnum(string: String): Operation {
            when (string){
                "multiply" -> return MULTIPLY
                "divide" -> return DIVIDE
                else -> return UNKNOWN
            }
        }
    }
}
data class OnePortValueData(val element: Element, var description: String = "", var value: Double? = null, var units: String = "")

/*
    Typical bond graph convetion is to express the constitutive laws for transformers and gyrators as follows:
    TF e1 = me2
    Gy e1 = Rf2
    Hence the naming convention below, take variable 2 and perform operation on it to get variable 1.
 */
data class TwoPortValueData(val element: Element, var units: String = "", var description: String = "", var operation: Operation,
                       var powerVar2: PowerVar, var bond2:Bond, var value: Double? = null, var powerVar1: PowerVar, var bond1:Bond)
@Serializable
class OnePortValueSerializationData(val elementId: Int, val description: String, val value: Double?, val units: String) {

    companion object {
        fun getData(onePortValueData: OnePortValueData): OnePortValueSerializationData {
            return with(onePortValueData) {
                OnePortValueSerializationData(
                    elementId = element.id,
                    description = description,
                    value = value,
                    units = units
                )
            }
        }

        fun makeOnePortValueData(bondGraph: BondGraph,  data: OnePortValueSerializationData): OnePortValueData{
            return with (data){
                OnePortValueData(
                    element = bondGraph.getElement(elementId)!!
                    ,description = description
                    ,value = value
                    ,units = units
                )
            }
        }
    }
}
@Serializable
class TwoPortValueSerializationData(val elementId: Int, val units: String, val description: String, val operation: String,
                                    val powerVar1: String, val bondId1: Int, val value: Double?, val powerVar2: String, val bondId2: Int) {
    companion object {
        fun getData(twoPortValueData: TwoPortValueData): TwoPortValueSerializationData {
            return with (twoPortValueData) {
                TwoPortValueSerializationData(
                    elementId = element.id
                    ,units = units
                    ,description = description
                    ,operation = operation.toString()
                    ,powerVar1 = powerVar1.toString()
                    ,bondId1 = bond1.id
                    ,value = value
                    ,powerVar2 = powerVar2.toString()
                    ,bondId2 = bond2.id
                )
            }
        }

        fun makeTwoPortValueData(bondGraph: BondGraph, data: TwoPortValueSerializationData): TwoPortValueData{
            return with(data) {
                TwoPortValueData(
                    element = bondGraph.getElement(elementId)!!
                    ,units = units
                    ,description = description
                    ,operation = Operation.toEnum(operation)
                    ,powerVar1 = PowerVar.toEnum(powerVar1)
                    ,bond1 =  bondGraph.getBond(bondId1)!!
                    ,value = value
                    ,powerVar2 = PowerVar.toEnum(powerVar2)
                    ,bond2 = bondGraph.getBond(bondId2)!!
                )
            }
        }
    }
}

@Serializable
class ValuesSetSerializationData (val id :Int, val description: String, val onePortData: List<OnePortValueSerializationData>, val twoPortData: List<TwoPortValueSerializationData>){
    companion object {
        fun getData(valuesSet: ValuesSet): ValuesSetSerializationData {
            with(valuesSet) {
                val onePortData = onePortValues.values.map { OnePortValueSerializationData.getData(it) }
                val twoPortData = twoPortValues.values.map { TwoPortValueSerializationData.getData(it) }
                return ValuesSetSerializationData(id, description, onePortData, twoPortData)
            }
        }

        fun makeValuesSet(bondGraph: BondGraph, data: ValuesSetSerializationData): ValuesSet {
            with (data) {
                val valuesSet = ValuesSet(id, description)
                onePortData.forEach { valuesSet.onePortValues[bondGraph.getElement(it.elementId)!!] = OnePortValueSerializationData.makeOnePortValueData(bondGraph, it) }
                twoPortData.forEach { valuesSet.twoPortValues[bondGraph.getElement(it.elementId)!!] = TwoPortValueSerializationData.makeTwoPortValueData(bondGraph, it) }
                return valuesSet
            }
        }
    }
}
@Serializable
class ValuesSetsSerializationDataList (val dataList: List<ValuesSetSerializationData>){

    companion object {
        fun getData(bondGraph: BondGraph): ValuesSetsSerializationDataList {
            val dataList = bondGraph.valuesSetsMap.values.map{ValuesSetSerializationData.getData(it)}
            return ValuesSetsSerializationDataList(dataList)
        }

        fun MakeValuesSets(bondGraph: BondGraph, dataList: ValuesSetsSerializationDataList){
            dataList.dataList.forEach { bondGraph.valuesSetsMap[it.id] = ValuesSetSerializationData.makeValuesSet(bondGraph, it) }
        }
    }
}

class ValuesSet(val id: Int, var description: String = "no description", bondGraph: BondGraph? = null) {
    val onePortValues = hashMapOf<Element, OnePortValueData>()
    val twoPortValues = hashMapOf<Element, TwoPortValueData>()

    init {
        if (bondGraph != null ) {
            val eList = arrayListOf<Element>()
            eList.addAll(
                userInterface.bondGraph.getElementList()
                    .filter { it is Capacitor }
                    .sortedBy { it.displayId.toString() }
            )


            eList.addAll(
                userInterface.bondGraph.getElementList()
                    .filter { it is Inertia }
                    .sortedBy { it.displayId.toString() }
            )


            eList.addAll(
                userInterface.bondGraph.getElementList()
                    .filter { it is Resistor }
                    .sortedBy { it.displayId.toString() }
            )

            eList.forEach { onePortValues[it] = OnePortValueData(it) }

            eList.clear()

            eList.addAll(
                userInterface.bondGraph.getElementList()
                    .filter { it is Transformer }
                    .sortedBy { it.displayId.toString() }
            )

            eList.addAll(
                userInterface.bondGraph.getElementList()
                    .filter { it is Gyrator }
                    .sortedBy { it.displayId.toString() }
            )

            eList.forEach {
                val bondList = it.getBondList()
                val bondPair = if (bondList[0].displayId < bondList[1].displayId)
                    Pair(bondList[0], bondList[1]) else Pair(bondList[1], bondList[0])
                if (it is Transformer) {
                    twoPortValues[it] =
                        TwoPortValueData(
                            element = it,
                            operation = MULTIPLY,
                            powerVar1 = EFFORT,
                            bond1 = bondPair.first,
                            powerVar2 = EFFORT,
                            bond2 = bondPair.second
                        )
                } else {
                    twoPortValues[it] =
                        TwoPortValueData(
                            element = it,
                            operation = MULTIPLY,
                            powerVar1 = EFFORT,
                            bond1 = bondPair.first,
                            powerVar2 = FLOW,
                            bond2 = bondPair.second
                        )
                }
            }
        }
    }

    fun copy(id: Int = this.id, description: String = this.description): ValuesSet{
        val valuesSet = ValuesSet(id, description)
        onePortValues.forEach { valuesSet.onePortValues[it.key] = it.value.copy() }
        twoPortValues.forEach{valuesSet.twoPortValues[it.key] = it.value.copy()}
        return valuesSet
    }
}


/*
The data needed to display a representation of the element on the screen.  The id, text and location are
pretty obvious, the width and height are the size of the text, and the centerLocation is the location of
the center of text.  This information is needed for drawing bonds to the element.  Every instance of
Element contains an ElementDisplayData instance as one of its properties.
 */
class ElementDisplayData (val id: Int, var text: AnnotatedString, var location: Offset, val width: Float, val height: Float, var centerLocation: Offset, var color: Color = MyConstants.defaultElementColor)

/*
    A data class that holds data for a element that can be serialized and saved to a file.  It also contains two functions.

    getData(element):ElementSerializationDat  can be used to generate an instance of the class for specific element

    makeElement(bondgraph, ElementSerializationData): Element  can be used to re-construct an element based on the data
 */
@Serializable
class ElementSerializationData(val id: Int, val type: ElementTypes,  val displayDatId: Int,  val locx: Float, val locy: Float,
                               val width: Float, val height: Float, val cenx: Float, val ceny: Float, val red: Float, val green: Float, val blue: Float) {
    companion object {
        fun getData(element: Element): ElementSerializationData {

            return with(element) {
                ElementSerializationData(
                    id,
                    elementType,
                    displayData.id,
                    displayData.location.x,
                    displayData.location.y,
                    displayData.width,
                    displayData.height,
                    displayData.centerLocation.x,
                    displayData.centerLocation.y,
                    displayData.color.red,
                    displayData.color.green,
                    displayData.color.blue

                )
            }
        }

        fun makeElement(bondgraph: BondGraph, data: ElementSerializationData): Element {
            val elementType = data.type
            val elementClass = Element.getElementClass(elementType)
            with(data) {
                if (elementClass != null) {
                   return elementClass.invoke(
                        bondgraph,
                        id,
                        elementType,
                        ElementDisplayData(displayDatId, elementType.toAnnotatedString(), Offset(locx, locy), width, height, Offset(cenx, ceny), Color(red, green, blue))
                    )
                } else {
                    throw BadGraphException("Error in function makeElement, invalid ElementType = $elementType, derived from string ${data.type}")
                }
            }
        }
    }
}

class Modulator () {
    var mToken: Token? = null

    /*
        The general convention for bond graphs is to express the constitutive laws for transformers and
        gyrators as follows:
        TF  e1 = me2
        GY e1 = rf2
        The valueNumber and bondToMultiply below represent the right side of the
        above equations. The other forms of the equations such as such rf1 = e2 will be derived
        from the below values.
    */
    var value: Double? = null
    var bomdToMulitply: Bond? = null

    fun createToken(bondList: ArrayList<Bond>) {
        val tokenString = if (bondList[0].element1 is Transformer || bondList[0].element2 is Transformer) "M" else "R"
        val bondIdPair = if (bondList[0].displayId < bondList[1].displayId)
            Pair(bondList[0], bondList[1]) else
            Pair(bondList[1], bondList[0])
        mToken = Token(
            bondIdPair.first.displayId,
            bondIdPair.second.displayId,
            AnnotatedString(tokenString),
            false,
            false,
            false,
            false
        )
        bomdToMulitply = bondIdPair.first
    }

    fun setValue(twoPortValueData: TwoPortValueData?) {
        // perform operation on powerVar2 to get powerVar1.  See comment above
        if (twoPortValueData != null && twoPortValueData.value != null){

            with(twoPortValueData) {
                this@Modulator.value = value
                if (powerVar2 == powerVar1) {
                    // Transformer
                    if (powerVar1 == EFFORT) {
                        bomdToMulitply = if (operation == MULTIPLY) bond2 else bond1
                    } else {
                        bomdToMulitply = if (operation == MULTIPLY) bond1 else bond2
                    }
                } else {
                    // Gyrator
                    bomdToMulitply = if (operation == MULTIPLY) bond2 else bond1
                }
            }
        }
    }

    fun getEffortModulator(bond: Bond): Expr {

        if (bomdToMulitply == null) throw BadGraphException("Error: Call to getEffortModulator on un-initialize modulator.  Bond = ${bond.displayId}")

        if (value != null) {
            return if (bond == bomdToMulitply) Number(1.0 / value!!) else Number(value!!)
        }

        return if (bond == bomdToMulitply) mToken!! else Term().divide(mToken!!)
    }

    fun getFlowModulator(bond: Bond):Expr {
        if (bomdToMulitply == null) throw BadGraphException("Error: Call to getFlowModulator on un-initialize modulator.  Bond = ${bond.displayId}")

        if (value != null) {
            return if (bond == bomdToMulitply) Number(value!!) else Number(1.0/value!!)
        }

        return if (bond == bomdToMulitply) Term().divide(mToken!!) else mToken!!
    }
}
abstract class Element(val bondGraph:  BondGraph, val id: Int, val elementType: ElementTypes, var displayData: ElementDisplayData){

    var displayId: @Contextual AnnotatedString = AnnotatedString(id.toString())

    // A list of all the bonds attached to this element
    val bondsMap = linkedMapOf<Int, Bond>()

    companion object {

        // Every bond is attached to two elements. Given a bond and one of the elements attached to it, return the
        // other element.
        fun getOtherElement(element: Element, bond: Bond) = if (element === bond.element1) bond.element2 else bond.element1

        fun  getElementClass(elementType: ElementTypes) =

            when (elementType) {
                ElementTypes.ZERO_JUNCTION -> ::ZeroJunction
                ElementTypes.ONE_JUNCTION -> ::OneJunction
                ElementTypes.CAPACITOR -> ::Capacitor
                ElementTypes.RESISTOR -> ::Resistor
                ElementTypes.INERTIA -> ::Inertia
                ElementTypes.TRANSFORMER -> ::Transformer
                ElementTypes.GYRATOR -> ::Gyrator
                ElementTypes.MODULATED_TRANSFORMER -> ::ModulatedTransformer
                ElementTypes.SOURCE_OF_EFFORT -> ::SourceOfEffort
                ElementTypes.SOURCE_OF_FLOW -> :: SourceOfFlow
                ElementTypes.INVALID_TYPE -> null
            }
    }

    // Convert bondsMap to a list
    //fun getBondList(): List<Bond> = ArrayList(bondsMap.values)
    fun getBondList() = ArrayList(bondsMap.values)

    /*
    Return any other elements this element is attached to other than the given element. To do
    this first create a list of all this element's bonds that are not attached to the given
    element.  Then return a list of the other element attached to each of those bonds.
     */
    private fun getOtherElements(element: Element): List<Element>{
        return bondsMap.filter{(_,v) -> v.element1 != element && v.element2 != element}
            .map{(_,v) -> if (v.element1 === this) v.element2 else v.element1}
    }

    /*
    Return a list of this element's bonds that have been assigned causality. Causality
    has been assigned if the effort element for the bond is non null.
     */
    fun getAssignedBonds(): List<Bond> = getBondList().filter{it.effortElement != null}

    /*
   Return a list of this element's bonds that have not been assigned causality. Causality
   has been assigned if the effort element for the bond is non null.
    */
    fun getUnassignedBonds(): List<Bond> = getBondList().filter{it.effortElement == null}


    // Get a list of bond attached to this element that doesn't include the given bond.
    fun getOtherBonds(bond: Bond): List<Bond> = getBondList().filter{ it !==  bond}

    /*
    Used when summing efforts or flows on zero and one junctions. Returns true of both
    bond arrows point toward the junction or both arrows pont away from the junction.
     */
    fun sameDirection(element: Element, bond1: Bond, bond2: Bond): Boolean =
        ((bond1.powerToElement === element &&  bond2.powerToElement === element) ||
        (bond1.powerToElement !== element &&  bond2.powerToElement !== element) )


    /*
    Display id is a unique id for identifying an element in a text field or error message.
    This is generic implementation that will be overridden by most subclasses.  If an id
    is provided it is just converted to a AnnotatedString. Otherwise, it builds an
    AnnotatedString based on the element type and the ids of the bonds it's attached to
    that looks like TF-1,2
     */
    open fun createDisplayId(id: String = ""){
        if (id != "") {
            displayId = AnnotatedString(id)
        } else {
            val bondDisplayIds = bondsMap.flatMap {listOf(it.value.displayId)}
            val s = StringBuilder("")
            for ((index, did) in bondDisplayIds.withIndex()){
                if (index > 0) s.append(",")
                s.append(did)
            }

            displayId =buildAnnotatedString {
                append (elementType.toAnnotatedString())
                append("-")
                append (s.toString())
                toAnnotatedString()
            }

        }
    }

    /*
    Token refers to the Token class defined in the algebra classes file. Each element type overrides
    this method to create the tokens it needs for equation derivation.  An element doesn't have the
    information needed to create its tokens until after the bond graph has been augmented. So the
    BondGraph instance will call createTokens on all elements after augmentation and before equation
    derivation.
     */
    abstract fun createTokens()

    // Add a bond to this elements bondsList.  Several subtypes will override this basic version. Element.addBond
    // should only be called by functions in the bond graph class as part as part of adding a bond to the bond graph.
    open fun addBond(bond: Bond) {
        bondsMap[bond.id] = bond
    }

    /*
    Each element has its own rules for assigning causality on its bonds so, the they all must
    override this function.  If an element assigns causality on one of its bonds, then it must
    call this function on the other element attached to the bond.  This is how causality is
    propagated through the bond graph.
     */
    abstract fun assignCausality()


    /*
    This function is called recursively to count all the elemenes in a bond graph reachable from
    some starting element. The function is given an element and a starting count. It adds 1
    for to the count for the given element, and then call itself again on all other elements
    attached to the given elements and adds in those counts.
     */
    open fun countElements(element: Element, count: Int): Int{
        // List of other elements attached to this element
        val elementList = getOtherElements(element)

        // add one for this element
        var cnt = 1

        // add counts from the other elements attached to this element. Cnt
        // grows as it is used as the seed in successive calls.
        elementList.forEach{cnt = it.countElements(this, cnt)}

        // add our accumulated count to the origin count and return.
        return count + cnt
    }

    // Remove the bond from this element's bondsMap
    fun removeBond(id: Int) {
        bondsMap.remove(id)
    }

    // Return algebraic expression for the flow based on the elements constitutive laws.
    abstract fun getFlow(bond: Bond): Expr

    // Return algebraic expression for the effort based on the elements constitutive laws.
    abstract fun getEffort(bond: Bond): Expr

}

abstract class OnePort (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {
    // eTOken is the token for the energy variable for a inertia or capacitor
    // eDotToken is the token for the time derivative of the energy variable.
    // vToken is for the value of the oneport, the capacitance for example
    var eToken = Token()
    var eDotToken = Token()
    var vToken = Token()

    // The value of the oneport. It will be either vToken or a Number() representing the numeric value.
    var valueExpr: Expr? = null


    // Expression to use in place of constitutive law in the case of resistors with arbitrarily assigned
    // causality or dependent inertias or capacitors with derivative causality.
    var substituteExprssion: Expr? = null

    //var elementValue: Double? = null

    abstract fun deriveEquation(): Equation

    // All elements must be able to express their constitutive laws for efforts and flows using the algebra classes.
    override abstract fun getEffort(bond: Bond): Expr

    override abstract fun getFlow(bond: Bond): Expr

    fun setValue(data: OnePortValueData? = null) {

        if (vToken.bondId1.equals("") )throw BadGraphException("Error: Call to assignValue and vToken has not been assigned. createTokens() should be called before assignValue()")

        if (data == null || data.value == null){
            valueExpr = vToken
        } else {
            valueExpr = Number(data.value!!)
        }
    }


      //  Add the bond to the element.  First remove the old bonds (there should only be one).
    override fun addBond(bond: Bond){
        if (bondsMap.size > 0){
            getBondList().forEach { bondGraph.removeBond(it.id) }
            bondsMap.clear()
        }
        bondsMap[bond.id] = bond
    }

    // One port display id is the element type followed by the number of the bond it is attched to. Example C1
    override fun createDisplayId(id: String) {
        val bondList = getBondList()
        if (bondList.isNotEmpty()){
            displayId = buildAnnotatedString {
                append(elementType.toAnnotatedString())
                append(bondList[0].displayId)
                toAnnotatedString()
            }
        }
    }
}
abstract class TwoPort (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {

    val modulator = Modulator()
    override abstract fun getEffort(bond: Bond): Expr

    override abstract fun getFlow(bond: Bond): Expr

    override fun createTokens() {
        modulator.createToken(getBondList())
    }

    fun setValue(twoPortValueData: TwoPortValueData?) {
        modulator.setValue(twoPortValueData)
    }

    /*
        Add the bond to this element.  If this element already has two bonds, we need to delete
        both of them because we don't know which one the user is trying to replace.
     */
    override fun addBond(bond: Bond) {
        if (bondsMap.size == 2){
            getBondList().forEach{ bondGraph.removeBond(it.id)}
            bondsMap.clear()
        }
        bondsMap[bond.id] = bond
    }

    // Two port display id is bond number element type bond number. Ex 5TF6
    override fun createDisplayId(id: String) {
        val bondList = getBondList()
        if (bondList.size == 1) throw BadGraphException("Error: The 2-port on bond ${bondList[0].displayId} is missing a bond")
        if (bondList.isNotEmpty()){

            val bondIdPair = if (bondList[0].displayId < bondList[1].displayId)
                Pair(bondList[0].displayId , bondList[1].displayId) else
                Pair(bondList[1].displayId , bondList[0].displayId)

            displayId = buildAnnotatedString {
                append(elementType.toAnnotatedString())
                append(bondIdPair.first)
                append(",")
                append(bondIdPair.second)
                toAnnotatedString()
            }
        }
    }

}

class OneJunction (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {

    /*
        There are two situations where a one junction can set causality.
        1. Some other element has set the flow on the one junction. Since all flows on a one junction are equal,
           only one bond can set the flow on the junction.  The one junction must then set the flow on the other
           bonds.
        2. There is only one bond that does not have causality set and no other bond is setting the flow on the
           one junction.  So this bond must set the flow on the one junction and the effort on the other element.
     */
    override fun assignCausality() {
        val assignedBonds = getAssignedBonds()
        val unassignedBonds = getUnassignedBonds()
        val settingFlow = assignedBonds.filter{ it.effortElement !== this}
        if (settingFlow.size > 1) throw BadGraphException("Error: Multiple bonds an 1 junction are setting flow. ${this.displayId}")
        if (settingFlow.size == 1) {
            // A bond is setting the flow on the one junction.  The other bonds must set the flow on the other elements.
            for (bond in unassignedBonds){
                val otherElement = getOtherElement(this, bond)
                bond.effortElement = this
                otherElement.assignCausality()  // Propagate causality to other element.

            }
        } else {
            if (unassignedBonds.size == 1) {  // Last bond so it has to be the one to set the flow
                val bond = unassignedBonds[0]
                val otherElement = getOtherElement(this , bond)
                bond.effortElement = otherElement
                otherElement.assignCausality() // Propagate causality to other element.
            }
        }
    }

    override fun createTokens() {}

    /*
        Only one bond determines the flow on a one junction and that flow is determined
        by the other element attached to that bond. So find the bond setting the flow,
        get the other element, and call getFLow on that element. Note: this function
        doesn't use the bond parameter.
     */
    override fun getFlow(bond: Bond): Expr {
        val bondsList = getBondList()
        val flowBond = bondsList.filter{it.effortElement !== this}[0]
        return getOtherElement(this, flowBond).getFlow(flowBond)

    }

    /*
        The effort on this bond is sum of the efforts of the other bonds attached to this one junction. The
        efforts on those bonds is determined by the other elements attached to those bonds. Whether to
        add or subtract an effort is determined by whether the arrow on the other bond points in the same
        direction as the arrow on this bond i.e. either towards or away from the one junction.  So return
        the algebraic sum of the efforts on the other bonds.
     */
    override fun getEffort(bond: Bond): Expr {

        val otherBonds = getOtherBonds(bond)
        val thisElement = this

        var sum: Expr = Sum()
        for (otherBond in otherBonds ) {
            val otherElement = getOtherElement(thisElement, otherBond)
            sum = if (sameDirection(thisElement, bond, otherBond)) sum.subtract(otherElement.getEffort(otherBond)) else sum.add(otherElement.getEffort(otherBond))
        }
        return sum
    }

}
class ZeroJunction (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {

    /*
       There are two situations where a zero junction can set causality.
       1. Some other element has set the effort on the zero junction. Since all efforts on a zero junction are equal,
          only one bond can set the effort on the junction.  The effort junction must then set the effort on the
          other bonds.
       2. There is only one bond that does not have causality set and no other bond is setting the effort on the
          zero junction.  So this bond must set the effort on the zero junction and the flow on the other element.
    */
    override fun assignCausality() {
        val assignedBonds = getAssignedBonds()
        val unassignedBonds = getUnassignedBonds()
        val settingEffort = assignedBonds.filter{ it.effortElement === this}
        if (settingEffort.size > 1) throw BadGraphException("Error: Multiple bonds on 0 junction are setting effort.  ${this.displayId}")
        if (settingEffort.size == 1) {

            for (bond in unassignedBonds) {
                val otherElement = getOtherElement(this, bond)
                bond.effortElement = otherElement
                otherElement.assignCausality()
            }
        } else {
            if (unassignedBonds.size == 1) {  // Last bond so it has to be the one to set the effort
                val bond = unassignedBonds[0]
                val otherElement = getOtherElement(this , bond)
                bond.effortElement = this
                otherElement.assignCausality()
            }
        }
    }

    override fun createTokens() {}

    /*
       Only one bond determines the effort on a zero junction and that effort is determined
       by the other element attached to that bond. So find the bond setting the effort,
       get the other element, and call getEffort on that element. Note: this function
       doesn't use the bond parameter.
    */
    override fun getEffort(bond: Bond): Expr {
        val bondsList = getBondList()
        val effortBond = bondsList.filter{it.effortElement === this}[0]
        return getOtherElement(this, effortBond).getEffort(effortBond)

    }

    /*
       The flow on this bond is sum of the flows of the other bonds attached to this zero junction. The
       flows on those bonds is determined by the other elements attached to those bonds. Whether to
       add or subtract a flow is determined by whether the arrow on the other bond points in the same
       direction as the arrow on this bond i.e. either towards or away from the zero junction.  So return
       the algebraic sum of the flows on the other bonds.
    */
    override fun getFlow(bond: Bond): Expr {

        val otherBonds = getOtherBonds(bond)
        val thisElement = this

        var sum: Expr = Sum()
        for (otherBond in otherBonds) {
            val otherElement = getOtherElement(thisElement, otherBond)
            sum = if (sameDirection(thisElement, bond, otherBond)) sum.subtract(otherElement.getFlow(otherBond)) else sum.add(otherElement.getFlow(otherBond))
        }
        return sum
    }
}

class Capacitor (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    //var cToken = Token() // the capacitance
    //override var eToken = Token() // the generalized displacement q
    //override var eDotToken = Token()  // time derivative of the displacement


    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]
        if (bond.effortElement == null) throw BadGraphException("Error: Attempt to create tokens on an element with no causality set. Has createdTokens been called before augmntation?")

        vToken = Token(bond.displayId, "",  elementType.toAnnotatedString(), false, false, false, false)
        eToken = Token(bond.displayId, "", AnnotatedString("q"), false, true, bond.effortElement !== this,false,)
        eDotToken = Token(bond.displayId, "", AnnotatedString("q"), false, true, bond.effortElement !== this, true)
    }


    /*
        The preferred causality for a capacitor is for it to set the effort on its bond, that is it imposes
        the effort on the rest of the system. Then the effort on this bond can be expressed as displacement
        divided by capacitance, q/C
     */
    override fun assignCausality() {

        val bond = getBondList()[0]
        if (bond.effortElement == null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = otherElement
            otherElement.assignCausality()
        }
        displayData.color = if (bond.effortElement !== this) MyConstants.defaultElementColor else MyConstants.derivativeCausalityColor
        bond.color = displayData.color
    }

    override fun getFlow(bond: Bond): Expr {

        if (substituteExprssion == null) throw BadGraphException("Error: no substitute expression for Capacitor in derivative causality = $displayId")
        return substituteExprssion as Expr
    }

    override fun getEffort(bond: Bond): Expr {

        if (valueExpr == null) throw BadGraphException("Error: getEffort called on ${displayId }but value has not been assigned.  Has assignValue() been called?")

        return Term().multiply(eToken).divide(valueExpr as Expr)
    }

    override fun deriveEquation(): Equation {
        val bond = getBondList()[0]

        if (eDotToken.bondId1.equals("")) throw BadGraphException("Error: getEffort called on $displayId but tokens have not been created.  Has createdTokens been called?")
        if (bond.effortElement !== this) {
            // Integral causality - independent variable return expression for derivative of displacement
            return Equation(eDotToken, getOtherElement(this, bond).getFlow(bond))
        } else {
            // derivative causality - dependent variable return expression for displacement in terms of other state variables.
            return Equation(eToken, getOtherElement(this, bond).getEffort(bond).multiply(valueExpr as Expr))
        }

    }


}

class Inertia (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, element, displayData) {


    //var iToken = Token()
    //override var eToken = Token()
    //override var eDotToken = Token()

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]
        if (bond.effortElement == null) throw BadGraphException("Error: Attempt to create tokens on an element with no causality set. Has createdTokens been called before augmntation?")

        vToken = Token(bond.displayId, "", elementType.toAnnotatedString(), false, false, false, false)
        eToken = Token(bond.displayId, "", AnnotatedString("p"), false, true, bond.effortElement === this,false,)
        eDotToken = Token(bond.displayId, "", AnnotatedString("p"), false, true, bond.effortElement !== this, true)
    }


    /*
        Preferred causality for an inertia is for it to set the flow on its bond, that is it imposes its flow
        on the rest of the system.  Then the flow on this bond can be expressed as momentum divided by inertia
        f = p/I.
     */
    override fun assignCausality() {

        val bond = getBondList()[0]
        if (bond.effortElement == null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = this
            otherElement.assignCausality()
        }
        displayData.color = if (bond.effortElement === this) MyConstants.defaultElementColor else MyConstants.derivativeCausalityColor
        bond.color = displayData.color
    }

    override fun getEffort(bond: Bond): Expr {
        if (substituteExprssion == null) throw BadGraphException("Error: no substitute expression for Inertia in derivative causality = $displayId")
        return substituteExprssion as Expr
    }

    override fun getFlow(bond: Bond): Expr {

        if (valueExpr == null) throw BadGraphException("Error: getEffort called on ${displayId }but value has not been assigned.  Has assignValue() been called?")
        return Term().multiply(eToken).divide(valueExpr as Expr)
    }

    override fun deriveEquation(): Equation {
        val bond = getBondList()[0]
        if (bond.effortElement === this) {
            // Integral causality - independent variable return expression for derivative of momentum
            return Equation(eDotToken, getOtherElement(this, bond).getEffort(bond))
        } else {
            // derivative causality - dependent variable return expression for momentum in terms of other state variables.
            return Equation(eToken, getOtherElement(this, bond).getFlow(bond).multiply(valueExpr as Expr))
        }
    }
}
/*
    The behaviour of a resistor depends and how the augmentation of the bond graph proceeded. If after assigning
    causality based on the sources and storage elements, causality of the graph is complete, then the causality
    of the resistor was forced on it by the system.  You might consider this the normal case.  In this case when
    asked, the resistor returns effort or flow based on its constitutive law e = fR.

    However, if causality wasn't complete, causality proceeds by arbitrarily assigning causality on one of the
    unassigned resistors. If you attempt to derive equations from a bond graph that has arbitrarily assigned
    resistors, you will run into an infinite loop.  To solve this problem, you must derive an equation for the
    effort or flow (depending on causality) for the resistor in terms of said effort or flow and the other state
    variables. This equation must then be solved for the said effort or flow.  Then during normal equation
    generation this expression must be used as a substitute for the normal constitutive law form of the
    said effort or flow.

    And this brings up a third case. When deriving the above equation, the derivation process will eventually
    come back to this resistor.  If at this point it returns its constitutive law form, the loop become infinite.
    So at this point the resistor must simply return a token representing the effort or flow we are deriving
    for.  This reintroduces that variable into the right side of the equation (creating an equation that must
    be solved) and ending the loop and derivation at that point.

    This is made more complicated if there is more than one unassigned resistors.  In this case there will be
    a set of simultaneous equations that must be solved.  So the solving the derived equation can't be done
    here with access to just one of the equations.  It is done in the BondGraph class which then sets the
    substituteExpression variable for each resistor.
 */
class Resistor (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    // eToken and eDotToken not needed for resistor
    //override var eToken = Token()
    //override var eDotToken = Token()

    //var rToken = Token()  // Resistance token
    var efToken = Token()  // Effort token
    var fToken = Token()  // FLow token
    //var derivingEquation = false  // True if we are in the process of deriving an equation
    var isCausalityArbitrarilyAssigned = false


    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]

        vToken = Token(bond.displayId, "", elementType.toAnnotatedString(), false, false, false, false)
        efToken = Token(bond.displayId, "", AnnotatedString("e"), false, false, false, false)
        fToken = Token(bond.displayId, "", AnnotatedString("f"), false, false, false, false)
    }


    /*
        If this function is called, then the causality of this resistor is being arbitrarily assigned
     */
    override fun assignCausality() {

        val bond = getBondList()[0]
        val otherElement = getOtherElement(this, bond)
        if (bond.effortElement === null) {
            bond.effortElement = otherElement
            otherElement.assignCausality()
        } else {
            if (bondGraph.arbitrarilyAssignedResistors.contains(this)){
                otherElement.assignCausality()
            }
        }
    }

    override fun getEffort(bond: Bond): Expr {
        if (isCausalityArbitrarilyAssigned) {
            if (substituteExprssion == null){
                return efToken
            } else {
                return substituteExprssion as Expr
            }
        } else {
            val sFlow = getOtherElement(this, bond).getFlow(bond)
            return Term().multiply(sFlow).multiply(valueExpr as Expr)
        }
    }

    override fun getFlow(bond: Bond): Expr {
        if (isCausalityArbitrarilyAssigned){
            if (substituteExprssion == null) {
                return fToken
            } else {
                return substituteExprssion as Expr
            }
        } else {
            val sEffort = getOtherElement(this, bond).getEffort(bond)
            return Term().multiply(sEffort).divide(valueExpr as Expr)
        }
    }

    override fun deriveEquation(): Equation {
        val bond = getBondList()[0]
        val otherElement = getOtherElement(this, bond)
        if (bond.effortElement === this){
            // Resistor is imposing flow on system so derive equation for the flow f = e/R
            return Equation(fToken, Term().multiply(otherElement.getEffort(bond)).divide(valueExpr as Expr))
        } else {
            // Resistor is imposing effort on system so derive equation for effort e = fR
            return Equation(efToken, Term().multiply(otherElement.getFlow(bond)).multiply(valueExpr as Expr))
        }
    }
}

class SourceOfEffort(bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    // eToken and eDotToken not needed for sourceOfEffort
    //override var eToken = Token()
    //override var eDotToken = Token()

    //var sToken = Token()

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]

        vToken = Token(bond.displayId, "", AnnotatedString("e"), true, false, false, false)
    }

    // Source of effort imposes effort on the rest of the system.
    override fun assignCausality() {
        val bond = getBondList()[0]
        if (bond.effortElement === null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = otherElement
            otherElement.assignCausality()

        } else {
            if (this === bond.effortElement) throw BadGraphException("Error: A source of effort has been forced into flow causality. ${this.displayId}")
        }
    }

    override fun getFlow(bond: Bond): Expr {
        if (true) throw BadGraphException("Error: call to SourceOfEffort.getFlow()  which makes no since and is clearly an error")
        return Term()
    }


    override fun getEffort(bond: Bond): Expr {
        return Term().multiply(vToken)
    }

    override fun deriveEquation(): Equation {
        if (true) throw BadGraphException("Error: Call to SourceOfFlow.deriveEquation which is not implemented.")

        return Equation(Term(), Term())
    }
}

class SourceOfFlow (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    // eToken and eDotToken not needed for SourceOfFlow
    //override var eToken = Token()
    //override var eDotToken = Token()

   // var sToken = Token()

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]

        vToken = Token(bond.displayId, "", AnnotatedString("f"), true, false, false, false)
    }


    // Source imposes flow on the rest of the system.
    override fun assignCausality() {
        val bond = getBondList()[0]

        if (bond.effortElement === null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = this
            otherElement.assignCausality()
        } else {
            if ( this !== bond.effortElement) throw BadGraphException("Error: A source of flow has been forced into effort causality. ${this.displayId}")
        }
    }

    override fun getEffort(bond: Bond): Expr {
        if (true) throw BadGraphException("Error: call to SourecOfFlow.getEffort()  which make no since and is clearly an error")
        return Term()
    }

    override fun getFlow(bond: Bond): Expr {
        return Term().multiply(vToken)
    }

    override fun deriveEquation(): Equation {
        if (true) throw BadGraphException("Error: Call to SourceOfFLow.deriveEquation which is not implemented.")

        return Equation(Term(), Term())
    }
}

open class Transformer (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): TwoPort(bondGraph, id, elementType, displayData) {

    //var tToken = Token()
    //var mToken = Token()
    //val modulator = Modulator()

    /*override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        //tToken = Token(bondsList[0].displayId, bondsList[1].displayId , elementType.toAnnotatedString(), false, false, false, false)
        //mToken = Token(bondsList[0].displayId, bondsList[1].displayId, AnnotatedString("M"), false, false, false, false)
        modulator.createToken(bondsList)
    }*/

    /*
        A transformer has two bonds.  Some other element has set the causality on one of the bonds and then
        called assignCausality on this element.  We have to set the causality on the other bond.  A transformer
        has two allowable causalities   |--- TF |---   or   ---| TF ---|
        So find the bond that has been set and set the other one appropriately.
     */
    override fun assignCausality() {
        if (bondsMap.size == 1) throw BadGraphException("Error transformer $displayId has only one bond.")
        val assignedBonds = getAssignedBonds()
        if (assignedBonds.size == 2){
            if ( (assignedBonds[0].effortElement === this &&  assignedBonds[1].effortElement === this)
                ||
                ( assignedBonds[1].effortElement !== this && assignedBonds[0].effortElement !== this)
            ) throw BadGraphException("Error: transformer $displayId is being forces into conflicting causality.")
        } else {
            val assignedBond = assignedBonds[0]
            val unassignedBond =  getUnassignedBonds()[0]
            val unassignedOther = getOtherElement(this, unassignedBond)
            if (this === assignedBond.effortElement){
                unassignedBond.effortElement = unassignedOther
            } else {
                unassignedBond.effortElement = this
            }
            unassignedOther.assignCausality()
        }
    }

    override fun getEffort(bond: Bond): Expr {
        //val modulator  = Modulator(getOtherElement(this, getBondList()[0]), mToken)
        //val mod = modulator.getEffortModulator(getOtherElement(this, bond))
        val mod = modulator.getEffortModulator(bond)
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod.multiply(otherElement.getEffort(otherBond))
    }

    override fun getFlow(bond: Bond): Expr {
        //val modulator  = Modulator(getOtherElement(this, getBondList()[0]), mToken)
        //val mod = modulator.getFlowModulator(getOtherElement(this, bond))
        val mod = modulator.getFlowModulator(bond)
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod.multiply(otherElement.getFlow(otherBond))
    }
}

class Gyrator (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): TwoPort(bondGraph, id, elementType, displayData) {

    //var gToken = Token()
    var mToken = Token()
    //val modulator = Modulator()

    /*override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        //gToken = Token(bondsList[0].displayId, bondsList[1].displayId, elementType.toAnnotatedString(), false, false, false, false )
        //mToken = Token(bondsList[0].displayId, bondsList[1].displayId, AnnotatedString("M"), false, false, false, false)
        modulator.createToken(bondsList)
    }*/

    /*
       A gyrator has two bonds.  Some other element has set the causality on one of the bonds and then
       called assignCausality on this element.  We have to set the causality on the other bond.  A gyrator
       has two allowable causalities   |--- GY ---|   or   ---| GY |---
       So find the bond that has been set and set the other one appropriately.
    */
    override fun assignCausality() {
        if (bondsMap.size == 1) throw BadGraphException("Error gyrator $displayId has only one bond.")
        val assignedBonds = getAssignedBonds()
        if (assignedBonds.size == 2) {
            if ((assignedBonds[0].effortElement === this && assignedBonds[1].effortElement !== this)
                ||
                (assignedBonds[0].effortElement !== this && assignedBonds[1].effortElement === this)
            ) throw BadGraphException("Error: gyrator $displayId is being forces into conflicting causality.")
        } else {
            val assignedBond = assignedBonds[0]
            val unassignedBond = getUnassignedBonds()[0]
            val unassignedOther = getOtherElement(this, unassignedBond)
            if (this === assignedBond.effortElement) {
                unassignedBond.effortElement = this
            } else {
                unassignedBond.effortElement = unassignedOther
            }
            unassignedOther.assignCausality()
        }
    }

    override fun getEffort(bond: Bond): Expr {
        val mod = modulator.getEffortModulator(bond)
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod.multiply(otherElement.getFlow(otherBond))
    }

    override fun getFlow(bond: Bond): Expr {
        val mod = modulator.getFlowModulator(bond)
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod.multiply(otherElement.getEffort(otherBond))
    }
}


class ModulatedTransformer (bondGraph: BondGraph, id: Int,  elementType: ElementTypes, displayData: ElementDisplayData): Transformer(bondGraph, id, elementType, displayData)