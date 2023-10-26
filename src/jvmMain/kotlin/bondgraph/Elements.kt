package bondgraph

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString


open class Element(val bondGraph: BondGraph, val id: Int, val elementType: ElementTypes, var displayData: GraphElementDisplayData){
    var displayId: AnnotatedString = AnnotatedString(id.toString())
    
    val bondsMap = linkedMapOf<Int, Bond>()
    fun getBondList(): List<Bond> {
        return ArrayList(bondsMap.values)
    }

    fun getOtherElements(element: Element): List<Element>{
        return bondsMap.filter{(_,v) -> v.element1 != element && v.element2 != element}
            .map{(k,v) -> if (v.element1 === this) v.element2 else v.element1}
    }

    fun getAssignedBonds(): List<Bond> = getBondList().filter{it.effortElement != null}

    fun getUnassignedBonds(): List<Bond> = getBondList().filter{it.effortElement == null}
    fun getOtherElement(element: Element, bond: Bond) = if (element === bond.element1) bond.element2 else bond.element1

    fun getOtherBonds(bond: Bond): List<Bond> = getBondList().filter{ it !==  bond}

    fun sameDirection(element: Element, bond1: Bond, bond2: Bond): Boolean =
        ((bond1.powerToElement === element &&  bond2.powerToElement === element) ||
        (bond1.powerToElement !== element &&  bond2.powerToElement !== element) )


    open fun creatDisplayId(id: String = ""){
        if (id != "") {
            displayId = AnnotatedString(id)
        } else {
            val bondDisplayIds = bondsMap.flatMap {listOf(it.value.displayId)}
            val s = StringBuilder("")
            for ((indx,did) in bondDisplayIds.withIndex()){
                if (indx > 0) s.append(",")
                s.append(did)
            }

            displayId =buildAnnotatedString {
                append (elementType.displayString())
                append("-")
                append (s.toString())
                toAnnotatedString()
            }

        }
        println("default displayId = $displayId")
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

open class OnePort (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): Element(bondGraph, id, element, displayData) {
    override fun addBond(bond: Bond){
        if (bondsMap.size > 0){
            getBondList().forEach { bondGraph.removeBond(it.id) }
            bondsMap.clear()
        }
        bondsMap[bond.id] = bond
        println("oneport add bond  ${bond.id}  ${bond.element1.id}  ${bond.element2.id}")
    }

    override fun creatDisplayId(id: String) {
        val bondList = getBondList()
        if (bondList.isNotEmpty()){
            displayId = buildAnnotatedString {
                append(elementType.displayString())
                append(bondList[0].displayId)
                toAnnotatedString()
            }
        }
        println("displayId = $displayId")
    }
}
open class TwoPort (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): Element(bondGraph, id, element, displayData) {

    override fun addBond(bond: Bond) {
        if (bondsMap.size == 2){
            getBondList().forEach{ bondGraph.removeBond(it.id)}
            bondsMap.clear()
        }
        bondsMap[bond.id] = bond
    }

    override fun creatDisplayId(id: String) {
        val bondList = getBondList()
        if (bondList.isNotEmpty()){
            displayId = buildAnnotatedString {
                append(bondList[0].displayId)
                append(elementType.displayString())
                append(bondList[1].displayId)
                toAnnotatedString()
            }
        }
        println("displayId = $displayId")
    }

}

class OneJunction (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): Element(bondGraph, id, element, displayData) {

    override fun assignCausality() {
        val assignedBonds = getAssignedBonds()
        val unassignedBonds = getUnassignedBonds()
        val settingFlow = assignedBonds.filter{ ! (it.effortElement === this)}
        if (settingFlow.size > 1) throw BadGraphException("Error: Multiple bonds an 1 junction are setting flow. ${this.displayId}")
        if (settingFlow.size == 1) {

            println("there are ${unassignedBonds.size} unassigned bonds")
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
        println("${this.displayId}")
        bondsList.forEach{println ("${it.displayId}  ${it.effortElement?.displayId}")}
        val flowBond = bondsList.filter{it.effortElement !== this}[0]
        return getOtherElement(this, flowBond).getFlow(flowBond)

    }

    override fun getEffort(bond: Bond): String {
        println ("element1 = ${bond.element1.displayId}  element2 = ${bond.element2.displayId}  powerToElement = ${bond.powerToElement?.displayId}  effortElement = ${bond.effortElement?.displayId}")

        val otherBonds = getOtherBonds(bond)
        val thisElement = this
        val sb = buildString {
            append("(")
            for (otherBond in otherBonds){
                println ("${bond.powerToElement?.displayId}  ${otherBond.powerToElement?.displayId}")

                append (if (sameDirection(thisElement, bond, otherBond)) " - " else " + " )

                append (getOtherElement(thisElement, otherBond).getEffort(otherBond))
            }
            append (")")
        }
        return sb.toString()
    }

}
class ZeroJunction (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): Element(bondGraph, id, element, displayData) {

    override fun assignCausality() {
        val assignedBonds = getAssignedBonds()
        println("ZeroJunction  assigned bonds")
        assignedBonds.forEach{println("${it.displayId}")}
        val unassignedBonds = getUnassignedBonds()
        val settingEffort = assignedBonds.filter{ it.effortElement === this}
        println("bonds setting effort")
        settingEffort.forEach{println("${it.displayId}")}
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
        println ("element1 = ${bond.element1.displayId}  element2 = ${bond.element2.displayId}  powerToElement = ${bond.powerToElement?.displayId}  effortElement = ${bond.effortElement?.displayId}")

        val otherBonds = getOtherBonds(bond)
        val thisElement = this
        val sb = buildString {
            append("(")
            for (otherBond in otherBonds){
                println ("${bond.powerToElement?.displayId}  ${otherBond.powerToElement?.displayId}")

                append (if (sameDirection(thisElement, bond, otherBond)) " - " else " + " )

                append (getOtherElement(thisElement, otherBond).getFlow(otherBond))
            }
            append (")")
        }
        return sb.toString()
    }
}

class Capacitor (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

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

class Inertia (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

    override fun assignCausality() {

        println("Inertia augment")
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

class Resistor (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

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
        return  sFlow + "*" + displayId.toString()
    }

    override fun getFlow(bond: Bond): String {
        val thisBond = getBondList()[0]
        val sEffort=  getOtherElement(this, thisBond).getEffort(thisBond)
        return sEffort + "/" + displayId.toString()
    }
}

class SourceOfEffort(bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

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

class SourceOfFlow (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {
    override fun assignCausality() {
        val bond = getBondList()[0]

        if (bond.effortElement === null) {
            val otherElement = getOtherElement(this, bond)
            bond.effortElement = this
            otherElement.assignCausality()
        } else {
            if ( ! (this === bond.effortElement)) throw BadGraphException("Error: A source of flow has been forced into effort causality. ${this.displayId}")
        }
    }

    override fun getFlow(bond: Bond): String {
        return "f" + getBondList()[0].displayId + "(t)"
    }

}

open class Transformer (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): TwoPort(bondGraph, id, element, displayData) {

    override fun assignCausality() {
        if (bondsMap.size == 1) throw BadGraphException("Error transformer ${displayId} has only one bond.")
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

}

class Gyrator (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): TwoPort(bondGraph, id, element, displayData) {
    override fun assignCausality() {
        if (bondsMap.size == 1) throw BadGraphException("Error gyrator ${displayId} has only one bond.")
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
}
class ModulatedTransformer (bondGraph: BondGraph, id: Int,  element: ElementTypes, displayData: GraphElementDisplayData): Transformer(bondGraph, id, element, displayData) {

}