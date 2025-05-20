package com.pulumi.example.stacktransformation;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.random.RandomString;
import com.pulumi.random.RandomStringArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceTransformation;
import com.pulumi.resources.StackOptions;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class App {

    private final static String RandomStringType = "random:index/randomString:RandomString";

    public static void main(String[] args) {
        Pulumi.withOptions(StackOptions.builder()
                        .resourceTransformations(App::scenario3)
                        .build())
                .run(App::stack);
    }

    private static void stack(Context ctx) {
        // Scenario #1 - apply a transformation to a CustomResource
        var res1 = new RandomString("res1",
                RandomStringArgs.builder().length(5).build(),
                CustomResourceOptions.builder()
                        .resourceTransformations(args -> Optional.of(new ResourceTransformation.Result(
                                args.args(),
                                CustomResourceOptions.merge(
                                        (CustomResourceOptions) args.options(),
                                        CustomResourceOptions.builder()
                                                .additionalSecretOutputs("length")
                                                .build()
                                )
                        )))
                        .build()
        );

        // Scenario #2 - apply a transformation to a Component to transform its children
        var res2 = new MyComponent("res2",
                ComponentResourceOptions.builder()
                        .resourceTransformations(args -> {
                            if (Objects.equals(args.resource().pulumiResourceType(), RandomStringType)
                                    && args.args() instanceof RandomStringArgs) {
                                var oldArgs = (RandomStringArgs) args.args();
                                var resultArgs = RandomStringArgs.builder()
                                        .length(oldArgs.length())
                                        .minUpper(2)
                                        .build();
                                var resultOpts = CustomResourceOptions.merge(
                                        (CustomResourceOptions) args.options(),
                                        CustomResourceOptions.builder()
                                                .additionalSecretOutputs("length")
                                                .build()
                                );
                                return Optional.of(new ResourceTransformation.Result(resultArgs, resultOpts));
                            }
                            return Optional.empty();
                        })
                        .build()
        );

        // Scenario #3 - apply a transformation to the Stack to transform all resources in the stack.
        var res3 = new RandomString("res3", RandomStringArgs.builder().length(5).build());

        // Scenario #4 - transformations are applied in order of decreasing specificity
        // 1. (not in this example) Child transformation
        // 2. First parent transformation
        // 3. Second parent transformation
        // 4. Stack transformation
        var res4 = new MyComponent("res4",
                ComponentResourceOptions.builder()
                        .resourceTransformations(
                                args -> scenario4(args, "value1"),
                                args -> scenario4(args, "value2")
                        )
                        .build()
        );

        // Scenario #5 - cross-resource transformations that inject dependencies on one resource into another.
        var res5 = new MyOtherComponent("res5",
                ComponentResourceOptions.builder()
                        .resourceTransformations(transformChild1DependsOnChild2())
                        .build()
        );
    }

    // Scenario #3 - apply a transformation to the Stack to transform all (future) resources in the stack
    private static Optional<ResourceTransformation.Result> scenario3(ResourceTransformation.Args args) {
        if (Objects.equals(args.resource().pulumiResourceType(), RandomStringType)
                && args.args() instanceof RandomStringArgs) {
            var oldArgs = (RandomStringArgs) args.args();
            var resultArgs = RandomStringArgs.builder()
                    .length(oldArgs.length())
                    .minUpper(oldArgs.minUpper().orElse(null)) // TODO: see if we can make this API more consistent
                    .overrideSpecial(Output.format("%sstackvalue", oldArgs.overrideSpecial().orElse(Output.of(""))))
                    .build();
            return Optional.of(new ResourceTransformation.Result(resultArgs, args.options()));
        }
        return Optional.empty();
    }

    private static Optional<ResourceTransformation.Result> scenario4(ResourceTransformation.Args args, String v) {
        if (Objects.equals(args.resource().pulumiResourceType(), RandomStringType)
                && args.args() instanceof RandomStringArgs) {
            var oldArgs = (RandomStringArgs) args.args();
            var resultArgs = RandomStringArgs.builder()
                    .length(oldArgs.length())
                    .overrideSpecial(Output.format("%s%s", oldArgs.overrideSpecial().orElse(Output.of("")), v))
                    .build();
            return Optional.of(new ResourceTransformation.Result(resultArgs, args.options()));
        }

        return Optional.empty();
    }

    private static ResourceTransformation transformChild1DependsOnChild2() {
        // Create a task completion source that wil be resolved once we find child2.
        // This is needed because we do not know what order we will see the resource
        // registrations of child1 and child2.
        var child2ArgsSource = new CompletableFuture<RandomStringArgs>();

        return (ResourceTransformation.Args args) -> {
            // Return a transformation which will rewrite child1 to depend on the promise for child2, and
            // will resolve that promise when it finds child2.
            if (args.args() instanceof RandomStringArgs) {
                var resourceArgs = (RandomStringArgs) args.args();
                var resourceName = args.resource().pulumiResourceName();
                if (resourceName.endsWith("-child2")) {
                    // Resolve the child2 promise with the child2 resource.
                    child2ArgsSource.complete(resourceArgs);
                    return Optional.empty();
                }
                if (resourceName.endsWith("-child1")) {
                    var child2Length = resourceArgs.length()
                            .apply(in -> {
                                if (in != 5) {
                                    // Not strictly necessary - but shows we can confirm invariants we expect to be true.
                                    throw new RuntimeException("unexpected input value");
                                }
                                return Output.of(child2ArgsSource.thenApply(child2Args -> child2Args.length()));
                            })
                            .apply(out -> out);
                    var newArgs = RandomStringArgs.builder().length(child2Length).build();
                    return Optional.of(new ResourceTransformation.Result(newArgs, args.options()));
                }
            }
            return Optional.empty();
        };
    }

    static class MyComponent extends ComponentResource {
        private final RandomString child;

        public MyComponent(String name, @Nullable ComponentResourceOptions options) {
            super("my:component:MyComponent", name, options);
            this.child = new RandomString(String.format("%s-child", name),
                    RandomStringArgs.builder()
                            .length(5)
                            .build(),
                    CustomResourceOptions.builder()
                            .parent(this)
                            .additionalSecretOutputs("special")
                            .build()
            );
        }

        public RandomString child() {
            return this.child;
        }
    }

    // Scenario #5 - cross-resource transformations that inject the output of one resource to the input
    // of the other one.
    static class MyOtherComponent extends ComponentResource {
        private RandomString child1;
        private RandomString child2;

        public MyOtherComponent(String name, @Nullable ComponentResourceOptions options) {
            super("my:component:MyComponent", name, options);
            this.child1 = new RandomString(String.format("%s-child1", name),
                    RandomStringArgs.builder().length(5).build(),
                    CustomResourceOptions.builder().parent(this).build()
            );

            this.child2 = new RandomString(String.format("%s-child2", name),
                    RandomStringArgs.builder().length(6).build(),
                    CustomResourceOptions.builder().parent(this).build()
            );
        }

        public RandomString child1() {
            return child1;
        }

        public RandomString child2() {
            return child2;
        }
    }
}