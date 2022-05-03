package io.pulumi.resources;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;

public interface ResourceTransformation {

    /**
     * ResourceTransformation#apply is the callback signature for @see {@link ResourceOptions#getResourceTransformations()}.
     * A transformation is passed the same set of inputs provided to the @see {@link Resource} constructor,
     * and can optionally return back alternate values for the "properties" and/or "options" prior to the resource
     * actually being created. The effect will be as though those "properties" and/or
     * "options" were passed in place of the original call to the @see {@link Resource} constructor.
     *
     * @return The new values to use for the "args" and "options" of the @see {@link Resource}
     * in place of the originally provided values.
     * Returns @see {@link Optional#empty()} if the resource will not be transformed.
     */
    Optional<ResourceTransformation.Result> apply(ResourceTransformation.Args args);

    @ParametersAreNonnullByDefault
    class Args {
        private final Resource resource;
        private final ResourceArgs args;
        private final ResourceOptions options;

        public Args(Resource resource, ResourceArgs args, ResourceOptions options) {
            this.resource = Objects.requireNonNull(resource);
            this.args = Objects.requireNonNull(args);
            this.options = Objects.requireNonNull(options);
        }

        /**
         * The Resource instance that is being transformed.
         */
        public Resource getResource() {
            return this.resource;
        }

        /**
         * The original properties passed to the Resource constructor.
         */
        public ResourceArgs getArgs() {
            return this.args;
        }

        /**
         * The original resource options passed to the Resource constructor.
         */
        public ResourceOptions getOptions() {
            return this.options;
        }
    }

    @ParametersAreNonnullByDefault
    class Result {
        private final ResourceArgs args;
        private final ResourceOptions options;

        public Result(ResourceArgs args, ResourceOptions options) {
            this.args = Objects.requireNonNull(args);
            this.options = Objects.requireNonNull(options);
        }

        /**
         * The original properties passed to the Resource constructor.
         */
        public ResourceArgs getArgs() {
            return args;
        }

        /**
         * The original resource options passed to the Resource constructor.
         */
        public ResourceOptions getOptions() {
            return options;
        }
    }
}
