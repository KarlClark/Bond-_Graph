package bondgraph


open class Element(val id: Int, val element: ElementTypes, var displayData: GraphElementDisplayData){
    var displayId: String = id.toString()
}

class oneJunction (id: Int, displayId: String, element: ElementTypes, displayData: GraphElementDisplayData): Element(id, element, displayData) {

}
class zeroJunction (id: Int, displayId: String, element: ElementTypes, displayData: GraphElementDisplayData): Element(id, element, displayData) {

}

class Capacitor (id: Int, displayId: String, element: ElementTypes, displayData: GraphElementDisplayData): Element(id, element, displayData) {

}

class Inertia (id: Int, displayId: String, element: ElementTypes, displayData: GraphElementDisplayData): Element(id, element, displayData) {

}

class Resistor (id: Int, displayId: String, element: ElementTypes, displayData: GraphElementDisplayData): Element(id, element, displayData) {

}

class Transformer (id: Int, displayId: String, element: ElementTypes, displayData: GraphElementDisplayData): Element(id, element, displayData) {

}

class Gyrator (id: Int, displayId: String, element: ElementTypes, displayData: GraphElementDisplayData): Element(id, element, displayData) {

}
class ModulatedTransformer (id: Int, displayId: String, element: ElementTypes, displayData: GraphElementDisplayData): Element(id, element, displayData) {

}