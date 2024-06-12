package algebra.operations

import algebra.*
import algebra.Number

fun multiply (expr1: Expr, expr2: Expr): Expr {
    println("multiply expr1 = ${expr1.toAnnotatedString()}: ${expr1::class.simpleName}  expr2 = ${expr2.toAnnotatedString()}: ${expr2::class.simpleName}")
    when (expr1) {

        is Token -> when (expr2) {
            is Token -> return multiply (expr1, expr2)
            is Number -> return multiply (expr1, expr2)
            is Term -> return multiply (expr1, expr2)
            is Sum -> return multiply (expr1, expr2)
        }

        is Number -> when (expr2) {
            is Token -> return multiply (expr1, expr2)
            is Number -> return multiply (expr1, expr2)
            is Term -> return multiply (expr1, expr2)
            is Sum -> return multiply (expr1, expr2)
        }

        is Term -> when (expr2) {
            is Token -> return multiply (expr1, expr2)
            is Number -> return multiply (expr1, expr2)
            is Term -> return multiply (expr1, expr2)
            is Sum -> return multiply (expr1, expr2)
        }

        is Sum -> when (expr2) {
            is Token -> return multiply (expr1, expr2)
            is Number -> return multiply (expr1, expr2)
            is Term -> return multiply (expr1, expr2)
            is Sum -> return multiply  (expr1, expr2)
        }
    }

    return Token("ERROR Multiply")
}

fun multiplyList(expr: Expr, exprList:ArrayList<Expr>): ArrayList<Expr> {

    if (expr is Sum)throw IllegalArgumentException("function multiplyList can't handle a Sum as the first argument. Argumetment was ${expr.toAnnotatedString()}")

    val newList = arrayListOf<Expr>()

    exprList.forEach {
        val term = Term()
        if (it is Term){
            term.numerators.addAll(it.numerators)
            term.denominators.addAll(it.denominators)
        } else {
            term.numerators.add(it)
        }
        if (expr is Term){
            term.numerators.addAll(expr.numerators)
            term.denominators.addAll(expr.denominators)
        } else {
            term.numerators.add(expr)
        }

        println("calling rationalizeTerm on ${term.toAnnotatedString()}")
        newList.add(rationalizeTerm(term))
    }

    return newList
}
// ***********************  Token   ****************************************************

fun multiply(token1: Token, token2: Token ): Expr {
    val term = Term()
    term.numerators.add(token1)
    term.numerators.add(token2)
    return term
}

fun multiply(token: Token, number: Number ): Expr {
    val term = Term()

    if (number.value == 1.0) {
        return token
    }
    term.numerators.add(token)
    term.numerators.add(number)
    return term
}

fun multiply(token: Token, term: Term): Expr {
    val expr = reduce(term)

    if (expr !is Term){
        return multiply(token, expr)
    }

    val newTerm = Term()

    newTerm.numerators.add(token)
    newTerm.numerators.addAll(expr.numerators)
    newTerm.denominators.addAll(expr.denominators)
    return rationalizeTerm(newTerm)
}

fun multiply(token: Token, sum: Sum): Expr {
    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return Number(0.0)
    }

    val newSum = Sum()

    newSum.plusTerms.addAll(multiplyList(token, sum.plusTerms))
    newSum.minusTerms.addAll(multiplyList(token, sum.minusTerms))
    return combineTerms(newSum)
}
// ***********************  Number   ****************************************************

fun multiply(number: Number, token: Token ): Expr {
   return multiply(token, number)
}

fun multiply(number1: Number, number2: Number ): Expr {
    return Number(number1.value * number2.value)
}

fun multiply(number: Number, term: Term): Expr {

    if (number.value == 1.0) {
        return term
    }

    val expr = reduce(term)

    if (expr !is Term){
        return multiply(number, expr)
    }

    val newTerm = Term()

    newTerm.numerators.add(number)
    newTerm.numerators.addAll(expr.numerators)
    newTerm.denominators.addAll(expr.denominators)
    return rationalizeTerm(newTerm)
}

fun multiply(number: Number, sum: Sum): Expr {

    if (number.value ==  1.0) {
        return sum
    }

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return Number(0.0)
    }

    val newSum = Sum()

    newSum.plusTerms.addAll(multiplyList(number, sum.plusTerms))
    newSum.minusTerms.addAll(multiplyList(number, sum.minusTerms))
    return combineTerms(newSum)
}

// ***********************  Term   ****************************************************

fun multiply (term: Term, token: Token): Expr{
    return multiply(token, term)
}

fun multiply(term: Term, number: Number): Expr {
    return multiply(number, term)
}

fun multiply(term1: Term, term2: Term): Expr{
    val expr1 = reduce(term1)
    val expr2 = reduce(term2)

    if (expr1 !is Term || expr2 !is Term){
        return multiply(expr1, expr2)
    }

    val term = Term()
    term.numerators.addAll(expr1.numerators)
    term.numerators.addAll(expr2.numerators)
    term.denominators.addAll(expr1.denominators)
    term.denominators.addAll(expr2.denominators)

    return rationalizeTerm(term)
}

fun multiply(term: Term, sum: Sum): Expr {
    val expr = reduce(term)

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return Number(0.0)
    }

    if (expr !is Term){
        return multiply(expr, sum)
    }

    val newSum = Sum()

    sum.plusTerms.forEach { newSum.plusTerms.add(multiply(expr, it)) }
    sum.minusTerms.forEach { newSum.minusTerms.add(multiply(expr, it))}

    return combineTerms(newSum)
}


// ***********************  Sum   ****************************************************

fun multiply(sum: Sum, token: Token): Expr {
    return multiply(token, sum)
}

fun multiply(sum: Sum, number: Number): Expr{
    return multiply(number, sum)
}

fun multiply(sum: Sum, term: Term): Expr {
    return multiply(term, sum)
}

fun multiply(sum1: Sum, sum2: Sum): Expr {

    if (sum1.plusTerms.size + sum1.minusTerms.size == 0){
        return Number(0.0)
    }

    if (sum2.plusTerms.size + sum2.minusTerms.size == 0){
        return Number(0.0)
    }

    val term = Term()
    term.numerators.add(sum1)
    term.numerators.add(sum2)
    return rationalizeTerm(term)
}