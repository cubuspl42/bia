# bia

Bia is a strict, purely functional programming language (at least as pure as a strict language can be). Currently it's in extremely early developement, but already has functions and a capability for expressing algebraic data types (through objects and tagged unions). All that with a fully-working type system, which supports higher-order types (also known as _generics_).

In the long term, Bia will be a polygon for experiments with a programmer-friendly support for programming with applicatives/monads (think Haskell's `do`, but better) and an alternative approach for bridging purely functional bits with the stateful and frightening "outside world" (an alternative to Haskell's all-or-nothing `Io` monad, based on fundamental principles of Functional Reactive Programming).

Currently, Bia is implemented in Kotlin and provides a simple all-in-one parser-compiler-interpreter solution capable of executing Bia code in-memory.
