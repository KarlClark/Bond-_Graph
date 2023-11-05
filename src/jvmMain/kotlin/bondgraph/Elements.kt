package bondgraph

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString


open class Element(val bondGraph: BondGraph, val id: Int, val elementType: ElementTypes, var displayData: ElementDisplayData){
    var displayId: AnnotatedString = AnnotatedString(id.toString())
    
    val bondsMap = linkedMapOf<Int, Bond>()
    fun getBondList(): List<Bond> = ArrayList(bondsMap.values)


    private fun getOtherElements(element: Element): List<Element>{
        return bondsMap.filter{(_,v) -> v.element1 != element && v.element2 != element}
            .map{(_,v) -> if (v.element1 === this) v.element2 else v.element1}
    }

    fun getAssignedBonds(): List<Bond> = getBondList().filter{it.effortElement != null}

    fun getUnassignedBonds(): List<Bond> = getBondList().filter{it.effortElement == null}
    fun getOtherElement(element: Element, bond: Bond) = if (element === bond.element1) bond.element2 else bond.element1

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

    open fun deriveEquation(): String = ""

    open fun getFlow(bond: Bond): String = ""

    open fun getEffort(bond: Bond): String = ""

}

open class OnePort (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {
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
open class TwoPort (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): Element(bondGraph, id, elementType, displayData) {

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

    override fun getFlow(bond: Bond): String {
        val bondsList = getBondList()
        val flowBond = bondsList.filter{it.effortElement !== this}[0]
        return getOtherElement(this, flowBond).getFlow(flowBond)

    }

    override fun getEffort(bond: Bond): String {

        val otherBonds = getOtherBonds(bond)
        val thisElement = this
        val sb = buildString {
            append("(")
            for (otherBond in otherBonds){
                append (if (sameDirection(thisElement, bond, otherBond)) " - " else " + " )
                append (getOtherElement(thisElement, otherBond).getEffort(otherBond))
            }
            append (")")
        }
        return sb
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

    override fun getEffort(bond: Bond): String {
        val bondsList = getBondList()
        val effortBond = bondsList.filter{it.effortElement === this}[0]
        return getOtherElement(this, effortBond).getEffort(effortBond)

    }

    override fun getFlow(bond: Bond): String {

        val otherBonds = getOtherBonds(bond)
        val thisElement = this
        val sb = buildString {
            append("(")
            for (otherBond in otherBonds){

                append (if (sameDirection(thisElement, bond, otherBond)) " - " else " + " )

                append (getOtherElement(thisElement, otherBond).getFlow(otherBond))
            }
            append (")")
        }
        return sb
    }
}

class Capacitor (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    override fun assignCausality() {

        val bond = getBondList()[0]
        if (bond.effortElement == null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = otherElement
            otherElement.assignCausality()
        }

    }

    override fun getEffort(bond: Bond): String {
        return "q" + getBondList()[0].displayId + "/" + displayId.toString()
    }

    override fun deriveEquation(): String {
        val bond = getBondList()[0]
        return "dq" + bond.displayId + "/dt = " + getOtherElement(this,bond).getFlow(bond)
    }


}

class Inertia (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, element, displayData) {

    override fun assignCausality() {

        val bond = getBondList()[0]
        if (bond.effortElement == null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = this
            otherElement.assignCausality()
        }
    }

    override fun getFlow(bond: Bond): String {
        return "p" + getBondList()[0].displayId +"/" + displayId
    }

    override fun deriveEquation(): String {
        val bond = getBondList()[0]
        return "dp" + bond.displayId + "/dt = " + getOtherElement(this,bond).getEffort(bond)
    }
}

class Resistor (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

    override fun assignCausality() {

        val bond = getBondList()[0]
        if (bond.effortElement === null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = bond.element1
            otherElement.assignCausality()
        }
    }

    override fun getEffort(bond: Bond): String {
        val thisBond = getBondList()[0]
        val sFlow = getOtherElement(this, thisBond).getFlow(thisBond)
        return "$sFlow*$displayId"
    }

    override fun getFlow(bond: Bond): String {
        val thisBond = getBondList()[0]
        val sEffort=  getOtherElement(this, thisBond).getEffort(thisBond)
        return "$sEffort*1/$displayId"
    }
}

class SourceOfEffort(bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {

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

    override fun getEffort(bond: Bond): String {
        return "e" + getBondList()[0].displayId + "(t)"
    }

}

class SourceOfFlow (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): OnePort(bondGraph, id, elementType, displayData) {
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

    override fun getFlow(bond: Bond): String {
        return "f" + getBondList()[0].displayId + "(t)"
    }

}

open class Transformer (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): TwoPort(bondGraph, id, elementType, displayData) {

    class Modulator(private val element1: Element, private val id: String){
        fun getEffortModulator(elementToMultiply: Element) = if (elementToMultiply === element1) "M$id" else "1/M$id"

        fun getFlowModulator (elementToMultiply: Element) = if (elementToMultiply === element1) "1/M$id" else "M$id"

    }

    private lateinit var modulator: Modulator
    override fun assignCausality() {
        if (bondsMap.size == 1) throw BadGraphException("Error transformer $displayId has only one bond.")
        modulator  = Modulator(getOtherElement(this, getBondList()[0]), getBondList()[0].displayId)
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

    override fun getEffort(bond: Bond): String {
        val mod = modulator.getEffortModulator(getOtherElement(this, bond))
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod + otherElement.getEffort(otherBond)
    }

    override fun getFlow(bond: Bond): String {
        val mod = modulator.getFlowModulator(getOtherElement(this, bond))
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod + otherElement.getFlow(otherBond)
    }
}

class Gyrator (bondGraph: BondGraph, id: Int, elementType: ElementTypes, displayData: ElementDisplayData): TwoPort(bondGraph, id, elementType, displayData) {

    class Modulator(private val element1: Element, private val id: String){
        fun getEffortModulator(elementToMultiply: Element) = if (elementToMultiply === element1) "M$id" else "1/M$id"

        fun getFlowModulator (elementToMultiply: Element) = if (elementToMultiply === element1) "1/M$id" else "M$id"

    }

    private lateinit  var modulator: Modulator

    override fun assignCausality() {
        if (bondsMap.size == 1) throw BadGraphException("Error gyrator $displayId has only one bond.")
        val assignedBonds = getAssignedBonds()
        if (assignedBonds.size == 2){
            if ( (assignedBonds[0].effortElement === this &&  assignedBonds[1].effortElement !== this)
                ||
                ( assignedBonds[0].effortElement !== this && assignedBonds[1].effortElement === this)
            ) throw BadGraphException("Error: gyrator $displayId is being forces into conflicting causality.")
        } else {
            val assignedBond = assignedBonds[0]
            val unassignedBond =  getUnassignedBonds()[0]
            val unassignedOther = getOtherElement(this, unassignedBond)
            if (this === assignedBond.effortElement){
                unassignedBond.effortElement = this
            } else {
                unassignedBond.effortElement = unassignedOther
            }
            unassignedOther.assignCausality()
        }
    }

    override fun getEffort(bond: Bond): String {
        val mod = modulator.getEffortModulator(getOtherElement(this, bond))
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod + otherElement.getFlow(otherBond)
    }

    override fun getFlow(bond: Bond): String {
        val mod = modulator.getFlowModulator(getOtherElement(this, bond))
        val otherBond = getOtherBonds(bond)[0]
        val otherElement = getOtherElement(this, otherBond)
        return mod + otherElement.getEffort(otherBond)
    }

}
class ModulatedTransformer (bondGraph: BondGraph, id: Int,  elementType: ElementTypes, displayData: ElementDisplayData): Transformer(bondGraph, id, elementType, displayData)