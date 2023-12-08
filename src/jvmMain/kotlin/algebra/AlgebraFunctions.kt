package algebra

import bondgraph.AlgebraException
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

    println("cancel ${term.toAnnotatedString()}")

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

    println("cancel numerators")
    numerators.forEach { println(" ${it.toAnnotatedString()}  ${it::class.simpleName}" ) }
    println("cancel denominators")
    denominators.forEach { println(" ${it.toAnnotatedString()}  ${it::class.simpleName}" ) }

    /*
    Iterate over copyOfNumerators because you can't modify a list you are iterating over. For every
    expression in numerators see if it exists in deonminators. If so delete it from both lists. Use
    our own .equals() functions because different objects may be equal as far as we are concerned
    i.e. (a + b) would equal (b + a).  Make sure to delete the correct object from the appropriate list.
     */
    copyOfNumerators.forEach {expr ->
        val denominator = denominators.find{d -> expr.equals(d)}
        println("numerator = ${expr.toAnnotatedString()}")
        println("denominator = ${denominator?.toAnnotatedString() ?: null}")
        if (denominator != null) {
            numerators.remove(expr)
            denominators.remove(denominator)
        }
    }

    // Create a new Term from the left over numerators and denominators.
    val newTerm = Term()
    newTerm.numerators.addAll(numerators)
    newTerm.denominators.addAll(denominators)
    println ("returning term = ${term.toAnnotatedString()}")
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

    if (equation.rightSide is Token){
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
    if (equation.rightSide is Token){
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

    println("checkIfHangingSum  source is ------------------------")
    source.forEach { println(it.toAnnotatedString()) }

    // Check every expression in the list to see if it is a hanging sum
    for (expr in source) {
        println("processing expr = ${expr.toAnnotatedString()} ${expr::class.simpleName}")
        if (expr is Sum) {
            when {
                expr.plusTerms.size == 1 && expr.minusTerms.size == 0 -> {
                    // positive hanging sum
                    println("hanging plus sum adding ${expr.plusTerms[0].toAnnotatedString()} to dest")
                    dest.add(expr.plusTerms[0])
                }
                expr.plusTerms.size == 0 && expr.minusTerms.size == 1 -> {
                    // negative hanging sum
                    println("hanging minus sum adding ${expr.minusTerms[0].toAnnotatedString()} to dest")
                    dest.add(expr.minusTerms[0])
                    localIsPlusTerm = ! localIsPlusTerm
                }
                else -> {
                    // normal sum
                    println("normal term adding ${expr.toAnnotatedString()} to dest")
                    dest.add(expr)
                }
            }
        } else {
            // an expression that is not a sum
            println("not a sum adding {expr.toAnnotatedString()} to dest"  )
            dest.add(expr)
        }
    }
    println("localPlusTerm = ${localIsPlusTerm}")
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
    println("checkForHangingSums expr = ${expr.toAnnotatedString()}")
    if (expr is Sum || expr is Token) {
        // no denominator, just one expression to add
        numerators.add(expr)
    } else {
        numerators.addAll((expr as Term).numerators)
        denominators.addAll(expr.denominators)
    }

    // Resolve hanging sums in both list keeping track of the sign of the expression.
    isPlusTerm = resloveHangingSums(numerators, newNumerators, true)
    isPlusTerm = resloveHangingSums(denominators,newDenominators, isPlusTerm)

    println("checkForHangingSums numerators")
    newNumerators.forEach { println(it.toAnnotatedString()) }
    println("checkForHangingSums denominators")
    newDenominators.forEach { println(it.toAnnotatedString()) }

    // Create a new term from the new resolved numerators and denominators and add it to
    // either the newPlusTerms list or the new minusTermsList.
    val term = Term()
    term.numerators.addAll(newNumerators)
    term.denominators.addAll(newDenominators)
    if (isPlusTerm) {
        println("adding ${term.toAnnotatedString()} to newPlusTerms")
        newPlusTerms.add(term)
    } else {
        println("adding ${term.toAnnotatedString()} to newMinusTerms")
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

    println("simplefy sum ${expr.toAnnotatedString()}")

    // before we can simplify we must expand the Sum.
    if (expr is Term) {
        println("simplify sum expr is a Term")
        if (expr.numerators.size == 1 && expr.numerators[0] is Sum) {
            sum = expandSum(expr.numerators[0] as Sum)
        } else {
            // We may want to extend this function to handle Term x Sum
            // i.e ab(a + b  + c -a) could be ab(b + c) But hanging sums
            // would complicate this.
            return expr
        }
    } else {
        println("simplify sum expr is a Sum")
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
                 println("here 4")
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

    println("Expand staring expression = ${expr.toAnnotatedString()}")

    if (expr is Token || expr is Sum){
        // nothing to expand
        return expr
    }



    // break expr apart into a list of Terms and a list o Sums.
    for (e in (expr as Term).numerators){
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
        println("expand final expression = ${term.toAnnotatedString()}")
        return term
    }
    println("expand final expression = ${sum.toAnnotatedString()}")
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
/*
    println("convert to common denominator")
    commonDenominator.forEach {

        println("${it.toAnnotatedString()} ${it::class.simpleName}")
        if (it is Sum) {
            for (term in (it as Sum).plusTerms){
                println("${term.toAnnotatedString()}  ${term::class.simpleName}")
            }
            for (term in (it as Sum).minusTerms){
                println("${term.toAnnotatedString()}  ${term::class.simpleName}")
            }
        }
    }*/




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
        println("Calling contains ###########################################")
        if (copyOfCommonDenominator.contains(term)) {
            copyOfCommonDenominator.remove(term)
        }
        println("done calling contains $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
    }

    // Create the new numerator and return the expanded form of it.
    val term = Term()
    term.numerators.addAll(expr.numerators)
    term.numerators.addAll(copyOfCommonDenominator)

    return expandProductOfSumAndTerm(term)
}

fun commonDenominator(sum: Sum): Expr {
    val commonDenominator = arrayListOf<Expr>()
    val copyOfCommonDenominator = arrayListOf<Expr>()
    val allTerms = arrayListOf<Expr>()
    var plusNumerators = arrayListOf<Expr>()
    var minusNumerators = arrayListOf<Expr>()
    allTerms.addAll(sum.plusTerms)
    allTerms.addAll(sum.minusTerms)

    println("commonDenominator starting sum is ${sum.toAnnotatedString()}")
    for (term in allTerms){

        copyOfCommonDenominator.clear()
        copyOfCommonDenominator.addAll(commonDenominator)

        if ( term is Term){
            for (dTerm in (term as Term).denominators) {
                if (copyOfCommonDenominator.contains(dTerm)) {
                    copyOfCommonDenominator.remove(dTerm)
                } else {
                    commonDenominator.add(dTerm)
                }
            }
        }
    }


    val t= Term(); t.numerators.addAll(commonDenominator); println("commonDenominator is ${t.toAnnotatedString()}")

    val newSum = Sum()
    for (term in sum.plusTerms) {
        newSum.add(convertExpressionNumeratorToCommonDenominator(term, commonDenominator))
    }
    for (term in sum.minusTerms){
        newSum.minus(convertExpressionNumeratorToCommonDenominator(term, commonDenominator))
    }

    val term = Term()
    term.numerators.add(newSum)
    term.denominators.addAll(commonDenominator)

    return term
}



fun factorSum(token: Token, sum: Sum): Expr  {

    val plusTerms = sum.plusTerms
    val minusTerms = sum.minusTerms
    val newPlusTerms = arrayListOf<Expr>()
    val newMinusTerms = arrayListOf<Expr>()


    for (term in plusTerms) {
        if (term is Term){
            if (term.numerators.contains(token)){
                newPlusTerms.add(term.removeToken(token))

            } else {
                throw AlgebraException("Attempt to factor token out to a term that doesn't contain the token." +
                        "  token = ${token.toAnnotatedString()}  term = ${sum.toAnnotatedString()}")
            }
        } else {
            throw AlgebraException("Attempt to factor token = ${token.toAnnotatedString()} out of a sum containing the term = ${term.toAnnotatedString()}")
        }
    }

    for (term in minusTerms) {
        if (term is Term){
            if (term.numerators.contains(token)){
                newMinusTerms.add(term.removeToken(token))
            } else {
                throw AlgebraException("Attempt to factor token out to a term that doesn't contain the token." +
                        "  token = ${token.toAnnotatedString()}  term = ${sum.toAnnotatedString()}")
            }
        } else {
            throw AlgebraException("Attempt to factor token = ${token.toAnnotatedString()} out of a sum containing the term = ${term.toAnnotatedString()}")
        }
    }

    val newSum = Sum()
    newSum.plusTerms.addAll(newPlusTerms)
    newSum.minusTerms.addAll(newMinusTerms)
    return newSum
}

fun factor  (token: Token, expr: Expr): Expr {

    if (expr is Token)throw AlgebraException ("Error: Attempt to facto a single token = ${token.toAnnotatedString()}")

    if (expr is Term) {
        if (expr.numerators.contains(token)) {
            val e =expr.removeToken(token)
            return e
        } else {
            if (expr.numerators.size == 1 && expr.numerators[0] is Sum){
                val fact = factorSum(token, expr.numerators[0] as Sum)
                val term = Term()
                term.denominators.addAll(expr.denominators)
                term.numerators.add(fact)
                return term
            }
        }
    }

    if (expr is Sum) {
        return factorSum(token, expr)
    }
     throw AlgebraException("Error don't know how to factor token = ${token.toAnnotatedString()} out of expression = ${expr.toAnnotatedString()}")

}

fun getKeyToken(term: Term): Token {
    for (expr in term.numerators){
        if (expr is Token && (expr.energyVar || expr.powerVar)) {
            return expr
            }
    }
    throw AlgebraException("Error: getKeyToken called on term with to power or energy variable = ${term.toAnnotatedString()}")
}

fun gatherLikeTerms(sum: Sum):Expr {
   val termsMap = mutableMapOf<Token,Expr>()

    for (term in sum.plusTerms ) {
        val token = getKeyToken(term as Term)
        if (termsMap.containsKey(token)) {
            var expr = termsMap[token]
            if (expr != null) {
                expr = expr.add(term)
                termsMap[token] = expr
            }
        } else {
            termsMap[token] = term
        }
    }

    for (term in sum.minusTerms){
        val token = getKeyToken(term as Term)
        if (termsMap.containsKey(token)) {
            var expr = termsMap[token]
            if (expr != null) {
                expr = expr.minus(term)
                termsMap[token] = expr
            }
        } else {
            termsMap[token] = Sum().minus(term)
        }
    }

    var localSum = Sum()
    termsMap.values.forEach {
        if (it is Term) {
            localSum = localSum.add(it) as Sum
        } else {
            if (it is Sum && it.plusTerms.size + it.minusTerms.size > 1) {
                localSum = localSum.add(commonDenominator(it)) as Sum
            } else {
                localSum = localSum.add(it) as Sum
            }
        }
    }
    return localSum
}

fun solve (token: Token, equation: Equation): Equation {

    println("Solve")
    var leftSide = equation.leftSide
    var rightSide = equation.rightSide

    println("rightSide = ${rightSide.toAnnotatedString()}")
    if (isTokenInDenominator(token, leftSide) || isTokenInDenominator(token, rightSide)) throw AlgebraException("Error: The token we are solving for occurs in the denominator of one of the terms.  " +
            "These algebra routines can't solve this")
    println("rightSide = ${rightSide.toAnnotatedString()}")


        /*if (rightSide is Term) {

            println("right side is a term = ${rightSide.toAnnotatedString()}")

            val numeratorTokensList = rightSide.getNumeratorTokens()
            val denominatorTokensList = rightSide.getDenominatorTokens()
            if (numeratorTokensList.contains(token) || denominatorTokensList.contains(token)) throw AlgebraException("Error: right side of equation is a single term containing the variable to solve for." +
                    "  We can't solve this.\n${leftSide.toAnnotatedString()} = ${rightSide.toAnnotatedString()}")

            numeratorTokensList.forEach {
                leftSide = leftSide.divide(it)
                rightSide = rightSide.divide(it)
            }
            println("denominatorTokensList.size = ${denominatorTokensList.size}")
            denominatorTokensList.forEach{println(it.toAnnotatedString())}
            denominatorTokensList.forEach {
                println("multiply both side by ${it.toAnnotatedString()}")
                leftSide = leftSide.multiply(it)
                rightSide = rightSide.multiply(it)
            }
            continue
        }*/

        if (rightSide is Sum) {
            val plusTerms = rightSide.plusTerms
            val minusTerms = rightSide.minusTerms
            //val matchingPlusterms = arrayListOf<Term>()
            //val matchingMinusTerms = arrayListOf<Term>()
            val matchingPlusTerms = plusTerms.filter { contains(token, it) }
            val matchingMinusTerms = minusTerms.filter{ contains(token, it) }

            matchingPlusTerms.forEach {
                leftSide = leftSide.minus(it)
                plusTerms.remove(it)
            }

            matchingMinusTerms.forEach {
                leftSide = leftSide.add(it)
                minusTerms.remove(it)
            }

            println("leftSIde is Term = ${leftSide is Term}  leftSide is Sum = ${leftSide is Sum}")
            val termsList = arrayListOf<Expr>()
            var commonFraction: Expr = Term()

            println ("Solve-------  leftSide")
            /*(leftSide as Sum).plusTerms.forEach {v ->
                println("${v.toAnnotatedString()}  ${v::class.simpleName}")
            }*/

            if (leftSide is Sum) {

                //termsList.addAll((leftSide as Sum).plusTerms)
                //termsList.addAll((leftSide as Sum).minusTerms)
                commonFraction = commonDenominator(leftSide as Sum)
                println("commonFactor = ${commonFraction.toAnnotatedString()}")

                (commonFraction as Term).numerators.forEach {
                    println("${it.toAnnotatedString()} ${it::class.simpleName}")
                    if (it is Sum){
                        (it as Sum).plusTerms.forEach{
                            v -> println("${v.toAnnotatedString()}  ${v::class.simpleName}")
                            if (v is Term) {
                                v.numerators.forEach { v2 -> println("${v2.toAnnotatedString()}  ${v2::class.simpleName}") }
                            }
                        }
                    }
                }
                println("commonFraction = ${commonFraction.toAnnotatedString()}")
                val factored = factor(token, commonFraction)
                println("factored = ${factored.toAnnotatedString()} ,  leftSide = ${leftSide.toAnnotatedString()}")

                (factored as Term).numerators.forEach {
                    if (it is Sum) {
                        it.plusTerms.forEach { v1 ->
                            println("${v1.toAnnotatedString()}  ${v1::class.simpleName}")
                            if (v1 is Term) {
                                v1.numerators.forEach { v2 ->
                                    println("${v2.toAnnotatedString()}  ${v2::class.simpleName}")
                                }
                            }
                        }
                    }
                }


                rightSide = rightSide.divide(factored)
                println("${token.toAnnotatedString()} = ${rightSide.toAnnotatedString()}")
                return Equation(token, rightSide)

            }


        }


    throw AlgebraException("Unknown error solving equation ${equation.toAnnotatedString()} for ${token.toAnnotatedString()}")
}