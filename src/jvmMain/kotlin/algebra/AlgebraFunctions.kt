package algebra

import bondgraph.AlgebraException
import kotlinx.serialization.descriptors.listSerialDescriptor

fun cancelled (expr: Expr, exprList: MutableList<Expr>): Boolean {
    if ( ! (expr is Token) ) return false
    val index = exprList.indexOf(expr)
    print("cancelled expr = ${expr.toAnnotatedString()}")
    exprList.forEach { print("  ${it.toAnnotatedString()}") }
    println("  index = $index ")
    if (index >= 0){
        exprList.removeAt(index)
        return true
    }
    return false
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

fun commonDemoninator(terms: ArrayList<Expr>): Expr {
    var denominators: ArrayList<ArrayList<Expr>> = arrayListOf()
    var numerators: ArrayList<ArrayList<Expr>> = arrayListOf()

    for (index in terms.indices){
        println("processing term $index = ${terms[index].toAnnotatedString()}")
        numerators.add( arrayListOf())
        denominators.add (arrayListOf())
        if (terms[index] is Token) {
            numerators[index] = arrayListOf(terms[index])
        } else {
            numerators[index].addAll((terms[index] as Term).numerators)
            denominators[index].addAll((terms[index] as Term).denomintors)
        }
    }

    var commonDenominator = arrayListOf<Expr>()
    for (index in denominators.indices){
        commonDenominator.addAll(denominators[index])
    }

    for (numeratorIndex in numerators.indices) {
        println("numeratorIndex = $numeratorIndex")
        numerators[numeratorIndex].forEach { println("${it.toAnnotatedString()}") }
        for (denominatorIndex in denominators.indices) {
            println("denominatorIndex = $denominatorIndex")
            denominators[denominatorIndex].forEach { println("${it.toAnnotatedString()}") }
            if (numeratorIndex != denominatorIndex) {
                numerators[numeratorIndex].addAll(denominators[denominatorIndex])
            }
        }
    }
    val dTerm = Term()
    dTerm.denomintors.addAll(commonDenominator)
    val sum = Sum()
    numerators.forEach {
        val term = Term()
        term.numerators.addAll(it)
        sum.add(term)
    }
    dTerm.numerators.add(sum)
    return dTerm
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

fun facctor  (token: Token, expr: Expr): Expr {

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

                termsList.addAll((leftSide as Sum).plusTerms)
                termsList.addAll((leftSide as Sum).minusTerms)
                commonFraction = commonDemoninator(termsList)
                println("commonFraction = ${commonFraction.toAnnotatedString()}")
                val factored = facctor(token, commonFraction)
                println("factored = ${factored.toAnnotatedString()} ,  leftSide = ${leftSide.toAnnotatedString()}")
                rightSide = rightSide.divide(factored)
                println("${token.toAnnotatedString()} = ${rightSide.toAnnotatedString()}")
                return Equation(token, rightSide)

            }


        }


    throw AlgebraException("Unknown error solving equation ${equation.toAnnotatedString()} for ${token.toAnnotatedString()}")
}