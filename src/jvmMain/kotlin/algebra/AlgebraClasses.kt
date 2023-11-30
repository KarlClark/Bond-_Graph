package algebra

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp



class Token(
    val bondId1: String = ""
    ,val bondId2: String = ""
    ,val name: AnnotatedString = AnnotatedString("")
    ,val powerVar: Boolean = false
    ,val energyVar: Boolean = false // Displacement on a capacitor or momentum on an inertia
    ,val independent: Boolean = false
    ,val differential: Boolean = false): Expr {

    val uniqueId = name.text + bondId1

    override fun add(expr: Expr): Expr {
        return Sum().add(expr).add(this)
    }

    override fun minus(expr: Expr): Expr {
        return Sum().minus(this).minus(expr)
    }

    override fun multiply(expr: Expr): Expr {
        return Term().multiply(this).multiply(expr)
    }

    override fun divide(expr: Expr): Expr {
        return Term().multiply(this).divide(expr)
    }

    override fun toAnnotatedString(exp: Int): AnnotatedString {

        val normalStyle = SpanStyle(fontSize = 20.sp)

        val superScript = SpanStyle(
            baselineShift = BaselineShift.Superscript,
            fontSize = 15.sp,
        )

        val subscript = SpanStyle(
            baselineShift = BaselineShift.Subscript,
            fontSize = 12.sp,
        )

        return buildAnnotatedString {
            pushStyle(normalStyle)
            append(name)
            if (differential) {
                append("\u0307")
            }
            pushStyle(subscript)
            append(bondId1)
            if ( ! bondId2.equals("")) {
                append(",")
                append(bondId2)
            }
            if (exp > 0) {
                pushStyle(superScript)
                append(exp.toString())
            }
        }
    }
}

interface Expr{

    fun add(expr: Expr): Expr

    fun minus(expr: Expr): Expr

    fun multiply(expr: Expr): Expr

    fun divide(expr: Expr): Expr

    fun toAnnotatedString(exp: Int = 0): AnnotatedString
}

class Term():Expr {

    val numerators = arrayListOf<Expr>()
    val denomintors = arrayListOf<Expr>()

    override fun toAnnotatedString(exp: Int): AnnotatedString {
        return buildAnnotatedString {
            if (numerators.size == 0) {
                append ("1")
            } else {
                numerators.forEach {
                    when (it) {
                        is Token -> append(it.toAnnotatedString())
                        is Term -> append(it.toAnnotatedString())
                        is Sum -> {
                            append("(")
                            append(it.toAnnotatedString())
                            append(")")
                        }
                    }
                }
            }
             if (denomintors.size > 0){
                 append("/")
                 denomintors.forEach {
                     when (it) {
                         is Token -> append(it.toAnnotatedString())
                         is Term -> append(it.toAnnotatedString())
                         is Sum -> {
                             append("(")
                             append(it.toAnnotatedString())
                             append(")")
                        }
                    }
                 }
             }
        }
    }

    override fun add(expr: Expr): Expr {
        return Sum().add(this).add(expr)
    }

    override fun minus(expr: Expr): Expr {
        return Sum().add(this).minus(expr)
    }

    override fun multiply(expr: Expr): Expr {

        println("multiply ${this.toAnnotatedString() } by ${expr.toAnnotatedString()}")
        when (expr) {

            is Token -> {

               /* if ( ! cancelled(expr, denomintors)) {
                    println("multiply canceled = false")
                    numerators.add(expr)
                }*/
                numerators.add(expr)
                cancel(this)
            }

            is Term -> {
                numerators.addAll(expr.numerators)
                denomintors.addAll(expr.denomintors)
                cancel(this)
            }

            is Sum -> {
                //numerators.add(expr)
                return expr.multiply(this)
            }
        }

        return this
    }

    override fun divide(expr: Expr): Expr {

        println("divide ${this.toAnnotatedString() } by ${expr.toAnnotatedString()}")
         when (expr) {

             is Token -> {

                 /*if ( ! cancelled(expr, numerators)) {
                     println("divide canceled = false")
                     println("add ${expr.toAnnotatedString()} to denominators of $this")
                     denomintors.add(expr)
                 }*/
                 denomintors.add(expr)
                 cancel(this)
             }

             is Term -> {
                 numerators.addAll(expr.denomintors)
                 denomintors.addAll(expr.numerators)
                 cancel(this)
             }

             is Sum -> {
                 denomintors.add(expr)
             }
         }

        if (denomintors.size == 0 && numerators.size == 1) {
            return numerators[0]
        }
        return this
    }

    fun getNumeratorTokens(): List<Token> {
        return numerators.filter { it is Token }.map{ it as Token}
    }

    fun getDenominatorTokens(): List<Token> {
        return denomintors.filter { it is Token }.map{ it as Token}
    }

    fun getDemominatorExpressions() : List<Expr> {
        return denomintors
    }
}

class Sum(): Expr {

    val plusTerms = arrayListOf<Expr>()
    val minusTerms = arrayListOf<Expr>()

    override fun toAnnotatedString(exp: Int): AnnotatedString {
        return buildAnnotatedString {
            var cnt = 0
            plusTerms.forEach {
                if (cnt > 0) {
                    append (" + ")
                }
                cnt++
                append (it.toAnnotatedString())
            }

            minusTerms.forEach{
                append (" - ")
                append (it.toAnnotatedString())
            }
        }
    }

    override fun add(expr: Expr): Expr {
        when (expr) {

            is Token -> {
                plusTerms.add(expr)
            }

            is Term -> {
                plusTerms.add(expr)
            }

            is Sum -> {
                plusTerms.addAll(expr.plusTerms)
                minusTerms.addAll(expr.minusTerms)
            }
        }
        return this
    }


    override fun minus(expr: Expr): Expr {
        when (expr) {

            is Token -> {
                minusTerms.add(expr)
            }

            is Term -> {
                minusTerms.add(expr)
            }

            is Sum -> {
                plusTerms.addAll(expr.minusTerms)
                minusTerms.addAll(expr.plusTerms)
            }
        }
        return(this)
    }

    override fun multiply(expr: Expr): Expr {
        for (index in 0 .. plusTerms.size -1){
            plusTerms[index] = plusTerms[index].multiply(expr)
        }

        for (index in 0 .. minusTerms.size -1){
            minusTerms[index] = minusTerms[index].multiply(expr)
        }
        return this
    }

    override fun divide(expr: Expr): Expr {
        for (index in 0 .. plusTerms.size -1){
            plusTerms[index] = plusTerms[index].divide(expr)
        }

        for (index in 0 .. minusTerms.size -1){
            minusTerms[index] = minusTerms[index].divide(expr)
        }
        return this
    }

    fun getAllExpressions(): List<Expr> {
        val l: ArrayList<Expr> = arrayListOf()
        l.addAll(plusTerms)
        l.addAll(minusTerms)
        return l
    }
    fun getPlusTerms(): List<Expr>{
        return plusTerms
    }

    fun getMinusTerms(): List<Expr> {
        return minusTerms
    }
}

class Equation(var leftSide: Expr, var rightSide: Expr) {

    companion object {
        fun empty(): Equation {
            return Equation(Term(), Term())
        }
    }

    fun toAnnotatedString (): AnnotatedString {
       return buildAnnotatedString {
            append (leftSide.toAnnotatedString())
            append(" = ")
            append (rightSide.toAnnotatedString())
        }
    }
}
