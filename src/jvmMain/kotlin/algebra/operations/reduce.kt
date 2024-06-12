package algebra.operations

import algebra.*

/*fun reduce (sum: Sum): Expr {

    if (sum.plusTerms.size + sum.minusTerms.size == 0){
        return Number(0.0)
    }

    if (sum.plusTerms.size == 1 && sum.minusTerms.size == 0){
        return sum.plusTerms[0]
    }
}*/

fun reduce (term: Term): Expr{

    if (term.numerators.size + term.denominators.size == 0){
        return Number(1.0)
    }

    if (term.numerators.size == 1 && term.denominators.size == 0){
        return term.numerators[0]
    }

    return term
}