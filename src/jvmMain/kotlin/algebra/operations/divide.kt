package algebra.operations

import algebra.*
import algebra.Number

fun divide (expr1: Expr, expr2: Expr): Expr {
    println("divide expr1 = ${expr1.toAnnotatedString()}: ${expr1::class.simpleName}  expr2 = ${expr2.toAnnotatedString()}: ${expr2::class.simpleName}")
    when (expr1) {

        is Token -> when (expr2) {
            is Token -> return divide (expr1, expr2)
            is Number -> return divide (expr1, expr2)
            is Term -> return divide (expr1, expr2)
            is Sum -> return divide (expr1, expr2)
        }

        is Number -> when (expr2) {
            is Token -> return divide (expr1, expr2)
            is Number -> return divide (expr1, expr2)
            is Term -> return divide (expr1, expr2)
            is Sum -> return divide (expr1, expr2)
        }

        is Term -> when (expr2) {
            is Token -> return divide (expr1, expr2)
            is Number -> return divide (expr1, expr2)
            is Term -> return divide (expr1, expr2)
            is Sum -> return divide (expr1, expr2)
        }

        is Sum -> when (expr2) {
            is Token -> return divide (expr1, expr2)
            is Number -> return divide (expr1, expr2)
            is Term -> return divide (expr1, expr2)
            is Sum -> return divide (expr1, expr2)
        }
    }

    return Token("ERROR Divide")
}

// ***********************  Token   ****************************************************

fun divide (token1: Token, token2: Token): Expr {
    val term = Term()

    if (token1.equals(token2)) {
        return Number(1.0)
    }

    term.numerators.add(token1)
    term.denominators.add(token2)
    return term
}

fun divide (token: Token, number: Number): Expr {

    if (number.value == 1.0) {
        return token
    }

    val term = Term()
    term.numerators.add(token)
    term.denominators.add(number)
    return term
}

fun divide (token: Token, term: Term): Expr {
    val expr = reduce(term)

    if (expr !is Term){
        return multiply(token, expr)
    }

    val newTerm = Term()
    newTerm.numerators.add(token)
    newTerm.numerators.addAll(expr.denominators)
    newTerm.denominators.addAll(expr.numerators)
    return rationalizeTerm(newTerm)
}

fun divide(token: Token, sum: Sum): Expr {

    println("divide(token, sum) token= ${token.name}  sum= ${sum.toAnnotatedString()}")

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        throw IllegalArgumentException("Divide by zero in divide(Token, Sum)  sum = ${sum.toAnnotatedString()} ")
    }
    val comDemExpr = commonDenominator(sum)
    val term = Term()
    term.numerators.add(token)
    if (comDemExpr is Term) {
        term.numerators.addAll(comDemExpr.denominators)
        term.denominators.addAll(comDemExpr.numerators)
    } else {
        term.denominators.add(comDemExpr)
    }
    return rationalizeTerm(term)
}
// ***********************  Number   ****************************************************

fun divide(number: Number, token: Token): Expr {
    val term = Term()
    term.numerators.add(number)
    term.denominators.add(token)
    return term
}

fun divide (number1: Number, number2: Number): Expr {

    return Number(number1.value / number2.value)
}

fun divide(number: Number, term: Term): Expr {

    val expr = reduce(term)

    if (expr !is Term) {
        return divide(number, expr)
    }

    val newTerm = Term()
    newTerm.numerators.add(number)
    newTerm.numerators.addAll(term.denominators)
    newTerm.denominators.addAll(term.numerators)
    return rationalizeTerm(newTerm)
}

fun divide (number: Number, sum: Sum): Expr {
    if (sum.plusTerms.size + sum.minusTerms.size == 0) {
        throw IllegalArgumentException("Divide by zero. sum passed to divide(Number, Sum) is zero")
    }

    val comDemExpr = commonDenominator(sum)
    val term = Term()
    term.numerators.add(number)
    if (comDemExpr is Term) {
        term.numerators.addAll(comDemExpr.denominators)
        term.denominators.addAll(comDemExpr.numerators)
    } else {
        term.denominators.add(comDemExpr)
    }

    return rationalizeTerm(term)
}

// ***********************  Term   ****************************************************

fun divide (term: Term, token: Token): Expr {
    var expr = reduce (term)
    if (expr !is Term) {
        return divide (expr, token)
    }

    val newTerm = Term()
    newTerm.numerators.addAll(expr.numerators)
    newTerm.denominators.addAll(expr.denominators)
    newTerm.denominators.add(token)
    return rationalizeTerm(newTerm)
}

fun divide (term: Term, number: Number): Expr {

    if (number.value == 1.0) {
        return term
    }

    var expr = reduce (term)
    if (expr !is Term) {
        return divide (expr, number)
    }

    val newTerm = Term()
    newTerm.numerators.addAll(expr.numerators)
    newTerm.denominators.addAll(expr.denominators)
    newTerm.denominators.add(number)
    return rationalizeTerm(newTerm)
}

fun divide (term1: Term, term2: Term): Expr {
    val expr1 = reduce(term1)
    val expr2 = reduce(term2)

    if (term1.equals(term2)) {
        return Number(1.0)
    }

    if (expr1 !is Term || expr2 !is Term){
        return divide(expr1, expr2)
    }

    val newTerm = Term()
    newTerm.numerators.addAll(expr1.numerators)
    newTerm.numerators.addAll(expr2.denominators)
    newTerm.denominators.addAll(expr1.denominators)
    newTerm.denominators.addAll(expr2.numerators)

    return rationalizeTerm(newTerm)
}

fun divide (term: Term, sum: Sum): Expr {
    val expr = reduce(term)

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        throw IllegalArgumentException("Divide by zero, Sum = ${sum.toAnnotatedString()}")
    }

    if (expr !is Term){
        return divide(expr, sum)
    }

    val comDenomExpr = commonDenominator(sum)
    val newTerm = Term()
    newTerm.numerators.addAll(expr.numerators)
    newTerm.denominators.addAll(expr.denominators)
    if (comDenomExpr is Term){
        newTerm.numerators.addAll(comDenomExpr.denominators)
        newTerm.denominators.addAll(comDenomExpr.numerators)
    } else {
        newTerm.denominators.add(comDenomExpr)
    }

    return rationalizeTerm(newTerm)
}
// ***********************  Sum   ****************************************************

fun divide (sum: Sum, token: Token): Expr {
    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return Number(0.0)
    }

    val newSum = Sum()

    sum.plusTerms.forEach {
        newSum.plusTerms.add(divide(it, token))
    }
    sum.minusTerms.forEach {
        newSum.minusTerms.add(divide(it, token))
    }

    return combineTerms(newSum)
}

fun divide (sum: Sum, number: Number): Expr {

    if (number.value == 1.0) {
        return sum
    }

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return Number(0.0)
    }

    val newSum = Sum()

    sum.plusTerms.forEach {
        newSum.plusTerms.add(divide(it, number))
    }
    sum.minusTerms.forEach {
        newSum.minusTerms.add(divide(it, number))
    }

    return combineTerms(newSum)
}

fun divide(sum: Sum, term: Term): Expr {
    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return Number(0.0)
    }

    val newSum = Sum()

    sum.plusTerms.forEach {
        newSum.plusTerms.add(divide(it, term))
    }
    sum.minusTerms.forEach {
        newSum.minusTerms.add(divide(it, term))
    }

    return combineTerms(newSum)

}

fun divide(sum1: Sum, sum2: Sum): Expr {

    if (sum2.plusTerms.size + sum2.minusTerms.size == 0){
        throw IllegalArgumentException("Divide by zero, sum2 = ${sum2.toAnnotatedString()}")
    }

    if (sum1.plusTerms.size + sum1.minusTerms.size == 0){
        return Number(0.0)
    }

    if (sum1.equals(sum2)) {
        return Number(1.0)
    }

    if (sum1.equals(negate(sum2))) {
        val sum = Sum()
        sum.minusTerms.add(Number(1.0))
        return sum
    }

    val comDenomExpr = commonDenominator(sum2)
    val newSum = Sum()

    sum1.plusTerms.forEach {
        newSum.plusTerms.add(divide(it, comDenomExpr))
    }

    sum1.minusTerms.forEach {
        newSum.minusTerms.add(divide(it, comDenomExpr))
    }

    return combineTerms(newSum)
}