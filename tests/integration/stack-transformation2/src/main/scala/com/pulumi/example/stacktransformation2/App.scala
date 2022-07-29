package com.pulumi.example.stacktransformation2

import com.pulumi.Context
import com.pulumi.Pulumi
import com.pulumi.core.Output
import com.pulumi.random.RandomString
import com.pulumi.random.RandomStringArgs
import com.pulumi.resources.ComponentResource
import com.pulumi.resources.ComponentResourceOptions
import com.pulumi.resources.CustomResourceOptions
import com.pulumi.resources.ResourceTransformation
import com.pulumi.resources.StackOptions
import javax.annotation.Nullable
import java.util.Optional
import java.util.concurrent.CompletableFuture

object App {
  private val RandomStringType = "random:index/randomString:RandomString"

  def main(args: Array[String]): Unit = Pulumi
    .withOptions(StackOptions.builder.resourceTransformations(App.scenario3(_)).build)
    .run(App.stack)

  private def stack(ctx: Context): Unit = {
    // Scenario #1 - apply a transformation to a CustomResource
    val res1 = new RandomString(
      "res1",
      RandomStringArgs.builder.length(5).build,
      CustomResourceOptions.builder.resourceTransformations { (args: ResourceTransformation.Args) =>
        Optional.of(
          new ResourceTransformation.Result(
            args.args,
            CustomResourceOptions.merge(
              args.options.asInstanceOf[CustomResourceOptions],
              CustomResourceOptions.builder
                .additionalSecretOutputs("length")
                .build
            )
          )
        )
      }.build
    )
    // Scenario #2 - apply a transformation to a Component to transform its children
    val res2 = new App.MyComponent(
      "res2",
      ComponentResourceOptions.builder.resourceTransformations { (args: ResourceTransformation.Args) =>
        args.args match {
          case oldArgs: RandomStringArgs if args.resource.getResourceType == RandomStringType =>
            val resultArgs = RandomStringArgs.builder
              .length(oldArgs.length)
              .minUpper(2)
              .build
            val resultOpts = CustomResourceOptions.merge(
              args.options.asInstanceOf[CustomResourceOptions],
              CustomResourceOptions.builder
                .additionalSecretOutputs("length")
                .build
            )
            Optional.of(new ResourceTransformation.Result(resultArgs, resultOpts))
          case _ =>
            Optional.empty[ResourceTransformation.Result]
        }
      }.build
    )
    // Scenario #3 - apply a transformation to the Stack to transform all resources in the stack.
    val res3 = new RandomString("res3", RandomStringArgs.builder.length(5).build)

    // Scenario #4 - transformations are applied in order of decreasing specificity
    // 1. (not in this example) Child transformation
    // 2. First parent transformation
    // 3. Second parent transformation
    // 4. Stack transformation
    val res4 = new App.MyComponent(
      "res4",
      ComponentResourceOptions.builder
        .resourceTransformations(
          (args: ResourceTransformation.Args) => scenario4(args, "value1"),
          (args: ResourceTransformation.Args) => scenario4(args, "value2")
        )
        .build
    )
    // Scenario #5 - cross-resource transformations that inject dependencies on one resource into another.
    val res5 = new App.MyOtherComponent(
      "res5",
      ComponentResourceOptions.builder
        .resourceTransformations(transformChild1DependsOnChild2(_))
        .build
    )
  }

  // Scenario #3 - apply a transformation to the Stack to transform all (future) resources in the stack
  private def scenario3(args: ResourceTransformation.Args): Optional[ResourceTransformation.Result] = {
    args.args match {
      case oldArgs: RandomStringArgs if args.resource.getResourceType == RandomStringType =>
        val resultArgs = RandomStringArgs.builder
          .length(oldArgs.length)
          .minUpper(oldArgs.minUpper.orElse(null)) // TODO: see if we can make this API more consistent
          .overrideSpecial(Output.format("%sstackvalue", oldArgs.overrideSpecial.orElse(Output.of(""))))
          .build
        Optional.of(new ResourceTransformation.Result(resultArgs, args.options))
      case _ =>
        Optional.empty[ResourceTransformation.Result]
    }
  }

  private def scenario4(
      args: ResourceTransformation.Args,
      v: String
  ): Optional[ResourceTransformation.Result] = {
    args.args match {
      case oldArgs: RandomStringArgs if args.resource.getResourceType == RandomStringType =>
        val resultArgs = RandomStringArgs.builder
          .length(oldArgs.length)
          .overrideSpecial(Output.format("%s%s", oldArgs.overrideSpecial.orElse(Output.of("")), v))
          .build
        Optional.of(new ResourceTransformation.Result(resultArgs, args.options))
      case _ =>
        Optional.empty[ResourceTransformation.Result]
    }
  }

  private val transformChild1DependsOnChild2: ResourceTransformation = {
    // Create a task completion source that wil be resolved once we find child2.
    // This is needed because we do not know what order we will see the resource
    // registrations of child1 and child2.
    val child2ArgsSource = new CompletableFuture[RandomStringArgs]
    (args: ResourceTransformation.Args) => {
      // Return a transformation which will rewrite child1 to depend on the promise for child2, and
      // will resolve that promise when it finds child2.
      args.args match {
        case resourceArgs: RandomStringArgs =>
          val resourceName = args.resource.getResourceName
          if (resourceName.endsWith("-child2")) { // Resolve the child2 promise with the child2 resource.
            child2ArgsSource.complete(resourceArgs)
            Optional.empty[ResourceTransformation.Result]
          } else if (resourceName.endsWith("-child1")) {
            val child2Length = resourceArgs.length
              .apply { (in: Integer) =>
                if (in != 5) { // Not strictly necessary - but shows we can confirm invariants we expect to be true.
                  throw new RuntimeException("unexpected input value")
                }
                Output.of(child2ArgsSource.thenApply[Output[Integer]](_.length()))
              }
              .apply(identity(_))
            val newArgs = RandomStringArgs.builder.length(child2Length).build
            Optional.of(new ResourceTransformation.Result(newArgs, args.options))
          } else
            Optional.empty[ResourceTransformation.Result]
        case _ =>
          Optional.empty[ResourceTransformation.Result]
      }
    }
  }

  class MyComponent(val name: String, @Nullable val options: ComponentResourceOptions)
      extends ComponentResource("my:component:MyComponent", name, options) {
    val child = new RandomString(
      String.format("%s-child", name),
      RandomStringArgs.builder.length(5).build,
      CustomResourceOptions.builder
        .parent(this)
        .additionalSecretOutputs("special")
        .build
    )
  }

  // Scenario #5 - cross-resource transformations that inject the output of one resource to the input
  // of the other one.
  class MyOtherComponent(val name: String, @Nullable val options: ComponentResourceOptions)
      extends ComponentResource("my:component:MyComponent", name, options) {
    val child1 = new RandomString(
      String.format("%s-child1", name),
      RandomStringArgs.builder.length(5).build,
      CustomResourceOptions.builder.parent(this).build
    )
    val child2 = new RandomString(
      String.format("%s-child2", name),
      RandomStringArgs.builder.length(6).build,
      CustomResourceOptions.builder.parent(this).build
    )
  }
}
