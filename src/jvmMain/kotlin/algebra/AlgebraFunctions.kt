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

    if (rightSide is Term){
        val tokensList = rightSide.getNumeratorTokens()
        if (tokensList.contains(token)) throw AlgebraException("Error: right side of equation is a single term containing the variable to solve for.  We can't solve this.\n${leftSide.toAnnotatedString()} = ${rightSide.toAnnotatedString()}")
        tokensList.forEach{
            println("divide leftSide = ${leftSide.toAnnotatedString()} by ${it.toAnnotatedString()}")
            leftSide = leftSide.divide(it)
            println("divide rightSide = ${rightSide.toAnnotatedString()} by ${it.toAnnotatedString()}")
            rightSide = rightSide.divide(it)

            println("solve leftside = ${leftSide.toAnnotatedString()}  rightSide = ${rightSide.toAnnotatedString()}")
        }
    }

    return Equation(leftSide, rightSide)
}