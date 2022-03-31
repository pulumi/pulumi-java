# API Design

## Examples Review

- [minimal](https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/minimal/app/src/main/java/minimal/MyStack.java)
- [random](https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/random/app/src/main/java/random/MyStack.java)
- [aws-java-webserver](https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/aws-java-webserver/app/src/main/java/webserver/MyStack.java)
- [aws-native-java-s3-folder](https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/aws-native-java-s3-folder/app/src/main/java/s3site/MyStack.java)
- [azure-java-appservice-sql](https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/azure-java-appservice-sql/app/src/main/java/appservice/MyStack.java)
- [azure-java-static-website](https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/azure-java-static-website/app/src/main/java/staticwebsite/MyStack.java)
- [gcp-java-hello-world](https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/gcp-java-gke-hello-world/app/src/main/java/gcpgke/MyStack.java)
- [eks-minimal](https://github.com/pulumi/pulumi-jvm/blob/main/tests/examples/eks-minimal/app/src/main/java/eksminimal/MyStack.java)


## Unroll Either

  https://github.com/pulumi/pulumi-jvm/issues/138

  ServicePortArgs.builder()
      .setPort(80)
      .setTargetPort("http")
      .build()


## Constructor Race Conditions

https://github.com/pulumi/pulumi-jvm/issues/314

There is a path to a non-breaking fix that would add a new protected
method invocation at the end of every resource constructor.


## Output

There are substantially pesky race conditions in Output chains that
get confused which Deployment object (key piece of global state) they
belong to. For now this is a test-only error. A test failure can be
mis-attributed or a test might hang. If we do not fix this, for
production use this simply limits the user to being able to execute
only one Pulumi operation at a time within the same JVM address space.

There is a prototype that switches to propagating Deployment object
directly:

    https://github.com/pulumi/pulumi-jvm/pull/305

Resource construction needs more args, like in Go. Unfortunate.

Most unfortunate is that Output combinators like `Output.all` need a
builder object `outputBuilder.all()` (we can tack this on to the
Context/deployment).

There may be a variation B where we do not take the hit on `Output`
combinators, but instead end up with a partial (not total) guarantee
that output chains do not get confused about the deployment. This is
again similar to Go, where Output may lose context if the code is not
careful.


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


## Function Calls

Streamlined syntax:
  https://github.com/pulumi/pulumi-jvm/issues/242

Before:
    io.pulumi.aws.ec2.GetAmi.invokeAsync(...)

After:
    ec2.getAmi(...)

Output versioned invokes:
  https://github.com/pulumi/pulumi-jvm/issues/163


## Deployment APIs

https://github.com/pulumi/pulumi-java/issues/27

The Deployment object is the global state where everything related to
the current operation is accessible. This seems to be a C#-ism. In Go
we have `pulumi.Context`. TBD details here - can we call it Context,
can we reduce the number of classes.


## Stack API

https://github.com/pulumi/pulumi-jvm/issues/243

Currently MyStack is a class extending Stack, and constructors need to
set `@`-annotated output properties. This follows C# closely.

We could consider Go-style desgin instead (or alongside?) where a
certain `Context` object is passed into a lambda and outptus are
defined against this object.


## Combinator Naming

Naming is relatively easy to push through with refactoring IDEs.

Cribbing names from CompletableFuture vs using names more like Pulumi.
https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html

We cannot call everything `apply()` because generic erasure considers
these variations "same" and does not compile on Java.

https://github.com/pulumi/pulumi-java/issues/142
