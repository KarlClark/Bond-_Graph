package algebra

import bondgraph.AlgebraException


fun cancel(term: Term){
    var numerators = arrayListOf<Expr>()

    numerators.addAll(term.numerators)
    numerators.forEach {
        if (it in term.numerators && it in term.denomintors) {
            term.numerators.remove(it)
            term.denomintors.remove(it)
        }
    }
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

fun putNumeratorsOverCommonDemoninator(expressions:List<Expr>, commonDenominator: List<Expr>): ArrayList<Expr>  {

    var copyOfCommonDenominator = arrayListOf<Expr>()
    var numerators = arrayListOf<Expr>()

    for (expr in expressions) {

        println("expr = ${expr.toAnnotatedString()}")
        var numerator = arrayListOf<Expr>()

        if (expr is Token){
            println("expr is a Token")
            numerator.add(expr)
        } else {
            numerator.addAll((expr as Term).numerators)
        }

        val t = Term(); t.numerators.addAll(numerator); println("numerator = ${t.toAnnotatedString()}")

        copyOfCommonDenominator.clear()
        copyOfCommonDenominator.addAll(commonDenominator)

        if (expr is Token) {
            numerator.addAll(commonDenominator)
            val t = Term(); t.numerators.addAll(numerator); println("expr is token so add all commonDenominator numerator - ${t.toAnnotatedString()}")
        } else {
            for (dTerm in (expr as Term).denomintors) {
                println("dTerm = ${dTerm.toAnnotatedString()}")
                if ((expr as Term).denomintors.contains(dTerm)) {
                    println("denominator contains dTerm")
                    copyOfCommonDenominator.remove(dTerm)
                }
            }
            numerator.addAll(copyOfCommonDenominator)
            val t = Term(); t.numerators.addAll(numerator);println("numerator = ${t.toAnnotatedString()}")
        }
        val newTerm = Term()
        newTerm.numerators.addAll(numerator)
        numerators.add(newTerm)
    }

    return numerators
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
            for (dTerm in (term as Term).denomintors) {
                if (copyOfCommonDenominator.contains(dTerm)) {
                    copyOfCommonDenominator.remove(dTerm)
                } else {
                    commonDenominator.add(dTerm)
                }
            }
        }
    }


    val term= Term(); term.numerators.addAll(commonDenominator); println("commonDenominator is ${term.toAnnotatedString()}")

    plusNumerators = putNumeratorsOverCommonDemoninator(sum.plusTerms, commonDenominator)
    minusNumerators = putNumeratorsOverCommonDemoninator(sum.minusTerms, commonDenominator)


    val newSum = Sum()
    newSum.plusTerms.addAll(plusNumerators)
    newSum.minusTerms.addAll(minusNumerators)

    val newTerm = Term()
    newTerm.numerators.add(newSum)
    newTerm.denomintors.addAll(commonDenominator)

    return newTerm
}



fun factorSum(token: Token, sum: Sum): Expr  {

    val plusTerms = sum.plusTerms
    val minusTerms = sum.minusTerms

    for (term in plusTerms) {
        if (term is Term){
            if (term.numerators.contains(token)){
                term.numerators.remove(token)
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
                term.numerators.remove(token)
            } else {
                throw AlgebraException("Attempt to factor token out to a term that doesn't contain the token." +
                        "  token = ${token.toAnnotatedString()}  term = ${sum.toAnnotatedString()}")
            }
        } else {
            throw AlgebraException("Attempt to factor token = ${token.toAnnotatedString()} out of a sum containing the term = ${term.toAnnotatedString()}")
        }
    }

    return sum
}

fun factor  (token: Token, expr: Expr): Expr {

    if (expr is Token)throw AlgebraException ("Error: Attempt to facto a single token = ${token.toAnnotatedString()}")

    if (expr is Term) {
        if (expr.numerators.contains(token)) {
            expr.numerators.remove(token)
            return expr
        } else {
            if (expr.numerators.size == 1 && expr.numerators[0] is Sum){
                val fact = factorSum(token, expr.numerators[0] as Sum)
                val term = Term()
                term.denomintors.addAll(expr.denomintors)
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
            if (leftSide is Sum) {

                //termsList.addAll((leftSide as Sum).plusTerms)
                //termsList.addAll((leftSide as Sum).minusTerms)
                commonFraction = commonDemoninator(leftSide as Sum)
                println("commonFraction = ${commonFraction.toAnnotatedString()}")
                val factored = factor(token, commonFraction)
                println("factored = ${factored.toAnnotatedString()} ,  leftSide = ${leftSide.toAnnotatedString()}")
                rightSide = rightSide.divide(factored)
                println("${token.toAnnotatedString()} = ${rightSide.toAnnotatedString()}")
                return Equation(token, rightSide)

            }


        }


    throw AlgebraException("Unknown error solving equation ${equation.toAnnotatedString()} for ${token.toAnnotatedString()}")
}