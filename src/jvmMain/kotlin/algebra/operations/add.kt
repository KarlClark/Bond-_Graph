package algebra.operations

import algebra.*
import algebra.Number
import java.security.spec.ECField

fun add(expr1: Expr, expr2: Expr): Expr {
    println("add expr1 = ${expr1.toAnnotatedString()}: ${expr1::class.simpleName}  expr2 = ${expr2.toAnnotatedString()}: ${expr2::class.simpleName}")
    when (expr1) {

        is Token -> when (expr2) {
            is Token -> return add(expr1, expr2)
            is Number -> return add(expr1, expr2)
            is Term -> return add(expr1, expr2)
            is Sum -> return add(expr1, expr2)
        }

        is Number -> when (expr2) {
            is Token -> return add(expr1, expr2)
            is Number -> return add(expr1, expr2)
            is Term -> return add(expr1, expr2)
            is Sum -> return add(expr1, expr2)
        }

        is Term -> when (expr2) {
            is Token -> return add(expr1, expr2)
            is Number -> return add(expr1, expr2)
            is Term -> return add(expr1, expr2)
            is Sum -> return add(expr1, expr2)
        }

        is Sum -> when (expr2) {
            is Token -> return add(expr1, expr2)
            is Number -> return add(expr1, expr2)
            is Term -> return add(expr1, expr2)
            is Sum -> return add(expr1, expr2)
        }
    }

    return Token("ERROR Add")
}

// ***********************  Token   ****************************************************

fun add (token1: Token, token2: Token): Expr {
    val sum = Sum()
    sum.plusTerms.add(token1)
    sum.plusTerms.add(token2)
    return sum
}

fun add(token: Token, number: Number): Expr {
    val sum = Sum()
    sum.plusTerms.add(token)
    sum.plusTerms.add(number)
    return sum
}

fun add(token: Token, term: Term): Expr{
    val sum = Sum()

    val expr = reduce(term)

    if (expr !is Term) {
        return add(token, expr)
    }

    sum.plusTerms.add(token)
    sum.plusTerms.add(term)
    return sum
}

fun add(token: Token, sum: Sum): Expr {

    //println ("add(Token, Sum token = ${token.toAnnotatedString()}, sum = ${sum.toAnnotatedString()}")
    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return token
    }

    val newSum = Sum()
    newSum.plusTerms.addAll(sum.plusTerms)
    newSum.minusTerms.addAll(sum.minusTerms)
    newSum.plusTerms.add(token)
    return combineTerms(newSum)
}

// ***********************    Number   ****************************************************

fun add (number: Number, token: Token):Expr {
    return add(token, number)
}
fun add(number1: Number, number2: Number): Expr {
    return Number(number1.value + number2.value)
}

fun add (number: Number, term: Term): Expr {
    val sum = Sum()

    val expr = reduce(term)

    if (expr !is Term) {
        return add(number, expr)
    }

    sum.plusTerms.add(number)
    sum.plusTerms.add(term)
    return sum
}

fun add(number: Number, sum: Sum): Expr {

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return number
    }

    val newSum = Sum()
    newSum.plusTerms.addAll(sum.plusTerms)
    newSum.minusTerms.addAll(sum.minusTerms)
    newSum.plusTerms.add(number)
    return combineTerms(newSum)
}

// ***********************  Term   ****************************************************

fun add(term: Term, token: Token): Expr {
    return add(token, term)
}

fun add(term: Term, number: Number): Expr {
    return add(number, term)
}

fun add (term1: Term, term2: Term): Expr{
    val expr1 = reduce(term1)
    val expr2 = reduce(term2)

    if (expr1 !is Term || expr2 !is Term){
        return add(expr1, expr2)
    }

    val sum = Sum()
    sum.plusTerms.add(expr1)
    sum.plusTerms.add(expr2)
    return combineTerms(sum)
}

fun add (term: Term, sum: Sum): Expr {

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return term
    }

    val expr = reduce(term)

    if (expr !is Term) {
        return add (expr, sum)
    }

    val newSum = Sum()
    newSum.plusTerms.addAll(sum.plusTerms)
    newSum.minusTerms.addAll(sum.minusTerms)
    newSum.plusTerms.add(term)
    return combineTerms(newSum)
}

// ***********************  Sum   ****************************************************

fun add (sum: Sum, token: Token): Expr {
    return add(token, sum)
}

fun add (sum: Sum, number: Number): Expr {
    return add(number, sum)
}

fun add(sum: Sum, term: Term): Expr {
    return add (term, sum)
}

fun add (sum1: Sum, sum2: Sum): Expr {

    val sum = Sum()
    sum.plusTerms.addAll(sum1.plusTerms)
    sum.minusTerms.addAll(sum1.minusTerms)
    sum.plusTerms.addAll(sum2.plusTerms)
    sum.minusTerms.addAll(sum2.minusTerms)
    return combineTerms(sum)
}
