package algebra

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
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
interface Expr{

    fun add(expr: Expr): Expr

    fun subtract(expr: Expr): Expr

    fun multiply(expr: Expr): Expr

    fun divide(expr: Expr): Expr

    fun toAnnotatedString(exp: Int = 0): AnnotatedString

    fun equals(expr: Expr): Boolean
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
            if ((name.text == "e") || name.text == "f") {
                append ("(t)")
            }
            if (exp > 0) {
                pushStyle(superScript)
                append(exp.toString())
            }
        }
    }
}

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
                return expr.multiply(term)  // Calling Sum.multiply since expr is Sum
            }
        }

        val term = Term()
        term.numerators.addAll(newNumerators)
        term.denominators.addAll(newDenominators)
        return cancel(term)
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
        val term = Term()
        term.numerators.addAll(newNumerators)
        term.denominators.addAll(newDenominators)
        return cancel(term)
    }

    /*
     Check to see of this object is equal to expr.  This is a basic test.  We compare the expressions in the
     then numerator and denominator of both terms to see if they are the same. They don't have to be in the order.
     However we don't expand any expressions.  If one term contains the two tokens ax and the other term contains
     a term(ax) this function won't find it.
     */
    override fun equals(expr: Expr): Boolean {

        val exprNumerators = arrayListOf<Expr>()
        val exprDenominators = arrayListOf<Expr>()

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

    fun getNumeratorTokens(): List<Token> {
        return numerators.filter { it is Token }.map{ it as Token}
    }

    fun getDenominatorTokens(): List<Token> {
        return denominators.filter { it is Token }.map{ it as Token}
    }

    fun getDemominatorExpressions() : List<Expr> {
        return denominators
    }

    fun removeToken(token: Token): Expr {
        numerators.remove(token)
        //denomintors.remove(token)
        if (numerators.size == 1 && denominators.size == 0) {
            return numerators[0]
        } else {
            return this
        }
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

        val newPlusTerms = arrayListOf<Expr>()
        val newMinusTerms = arrayListOf<Expr>()

        newPlusTerms.addAll(plusTerms)
        newMinusTerms.addAll(minusTerms)

        when (expr) {

            is Token -> {
                newPlusTerms.add(expr)
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
        return sum
    }


    override fun subtract(expr: Expr): Expr {

        val newPlusTerms = arrayListOf<Expr>()
        val newMinusTerms = arrayListOf<Expr>()

        newPlusTerms.addAll(plusTerms)
        newMinusTerms.addAll(minusTerms)

        when (expr) {

            is Token -> {
                newMinusTerms.add(expr)
            }

            is Term -> {
                newMinusTerms.add(expr)
            }

            is Sum -> {
                newPlusTerms.addAll(expr.minusTerms)
                newMinusTerms.addAll(expr.plusTerms)
            }
        }

        val sum = Sum()
        sum.plusTerms.addAll(newPlusTerms)
        sum.minusTerms.addAll(newMinusTerms)
        return sum
    }

    override fun multiply(expr: Expr): Expr {

        val newPlusTerms = arrayListOf<Expr>()
        val newMinusTerms = arrayListOf<Expr>()

        newPlusTerms.addAll(plusTerms)
        newMinusTerms.addAll(minusTerms)


        for (index in 0 .. newPlusTerms.size -1){
            newPlusTerms[index] = newPlusTerms[index].multiply(expr)
        }

        for (index in 0 .. newMinusTerms.size -1){
            newMinusTerms[index] = newMinusTerms[index].multiply(expr)
        }

        val sum = Sum()
        sum.plusTerms.addAll(newPlusTerms)
        sum.minusTerms.addAll(newMinusTerms)
        return sum
    }

    override fun divide(expr: Expr): Expr {

        val newPlusTerms = arrayListOf<Expr>()
        val newMinusTerms = arrayListOf<Expr>()

        newPlusTerms.addAll(plusTerms)
        newMinusTerms.addAll(minusTerms)

        for (index in 0 .. newPlusTerms.size -1){
            newPlusTerms[index] = newPlusTerms[index].divide(expr)
        }

        for (index in 0 .. minusTerms.size -1){
            newMinusTerms[index] = newMinusTerms[index].divide(expr)
        }

        val sum = Sum()
        sum.plusTerms.addAll(newPlusTerms)
        sum.minusTerms.addAll(newMinusTerms)
        return sum
    }

    override fun equals(expr: Expr): Boolean {

        val exprPlusTerms = arrayListOf<Expr>()
        val exprMinusTerms = arrayListOf<Expr>()
        val copy = arrayListOf<Expr>()
        var foundOne = false


        if (this === expr ) {
            return true
        }

        if ( ! (expr is Sum)) {
            return false
            }

        exprPlusTerms.addAll(expr.plusTerms)
        exprMinusTerms.addAll(expr.minusTerms)

        for (e1 in plusTerms) {
            foundOne = false
            copy.clear()
            copy.addAll(exprPlusTerms)
            for (e2 in copy) {
                if (e1.equals(e2)){
                    foundOne = true
                    exprPlusTerms.remove(e2)
                    break
                }
            }

            if ( ! foundOne) {
                return false
            }
        }

        for (e1 in minusTerms) {
            foundOne = false
            copy.clear()
            copy.addAll(exprMinusTerms)
            for (e2 in copy) {
                if (e1.equals(e2)){
                    foundOne = true
                    exprMinusTerms.remove(e2)
                    break
                }
            }

            if ( ! foundOne) {
                return false
            }
        }

        return true
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
