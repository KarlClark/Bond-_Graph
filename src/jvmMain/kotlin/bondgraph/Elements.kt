package bondgraph


open class Element(val bondGraph: BondGraph, val id: Int, val elementType: ElementTypes, var displayData: GraphElementDisplayData){
    var displayId: String = id.toString()
    
    val bondsMap = linkedMapOf<Int, Bond>()
    fun getBondList(): List<Bond> {
        return ArrayList(bondsMap.values)
    }

    fun getOtherElements(element: Element): List<Element>{
        return bondsMap.filter{(_,v) -> v.element1 != element && v.element2 != element}
            .map{(k,v) -> if (v.element1 == this) v.element2 else v.element1}
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
}
open class TwoPort (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): Element(bondGraph, id, element, displayData) {

    override fun addBond(bond: Bond) {
        if (bondsMap.size == 2){
            getBondList().forEach{ bondGraph.removeBond(it.id)}
            bondsMap.clear()
        }
        bondsMap[bond.id] = bond
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