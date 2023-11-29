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

fun solve (token: Token, equation: Equation): Equation {

    println("Solve")
    var leftSide = equation.leftSide
    var rightSide = equation.rightSide

    println("rightSide = ${rightSide.toAnnotatedString()}")
    if (isTokenInDemoninator(token, leftSide) || isTokenInDemoninator(token, rightSide)) throw AlgebraException("Error: The token we are solving for occurs in the denominator of one of the terms.  " +
            "These algebra routines can't solve this")
    println("rightSide = ${rightSide.toAnnotatedString()}")

    var done = false
    while (! done) {
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
            if (leftSide is Sum) {

                termsList.addAll((leftSide as Sum).plusTerms)
                termsList.addAll((leftSide as Sum).minusTerms)
                val commonFracton = commonDemoninator(termsList)
                println("commonFraction = ${commonFracton.toAnnotatedString()}")
            }
        }

        done = true
        println("done = $done")
    }

    return Equation(leftSide, rightSide)
}