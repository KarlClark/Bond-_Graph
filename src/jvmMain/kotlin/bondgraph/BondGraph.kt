package bondgraph

import androidx.compose.ui.geometry.Offset
import bondgraph.Elements.*
import javax.swing.plaf.metal.MetalTextFieldUI

enum class Elements {
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
        fun toEnum(value: String): Elements {
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

class Bond(val id: Int, val element1: Element, val element2: Element, var powerToElement: Element){
    var displayId: String = id.toString()


}
class BondGraph(var name: String) {
    //private val graphElementsDisplayDataMap = linkedMapOf<Int, GraphElementDisplayData>()
    val elementsMap = linkedMapOf<Int, Element>()
    fun add(id: Int, element: Elements, x: Float, y: Float, centerOffset: Offset): Unit {
        elementsMap[id] = Element(id, element, GraphElementDisplayData(id, element.displayString(), x, y, centerOffset.x * 2f, centerOffset.y * 2f, Offset(x + centerOffset.x, y + centerOffset.y)))
    }

    fun getElementsMap():Map<Int, Element> = elementsMap

    fun remove (id: Int) {
        elementsMap.remove(id)
    }
}