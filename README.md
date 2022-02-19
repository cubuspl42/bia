# bia

Bia is a strict, purely functional programming language (at least as pure as a strict language can be). Currently it's in extremely early developement, but already has functions and a capability for expressing algebraic data types (through objects and tagged unions). All that with a fully-working type system, which supports higher-order types (also known as _generics_).

In the long term, Bia will be a polygon for experiments with a programmer-friendly support for programming with applicatives/monads (think Haskell's `do`, but better) and an alternative approach for bridging purely functional bits with the stateful and frightening "outside world" (an alternative to Haskell's all-or-nothing `Io` monad, based on fundamental principles of Functional Reactive Programming).

Currently, Bia is implemented in Kotlin and provides a simple all-in-one parser-compiler-interpreter solution capable of executing Bia code in-memory.

## Sample program

Please note that, as the language is in _extremely_ active developement, the program you read might not compile or correctly run on the tip of the `main` branch.

```
// A palindromic number reads the same both ways.
// The largest palindrome made from the product of two 2-digit numbers is 9009 = 91 × 99.

// Find the largest palindrome made from the product of two 3-digit numbers.

val threeDigitNumbers = to(100, 999)

def multiply(a : Number, b : Number) {
    return a * b
}

val candidates = product:L2(
   threeDigitNumbers,
   threeDigitNumbers,
   multiply
)

def extractDigits(n : Number) {
    return if (n < 10) then listOf(n)
    else (concat:L(extractDigits(n ~/ 10), listOf(n % 10)))
}

def isListPalindrome(l : List<Number>): Boolean {
    def recurse() {
        val fst = first:L(l)
        val lst = last:L(l)

        return if (fst == lst) then isListPalindrome(dropLast:L(drop:L(l, 1), 1))
        else false
    }

    return if (size:L(l) < 2) then true
    else (recurse())
}

def isNumberPalindrome(n : Number) {
    return isListPalindrome(extractDigits(n))
}

val palindromes = filter(candidates, isNumberPalindrome)

val largestPalindrome = max:L(palindromes)

return largestPalindrome
```
