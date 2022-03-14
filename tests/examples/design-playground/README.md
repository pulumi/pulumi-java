# API Design

## Review examples

https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/minimal/app/src/main/java/minimal/MyStack.java
https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/random/app/src/main/java/random/MyStack.java
https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/aws-java-webserver/app/src/main/java/webserver/MyStack.java
https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/aws-native-java-s3-folder/app/src/main/java/s3site/MyStack.java
https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/azure-java-appservice-sql/app/src/main/java/appservice/MyStack.java
https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/azure-java-static-website/app/src/main/java/staticwebsite/MyStack.java
https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/gcp-java-gke-hello-world/app/src/main/java/gcpgke/MyStack.java

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

### Lambda Buidlers

Could add overloads accepting `Function<XBuilder,XBuilder>` where `X`
was expected, resolving to `f.apply(new XBuilder()).build()`.

Before:

    ServiceArgs.builder()
        .setMetadata(ObjectMetaArgs.builder()
            .clusterName("clusterName")
            .build())

After:

    ServiceArgs.builder()
        .setMetadata($ -> $.clusterName("clusterName"))

### Accept Builder to avoid build() call

Could add overloads to methods accepting `Foo` to also accept
`FooBuilder`, avoiding the `build()` method call at the end.

Before:

    ServiceArgs.builder()
        .setMetadata(ObjectMetaArgs.builder()
            .clusterName("clusterName")
            .build())

After:

    ServiceArgs.builder()
        .setMetadata(ObjectMetaArgs.builder()
            .clusterName("clusterName"))

The gain is somewhat redundant if Lambda Builders are accepted.

### Use varargs where List is expected

Before:

    ServiceSpecArgs.builder()
        .setPorts(List.of(ServicePortArgs.builder()
            .setPort(80)
            .setTargetPort(Either.ofRight("http"))
            .build()))
        .build()

After:

    ServiceSpecArgs.builder()
        .setPorts(ServicePortArgs.builder()
            .setPort(80)
            .setTargetPort(Either.ofRight("http"))
            .build())
        .build()

### Accept String overloads where string-y enum is expected

    ServicePortArgs.builder()
        .setPort(80)
        .setTargetPort("http")
        .build()

## Input

We should remove `Input<T>` and leave `Output<T>`. TBD - expand the
section here.

To lift a value do `Output.of()`.

    int x = 1;
    Output<Integer> y = Output.of(x);

https://github.com/pulumi/pulumi-java/issues/28

This also removes the need from `toOutput` conversions, it "just
works":

https://github.com/pulumi/pulumi-java/issues/139

## Output

### Combinator Naming


Cribbing names from CompletableFuture vs using names more like Pulumi.
https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html

We cannot call everything `apply()` because generic erasure considers
these variations "same" and does not compile on Java.

https://github.com/pulumi/pulumi-java/issues/142

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

## Optionals

https://github.com/pulumi/pulumi-java/issues/25

Should we use `java.lang.Optional` in codegen / resource providers or
should we use null?

We currently use it in a few places in providers:

- Output types:

    GetAnalyzerResult result;
    Optional<String> arn = result.getArn();

- Input args types but not builders:

    CertificateAuthorityCrlConfiguration config:
    Optional<Integer> expirationDays = config.getExpirationInDays();

- Config

    Optional<string> accessKey = config.accessKey();

We do not use Optional in buiders.

CDK does not use it: in input nor output positions.

Situations with `Optional<Optional<X>>` or `List<Optional<X>>` do not
arise in our providers.

Optional also has a quirk: `Optional.of(null)` always throws.

## Unions

https://github.com/pulumi/pulumi-java/issues/29

The schema implies anonymous union types, which Java does not support.

Current situation is incomplete/incorrect and a bit anonying:

- Either<A,B> type is used for N=2 unions
- Object is used for N>2 unions

We should definitely consider:

- Builder overloads for every union element with auto-promotion

- How to rerpesent proeprties of the union type in args and result types?

  - Either<A,B> + extend that for N=3,4,5?
  - Use any existing libraries or stdlib types that do that?
  - Just use Object?

It would be interesting to find how CDK deals with these.

## Function Calls

TODO elaborate.


## Deployment APIs

https://github.com/pulumi/pulumi-java/issues/27

The Deployment object is the global state where everything related to
the current operation is accessible. This seems to be a C#-ism. In Go
we have `pulumi.Context`. TBD details here - can we call it Context,
can we reduce the number of classes.


## Stack API

TODO


## Resoure and Invoke Options

Make it easy to use default resource and invoke options e.g.:

https://github.com/pulumi/pulumi-java/issues/226


Are there builders for Resource options?

## Naming


### Rename annotations

    @OutputCustomType -> @CustomType
    @InputImport -> @Import
    @OutputExport -> @Export
