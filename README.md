# truffled-purescript

[Truffle](https://wiki.openjdk.java.net/display/Graal/Truffle+FAQ+and+Guidelines) is an API for writing AST interpreters on the JVM. When combined with [Graal](https://wiki.openjdk.java.net/display/Graal/Main), Truffle can partially evaluate and specialise AST nodes to generate efficient machine code.

[PureScript](http://www.purescript.org/) is a small strongly typed programming language that compiles to JavaScript.

This project combines both together so that we can run PureScript on the JVM.

## Implementation

### purescript-corefn-gen

Takes PureScript (since 0.7) code, compiles to the intermediate *CoreFn* representation and dumps it out as JSON.

### purescript-truffle-eval

Takes the *CoreFn* JSON, constructs Truffle nodes and evaluates the root node.
