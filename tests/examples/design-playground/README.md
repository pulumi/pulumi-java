# API Design

## Required Args

Should we fail at compile time when the user fails to provide required
args to a resource or function?

No, as there are no good solutions for non-breaking changes, see:

https://github.com/pulumi/pulumi-java/issues/134
https://github.com/pulumi/pulumi-java/issues/206

## Builders

Should we use builders for ResourceArgs?

We played with a few alternatives without builders to try to save on
generated code size, but leaning to keep using them:

- Although we do not have any class invariants on ReourceArgs, we do
  have one: we can check that all required properties were supplied.
  Doing so in the `build()` method of the builder feels like a good
  place.

- AWS CDK uses Builders

- Likely idiomatic Java

- Decided to use short (not set-prefixed) names for builders,
  `FooArgs.Builder().bar(1).build()`, not `setBar(1)`

Should we have builders for output types? We currently do. It is less
clear. Perhaps useful for mock testing. Skipping conserves code size.

## Input

We should remove `Input<T>` and leave `Output<T>`. TBD - expand the
section here.

## Output

### Catching Exceptions

Should we let user catch exceptions that happen in `o.apply? Other
SDKs do not. TODO elaborate.

### Awaiting Apply

Should `o.apply(x -> System.out.println(x))` have the program wait
until the print effect finishes like in other SDKs?

### Uncaught Exceptions

Should uncaught exceptions from `o.apply(x -> throw new
MyException("!"))` lead to the early termination of the program like
in other SDKs?

### Executor

Currently default global Executor is used for all `Outputs`. Should we
let the user pick a dedicated executor?

### Context

To track Executor, uncaught exceptions, and outstanding effects, we
need context. Currently Deployment and related classes use global
state for this context. How can propagate Context explicitly or
otherwise, allowing more than one instance of a stack operation to
proceed simultaneously within the same JVM? TODO elaborate options.


## Function Calls

TODO elaborate.
