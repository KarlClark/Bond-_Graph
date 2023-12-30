package algebra

import androidx.compose.ui.text.AnnotatedString
import bondgraph.AlgebraException
import bondgraph.Element
import kotlin.math.absoluteValue

fun List<Expr>.containsExpr(expr: Expr): Boolean {
    return this.any{expr.equals(it)}
}
fun testCases(){

    val at = Token("1", "", AnnotatedString("A"))
    val bt = Token("1", "", AnnotatedString("B"))
    val ct = Token("1", "", AnnotatedString("C"))
    val xt = Token("1", "", AnnotatedString("X"))
    val yt = Token("1", "", AnnotatedString("Y"))
    val zt = Token("1", "", AnnotatedString("Z"))
    val n10 = Number(10.0)
    val n2 = Number(2.0)
    val n6 = Number(6.0)


    var term1 = Term()
    var term2 = Term()
    var sum1 = Sum()
    var sum2 = Sum()
    var expr1: Expr
    var expr2: Expr
    var expr3: Expr
    var expr4: Expr
    var expr5: Expr
    println("Test cases &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&")
    println(Term().multiply(xt).divide(yt).multiply(n10).toAnnotatedString())
    println("-------------------------------------------------------------")
    expr1 = sum1.add(at).add(bt).add(n10)
    println("expr1 = ${expr1.toAnnotatedString()}")
    expr2 = term1.multiply(xt).divide(yt)
    println("expr2 = ${expr2.toAnnotatedString()}")
    expr3 = expr1.multiply(expr2)
    expr4 = expr2.multiply(at).divide(bt).divide(n10)
    expr5 = Term().multiply(bt).divide(ct).multiply(n6)

    println("expr3 = ${expr3.toAnnotatedString()}")
    println("expr4 = ${expr4.toAnnotatedString()}")
    println("expr5 = ${expr5.toAnnotatedString()}")
    expr5 = expr3.divide(expr5)
    println("expr5 = ${expr5.toAnnotatedString()}")
    println ("result ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
    println("${expr1.toAnnotatedString()} x ${expr2.toAnnotatedString()} = ${expr3.toAnnotatedString()}  ${expr3::class.simpleName}")
    expr3 = expr1.divide(n2)
    println(expr3.toAnnotatedString())
    expr3 = expr3.multiply(n10)
    println(expr3.toAnnotatedString())
    //expr3 = expr3.multiply(expr1)
    expr3 = expr3.divide(n6)
    println(expr3.toAnnotatedString())
    expr3 = expr3.subtract(n10)
    println(expr3.toAnnotatedString())
    expr3 = expr3.multiply(n6)
    println(expr3.toAnnotatedString())
    expr3 = expr3.add(n6)
    println(expr3.toAnnotatedString())
    expr3 = expr3.add(expr3)
    println(expr3.toAnnotatedString())
    println ("result ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
    println("${expr2.toAnnotatedString()} x ${expr1.toAnnotatedString()} = ${expr3.toAnnotatedString()}  ${expr3::class.simpleName}")
}
/*
A Term may be made up of other Terms.  For example a Term could consist of a Token and a another Term.
If the Token represents x and the other Term represents yz the whole Term represents xyz.  Most of
the time this is fine but causes a problem when we want to cancel like factors in the numerator and
denominator of a fraction (also a Term) If the numerator is made up of the above Term and the denominator
was say abz all made up of all Tokens, then we can't see the z in the numerator to cancel it. So this
function takes a Term (that is not a fraction) and expands it to an arraylist of Tokens and Sums.

Note: A Sum can also mask a single Token.  I call these hanging sums. But we don't deal with that here
since a Sum can also be controlling the sign of the whole Term.  These Sums crop up when simplifying
a Sum like (a + b -a). After simplifying we are left with a Sum with just one Token b. The simplifySums
function resolves these to single Tokens.  But as this code evolves, such sums may crop up in other areas.
 */

fun stripCoefficentFromList(source: ArrayList<Expr>, dest: ArrayList<Expr>, startNum: Double, operation: (Double, Double) -> Double): Double {
    var num = startNum
    source.forEach {
        when (it) {
            is Token -> dest.add(it)
            is Number -> num = operation(num, it.value)
            is Term -> {
                val p = stripCoefficient(it)
                dest.add(p.second)
                num = operation(num, p.first)
            }
            is Sum -> dest.add(it)
        }
    }
    return num
}

fun rationalizeCoefficient(term: Term): Term {

    val numerators = arrayListOf<Expr>()
    val denominators = arrayListOf<Expr>()
    var num = 1.0

    num = stripCoefficentFromList(term.numerators, numerators, 1.0, Double::times)
    num = stripCoefficentFromList(term.denominators, denominators, num, Double::div)

    val newTerm = Term()
    if (num != 1.0) {
        newTerm.numerators.add(Number(num))
    }
    newTerm.numerators.addAll(numerators)
    newTerm.denominators.addAll(denominators)
    return newTerm
}

/*
    Take a term and strip off the number coefficient and return a pair containing the value
    of the coefficient and the rest of the term.  If the rest of the program is working correctly,
    the coefficient should be the first item in the numerator.  But, this function searches all
    items in both the numerator and denominator just in case.  In addition, if more than one
    number is found we calculate a new number by multiplying/dividing the numbers together.
 */
fun stripCoefficient(term: Term): Pair<Double, Term>{

    val numerators = arrayListOf<Expr>()
    val denominators = arrayListOf<Expr>()
    var num = 1.0


    num = stripCoefficentFromList(term.numerators, numerators, 1.0, Double::times)
    num = stripCoefficentFromList(term.denominators, denominators, num, Double::div)

    val term = Term()
    term.numerators.addAll(numerators)
    term.denominators.addAll(denominators)
    return Pair(num, term)
}

fun combineTerms(sum: Sum): Expr {

    val termValueMap = hashMapOf<Expr, Double>()
    var number: Double
    var foundOne = false

    fun processTerms(exprs: ArrayList<Expr>, startNum: Double, operation: (Double, Double) -> Double): Double{

        var num = startNum
        var value1 = 1.0

        for (expr in exprs) {
            var expr1 = expr
            if (expr is Term){
                val pair = stripCoefficient(expr)
                expr1 = pair.second
                value1 = pair.first
            }
            foundOne = false
            for (expr2 in termValueMap.keys) {
                if (expr1.equals(expr2)) {

                    foundOne = true
                    val value2 = termValueMap[expr2]
                    termValueMap[expr2] = operation(value2!!, value1)
                   /* when (expr1) {
                        is Token -> termValueMap[expr2] = operation(value2!!, value1)
                        //is Number -> num = operation(num, expr1.value)
                        is Term -> termValueMap[expr2] = operation(termValueMap[expr2]!!, stripCoefficient(expr1).first)
                        is Sum -> termValueMap[expr2] = operation(value2!!, 1.0)
                    }*/
                }

            }
            if (!foundOne) {
                /*when (expr1) {
                    is Token -> termValueMap[expr1] = operation(0.0, 1.0)
                    is Number -> num = operation(num, expr1.value)
                    is Term -> {
                        val pair = stripCoefficient(expr1)
                        termValueMap[pair.second] = operation(0.0, pair.first)
                    }

                    is Sum -> termValueMap[expr1] = operation(0.0, 1.0)
                }*/
                if (expr1 is Number) {
                    num = operation(num, value1)
                } else {
                    termValueMap[expr1] = operation(0.0, value1)
                }
            }
        }

        return num
    }
    number = processTerms(sum.plusTerms, 0.0,  Double::plus)
    number = processTerms(sum.minusTerms, number, Double::minus)

    val sum = Sum()
    termValueMap.forEach{(key, value) ->
        if (value != 0.0) {
            var expr: Expr = Term()
            if (value != 1.0) {
                expr = expr.multiply(Number(value.absoluteValue))
            }
            expr = expr.multiply(key)
            if (value < 0) {
                sum.minusTerms.add(expr)
            } else {
                sum.plusTerms.add(expr)
            }
        }
    }

    if (number != 0.0) {
        sum.plusTerms.add(Number(number))
    }

   /* if (sum.plusTerms.size == 1 && sum.minusTerms.size == 0){
        return sum.plusTerms[0]
    }*/
    return sum
}
fun expandTerm(term: Term): ArrayList<Expr> {

    val newTerms = arrayListOf<Expr>()

    if (term.denominators.size > 0 ){
        return newTerms
    }

    // Add everything in the numerators to the new list. If you find another Term expand it too.
    term.numerators.forEach {
        if (it is Term ) {
            // If we find another Term call ourself recursively to expand it also.
            newTerms.addAll(expandTerm(it))
        } else {
            newTerms.add(it)
        }
    }
    return newTerms
}
/*
This function cancels like factors in the numerator and denominator of a fraction. Basically, create
lists of all the Tokens and Sums in the numerator and denominator and then get rid of any that occur
in both lists.
 */
fun cancel(term: Term): Expr{
    val numerators = arrayListOf<Expr>()
    val denominators = arrayListOf<Expr>()
    val copyOfNumerators = arrayListOf<Expr>()

/*
 Go through items in the numerator and denominators of the Term. Store all the Tokens and Sums
 in separate lists.  Expand any Terms that are found and add their Tokens and Sums to the lists.
 */
    term.numerators.forEach {
        if (it is Term) {
            numerators.addAll(expandTerm(it))
        } else {
            numerators.add(it)
        }
    }

    term.denominators.forEach {
        if (it is Term) {
            denominators.addAll(expandTerm(it))
        } else {
            denominators.add(it)
        }
    }

    copyOfNumerators.addAll(numerators)


    /*
    Iterate over copyOfNumerators because you can't modify a list you are iterating over. For every
    expression in numerators see if it exists in deonminators. If so delete it from both lists. Use
    our own .equals() functions because different objects may be equal as far as we are concerned
    i.e. (a + b) would equal (b + a).  Make sure to delete the correct object from the appropriate list.
     */
    copyOfNumerators.forEach {expr ->
        val denominator = denominators.find{d -> expr.equals(d)}
        if (denominator != null) {
            numerators.remove(expr)
            denominators.remove(denominator)
        }
    }

    // Create a new Term from the left over numerators and denominators.
    val newTerm = Term()
    newTerm.numerators.addAll(numerators)
    newTerm.denominators.addAll(denominators)
    return newTerm
}
/*
 Iterate over a given list of Expr, and create a new list of the Exprs where cancel has be called on every Expr
 that is a Term
 */
fun callCancelOnList(source: ArrayList<Expr>): ArrayList<Expr>{

    val newList = arrayListOf<Expr>()

    for (expr in source) {
        if (expr is Term) {
            newList.add(cancel(expr))
        } else {
            newList.add(expr)
        }
    }
    return newList
}
/*
The right side of an equation is either a Term or a Sum of Terms. Return a new equation where cancel
has been called on every Term on the right side.
 */
fun cancel(equation: Equation): Equation {
    val newPlusTerms = arrayListOf<Expr>()
    val newMinusTerms = arrayListOf<Expr>()

    if (equation.rightSide is Token || equation.rightSide is Number){
        return equation
    }

    if (equation.rightSide is Term) {
        return Equation(equation.leftSide, cancel(equation.rightSide as Term))
    }
    // Call cancel on all the plus terms and then on all the minus terms.
    newPlusTerms.addAll(callCancelOnList((equation.rightSide as Sum).plusTerms))
    newMinusTerms.addAll(callCancelOnList((equation.rightSide as Sum).minusTerms))

    // create new Sum with the canceled terms.
    val sum = Sum()
    sum.plusTerms.addAll(newPlusTerms)
    sum.minusTerms.addAll(newMinusTerms)

    return Equation(equation.leftSide, sum)
}

/*
The following function simplifies the Sums in all the terms of the right side
of the given Equation. Simplifying a Sum means taking something like this
(a + b  -a + c) and turning it into something like this (b + c)
 */
fun simplifySums(equation: Equation): Equation {

    val plusTerms = arrayListOf<Expr>()
    val minusTerms = arrayListOf<Expr>()
    val newPlusTerms = arrayListOf<Expr>()
    val newMinusTerms = arrayListOf<Expr>()

    // No Sum to simplify.
    if (equation.rightSide is Token || equation.rightSide is Number){
        return equation
    }

    // Build lists of plus and minus terms.
    if (equation.rightSide is Term){
        plusTerms.add(equation.rightSide)
    } else {
        plusTerms.addAll((equation.rightSide as Sum).plusTerms)
        minusTerms.addAll((equation.rightSide as Sum).minusTerms)
    }

    /*
     Call simplifySum an all the Terms in our lists, building new list of
     the simplified terms.  A couple of things to note.

     The checkForHangingSums function modifies its input lists.

     The lists are reversed in the call in the second loop.  This is because
     a plus factor from the minus terms stays in the minus terms.  But a negative
     factor from the minus terms -(-a) belongs in the plus terms.
     */
    for (term in plusTerms){
        resolveHangingSums(simplifySum(term), newPlusTerms, newMinusTerms)
    }
    for (term in minusTerms){
        resolveHangingSums(simplifySum(term), newMinusTerms, newPlusTerms)
    }

    // Create a new Sum from the new lists.
    val sum = Sum()
    sum.plusTerms.addAll(newPlusTerms)
    sum.minusTerms.addAll(newMinusTerms)

    // If sum is a positive hanging sum then resolve it.
    if (sum.plusTerms.size == 1 && sum.minusTerms.size == 0){
        val term = Term()
        term.numerators.add(sum.plusTerms[0])
        return Equation(equation.leftSide, term)
    }

    return Equation(equation.leftSide, sum)
}

/*
    The next two functions deal with what I call hanging sums.  When you simplify a Sum like Sum(a + b - a)
    you are left with a Sum that has just one factor in it Sum(b).  Internally a hanging Sum is a Sum that
    contains just one Term or Token in either its plusTerms list or its minusTerms list and nothing it the
    other list. This sum should be reduced to a Term or a Token.  The reason hanging sums are a problem is
    if you have an expression ab/cb you would like to be able to cancel the b.  But if the expression is
    constructed as aSum(b)/cb you can't see the b in the numerator to do the cancelling. Simplifying
    expressions with hanging sums is tricky because you may have a situation like Sum(a - b - a) that was
    reduced to Sum(-b).  This Sum may now be setting the sign for an entire expression. If you have an
    expression aSum(-b)/xy when you simplify this to ab/xy you need to move the entire expression into
    the minusTerms of whatever Sum it is part of or create a new Sum(-(ab/xy)).

    This first resolveHangingSums function takes a source list of Expressions and builds a new list that is
    the same as the source list except any hanging sums have been resolved to their base Term or Token. The
    source list will be the numerators or denominators of some other expression.  The caller needs
    to keep track if the sign of the expression has changed because of negative Sum.  So the caller
    provides an initial value for the isPlusTerm flag.  Every time this function finds a negative hanging
    sum it toggles this flag and returns it when it is finished.
 */
fun resloveHangingSums(source: ArrayList<Expr>, dest: ArrayList<Expr>, isPlusTerm: Boolean): Boolean {

    var localIsPlusTerm = isPlusTerm


    // Check every expression in the list to see if it is a hanging sum
    for (expr in source) {
        if (expr is Sum) {
            when {
                expr.plusTerms.size == 1 && expr.minusTerms.size == 0 -> {
                    // positive hanging sum
                    dest.add(expr.plusTerms[0])
                }
                expr.plusTerms.size == 0 && expr.minusTerms.size == 1 -> {
                    // negative hanging sum
                    dest.add(expr.minusTerms[0])
                    localIsPlusTerm = ! localIsPlusTerm
                }
                else -> {
                    // normal sum
                    dest.add(expr)
                }
            }
        } else {
            // an expression that is not a sum
            dest.add(expr)
        }
    }
    return localIsPlusTerm
}

/*
    Examine the given expression and resolve any hanging terms.  If the Expression is a fractions then
    resolve all hanging sums in both the numerator and denominator.  If the resolved expression is
    positive add it to the newPlusTerms list, otherwise add it to the newMinusTerms list.
 */
fun resolveHangingSums(expr: Expr, newPlusTerms: ArrayList<Expr>, newMinusTerms: ArrayList<Expr>) {

    val numerators = arrayListOf<Expr>()
    val denominators = arrayListOf<Expr>()
    val newNumerators = arrayListOf<Expr>()
    val newDenominators = arrayListOf<Expr>()
    var isPlusTerm: Boolean

    //Build lists for the terms in the numerator and denominator.
    if (expr !is Term) {
        // no denominator, just one expression to add
        numerators.add(expr)
    } else {
        numerators.addAll((expr as Term).numerators)
        denominators.addAll(expr.denominators)
    }

    // Resolve hanging sums in both list keeping track of the sign of the expression.
    isPlusTerm = resloveHangingSums(numerators, newNumerators, true)
    isPlusTerm = resloveHangingSums(denominators,newDenominators, isPlusTerm)


    // Create a new term from the new resolved numerators and denominators and add it to
    // either the newPlusTerms list or the new minusTermsList.
    val term = Term()
    term.numerators.addAll(newNumerators)
    term.denominators.addAll(newDenominators)
    if (isPlusTerm) {
        newPlusTerms.add(term)
    } else {
        newMinusTerms.add(term)
    }
}


/*
    The next two functions are used for expanding Sums. This means to get rid of nested Sums in a Sum. For
    example Sum( a + Sum( b + c) will be expanded to Sum(a + b + c)
    In the following functions the Sum class is used as a convenient data class to hold two lists, one
    of plus terms, and one of minus terms.
 */

/*
    This functions expands a Sum by looking for Sums in its plus term list and in its minus term list.
    Note that expandSum(Sum) calls expandSum(List) which may call expandSum(Sum) recursively.
 */
fun expandSum(sum: Sum): Sum {

    val plusTerms = arrayListOf<Expr>()
    val minusTerms = arrayListOf<Expr>()

    var newSum: Sum

    /*
    Expand the plus terms and the minus terms.
    Note that expanding the plus terms may produce plus terms and minus terms.
    Same with the minus terms.
    Note: the plus terms list produced from the minus terms should be added to
    our minus terms list and vice versa.
    */
    newSum = expandSum(sum.plusTerms)
    plusTerms.addAll(newSum.plusTerms)
    minusTerms.addAll(newSum.minusTerms)

    newSum = expandSum(sum.minusTerms)
    plusTerms.addAll(newSum.minusTerms)
    minusTerms.addAll(newSum.plusTerms)

    newSum = Sum()
    newSum.plusTerms.addAll(plusTerms)
    newSum.minusTerms.addAll(minusTerms)

    return newSum
}

/*
    Check every expression in the given list and expand any Sums found.  Since expanding a
    Sum will produce two list, one for plus terms and one for minus terms we use a Sum instance
    to return the two lists.
 */
fun expandSum (source: ArrayList<Expr>): Sum {

    val plusTerms = arrayListOf<Expr>()
    val minusTerms = arrayListOf<Expr>()

    for (expr in source){
        if (expr is Sum){
            // Recursive call to expandSum(Sum)
            var sum : Sum = expandSum(expr)
            plusTerms.addAll(sum.plusTerms)
            minusTerms.addAll(sum.minusTerms)

        } else {
            plusTerms.add(expr)
        }
    }

    val sum = Sum()
    sum.plusTerms.addAll(plusTerms)
    sum.minusTerms.addAll(minusTerms)
    return sum
}

/*
    The following function simplifies a Sum.  To simplify a sum means to take something
    like (a + b + c -a) and change it to (b + c).  If the given expression is fraction
    with a single Sum in the numerator then return the fraction with the numerator simplified.
 */
fun simplifySum(expr: Expr ): Expr {

    val copyOfPlusTerms = arrayListOf<Expr>()
    val copyOfMinusTerms = arrayListOf<Expr>()
    var sum: Sum

    if (expr is Token) {
        // nothing to simplify
        return expr
    }


    // before we can simplify we must expand the Sum.
    if (expr is Term) {
        if (expr.numerators.size == 1 && expr.numerators[0] is Sum) {
            sum = expandSum(expr.numerators[0] as Sum)
        } else {
            // We may want to extend this function to handle Term x Sum
            // i.e ab(a + b  + c -a) could be ab(b + c) But hanging sums
            // would complicate this.
            return expr
        }
    } else {
        sum = expandSum(expr as Sum)

    }

    if (sum.plusTerms.size == 0  || sum.minusTerms.size == 0){
        // Nothing to add or subtract
        return expr
    }

    // Need copy because we can't modify list we are iterating over.
    copyOfPlusTerms.addAll(sum.plusTerms)

    /*
    For each term in the plus terms list see if there is a matching term
    in the minus term list.  If there is, remove the term from both lists.
    Make a new copy of the minus terms list on each iteration because a
    term might occur twice in the plus terms but only once in the minus
    terms. Use our Expr.equals() function for comparisons.  Be sure to
    remove the correct objects from the correct list since they may not
    be equal from an object point of view.
    */
    for (e1 in copyOfPlusTerms){
        copyOfMinusTerms.clear()
        copyOfMinusTerms.addAll(sum.minusTerms)
        for (e2 in copyOfMinusTerms){
            if (e1.equals(e2)) {
                sum.plusTerms.remove(e1)
                sum.minusTerms.remove(e2)
            }
        }
    }

    if (expr is Term) {
        val term = Term()
        term.numerators.add(sum)
        term.denominators.addAll(expr.denominators)
        return term
    }

    return sum
}

/*
    This function checks to see if the Token occurs anywhere in the denominator
    of the expression.  This program solves equations for certain Tokens.  It
    currently can't solve an equation if the Token occurs in a denominator.
 */
fun isTokenInDenominator(token: Token, expr: Expr): Boolean {

    /*
    If the expression is a term then check every expression in its denominator.  If
    this new expression is a token, and it matches the input token then return true.
    If it not a token then call ourself recursively.
     */
    if (expr is Term) {
        for (ex in expr.denominators){
            if (ex is Token && ex === token) {
                return true
            }
            if ( isTokenInDenominator(token, ex)) {
                return true
            }
        }
    }

    // If expression is a Sum then call ourself recursively on every term  in the Sum.
     if (expr is Sum){
         val exprsList = expr.getAllExpressions()
         for (ex in exprsList) {
             if (isTokenInDenominator(token, ex)) {
                 return true
             }
         }
     }

    return false
}

/*
    Check t see if the token occurs in the numerator of the expression.
 */
fun contains(token: Token, expr: Expr): Boolean {
    return if (expr is Term) expr.getNumeratorTokens().contains(token) else false
}

/*
    The following function expands the product or a Term and a Sum.
    i.e ab(x + y)  to (abx + aby)
    If expr is a fraction, the expand the numerator.
 */
fun expandProductOfSumAndTerm(expr: Expr): Expr {

    val termList = arrayListOf<Expr>()
    val sumList = arrayListOf<Expr>()
    val plusNumerators = arrayListOf<Expr>()
    val minusNumerators = arrayListOf<Expr>()

    // create a Term that is the product of expr and all the Terms in the termList
    fun productTerm(expr: Expr): Expr{
        val term = Term()
        term.numerators.addAll(termList)
        term.numerators.add(expr)
        return term
    }


    if (expr !is Term){
        // nothing to expand
        return expr
    }



    // break expr apart into a list of Terms and a list o Sums.
    for (e in (expr).numerators){
        if (e is Token || e is Term) {
            termList.add(e)
        } else {
            sumList.add(e)
        }
    }

    if (sumList.size != 1) {
        // This function can't handle multiplying two or more sums together.
        return expr
    }

    // Expand any nested sums
    var sum = expandSum(sumList[0] as Sum)

    // multiply each Term in the Sum by the Terms outside the Sum and
    // store the new terms in the plusNumerators and minusNumerators lists.
    sum.plusTerms.forEach { plusNumerators.add(productTerm(it)) }
    sum.minusTerms.forEach { minusNumerators.add(productTerm(it)) }

    // Create a new expanded Sum from the plus and minus lists.
    sum = Sum()
    sum.plusTerms.addAll(plusNumerators)
    sum.minusTerms.addAll(minusNumerators)

    // If the original expression was a fraction return a new fraction
    // with the expanded Sum over the original denominator.
    if (expr is Term && expr.denominators.size > 0) {
        val term = Term()
        term.numerators.add(sum)
        term.denominators.addAll(expr.denominators)
        return term
    }
    // Otherwise return the expanded Sum.
    return sum
}

/*
    Take the expression and convert it to a equivalent expression with a denominator
    equal to the common denominator.  This os done by multiplying the numerator by all
    the terms in the common denominator that are not in the expression's denominator.
    example expression ab/xy  common denominator xymn  new expression abmn/xymn. This
    function just calculates and returns the numerator part 
 */
fun convertExpressionNumeratorToCommonDenominator(expr: Expr, commonDenominator: List<Expr>): Expr {

    val copyOfCommonDenominator = arrayListOf<Expr>()

    if (expr is Token || expr is Sum) {
        // no denominator. Multiply the entire expression by the common denominator
        val term = Term()
        term.numerators.add(expr)
        term.numerators.addAll(commonDenominator)
        return expandProductOfSumAndTerm(term)
    }
    
    // Make a copy of the common denominator.  Then remove each term in the expression's 
    // denominator from the copy of the common denominator.  What's left is what we need
    // to multiply the numerator by.    
    copyOfCommonDenominator.addAll(commonDenominator)
    for (term in (expr as Term).denominators) {
        if (copyOfCommonDenominator.containsExpr(term)) {
            copyOfCommonDenominator.remove(term)
        }
    }

    // Create the new numerator and return the expanded form of it.
    val term = Term()
    term.numerators.addAll(expr.numerators)
    term.numerators.addAll(copyOfCommonDenominator)

    return expandProductOfSumAndTerm(term)
}

/*
Calculates a common denominator for all the terms in the sum.  Then replaces the sum
by an equivalent term whose denominator is the common denominator. The common
denominator is made by multiplying the terms in the denominators together.  If a term
appears in more than one denominator it only needs to appear once in the common denominator.
But if it appears say twice in a particular denominator then it needs to appear twice in
the common denominator. Exapmple  a/xy + b/xef + c/zeef  common denominator = xyefze x and f
only need to be included once, but e must be included twice.  The new term would be
(aefze + byze + cxy)/xyefze
 */
fun commonDenominator(sum: Sum): Expr {
    val commonDenominator = arrayListOf<Expr>()
    val copyOfCommonDenominator = arrayListOf<Expr>()
    val allTerms = arrayListOf<Expr>()

    allTerms.addAll(sum.plusTerms)
    allTerms.addAll(sum.minusTerms)


    /*
    For each term in the sum add the terms in its denominator to the common denominator.
    To make sure a denominator term is only added once, make a copy of the common denominator each
    time through the loop.  Check to see if the denominator term in is in the copy before adding it
    to the common denominator.  But if it is already in the copy, then remove it from the
    copy.  Then if the denominator term comes up again in the same sum term it will get added.
     */
    for (term in allTerms){

        copyOfCommonDenominator.clear()
        copyOfCommonDenominator.addAll(commonDenominator)

        if ( term is Term){
            for (dTerm in (term).denominators) {
                if (copyOfCommonDenominator.containsExpr(dTerm)) {
                    copyOfCommonDenominator.remove(dTerm)
                } else {
                    commonDenominator.add(dTerm)
                }
            }
        }
    }



    // Calculate new numerators for each term in the original sum based on the common denominator. Place
    // them all in a new sum.
    var newSum:Expr  = Sum()
    for (term in sum.plusTerms) {
        newSum = newSum.add(convertExpressionNumeratorToCommonDenominator(term, commonDenominator))
    }
    for (term in sum.minusTerms){
        newSum = newSum.subtract(convertExpressionNumeratorToCommonDenominator(term, commonDenominator))
    }

    // Return a term that is the new sum divided by the common denominator.
    val term = Term()
    term.numerators.add(newSum)
    term.denominators.addAll(commonDenominator)

    return term
}

/*
    Remove the token from each term in the sum
    Example: factor a from (am + asm + ax) yields (m + sm + x)
 */
fun factorSum(token: Token, sum: Sum): Expr  {

    val plusTerms = sum.plusTerms
    val minusTerms = sum.minusTerms
    val newPlusTerms = arrayListOf<Expr>()
    val newMinusTerms = arrayListOf<Expr>()

    // remove the token from the terms in the source list, and put the
    // new terms in the dest list.
    fun removeToken(source: ArrayList<Expr>, dest: ArrayList<Expr>) {
        for (term in source){
            if (term is Term){
                if (term.numerators.contains(token)){
                    dest.add(term.removeToken(token))

                } else {
                    throw AlgebraException("Attempt to factor token out to a term that doesn't contain the token." +
                            "  token = ${token.toAnnotatedString()}  term = ${sum.toAnnotatedString()}")
                }
            } else {
                throw AlgebraException("Attempt to factor token = ${token.toAnnotatedString()} out of a sum containing the term = ${term.toAnnotatedString()}")
            }
        }
    }

    // remove the token from the plus and minus terms.
    removeToken(plusTerms, newPlusTerms)
    removeToken(minusTerms, newMinusTerms)

    val newSum = Sum()
    newSum.plusTerms.addAll(newPlusTerms)
    newSum.minusTerms.addAll(newMinusTerms)
    return newSum
}

fun factor  (token: Token, expr: Expr): Expr {

    if (expr is Token)throw AlgebraException ("Error: Attempt to factor a single token = ${token.toAnnotatedString()}")

    if (expr is Term) {
        if (expr.numerators.contains(token)) {
            val e = expr.removeToken(token)
            return e
        } else {
            if (expr.numerators.size == 1 && expr.numerators[0] is Sum){
                // Single sum in numerator
                val fact = factorSum(token, expr.numerators[0] as Sum)
                val term = Term()
                term.numerators.add(fact)
                term.denominators.addAll(expr.denominators)
                return term
            }
        }
    }

    if (expr is Sum) {
        return factorSum(token, expr)
    }
     throw AlgebraException("Error don't know how to factor token = ${token.toAnnotatedString()} out of expression = ${expr.toAnnotatedString()}")

}

/*
    The state token represents the sate variable in the term, a
    source variable or displacement on a capacitor, a momentum on
    and inertia or the derivative of a displacement or momentum.
    Everything else in the term is coefficient of the state
    variable.  So this function looks at term and figures out
    which token is the state variablke.
 */
fun getStateToken(term: Term): Token {
    for (expr in term.numerators){
        if (expr is Token && (expr.energyVar || expr.powerVar)) {
            return expr
            }
    }
    throw AlgebraException("Error: getKeyToken called on term with no power or energy variable = ${term.toAnnotatedString()}")
}

fun replaceToken(token: Token, newToken: Token, expr: Expr): Expr{
    val numerators = arrayListOf<Expr>()
    val denominators = arrayListOf<Expr>()

    if (expr is Token){
        if (token === expr) {
            return newToken
        } else {
            return expr
        }
    }

    if (expr is Sum) {
        return expr
    }

    numerators.addAll((expr as Term).numerators)
    denominators.addAll(expr.denominators)

    if (numerators.contains(token)){
        numerators.remove(token)
        numerators.add(newToken)
    }

    if (denominators.contains(token)){
        denominators.remove(token)
        denominators.add(newToken)
    }

    val term = Term()
    term.numerators.addAll(numerators)
    term.denominators.addAll(denominators)

    return term
}

fun replaceTokens(equation: Equation, replacementMap: Map<Token, Token>): Equation{

    var token: Token
    val leftSidePlusTerms = arrayListOf<Expr>()
    val rightSidePlusTerms = arrayListOf<Expr>()
    val leftSideMinusTerms = arrayListOf<Expr>()
    val rightSideMinusTerms = arrayListOf<Expr>()
    val newLeftSidePlusTerms = arrayListOf<Expr>()
    val newRightSidePlusTerms = arrayListOf<Expr>()
    val newLeftSideMinusTerms = arrayListOf<Expr>()
    val newRightSideMinusTerms = arrayListOf<Expr>()

    fun createSum(expr: Expr, plusTerms: ArrayList<Expr>, minusTerms: ArrayList<Expr>, newPlusTerms: ArrayList<Expr>) {

        when (expr) {

            is Token -> {
                if (expr in replacementMap.keys) {
                    newPlusTerms.add(replacementMap[expr]!!)
                } else {
                    newPlusTerms.add(expr)
                }
            }

            is Term -> {
                plusTerms.add(expr)
            }

            is Sum -> {
                plusTerms.addAll(expr.plusTerms)
                minusTerms.addAll(expr.minusTerms)
            }
        }
    }

    fun checkTerm(term: Term): Expr{
        val token = getStateToken(term)
        if (token in replacementMap.keys) {
            return replaceToken(token, replacementMap[token]!!, term)
        }

        return term
    }

    createSum(equation.leftSide, leftSidePlusTerms, leftSideMinusTerms, newLeftSidePlusTerms)
    createSum(equation.rightSide, rightSidePlusTerms, rightSideMinusTerms, newRightSidePlusTerms)

    leftSidePlusTerms.forEach { newLeftSidePlusTerms.add(checkTerm(it as Term)) }
    leftSideMinusTerms.forEach { newLeftSideMinusTerms.add(checkTerm(it as Term)) }
    rightSidePlusTerms.forEach { newRightSidePlusTerms.add(checkTerm(it as Term)) }
    rightSideMinusTerms.forEach { newRightSideMinusTerms.add(checkTerm(it as Term)) }

    var leftSide: Expr
    var rightSide: Expr

    if (newLeftSidePlusTerms.size == 1 && newLeftSideMinusTerms.size == 0) {
        leftSide = newLeftSidePlusTerms[0]
    } else {
        val sum = Sum()
        sum.plusTerms.addAll(newLeftSidePlusTerms)
        sum.minusTerms.addAll(newLeftSideMinusTerms)
        leftSide = sum
    }
    if (newRightSidePlusTerms.size == 1 && newRightSideMinusTerms.size == 0) {
        rightSide = newRightSidePlusTerms[0]
    } else {
        val sum = Sum()
        sum.plusTerms.addAll(newRightSidePlusTerms)
        sum.minusTerms.addAll(newRightSideMinusTerms)
        rightSide = sum
    }
     return Equation(leftSide, rightSide)
}

/*
    Each term in the sum is basically a coefficient times a state variable.  There may be
    several terms for any given state variable.  This function creates a new sum where there
    is just one term for each state variable.  This involves finding all the terms for a
    particular state variable, calculating a common denominator for the terms and then
    creating new numerators based on the common denominator.
    Example  p1R1/R2 + q7R3/(R1 + R2) + p1(R5)/R3 - q7R4)/R6 becomes
             (P1R1R3 + P1R5R2)/R2R3 + (q7R3R6 -q7R4(R1 = R2))/R6(R1 + R2)
    This function does not factor out the state variable.  This is done in a separate step.
 */
fun gatherLikeTerms(sum: Sum):Expr {
   val termsMap = mutableMapOf<Token,Expr>()

    // Add/subtract each term in the array list to the appropriate sum in
    // in the termsMap. If the isPlusTerm flag is true then add the
    // term to the sum otherwise subtract it.  See the comment below.
    fun groupTerms(source: ArrayList<Expr>, isPlusTerm: Boolean) {
        //println("group terms terms= "); source.forEach { println(it.toAnnotatedString()) }
        for (term in source){
            val token = getStateToken(term as Term)
            if (termsMap.containsKey(token)) {
                //key already exist to add/subtract this term from the sum
                var expr = termsMap[token]
                if (expr != null) {
                    expr = if (isPlusTerm) expr.add(term) else expr.subtract(term)
                    termsMap[token] = expr
                }
            } else {
                // new key so create new entry.
                termsMap[token] = if (isPlusTerm) term else Sum().subtract(term)
            }
        }
    }
    // First create a map where the keys are the state tokens, and the
    // values are a sum of all the terms that that state variable occurs
    // in. Example key p  value (pR3/R2 + pR5/R3 )
    groupTerms(sum.plusTerms, true)
    groupTerms(sum.minusTerms, false)

    // Create the new sum.
    var localSum = Sum()
    termsMap.values.forEach {
        if (it is Term) {
            // just one term for this state variable so just add it to the sum.
            localSum = localSum.add(it) as Sum
        } else {
            if (it is Sum && it.plusTerms.size + it.minusTerms.size > 1) {
                // sum with several terms.  Must create a new term with a common denominator
                localSum = localSum.add(commonDenominator(it)) as Sum
            } else {
                // This is a sum with a single negative term.
                localSum = localSum.add(it) as Sum
            }
        }
    }
    return localSum
}

/*
    This function can solve a specific form of an algebraic equation for the variable represented by token.
    The form is as follows:
    1. The left side is a single term containing the token as one of the factors, usually just the token.
    2. The right side has one or more terms containing the token and at least one other term or constant.
    3. The token can not appear in the denominator of any term.
    4. The token cannot be a parameter of a function, such as log, sin or exp.
    5. The token can't appear in the product of two sums i.e. (x + a)(x + b) which is of course the same
       as appearing in an exp function since there would ba an x squared.
    This function follows the brute force method of solving such an equation.
    1. Add/subtract terms containing the token so that they eliminated on the right side of the equation
       and appear on the left side.
    2. Calculate a common denominator for the terms on the left side.
    3. Calculate a sum based on the numerators of the left side and the common denominator creating a new fraction
       with this sum divided by the common denominator.  See comments in commonDenominator function.
    4. Factor the token out of the sum in the numerator of this fraction.
    5. Divide both sides of the equation by this fraction, leaving just the token on the left.
 */
fun solve (token: Token, equation: Equation): Equation {

    println("Solve")
    var leftSide = equation.leftSide
    var rightSide = equation.rightSide

    if (equation.rightSide is Term){
        if ((equation.rightSide as Term).numerators.contains(token) || (equation.rightSide as Term).denominators.contains(token)) {
            throw(AlgebraException("Error: Can't solve equation for ${token.toAnnotatedString()} because there is just one "
                    + "term on each side of the equation and ${token.toAnnotatedString()} appears in both of them."
                    +"\nEquation is ${equation.toAnnotatedString()}"))
        }
        return equation
    }

    if (isTokenInDenominator(token, leftSide) || isTokenInDenominator(token, rightSide)) throw AlgebraException("Error: The token we are solving for occurs in the denominator of one of the terms.  " +
            "These algebra routines can't solve this")
        if (rightSide is Sum) {
            val plusTerms = rightSide.plusTerms
            val minusTerms = rightSide.minusTerms
            val matchingPlusTerms = plusTerms.filter { contains(token, it) }
            val matchingMinusTerms = minusTerms.filter{ contains(token, it) }

            if (matchingPlusTerms.size + matchingMinusTerms.size == 0){
                return equation
            }

            // Subtract plus terms from both sides of equation.
            matchingPlusTerms.forEach {
                leftSide = leftSide.subtract(it)
                plusTerms.remove(it)
            }

            // Add minus terms to both sides of equation
            matchingMinusTerms.forEach {
                leftSide = leftSide.add(it)
                minusTerms.remove(it)
            }

            println("leftSIde is Term = ${leftSide is Term}  leftSide is Sum = ${leftSide is Sum}")

            var commonFraction: Expr

            if (leftSide is Sum) {

                // Calculate common denominator for left side, and create
                // single term on left side.  See comments in commonDenominator function.
                commonFraction = commonDenominator(leftSide as Sum)

                // Factor token out of the numerator on the fraction
                val factored = factor(token, commonFraction)

                // Divide the right side by the fraction.  We don't bother dividing the left side
                // since we know only the token will be left.
                rightSide = rightSide.divide(factored)

                // Return new equation with token on the left side and the new right side.
                return Equation(token, rightSide)

            }


        }


    throw AlgebraException("Unknown error solving equation ${equation.toAnnotatedString()} for ${token.toAnnotatedString()}")
}

fun solveSimultaneousEquations(equations: Map<Element, Equation>): LinkedHashMap<Element, Equation> {

    val solvedEquations = linkedMapOf<Element, Equation>()

    if (equations.size == 1) {
        equations.forEach{(key, value) -> solvedEquations[key] = solve((value.leftSide as Token), value)}
        return solvedEquations
    }
    /*
    TODO    implement solving set of simultaneous equations.
     */
    equations.forEach { (key, value) -> solvedEquations[key] = Equation(Term(), Term()) }
    return solvedEquations
}