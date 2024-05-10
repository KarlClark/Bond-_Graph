package algebra

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import java.lang.IllegalStateException
import java.text.NumberFormat
import java.util.LinkedList
import kotlin.math.absoluteValue

/*
    This program can perform a limited amount of symbolic algebra, enough to generate and solve basic
    equations produced from bond graphs.  This capability is made up of 4 classes, Equation, Token, Term and Sum,
    the expression (Expr) interface and a bunch of functions. Briefly:

    Equation: is trivial class containing two expressions, one for the left side of the equation and one for the right side.

    Token: the basic building block of an expression. They represent the entities used in bond graph equations, such
           as momentum, capacitance, resistance etc. For example in the expression q1(R1 + R2) q1, R1 and R2 would
           all be represented by tokens. Tokens are created by the element classes.  For example a capacitor object
           would create tokens for its capacitance, the displacement on its bond and the derivative of the displacement.
           An element  object creates its tokens once and uses the same ones every time it is called on to supply them
           for equation generation. So tokens can be compared on the object level i.e. t1 === t2.

    Term:  A term keeps lists of expressions that are multiplied or divided into each other.  Example C1R2/R3(R4 + R5)

    Sum:   A sum keeps lists of expressions that are added or subtracted from each other. Example (C2 + I3R2 + I4/R3R3)

    From the examples we see that terms can contain sums, and sums may contain terms.

    The Token, Term and Sum classes implement the Expr interface.  They each provide functions for how to add, subtract,
    multiply and divide itself by another expression.  Specific details are commented below, but in general we try to
    follow the following rules:
    1. Don't allow fractions that multiply or divide other fractions.  Take something like R3/(R5/R6) and turn it
       into R3R6/R5.  Basically maintain one level of numerators and denominators.
    2. Products of a term and a sum are expanded.
       Example R3(I4 + R5)/R2R6  would become (R3I4/R2R6 + R3R5/R2R6) i.e. numerator expanded.  This is because
       looking for like terms is easier when they are already broken out like this. Occasionally, we have to
       factor out the R3 to produce the first form.

    Expr's must also implement equals(Expr).  This is because we want different objects to possibly be equal.
    Example  (a + b) = (b + a)  or ab = ba.
 */


// Function for comparing two lists. For each element in the first list see if it exists in the second list.
// The order doesn't matter.  Remove elements in the second list as they are found in case an element
// occurs twice in the first list but only once in the second list.
fun compareLists(list1: ArrayList<Expr>, list2: ArrayList<Expr>): Boolean {
    val copyList2 = arrayListOf<Expr>()
    var foundOne: Boolean

    for (e1 in list1){
        foundOne = false
        copyList2.clear()
        copyList2.addAll(list2)
        for (e2 in copyList2) {
            if (e1.equals(e2)) {
                foundOne = true
                list2.remove(e2)
                break
            }
        }
        if (! foundOne) {
            return false
        }
    }
    return true
}

/*fun bulidSum(num: Number, sum: Sum, operation: (Double, Double) -> Double): Sum {
    val newPlusTerms = arrayListOf<Expr>()
    var newNum = num.value
    sum.plusTerms.forEach {
        if (it is Number){
            newNum = operation(it.value, newNum)
        } else {
            newPlusTerms.add(it)
        }
    }

    newPlusTerms.add(Number(newNum))
    val newSum = Sum()
    newSum.plusTerms.addAll(newPlusTerms)
    newSum.minusTerms.addAll(sum.minusTerms)
    return newSum
}*/

fun buildSum(plusTerms: ArrayList<Expr>, minusTerms: ArrayList<Expr>, num: Number, operation: (Double, Double) -> Double): Expr{
    val newPlusTerms = arrayListOf<Expr>()
    val newMinusTerms = arrayListOf<Expr>()
    var newValue: Double = 0.0

    plusTerms.forEach {
        if (it is Number) {
            newValue += it.value
        } else {
            newMinusTerms.add(it)
        }
    }

    minusTerms.forEach{
        if (it is Number){
            newValue -= it.value
        } else {
            newMinusTerms.add(it)
        }
    }

    newValue = operation(newValue, num.value)

    when {
        newValue == 0.0 -> {}
        newValue > 0.0 -> newPlusTerms.add(Number(newValue))
        newValue < 0 -> newMinusTerms.add(Number(-newValue))
    }

    if (newPlusTerms.size + newMinusTerms.size == 0){
        return Number(0.0)
    }

    if (newPlusTerms.size == 1 && newMinusTerms.size == 0){
        return newPlusTerms[0]
    }

    val sum = Sum()
    sum.plusTerms.addAll(newPlusTerms)
    sum.minusTerms.addAll(newMinusTerms)
    return sum
}


interface Expr{

    fun add(expr: Expr): Expr

    fun subtract(expr: Expr): Expr

    fun multiply(expr: Expr): Expr

    fun divide(expr: Expr): Expr

    fun toAnnotatedString(exp: Int = 0): AnnotatedString

    fun equals(expr: Expr): Boolean

    fun clone(): Expr
}

class Number(val value: Double = 0.0): Expr {

    fun buildTerm(numerators: ArrayList<Expr>, denominators: ArrayList<Expr>, num: Double): Expr {
        val newNumerators = arrayListOf<Expr>()
        val newDenominators = arrayListOf<Expr>()
        var newNum = num

        numerators.forEach {
            if (it is Number){
                newNum *= it.value
            } else {
                newNumerators.add(it)
            }
        }

        denominators.forEach {
            if (it is Number){
                newNum /= it.value
            } else {
                newDenominators.add(it)
            }
        }

        if (newNumerators.size + newDenominators.size == 0){
            return Number(newNum)
        }

        //newNumerators.add(Number(newNum))
        if (newNum == 1.0) {
            if (newNumerators.size == 0){
                newNumerators.add(Number(1.0))
            }
        } else {
            newNumerators.add(Number(newNum))
        }
        val term = Term()
        term.numerators.addAll(newNumerators)
        term.denominators.addAll(newDenominators)
        return term
    }
    override fun add(expr: Expr): Expr {
        when (expr){
            is Number -> {
                return Number(value + expr.value)
            }

            is Token -> {
                val sum = Sum()
                sum.plusTerms.add(expr)
                sum.plusTerms.add(this)
                return(sum)
            }

            is Term -> {
                if (expr.numerators.size == 1 && expr.denominators.size == 0 && expr.numerators[0] is Number){
                    return Number(this.value + (expr.numerators[0] as Number).value)
                } else {
                    val sum = Sum()
                    sum.plusTerms.add(expr)
                    sum.plusTerms.add(this)
                    return sum
                }
            }

            is Sum -> {
                return buildSum(expr.plusTerms, expr.minusTerms, this, Double::plus)

            }
        }

        return Token("ERROR")
    }

    override fun subtract(expr: Expr): Expr {
        when (expr){

            is Token -> {
                val sum = Sum()
                sum.minusTerms.add(expr)
                sum.plusTerms.add(this)
                return sum
            }

            is Number -> {
                val num = this.value - expr.value
                if (num >= 0) {
                    return Number(num)
                } else {
                    val sum = Sum()
                    sum.minusTerms.add(Number(-num))
                    return sum
                }
            }

            is Term -> {
                if (expr.numerators.size == 1 && expr.denominators.size == 0 && expr.numerators[0] is Number){
                    val num = this.value - (expr.numerators[0] as Number).value
                    if (num >= 0) {
                        return Number(num)
                    } else {
                        val sum = Sum()
                        sum.minusTerms.add(Number(-num))
                        return sum
                    }
                } else {
                    val sum = Sum()
                    sum.minusTerms.add(expr)
                    sum.plusTerms.add(this)
                    return sum
                }
            }

            is Sum -> {
                return buildSum(expr.minusTerms, expr.plusTerms, this, Double::plus) //negate the sum and add the number

            }
        }

        return Token("ERROR")
    }

    override fun multiply(expr: Expr): Expr {
        when (expr){

            is Token -> {
                val term = Term()
                term.numerators.add(this)
                term.numerators.add(expr)
                return term
            }

            is Number -> {
                return Number(expr.value * value)
            }

            is Term -> {
                return buildTerm(expr.numerators, expr.denominators, value)
            }

            is Sum -> {
                val newPlusTerms = arrayListOf<Expr>()
                val newMinusTerms = arrayListOf<Expr>()

                expr.plusTerms.forEach {
                    newPlusTerms.add(multiply(it))
                }

                expr.minusTerms.forEach {
                    newMinusTerms.add(multiply(it))
                }

                val sum = Sum()
                sum.plusTerms.addAll(newPlusTerms)
                sum.minusTerms.addAll(newMinusTerms)
                return sum
            }
        }
        return Token("ERROR")
    }

    override fun divide(expr: Expr): Expr {
        when (expr){

            is Token -> {
                val term = Term()
                term.numerators.add(this)
                term.denominators.add(expr)
                return term
            }

            is Number -> {
                return Number(value / expr.value)
            }

            is Term -> {
                return buildTerm(expr.denominators, expr.numerators, value)
            }

            is Sum -> {
                val term = Term()
                term.numerators.add(this)
                term.denominators.add(expr)
                /*val coefficientAndExpr = getCoefficientAndExpr(term)
                return getExprFromCoefficientAndExpr(coefficientAndExpr)*/
                return rationalizeTerm(term)
            }
        }

        return Token("ERROR")
    }

    override fun toAnnotatedString(exp: Int): AnnotatedString {
        val formatter = NumberFormat.getInstance()
        formatter.maximumFractionDigits = 3
        return AnnotatedString(formatter.format(value))
    }

    override fun equals(expr: Expr): Boolean {
        
        if (expr is Number) {
            return expr.value == value
        }
        return false
    }

    override fun clone(): Expr {
        return Number(value)
    }
}

/*
    A token represents a bond graph that would appear in an equation, such as momentum, resistance,
    capacitance etc.  A token is associated with at least one bond. The modulus on a transformer or
    gyrator is associated with two bonds. The class contains various flag to describe the nature of
    the token that are needed for generating and displaying equations.
 */
class Token(
    val bondId1: String = ""
    ,val bondId2: String = ""
    ,val name: AnnotatedString = AnnotatedString("")
    ,val powerVar: Boolean = false  // source of effort and source of flow tokens
    ,val energyVar: Boolean = false // Displacement on a capacitor or momentum on an inertia
    ,val independent: Boolean = false
    ,val differential: Boolean = false
    ): Expr {

    //val uniqueId = name.text + bondId1


    // The add, subtract, multiply and divide functions for this class are easy.  Just create the
    // appropriate type of expression and then use functions from the expression.
    override fun add(expr: Expr): Expr {
        return Sum().add(expr).add(this)
    }

    override fun subtract(expr: Expr): Expr {
        return Sum().add(this).subtract(expr)
    }

    override fun multiply(expr: Expr): Expr {
        return Term().multiply(this).multiply(expr)
    }

    override fun divide(expr: Expr): Expr {
        return Term().multiply(this).divide(expr)
    }

    // An element object create only copy of each of its tokens so they can be compared at the object level.
    override fun equals(expr: Expr): Boolean {

        return this === expr
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
                append("\u0307") // put a dot over the previous character.
            }

            pushStyle(subscript)
            append(bondId1)
            if ( ! bondId2.equals("")) {
                append(",")
                append(bondId2)
            }
            pop()
            if (powerVar) {
                append ("(t)")
            }
            if (exp > 0) {
                pushStyle(superScript)
                append(exp.toString())
            }
        }
    }

    override fun clone(): Expr {
        return this  // do not copy tokens. They need to be unique.
    }
}
var count = 0
/*
    A term is made up of numerator and a denominator.  The numerator and denominator are both made up of
    a list of expressions that are multiply together. So the numerator list R1, C1, and the denominator list
    I4, R2, R3 represents R1C1/I4R2R3
 */
class Term():Expr {

    val numerators = arrayListOf<Expr>()
    val denominators = arrayListOf<Expr>()


    override fun toAnnotatedString(exp: Int): AnnotatedString {
        return buildAnnotatedString {
            if (numerators.size == 0) {
                append ("1")
            } else {
                numerators.forEach {
                    when (it) {
                        is Token -> append(it.toAnnotatedString())
                        is Number -> append(it.toAnnotatedString())
                        is Term -> append(it.toAnnotatedString())
                        is Sum -> {
                            append("(")
                            append(it.toAnnotatedString())
                            append(")")
                        }
                    }
                }
            }
             if (denominators.size > 0){
                 append("/")
                 denominators.forEach {
                     when (it) {
                         is Token -> append(it.toAnnotatedString())
                         is Number -> append(it.toAnnotatedString()) // shouldn't be any numbers in denominators
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
    fun bulidTerm(num: Number, term: Term, operation: (Double, Double) -> Double): Expr {
        val newNumerators = LinkedList<Expr>()
        var newNum = operation (1.0, num.value)


        term.numerators.forEach {
            if (it is Number){
                newNum *= it.value
            } else {
                newNumerators.add(it)
            }
        }

        if (newNum != 1.0) {
            newNumerators.addFirst(Number(newNum.absoluteValue))
        }
        val newTerm = Term()
        newTerm.numerators.addAll(newNumerators)
        newTerm.denominators.addAll(term.denominators)

        if (newNum < 0){
            return Sum().subtract(newTerm)
        }

        return  rationalizeTerm(newTerm)
    }
    override fun add(expr: Expr): Expr {
        var sum = Sum().add(this)
        sum = sum.add(expr)
        return sum
    }

    override fun subtract(expr: Expr): Expr {

        return Sum().add(this).subtract(expr)
    }

    override fun multiply(expr: Expr): Expr {

        val newNumerators = arrayListOf<Expr>()
        val newDenominators = arrayListOf<Expr>()

        newNumerators.addAll(numerators)
        newDenominators.addAll(denominators)


        when (expr) {

            is Token -> {
                newNumerators.add(expr)
            }

            is Number -> {
                val newTerm = bulidTerm(expr, this, Double::times)
                return newTerm
            }

            is Term -> {
                // given a/b X x/y we want ax/by  not a(x/y)/b
                newNumerators.addAll(expr.numerators)
                newDenominators.addAll(expr.denominators)
            }

            is Sum -> {
                // If sum looks like (a + b) we want a Sum (this X a + this X b) not a Term this( a + b)
                // We can get this by calling the Sum multiply function.
                val term = Term()
                term.numerators.addAll(newNumerators)
                term.denominators.addAll(newDenominators)
                val newExpr = expr.multiply(term)
                return newExpr  // This will call Sum.multiply since expr is a Sum
            }
        }

        var term: Term = Term()
        term.numerators.addAll(newNumerators)
        term.denominators.addAll(newDenominators)
        /*val coefficientAndTerm = getCoefficientAndExpr(term)

        term.numerators.add(Number(coefficientAndTerm.coefficient.absoluteValue))
        val newExpr = cancel(term)
        if (coefficientAndTerm.coefficient < 0) {
            val sum = Sum()
            sum.minusTerms.add(expr)
            return sum
        }
        return newExpr*/

        cancel(term)
        return rationalizeTerm(term)
    }

    override fun divide(expr: Expr): Expr {

        val newNumerators = arrayListOf<Expr>()
        val newDenominators = arrayListOf<Expr>()

        newNumerators.addAll(numerators)
        newDenominators.addAll(denominators)


         when (expr) {

             is Token -> {
                 newDenominators.add(expr)
             }

             is Number -> {
                 return bulidTerm(expr, this, Double::div)
             }

             is Term -> {
                 // Since we are dividing add numerators to the denominator and denominators to the numerator.
                 newNumerators.addAll(expr.denominators)
                 newDenominators.addAll(expr.numerators)
             }

             is Sum -> {
                 newDenominators.add(expr)
             }
         }

        if (newDenominators.size == 0 && newNumerators.size == 1) {
            // Don't create a term that is just holding one other expression. Just return the expression.
            return newNumerators[0]
        }

        // Create a new term and call cancel on it.
        var term = Term()
        term.numerators.addAll(newNumerators)
        term.denominators.addAll(newDenominators)
       /* val coefficientAndTerm = getCoefficientAndExpr(term)
        //term = coefficientAndTerm.term
        term.numerators.add(Number(coefficientAndTerm.coefficient.absoluteValue))
        val expr = cancel(term)
        if (coefficientAndTerm.coefficient < 0) {
            val sum = Sum()
            sum.minusTerms.add(expr)
            return sum
        }

        return term*/

        cancel (term)
        return rationalizeTerm(term)
    }

    /*
     Check to see of this object is equal to expr.  This is a basic test.  We compare the expressions in the
     then numerator and denominator of both terms to see if they are the same. They don't have to be in the order.
     However, we don't expand any expressions.  If one term contains the two tokens 'ax' and the other term contains
     a term 'term(ax)' this function won't find it.
     */
    override fun equals(expr: Expr): Boolean {

        val exprNumerators = arrayListOf<Expr>()
        val exprDenominators = arrayListOf<Expr>()


        if (this === expr){
            // Try this first since it is quick
            return true
        }

        if (expr !is Term) {
            // can't be = to this if it isn't the same type of object.
            return false
        }

        exprNumerators.addAll(expr.numerators)
        exprDenominators.addAll(expr.denominators)

        // If both expressions don't contain the same number of elements they can't be equal.
        if (exprNumerators.size != numerators.size || exprDenominators.size != denominators.size) {
            return false
        }

        // Done with quick easy checks.  Compare expressions term by term.
        if (compareLists(numerators, exprNumerators)) {
            if (compareLists(denominators, exprDenominators)) {
                return true
            }
        }

        return false
    }

    override fun clone(): Expr {

        val term = Term()
        term.numerators.addAll(numerators)
        term.denominators.addAll(denominators)
        return term
    }

    fun getNumeratorTokens(): List<Token> {
        return numerators.filter { it is Token }.map{ it as Token}
    }

    // Remove the token from the numerator of this term.  We use this function for factoring.
    fun removeToken(token: Token): Expr {
        numerators.remove(token)
        if (numerators.size == 1 && denominators.size == 0) {
            return numerators[0]
        } else {
            return this
        }
    }

}
/*
    The Sum class is used for adding and subtracting expressions.  A Sum maintains two lists, one of
    expressions that should be added to the Sum and one of expressions that should be subtracted
    from the Sum.
 */
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
                //println ("Sum.toAnnotatedString minusTerm ${it}: ${it::class.simpleName}")
                append (" - ")
                //println("append minus sign")
                if (it is Sum) append("(")
                append (it.toAnnotatedString())
                if (it is Sum) append(")")
            }
        }
    }

    /*
        Pretty self explanatory. If expression is a token or term add it to the plusTerms list.  If it
        is a sum add the sum's plus term to the plusTerms list and its minus terms to the minusTerms list.
     */
    override fun add(expr: Expr): Expr {

        val newPlusTerms = arrayListOf<Expr>()
        val newMinusTerms = arrayListOf<Expr>()


        newPlusTerms.addAll(plusTerms)
        newMinusTerms.addAll(minusTerms)


        when (expr) {

            is Token -> {
                newPlusTerms.add(expr)
            }

            is Number -> {
                return buildSum(plusTerms, minusTerms, expr, Double::plus)
            }

            is Term -> {
                newPlusTerms.add(expr)
            }

            is Sum -> {
                newPlusTerms.addAll(expr.plusTerms)
                newMinusTerms.addAll(expr.minusTerms)
            }
        }

        val sum = Sum()
        sum.plusTerms.addAll(newPlusTerms)
        sum.minusTerms.addAll(newMinusTerms)
        //val expression =  combineTerms(sum)
        if (sum.plusTerms.size + sum.minusTerms.size > 1) {
            val expr = combineTerms(sum)
            return expr
            }
        return sum
    }


    // Same as add above only add expression to minusTerms list
    override fun subtract(expr: Expr): Expr {

        val newPlusTerms = arrayListOf<Expr>()
        val newMinusTerms = arrayListOf<Expr>()

        newPlusTerms.addAll(plusTerms)
        newMinusTerms.addAll(minusTerms)
        when (expr) {

            is Token -> {
                newMinusTerms.add(expr)
            }

            is Number -> {
                    return buildSum(plusTerms, minusTerms, expr, Double::minus)
            }

            is Term -> {
                newMinusTerms.add(expr)
            }

            is Sum -> {
                newPlusTerms.addAll(expr.minusTerms)
                newMinusTerms.addAll(expr.plusTerms)
            }
        }

        var sum = Sum()
        sum.plusTerms.addAll(newPlusTerms)
        sum.minusTerms.addAll(newMinusTerms)
        if (sum.plusTerms.size + sum.minusTerms.size > 1) {
            val expr = combineTerms(sum)
            return expr
        }
        return sum
    }

    // We want to keep the sum expanded so multiply each term in the sum by the expression.
    override fun multiply(expr: Expr): Expr {

        val newPlusTerms = arrayListOf<Expr>()
        val newMinusTerms = arrayListOf<Expr>()

        //newPlusTerms.addAll(plusTerms)
        //newMinusTerms.addAll(minusTerms)



        plusTerms.forEach {
            val newExpr = it.multiply(expr)
            if (newExpr is Sum && newExpr.plusTerms.size == 0 && newExpr.minusTerms.size == 1) {
                newMinusTerms.add(newExpr.minusTerms[0])
            } else {
                newPlusTerms.add(newExpr)
            }
        }
        minusTerms.forEach {
            val newExpr = it.multiply(expr)
            if (newExpr is Sum){
                if (newExpr.plusTerms.size == 0 && newExpr.minusTerms.size == 1) {
                    newPlusTerms.add(newExpr.minusTerms[0])
                } else {
                    newPlusTerms.addAll(newExpr.minusTerms)
                    newMinusTerms.addAll(newExpr.plusTerms)
                }
            } else {
                newMinusTerms.add(newExpr)
            }
        }
        /*for (index in 0 .. newPlusTerms.size -1){
            newPlusTerms[index] = newPlusTerms[index].multiply(expr)
        }

        for (index in 0 .. newMinusTerms.size -1){
            newMinusTerms[index] = newMinusTerms[index].multiply(expr)
        }*/

        val sum = Sum()
        sum.plusTerms.addAll(newPlusTerms)
        sum.minusTerms.addAll(newMinusTerms)
        return combineTerms(sum)
    }

    // We want to keep the sum expanded so divide each term in the sum by the expression.
    override fun divide(expr: Expr): Expr {

        val newPlusTerms = arrayListOf<Expr>()
        val newMinusTerms = arrayListOf<Expr>()


        //plusTerms.forEach { newPlusTerms.add(it.divide(expr)) }
        //minusTerms.forEach { newMinusTerms.add(it.divide(expr)) }

        plusTerms.forEach {
            val newExpr = it.divide(expr)
            if (newExpr is Sum && newExpr.plusTerms.size == 0 && newExpr.minusTerms.size == 1) {
                newMinusTerms.add(newExpr.minusTerms[0])
            } else {
                newPlusTerms.add(newExpr)
            }
        }
        minusTerms.forEach {
            val newExpr = it.divide(expr)
            if (newExpr is Sum){
                if (newExpr.plusTerms.size == 0 && newExpr.minusTerms.size == 1) {
                    newPlusTerms.add(newExpr.minusTerms[0])
                } else {
                    newPlusTerms.addAll(newExpr.minusTerms)
                    newMinusTerms.addAll(newExpr.plusTerms)
                }
            } else {
                newMinusTerms.add(newExpr)
            }
        }

        val sum = Sum()
        sum.plusTerms.addAll(newPlusTerms)
        sum.minusTerms.addAll(newMinusTerms)
        val expr = combineTerms(sum)
        return expr
    }

    /*
    Check to see of this object is equal to expr.  This is a basic test.  We compare the expressions in the
    plusTerms and minusTerms of both sums to see if they are the same. They don't have to be in the order.
    However, we don't expand any expressions.  If one sum contains the two  ( a = b + c) and the other term contains
    a term (a + sum(b + c) this function won't find it.
    */
    override fun equals(expr: Expr): Boolean {

        val exprPlusTerms = arrayListOf<Expr>()
        val exprMinusTerms = arrayListOf<Expr>()
        val copy = arrayListOf<Expr>()
        var foundOne = false


        // Quickest easiest test first
        if (this === expr ) {
            return true
        }

        // If expr is not the same type of object it can't be equal
        if ( ! (expr is Sum)) {
            return false
            }

        exprPlusTerms.addAll(expr.plusTerms)
        exprMinusTerms.addAll(expr.minusTerms)

        // If this and expression don't have the same number of terms, they can't be equal.
        if (plusTerms.size != exprPlusTerms.size || minusTerms.size != exprMinusTerms.size) {
            return false
        }

        // Done with easy check, have to compare term by term.
        if (compareLists(plusTerms, exprPlusTerms)) {
            if (compareLists(minusTerms, exprMinusTerms)) {
                return true
            }
        }

        return false
    }

    override fun clone(): Expr {
        val sum = Sum()
        sum.plusTerms.addAll(plusTerms)
        sum.minusTerms.addAll(minusTerms)
        return sum
    }

    fun getAllExpressions(): List<Expr> {
        val l: ArrayList<Expr> = arrayListOf()
        l.addAll(plusTerms)
        l.addAll(minusTerms)
        return l
    }


}

/*
Simple data class.  Holds one expression for the left side of the equation and another for the right side.
 */
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

    fun clone(): Equation {
        return Equation(leftSide.clone(), rightSide.clone())
    }

}

class Matrix private constructor(val data: ArrayList<ArrayList<Expr>>) {

    companion object {
        fun solveCramer(coeff: Matrix, variables: ArrayList<Token>, const: ArrayList<Expr>): ArrayList<Equation> {

            val equations = arrayListOf<Equation>()

            val coeffDet = coeff.det()
            val size = coeff.data.size
            for (matrixIndex in 0 until size){
                val builder = Matrix.Builder(size)
                for (row in 0 until size){
                    for (col in 0 until size){
                        if (matrixIndex == col){
                            builder.add(const[row])
                        } else {
                            builder.add(coeff.data[row][col])
                        }
                    }
                }
                val matrix = builder.build()
                equations.add(Equation(variables[matrixIndex], matrix.det().divide(coeffDet)))
            }
            return equations
        }

    }
    class Builder (val order: Int){
        val data = arrayListOf<ArrayList<Expr>>()
        var cnt = 0

        fun add (expr: Expr): Builder{
            if (cnt >= order * order) throw IllegalArgumentException("This call to Matrix.Builder.add exceeds the amount of data this matrix (order $order) can hold.")
            if (cnt.div(order) + 1 > data.size ) {
                data.add(ArrayList())
            }
            data[cnt.div(order)].add(expr)
            cnt++
            return this
        }

        fun build(): Matrix {
            if (cnt != (order * order) ) throw IllegalStateException("Not enough data to build this matrix of order $order. Number of data points = $cnt")
            return Matrix (data)
        }
    }

    fun cofactor(row: Int, column: Int): Matrix {
        val builder = Matrix.Builder(data.size - 1)
        for (i in 0 until data.size){
            if (i != row){
                for (j in 0 until data.size){
                    if (j != column){
                        builder.add(data[i][j])
                    }
                }
            }
        }
        return builder.build()
    }

    fun det(): Expr {
        if (data.size == 2) {
            val product1 = Term().multiply(data[0][0]).multiply(data[1][1])
            val product2 = Term().multiply(data[1][0]).multiply(data[0][1])
            val difference = product1.subtract(product2)
            //return(Term().multiply(data[0][0]).multiply(data[1][1]).subtract((Term().multiply(data[1][0]).multiply(data[0][1]))))
            return difference
        } else {
            var det:Expr = Sum()
            var addIt = true
            for (column in 0 until data.size){
                if (addIt){
                    val cofactorDet = cofactor(0,column).det()
                    //val product = Term().multiply(data[0][column]).multiply(cofactorDet)
                    val product = data[0][column].multiply(cofactorDet)
                    det = det.add(product)
                } else {
                    val cofactorDet = cofactor(0,column).det()
                    //val product = Term().multiply(data[0][column]).multiply(cofactorDet)
                    val product = data[0][column].multiply(cofactorDet)
                    det = det.subtract(product)
                }
                addIt = ! addIt
            }
            return det
        }
    }

    fun printOut(){
        for (i in 0 until data.size){
            for (j in 0 until data.size){
                if (j != 0) print(", ")
                print("${data[i][j].toAnnotatedString()}: ${data[i][j]::class.simpleName}")
            }
            println()
        }
    }
}
