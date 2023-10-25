package bondgraph

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
            .map{(k,v) -> if (v.element1 == this) v.element2 else v.element1}
    }

    open fun creatDisplayId(id: String = ""){
        if (id != "") {
            displayId = AnnotatedString(id)
        } else {
            val bondDisplayIds = bondsMap.flatMap {listOf(it.value.displayId)}
            val s = StringBuilder("")
            for ((indx,id) in bondDisplayIds.withIndex()){
                if (indx > 0) s.append(",")
                s.append(id)
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

}
class ZeroJunction (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): Element(bondGraph, id, element, displayData) {

}

class Capacitor (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

}

class Inertia (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

}

class Resistor (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

}

class SourceOfEffort(bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

}

class SourceOfFlow (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): OnePort(bondGraph, id, element, displayData) {

}

class Transformer (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): TwoPort(bondGraph, id, element, displayData) {

}

class Gyrator (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): TwoPort(bondGraph, id, element, displayData) {

}
class ModulatedTransformer (bondGraph: BondGraph, id: Int,  element: ElementTypes, displayData: GraphElementDisplayData): TwoPort(bondGraph, id, element, displayData) {

}