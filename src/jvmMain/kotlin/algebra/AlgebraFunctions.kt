package algebra

import bondgraph.AlgebraException

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

fun solve (token: Token, equation: Equation): Equation {

    println("Solve")
    var leftSide = equation.leftSide
    var rightSide = equation.rightSide
    var done = false
    while (! done) {
        if (rightSide is Term) {

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
        }

        done = true
        println("done = $done")
    }

    return Equation(leftSide, rightSide)
}