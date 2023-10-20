package bondgraph


open class Element(val bondGraph: BondGraph, val id: Int, val element: ElementTypes, var displayData: GraphElementDisplayData){
    var displayId: String = id.toString()
    
    val bondsMap = linkedMapOf<Int, Bond>()
    fun getBondList():List<Bond> {
        return ArrayList(bondsMap.values)
    }

    open fun addBond(bond: Bond) {
        bondsMap[bond.id] = bond
    }

    fun removeBond(id: Int) {
        bondsMap.remove(id)
    }
}

open class OnePort (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): Element(bondGraph, id, element, displayData) {
    override fun addBond(bond: Bond){
        if (bondsMap.size > 0){
            bondsMap.forEach { (_,v) -> bondGraph.elementRemoveBond(v) }
            bondsMap.clear()
        }
        bondsMap[bond.id] = bond
    }
}
open class TwoPort (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): Element(bondGraph, id, element, displayData) {

    override fun addBond(bond: Bond) {
        if (bondsMap.size == 2){
            bondsMap.forEach{(_,v) -> bondGraph.elementRemoveBond(v)}
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

class Transformer (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): TwoPort(bondGraph, id, element, displayData) {

}

class Gyrator (bondGraph: BondGraph, id: Int, element: ElementTypes, displayData: GraphElementDisplayData): TwoPort(bondGraph, id, element, displayData) {

}
class ModulatedTransformer (bondGraph: BondGraph, id: Int,  element: ElementTypes, displayData: GraphElementDisplayData): TwoPort(bondGraph, id, element, displayData) {

}