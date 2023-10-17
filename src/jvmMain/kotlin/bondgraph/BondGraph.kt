package bondgraph

import androidx.compose.ui.geometry.Offset

class GraphElementDisplayData (val id: Int, var text: String, val x: Float, val y: Float, val width: Float, val height: Float, val centerLocation: Offset)
class BondGraph(var name: String) {
    private val graphElementsDisplayDataMap = linkedMapOf<Int, GraphElementDisplayData>()
    fun add(id: Int, typeName: String, x: Float, y: Float, centerOffset: Offset): Unit {
        graphElementsDisplayDataMap[id] = GraphElementDisplayData(id, typeName, x, y, centerOffset.x * 2f, centerOffset.y * 2f, Offset(x + centerOffset.x, y + centerOffset.y))
    }

    fun getGraphElementsDisplayDataMap():Map<Int, GraphElementDisplayData> = graphElementsDisplayDataMap

    fun remove (id: Int) {
        graphElementsDisplayDataMap.remove(id)
    }
}