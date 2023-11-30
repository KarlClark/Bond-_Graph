package bondgraph

import algebra.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import userInterface.MyConstants
import kotlin.reflect.KClass

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

/*
The data needed to display a representation of the element on the screen.  The id, text and location are
pretty obvious, the width and height are the size of the text, and the centerLocation is the location of
the center of text.  This information is needed for drawing bonds to the element.  Every instance of
Element contains an ElementDisplayData instance as one of its properties.
 */
class ElementDisplayData (val id: Int, var text: AnnotatedString, var location: Offset, val width: Float, val height: Float, var centerLocation: Offset)

@Serializable
class ElementSerializationData(val id: Int, val type: ElementTypes,  val displayDatId: Int,  val locx: Float, val locy: Float, val width: Float, val height: Float, val cenx: Float, val ceny: Float) {
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
                    displayData.centerLocation.y
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
                        ElementDisplayData(displayDatId, elementType.toAnnotatedString(), Offset(locx, locy), width, height, Offset(cenx, ceny))
                    )
                } else {
                    throw BadGraphException("Error in function makeElement, invalid ElementType = $elementType, derived from string ${data.type}")
                }
            }
        }
    }
}

abstract class Element(val bondGraph:  BondGraph, val id: Int, val elementType: ElementTypes, var displayData: ElementDisplayData){
    var displayId: @Contextual AnnotatedString = AnnotatedString(id.toString())
    
    val bondsMap = linkedMapOf<Int, Bond>()

    companion object {
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
    fun getBondList(): List<Bond> = ArrayList(bondsMap.values)


    private fun getOtherElements(element: Element): List<Element>{
        return bondsMap.filter{(_,v) -> v.element1 != element && v.element2 != element}
            .map{(_,v) -> if (v.element1 === this) v.element2 else v.element1}
    }

    fun getAssignedBonds(): List<Bond> = getBondList().filter{it.effortElement != null}

    fun getUnassignedBonds(): List<Bond> = getBondList().filter{it.effortElement == null}


    fun getOtherBonds(bond: Bond): List<Bond> = getBondList().filter{ it !==  bond}

    fun sameDirection(element: Element, bond1: Bond, bond2: Bond): Boolean =
        ((bond1.powerToElement === element &&  bond2.powerToElement === element) ||
        (bond1.powerToElement !== element &&  bond2.powerToElement !== element) )


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

    abstract fun createTokens()

    open fun addBond(bond: Bond) {
        bondsMap[bond.id] = bond
    }

    open fun assignCausality(){}


    open fun countElements(element: Element, count: Int): Int{
        val elementList = getOtherElements(element)

        var cnt = 1
        elementList.forEach{cnt = it.countElements(this, cnt)}

        return count + cnt
    }

    fun removeBond(id: Int) {
        bondsMap.remove(id)
    }

    open fun implement(){}

    abstract fun deriveEquation(): Equation

    abstract fun getFlow(bond: Bond): Expr

    abstract fun getEffort(bond: Bond): Expr

}

abstract class OnePort (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {

    override abstract fun deriveEquation(): Equation

    override abstract fun getEffort(bond: Bond): Expr

    override abstract fun getFlow(bond: Bond): Expr


    override fun addBond(bond: Bond){
        if (bondsMap.size > 0){
            getBondList().forEach { bondGraph.removeBond(it.id) }
            bondsMap.clear()
        }
        bondsMap[bond.id] = bond
    }

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

    override abstract fun deriveEquation(): Equation

    override abstract fun getEffort(bond: Bond): Expr

    override abstract fun getFlow(bond: Bond): Expr


    override fun addBond(bond: Bond) {
        if (bondsMap.size == 2){
            getBondList().forEach{ bondGraph.removeBond(it.id)}
            bondsMap.clear()
        }
        bondsMap[bond.id] = bond
    }

    override fun createDisplayId(id: String) {
        val bondList = getBondList()
        if (bondList.size == 1) throw BadGraphException("Error: The 2-port on bond ${bondList[0].displayId} is missing a bond")
        if (bondList.isNotEmpty()){
            displayId = buildAnnotatedString {
                append(bondList[0].displayId)
                append(elementType.toAnnotatedString())
                append(bondList[1].displayId)
                toAnnotatedString()
            }
        }
    }

}

class OneJunction (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {

    override fun assignCausality() {
        val assignedBonds = getAssignedBonds()
        val unassignedBonds = getUnassignedBonds()
        val settingFlow = assignedBonds.filter{ it.effortElement !== this}
        if (settingFlow.size > 1) throw BadGraphException("Error: Multiple bonds an 1 junction are setting flow. ${this.displayId}")
        if (settingFlow.size == 1) {

            for (bond in unassignedBonds){
                //bond.effortElement = this
                val otherElement = getOtherElement(this, bond)
                bond.effortElement = this
                otherElement.assignCausality()

            }
        } else {
            if (unassignedBonds.size == 1) {  // Last bond so it has to be the one to set the flow
                val bond = unassignedBonds[0]
                val otherElement = getOtherElement(this , bond)
                bond.effortElement = otherElement
                otherElement.assignCausality()
            }
        }
    }

    override fun createTokens() {}
    override fun deriveEquation(): Equation {
        return Equation.empty()
    }

    override fun getFlow(bond: Bond): Expr {
        val bondsList = getBondList()
        val flowBond = bondsList.filter{it.effortElement !== this}[0]
        return getOtherElement(this, flowBond).getFlow(flowBond)

    }

    override fun getEffort(bond: Bond): Expr {

        val otherBonds = getOtherBonds(bond)
        val thisElement = this


        val sum = Sum()
        for (otherBond in otherBonds ) {
            val otherElement = getOtherElement(thisElement, otherBond)
            if (sameDirection(thisElement, bond, otherBond)) sum.minus(otherElement.getEffort(otherBond)) else sum.add(otherElement.getEffort(otherBond))
        }
        return sum
    }

}
class ZeroJunction (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {

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

    override fun deriveEquation(): Equation {
        return Equation.empty()
    }

    override fun getEffort(bond: Bond): Expr {
        val bondsList = getBondList()
        val effortBond = bondsList.filter{it.effortElement === this}[0]
        return getOtherElement(this, effortBond).getEffort(effortBond)

    }

    override fun getFlow(bond: Bond): Expr {

        val otherBonds = getOtherBonds(bond)
        val thisElement = this

        val sum = Sum()
        for (otherBond in otherBonds) {
            val otherElement = getOtherElement(thisElement, otherBond)
            if (sameDirection(thisElement, bond, otherBond)) sum.minus(otherElement.getFlow(otherBond)) else sum.add(otherElement.getFlow(otherBond))
        }
        return sum
    }
}

class Capacitor (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    var cToken = Token()
    var qToken = Token()
    var qDotToken = Token()


    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]
        if (bond.effortElement == null) throw BadGraphException("Error: Attempt to create tokens on an element with no causality set. Has createdTokens been called before augmntation?")

        cToken = Token(bond.displayId, "",  elementType.toAnnotatedString(), false, false, false, false)
        qToken = Token(bond.displayId, "", AnnotatedString("q"), false, true, bond.effortElement !== this,false,)
        qDotToken = Token(bond.displayId, "", AnnotatedString("q"), false, true, bond.effortElement !== this, true)
    }


    override fun assignCausality() {

        val bond = getBondList()[0]
        if (bond.effortElement == null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = otherElement
            otherElement.assignCausality()
        }

    }

    override fun getFlow(bond: Bond): Expr {
        if (true) throw BadGraphException("Error: call to Capacitor.getFlow() which is not implemented")
        return Term()
    }

    override fun getEffort(bond: Bond): Expr {

        if (cToken.bondId1.equals("")) throw BadGraphException("Error: getEffort called but tokens have not been created.  Has createdTokens been called?")

        return Term().multiply(qToken).divide(cToken)
    }

    override fun deriveEquation(): Equation {
        val bond = getBondList()[0]

        if (qDotToken.bondId1.equals("")) throw BadGraphException("Error: getEffort called but tokens have not been created.  Has createdTokens been called?")
        return Equation(qDotToken, getOtherElement(this, bond).getFlow(bond))

    }


}

class Inertia (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, element, displayData) {


    var iToken = Token()
    var pToken = Token()
    var pDotToken = Token()

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]
        if (bond.effortElement == null) throw BadGraphException("Error: Attempt to create tokens on an element with no causality set. Has createdTokens been called before augmntation?")

        iToken = Token(bond.displayId, "", elementType.toAnnotatedString(), false, false, false, false)
        pToken = Token(bond.displayId, "", AnnotatedString("p"), false, true, bond.effortElement !== this,false,)
        pDotToken = Token(bond.displayId, "", AnnotatedString("p"), false, true, bond.effortElement !== this, true)
    }


    override fun assignCausality() {

        val bond = getBondList()[0]
        if (bond.effortElement == null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = this
            otherElement.assignCausality()
        }
    }

    override fun getEffort(bond: Bond): Expr {
        if (true) throw BadGraphException("Error: call to Inertia.getEffort which is not implemented")
        return Term()
    }

    override fun getFlow(bond: Bond): Expr {

        if (iToken.bondId1.equals("")) throw BadGraphException("Error: getEffort called but tokens have not been created.  Has createdTokens been called?")
        return Term().multiply(pToken).divide(iToken)
    }

    override fun deriveEquation(): Equation {
        val bond = getBondList()[0]
        return Equation(pDotToken, getOtherElement(this, bond).getEffort(bond))
    }
}

class Resistor (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    var rToken = Token()
    var eToken = Token()
    var fToken = Token()
    var derivingEquation = false
    var substituteExprssion: Expr? = null

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]

        rToken = Token(bond.displayId, "", elementType.toAnnotatedString(), false, false, false, false)
        eToken = Token(bond.displayId, "", AnnotatedString("e"), false, false, false, false)
        fToken = Token(bond.displayId, "", AnnotatedString("f"), false, false, false, false)
    }


    override fun assignCausality() {

        val bond = getBondList()[0]
        if (bond.effortElement === null) {
            val otherElement = getOtherElement(this, bond)
            //bond.effortElement = bond.element1
            bond.effortElement = otherElement
            otherElement.assignCausality()
        }
    }

    override fun getEffort(bond: Bond): Expr {
        //println("$displayId getEffort derivingEquation = $derivingEquation")
        if (derivingEquation){
            derivingEquation = false
            return(eToken)
        } else {
            val thisBond = getBondList()[0]

            if (thisBond.effortElement !== this && substituteExprssion != null) {
                return substituteExprssion as Expr
            }

            val sFlow = getOtherElement(this, thisBond).getFlow(thisBond)
            return Term().multiply(sFlow).multiply(rToken)
        }
    }

    override fun getFlow(bond: Bond): Expr {
        //println("$displayId getFlow derivingEquation = $derivingEquation")
        if (derivingEquation){
            derivingEquation = false
            return(fToken)
        } else {
            val thisBond = getBondList()[0]

            if (thisBond.effortElement === this && substituteExprssion != null) {
                return substituteExprssion as Expr
            }

            val sEffort = getOtherElement(this, thisBond).getEffort(thisBond)
            return Term().multiply(sEffort).divide(rToken)
        }
    }

    override fun deriveEquation(): Equation {

        derivingEquation = true
        println("deriveEquation for $displayId, derivingEquation =$derivingEquation  ")
        val bond = getBondList()[0]
        val otherElement = getOtherElement(this, bond)
        if (bond.effortElement === this){
            return Equation(fToken, Term().multiply(otherElement.getEffort(bond)).divide(rToken))
        } else {
            return Equation(eToken, Term().multiply(otherElement.getFlow(bond)).multiply(rToken))
        }
    }
}

class SourceOfEffort(bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    var sToken = Token()

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]

        sToken = Token(bond.displayId, "", elementType.toAnnotatedString(), true, false, false, false)
    }

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
        if (true) throw BadGraphException("Error: call to SourecOfEffort.getFlow()  which make no since and is clearly an error")
        return Term()
    }


    override fun getEffort(bond: Bond): Expr {
        return Term().multiply(sToken)
    }

    override fun deriveEquation(): Equation {
        if (true) throw BadGraphException("Error: Call to SourceOfFlow.deriveEquation which is not implemented.")

        return Equation(Term(), Term())
    }
}

class SourceOfFlow (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    var sToken = Token()

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        val bond = bondsList[0]

        sToken = Token(bond.displayId, "", elementType.toAnnotatedString(), true, false, false, false)
    }


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
        return Term().multiply(sToken)
    }

    override fun deriveEquation(): Equation {
        if (true) throw BadGraphException("Error: Call to SourceOfFLow.deriveEquation which is not implemented.")

        return Equation(Term(), Term())
    }
}

open class Transformer (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): TwoPort(bondGraph, id, elementType, displayData) {

    //var tToken = Token()
    var mToken = Token()

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        //tToken = Token(bondsList[0].displayId, bondsList[1].displayId , elementType.toAnnotatedString(), false, false, false, false)
        mToken = Token(bondsList[0].displayId, bondsList[1].displayId, AnnotatedString("M"), false, false, false, false)
    }



    class Modulator(private val element1: Element, val token: Token){
        fun getEffortModulator(elementToMultiply: Element) = if (elementToMultiply === element1) Term().multiply(token) else Term().divide(token)

        fun getFlowModulator (elementToMultiply: Element) = if (elementToMultiply === element1) Term().divide(token) else Term().multiply(token)

    }

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
        val modulator  = Modulator(getOtherElement(this, getBondList()[0]), mToken)
        val mod = modulator.getEffortModulator(getOtherElement(this, bond))
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod.multiply(otherElement.getEffort(otherBond))
    }

    override fun getFlow(bond: Bond): Expr {
        val modulator  = Modulator(getOtherElement(this, getBondList()[0]), mToken)
        val mod = modulator.getFlowModulator(getOtherElement(this, bond))
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod.multiply(otherElement.getFlow(otherBond))
    }

    override fun deriveEquation(): Equation {
        if (true) throw BadGraphException("Error: Call to Transformer.deriveEquation which is not implemented.")

        return Equation(Term(), Term())
    }
}

class Gyrator (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): TwoPort(bondGraph, id, elementType, displayData) {

    //var gToken = Token()
    var mToken = Token()

    override fun createTokens() {
        val bondsList = getBondList()
        if (bondsList.isEmpty()) throw BadGraphException("Error: Attempt to create tokens on an element with no bonds. Has createTokens been called before augmentation?")
        //gToken = Token(bondsList[0].displayId, bondsList[1].displayId, elementType.toAnnotatedString(), false, false, false, false )
        mToken = Token(bondsList[0].displayId, bondsList[1].displayId, AnnotatedString("M"), false, false, false, false)
    }


    class Modulator(private val element1: Element, private val token: Token) {
        fun getEffortModulator(elementToMultiply: Element) =
            if (elementToMultiply === element1) Term().multiply(token) else Term().divide(token)

        fun getFlowModulator(elementToMultiply: Element) =
            if (elementToMultiply === element1) Term().divide(token) else Term().multiply(token)

    }


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
        val modulator = Transformer.Modulator(getOtherElement(this, getBondList()[0]), mToken)
        val mod = modulator.getEffortModulator(getOtherElement(this, bond))
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod.multiply(otherElement.getFlow(otherBond))
    }

    override fun getFlow(bond: Bond): Expr {
        val modulator = Transformer.Modulator(getOtherElement(this, getBondList()[0]), mToken)
        val mod = modulator.getFlowModulator(getOtherElement(this, bond))
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod.multiply(otherElement.getEffort(otherBond))
    }

    override fun deriveEquation(): Equation {
        if (true) throw BadGraphException("Error: Call to Gyrator.deriveEquation which is not implemented.")

        return Equation(Term(), Term())
    }
}


class ModulatedTransformer (bondGraph: BondGraph, id: Int,  elementType: ElementTypes, displayData: ElementDisplayData): Transformer(bondGraph, id, elementType, displayData)