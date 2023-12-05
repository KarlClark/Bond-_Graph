package algebra

import bondgraph.AlgebraException
/*
A Term may be made up of other Terms.  For example a Term could consist of a Token and a another Term.
If the Token represents x and the other Term represents yz the whole Term represents xyz.  Most of
the time this is fine but causes a problem when we want to cancel like factors in the numerator and
denominator of a fraction (also a Term) If the numerator is made up of the above Term and the denominator
was say abz all made up of all Tokens, then we can't see the z in the numerator to cancel it. So this
function takes a Term (that is not a fraction) and expands it to an arraylist of Tokens and Sums.

Note: A Sum can also mask a single Token.  But we don't deal with that here since a Sum can also be
controlling the sign of the whole Term.  These Sums crop up when simplifying a Sum like (a + b -a).
After simplifying we are left with a Sum with just one Token b. The simplifySums function resolves
these to single Tokens.  But as this code evolves, such sums may crop up in other areas.
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
fun simplefySums(equation: Equation): Equation {

    val plusTerms = arrayListOf<Expr>()
    val minusTerms = arrayListOf<Expr>()
    val newPlusTerms = arrayListOf<Expr>()
    val newMinusTerms = arrayListOf<Expr>()

    if (equation.rightSide is Token){
        return equation
    }
    if (equation.rightSide is Term){
        plusTerms.add(equation.rightSide)
    } else {
        plusTerms.addAll((equation.rightSide as Sum).plusTerms)
        minusTerms.addAll((equation.rightSide as Sum).minusTerms)
    }

    for (term in plusTerms){
        checkForHangingSums(simplefySum(term), newPlusTerms, newMinusTerms)
        //newPlusTerms.add(simplefySum(term))
    }

    for (term in minusTerms){
        checkForHangingSums(simplefySum(term), newMinusTerms, newPlusTerms)
        //newMinusTerms.add(simplefySum(term))
    }

    val sum = Sum()
    sum.plusTerms.addAll(newPlusTerms)
    sum.minusTerms.addAll(newMinusTerms)
    return Equation(equation.leftSide, sum)
}
fun checkIfHangingSum(source: ArrayList<Expr>, dest: ArrayList<Expr>, isPlusTerm: Boolean): Boolean {

    var localIsPlusTerm = isPlusTerm

    println("checkIfHangingSum  source is ------------------------")
    source.forEach { println(it.toAnnotatedString()) }

    for (expr in source) {

        println("processing expr = ${expr.toAnnotatedString()} ${expr::class.simpleName}")
        if (expr is Sum) {
            when {
                expr.plusTerms.size == 1 && expr.minusTerms.size == 0 -> {
                    println("hanging plus sum adding ${expr.plusTerms[0].toAnnotatedString()} to dest")
                    dest.add(expr.plusTerms[0])
                }
                expr.plusTerms.size == 0 && expr.minusTerms.size == 1 -> {
                    println("hanging minus sum adding ${expr.minusTerms[0].toAnnotatedString()} to dest")
                    dest.add(expr.minusTerms[0])
                    localIsPlusTerm = ! localIsPlusTerm
                }
                else -> {
                    println("normal term adding ${expr.toAnnotatedString()} to dest")
                    dest.add(expr)
                }
            }
        } else {
            println("not a sum adding {expr.toAnnotatedString()} to dest"  )
            dest.add(expr)
        }
    }
    println("localPlusTerm = ${localIsPlusTerm}")
    return localIsPlusTerm
}
fun checkForHangingSums(expr: Expr, newPlusTerms: ArrayList<Expr>, newMinusTerms: ArrayList<Expr>) {

    val numerators = arrayListOf<Expr>()
    val denominators = arrayListOf<Expr>()
    val newNumerators = arrayListOf<Expr>()
    val newDenominators = arrayListOf<Expr>()
    val plusTerms = arrayListOf<Expr>()
    val minusTerms = arrayListOf<Expr>()
    var isPlusTerm: Boolean

    println("checkForHangingSums expr = ${expr.toAnnotatedString()}")
    if (expr is Sum || expr is Token) {
        numerators.add(expr)
    } else {
        numerators.addAll((expr as Term).numerators)
        denominators.addAll(expr.denominators)
    }
    isPlusTerm = checkIfHangingSum(numerators, newNumerators, true)
    isPlusTerm = checkIfHangingSum(denominators,newDenominators, isPlusTerm)

    println("checkForHangingSums numerators")
    newNumerators.forEach { println(it.toAnnotatedString()) }
    println("checkForHangingSums denominators")
    newDenominators.forEach { println(it.toAnnotatedString()) }
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


fun getPlusTerms(sum: Sum): ArrayList<Expr> {
    println("get plus terms ${sum.toAnnotatedString()}")
    val plusTerms = arrayListOf<Expr>()
    for (term in sum.plusTerms) {
        println("plus term is ${term.toAnnotatedString()} ")
        if (term is Sum) {
            plusTerms.addAll(getPlusTerms(term))
            println("term is Sum")
            plusTerms.forEach { println("${it.toAnnotatedString()}") }
        } else {
            plusTerms.add(term)
            println("term is Term")
            plusTerms.forEach { println("${it.toAnnotatedString()}") }
        }
    }

    return plusTerms
}

fun getMinusTerms(sum: Sum): ArrayList<Expr> {
    val minusTerms = arrayListOf<Expr>()
    println("get minus terms ${sum.toAnnotatedString()}")
    for (term in sum.minusTerms) {
        println("minus term is ${term.toAnnotatedString()} ")
        if (term is Sum) {
            minusTerms.addAll(getMinusTerms(term))
            println("term is Sum")
            minusTerms.forEach { println("${it.toAnnotatedString()}") }
        } else {
            minusTerms.add(term)
            println("term is Term")
            minusTerms.forEach { println("${it.toAnnotatedString()}") }
        }
    }

    return minusTerms
}
fun simplefySum(expr: Expr ): Expr {

    val plusTerms = arrayListOf<Expr>()
    val minusTerms = arrayListOf<Expr>()
    val copyOfPlusTerms = arrayListOf<Expr>()
    val copyOfMinusTerms = arrayListOf<Expr>()

    if (expr is Token) {
        return expr
    }

    println("simplefy sum ${expr.toAnnotatedString()}")

    if (expr is Term) {
        println("simplefy sum expr is a Term")
        if (expr.numerators.size == 1 && expr.numerators[0] is Sum) {
            //plusTerms .addAll( (expr.numerators[0] as Sum).plusTerms)
            plusTerms.addAll(getPlusTerms(expr.numerators[0] as Sum))
           // minusTerms.addAll((expr.numerators[0] as Sum).minusTerms)
            minusTerms.addAll(getMinusTerms(expr.numerators[0] as Sum))

        } else {
            return expr
        }
    } else {
        println("simplefy sum expr is a Sum")
        plusTerms.addAll(getPlusTerms(expr as Sum))
        minusTerms.addAll(getMinusTerms(expr))
    }
    println("simplefy sum plusTerms")
    plusTerms.forEach { println("${it.toAnnotatedString()}  ${it::class.simpleName}") }
    println("simplefy sum minusTerms")
    minusTerms.forEach { println("${it.toAnnotatedString()}  ${it::class.simpleName}") }

    if (plusTerms.size == 0  || minusTerms.size == 0){
        return expr
    }

    copyOfPlusTerms.addAll(plusTerms)

    for (e1 in copyOfPlusTerms){
        copyOfMinusTerms.clear()
        copyOfMinusTerms.addAll(minusTerms)
        for (e2 in copyOfMinusTerms){
            if (e1.equals(e2)) {
                plusTerms.remove(e1)
                minusTerms.remove(e2)
            }
        }
    }

    val sum = Sum()
    sum.plusTerms.addAll(plusTerms)
    sum.minusTerms.addAll(minusTerms)

    if (expr is Term) {
        val term = Term()
        term.numerators.add(sum)
        term.denominators.addAll(expr.denominators)
        return term
    }

    return sum
}
var count = 0
fun isTokenInDemoninator(token: Token, expr: Expr): Boolean {

    //if (count++ > 4) return false
    /*if (expr is Token) {
        println ("token = $token  expr = $expr")
        return token === expr
    }*/

    if (expr is Term) {
        val exprsList = expr.getDemominatorExpressions()

        for (index in exprsList.indices){
            println("Term ${expr.toAnnotatedString()} expr[$index] = ${exprsList[index].toAnnotatedString()}")
            if (exprsList[index] is Token && token === exprsList[index]) {
                println("here 1")
                return true
            }
            if ( isTokenInDemoninator(token, exprsList[index])) {
                println("here 2")
                return true
            }
        }
    }

     if (expr is Sum){
         val exprsList = expr.getAllExpressions()
         for (index in exprsList.indices) {
             println("Sum ${expr.toAnnotatedString()} expr[$index] = ${exprsList[index].toAnnotatedString()}")
             if (exprsList[index] is Token && token === exprsList[index]) {
                 println("here 3")
                 return true
             }
             if (isTokenInDemoninator(token, exprsList[index])) {
                 println("here 4")
                 return true
             }
         }
     }

    return false
}

fun contains(token: Token, expr: Expr): Boolean {
    return if (expr is Term) expr.getNumeratorTokens().contains(token) else false
}

fun expandSum(expr: Expr): Expr {

    val termList = arrayListOf<Expr>()
    val sumList = arrayListOf<Expr>()
    val plusNumerators = arrayListOf<Expr>()
    val minusNumerators = arrayListOf<Expr>()

    println("Expand staring expression = ${expr.toAnnotatedString()}")
    if (expr is Token || expr is Sum){
        return expr
    }

    for (e in (expr as Term).numerators){
        if (e is Token || e is Term) {
            termList.add(e)
        } else {
            sumList.add(e)
        }
    }

    if (sumList.size != 1) {
        return expr
    }

    for (e in(sumList[0] as Sum).plusTerms){
        if (e is Sum) {
            return expr
        }
        val term = Term()
        term.numerators.addAll(termList)
        term.numerators.add(e)
        plusNumerators.add(term)
    }

    for (e in(sumList[0] as Sum).minusTerms){
        if (e is Sum) {
            return expr
        }
        val term = Term()
        term.numerators.addAll(termList)
        term.numerators.add(e)
        minusNumerators.add(term)
    }

    val sum = Sum()
    sum.plusTerms.addAll(plusNumerators)
    sum.minusTerms.addAll(minusNumerators)

    if (expr is Term && expr.denominators.size > 0) {
        val term = Term()
        term.numerators.add(sum)
        term.denominators.addAll(expr.denominators)
        println("expand final expression = ${term.toAnnotatedString()}")
        return term
    }
    println("expand final expression = ${sum.toAnnotatedString()}")
    return sum
}

fun convertExpressionNumeratorToCommonDenominator(expr: Expr, commonDenominator: List<Expr>): Expr {

    val copyOfCommonDenominator = arrayListOf<Expr>()

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
    }




    if (expr is Token || expr is Sum) {

        val term = Term()
        term.numerators.add(expr)
        term.numerators.addAll(commonDenominator)
        return expandSum(term)
    }

    copyOfCommonDenominator.addAll(commonDenominator)
    for (term in (expr as Term).denominators) {
        if (copyOfCommonDenominator.contains(term)) {
            copyOfCommonDenominator.remove(term)
        }
    }

    val term = Term()
    term.numerators.addAll(expr.numerators)
    term.numerators.addAll(copyOfCommonDenominator)

    return expandSum(term)
}

fun commonDemoninator(sum: Sum): Expr {
    var commonDenominator = arrayListOf<Expr>()
    var copyOfCommonDenominator = arrayListOf<Expr>()
    var allTerms = arrayListOf<Expr>()
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
                localSum = localSum.add(commonDemoninator(it)) as Sum
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
    if (isTokenInDemoninator(token, leftSide) || isTokenInDemoninator(token, rightSide)) throw AlgebraException("Error: The token we are solving for occurs in the denominator of one of the terms.  " +
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
                commonFraction = commonDemoninator(leftSide as Sum)
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