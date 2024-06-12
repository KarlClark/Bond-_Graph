package algebra.operations


import algebra.*
import algebra.Number
import kotlin.math.absoluteValue

fun subtract(expr1: Expr, expr2: Expr): Expr {
    println("subtract expr1 = ${expr1.toAnnotatedString()}: ${expr1::class.simpleName}  expr2 = ${expr2.toAnnotatedString()}: ${expr2::class.simpleName}")
    when (expr1) {

        is Token -> when (expr2) {
            is Token -> return subtract (expr1, expr2)
            is Number -> return subtract (expr1, expr2)
            is Term -> return subtract (expr1, expr2)
            is Sum -> return subtract (expr1, expr2)
        }

        is Number -> when (expr2) {
            is Token -> return subtract (expr1, expr2)
            is Number -> return subtract (expr1, expr2)
            is Term -> return subtract (expr1, expr2)
            is Sum -> return subtract (expr1, expr2)
        }

        is Term -> when (expr2) {
            is Token -> return subtract (expr1, expr2)
            is Number -> return subtract (expr1, expr2)
            is Term -> return subtract (expr1, expr2)
            is Sum -> return subtract (expr1, expr2)
        }

        is Sum -> when (expr2) {
            is Token -> return subtract (expr1, expr2)
            is Number -> return subtract (expr1, expr2)
            is Term -> return subtract (expr1, expr2)
            is Sum -> return subtract (expr1, expr2)
        }
    }

    return Token("ERROR Subtract")
}

// ***********************  Token   ****************************************************

fun subtract(token: Token, expr: Expr): Expr {
    when (expr) {
        is Token -> return subtract(token, expr)
        is Number -> return subtract(token, expr)
        is Term -> return subtract(token, expr)
        is Sum -> return subtract(token, expr)
    }
    return Token("ERROR")
}

fun subtract (token1: Token, token2: Token): Expr {
    val sum = Sum()
    sum.plusTerms.add(token1)
    sum.minusTerms.add(token2)
    return sum
}

fun subtract (token: Token, number: Number): Expr {
    val sum = Sum()
    sum.plusTerms.add(token)
    sum.minusTerms.add(number)
    return sum
}

fun subtract (token: Token, term: Term): Expr{
    val sum = Sum()

    val expr = reduce(term)

    if (expr !is Term) {
        return subtract(token, expr)
    }

    sum.plusTerms.add(token)
    sum.minusTerms.add(expr)
    return sum
}

fun subtract (token: Token, sum: Sum): Expr {

    println ("subtract(Token, Sum token = ${token.toAnnotatedString()}, sum = ${sum.toAnnotatedString()}")
    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return token
    }

    val newSum = Sum()
    newSum.minusTerms.addAll(sum.plusTerms)
    newSum.plusTerms.addAll(sum.minusTerms)
    newSum.plusTerms.add(token)
    return combineTerms(newSum)
}

// ***********************  Number   ****************************************************
fun subtract (number: Number, expr: Expr): Expr {
    //println("add number, expr expr = ${expr.toAnnotatedString()}: ${expr::class.simpleName}")
    when (expr){
        is Token -> return subtract (number, expr)
        is Number -> return subtract (number, expr)
        is Term -> return subtract (number, expr)
        is Sum -> return subtract (number, expr)
    }
    return Token("ERROR")
}

fun subtract (number: Number, token: Token):Expr {
    val sum = Sum()
    sum.plusTerms.add(number)
    sum.minusTerms.add(token)
    return sum
}
fun subtract (number1: Number, number2: Number): Expr {
    val num = number1.value - number2.value
    if (num >= 0) {
        return Number(num)
    } else {
        val sum = Sum()
        sum.minusTerms.add(Number(num.absoluteValue))
        return sum
    }
}

fun subtract (number: Number, term: Term): Expr {
    val sum = Sum()

    val expr = reduce(term)

    if (expr !is Term) {
        return subtract(number, expr)
    }

    sum.plusTerms.add(number)
    sum.minusTerms.add(expr)
    return sum
}

fun subtract(number: Number, sum: Sum): Expr {

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return number
    }

    val newSum = Sum()
    newSum.minusTerms.addAll(sum.plusTerms)
    newSum.plusTerms.addAll(sum.minusTerms)
    newSum.plusTerms.add(number)
    return combineTerms(newSum)
}

// ***********************  Term   ****************************************************

fun subtract(term: Term, token: Token): Expr {
    val expr = reduce(term)

    if (expr !is Term) {
        return subtract(expr, token)
    }

    val sum = Sum()
    sum.plusTerms.add(expr)
    sum.minusTerms.add(token)
    return sum
}

fun subtract(term: Term, number: Number): Expr {
    val expr = reduce(term)

    if (expr !is Term) {
        return subtract(expr, number)
    }

    val sum = Sum()
    sum.plusTerms.add(expr)
    sum.minusTerms.add(number)
    return sum
}

fun subtract(term1: Term, term2: Term): Expr {
    val expr1 = reduce(term1)
    val expr2 = reduce(term2)

    if (expr1 !is Term || expr2 !is Term) {
        return subtract(expr1, expr2)
    }

    val sum = Sum()
    sum.plusTerms.add(expr1)
    sum.minusTerms.add(expr2)
    return combineTerms(sum)
}

fun subtract(term: Term, sum: Sum): Expr {

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return term
    }

    val expr = reduce(term)
    if (term !is Term){
        return subtract(expr, sum)
    }

    val newSum = Sum()
    newSum.minusTerms.addAll(sum.plusTerms)
    newSum.plusTerms.addAll(sum.minusTerms)
    newSum.plusTerms.add(expr)
    return combineTerms(newSum)
}
// ***********************  Sum   ****************************************************

fun subtract(sum: Sum, token: Token): Expr {
    val newSum = Sum()
    newSum.plusTerms.addAll(sum.plusTerms)
    newSum.minusTerms.addAll(sum.minusTerms)
    newSum.minusTerms.add(token)
    return combineTerms(newSum)
}

fun subtract(sum: Sum, number: Number): Expr {
    val newSum = Sum()
    newSum.plusTerms.addAll(sum.plusTerms)
    newSum.minusTerms.addAll(sum.minusTerms)
    newSum.minusTerms.add(number)
    return combineTerms(newSum)
}

fun subtract(sum: Sum, term: Term): Expr {
    val expr = reduce(term)

    if (expr !is Term){
        return subtract(sum, expr)
    }

    val newSum = Sum()
    newSum.plusTerms.addAll(sum.plusTerms)
    newSum.minusTerms.addAll(sum.minusTerms)
    newSum.minusTerms.add(expr)
    return combineTerms(newSum)
}

fun subtract (sum1: Sum, sum2: Sum): Expr {
    val sum =  Sum()
    sum.plusTerms.addAll(sum1.plusTerms)
    sum.minusTerms.addAll(sum1.minusTerms)
    sum.plusTerms.addAll(sum2.minusTerms)
    sum.minusTerms.addAll(sum2.plusTerms)
    return combineTerms(sum)
}
