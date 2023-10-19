package bondgraph

import androidx.compose.ui.geometry.Offset

enum class ElementTypes {
    ZERO_PORT{
         override fun displayString () = "0"
         },
    ONE_PORT{
        override fun displayString() = "1"
        },
    CAPACITOR{
        override fun displayString() = "C"
        },
    RESISTOR{
        override fun displayString() = "R"
        },
    INERTIA{
        override fun displayString() = "I"
        },
    TRANSFORMER{
        override fun displayString() = "TF"
        },
    GYRATOR{
        override fun displayString() = "GY"
        },
    MODULATED_TRANSFORMER{
        override fun displayString()  = "MTF"
        },
    INVALID {
        override fun displayString() = ""
        };

    abstract fun displayString(): String

    companion object {
        fun toEnum(value: String): ElementTypes {
            return when (value) {
                "0" -> ZERO_PORT
                "1" -> ONE_PORT
                "C" -> CAPACITOR
                "R" -> RESISTOR
                "I" -> INERTIA
                "TF" -> TRANSFORMER
                "GY" -> GYRATOR
                "MTF" -> MODULATED_TRANSFORMER

                else -> INVALID
            }
        }
    }
}

class GraphElementDisplayData (val id: Int, var text: String, val x: Float, val y: Float, val width: Float, val height: Float, val centerLocation: Offset)

class Bond(val id: Int, val element1: Element?, val offset1: Offset, val element2: Element?, val offset2: Offset, var powerToElement: Element?){
    var displayId: String = id.toString()
    var casualToElement: Element? = null


}
class BondGraph(var name: String) {
    //private val graphElementsDisplayDataMap = linkedMapOf<Int, GraphElementDisplayData>()
    val elementsMap = linkedMapOf<Int, Element>()
    val bondsMap = linkedMapOf<Int, Bond>()
    fun addElement(id: Int, element: ElementTypes, x: Float, y: Float, centerOffset: Offset): Unit {
        elementsMap[id] = Element(id, element, GraphElementDisplayData(id, element.displayString(), x, y, centerOffset.x * 2f, centerOffset.y * 2f, Offset(x + centerOffset.x, y + centerOffset.y)))
    }

    fun getElementsMap():Map<Int, Element> = elementsMap

    fun getElement(id: Int): Element? {
        return elementsMap[id]
    }

    fun removeElement (id: Int) {
        elementsMap.remove(id)
    }

    fun addBond(id: Int, elementId1: Int, offset1: Offset, elementId2: Int, offset2: Offset, powerToElementId: Int) {
        bondsMap[id] = Bond(id, elementsMap[elementId1], offset1, elementsMap[elementId2], offset2, elementsMap[powerToElementId])
    }

    fun getBond(id: Int): Bond? {
        return bondsMap[id]
    }

    fun removeBond(id: Int){
        bondsMap.remove(id)
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
                bondsMap[id]?.casualToElement = element
            }
        }
    }
}