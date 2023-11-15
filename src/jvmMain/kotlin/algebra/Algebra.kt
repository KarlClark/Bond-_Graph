package algebra

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp

class token(
    val bondId: Int = -1
    ,val name: AnnotatedString = AnnotatedString("")
    ,val powerVar: Boolean = false
    ,val energyVar: Boolean = false
    ,val independent: Boolean = false
    ,val differential: Boolean = false) {

   val uniqueId = name.text + bondId.toString()
    fun toAnnotatedString(exp: Int = 0): AnnotatedString{

        val normalStyle = SpanStyle( fontSize = 20.sp)

        val superScript = SpanStyle(
            baselineShift = BaselineShift.Superscript,
            fontSize = 15.sp,
        )

        val subscript = SpanStyle(
            baselineShift = BaselineShift.Subscript,
            fontSize = 15.sp,
        )
        val x = name
        return buildAnnotatedString {
            pushStyle(normalStyle)
            append(name)
            if (differential) {
                append("\u0307")
            }
            pushStyle(subscript)
            append(bondId.toString())
            if (exp > 0) {
                pushStyle(subscript)
                append(exp.toString())
            }
        }
    }
}