package com.pulumi.resources;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The callback signature for the {@code transformations} resource option.
 * @see #apply(Args)
 */
public interface ResourceTransformation {

    /**
     * ResourceTransformation#apply is the callback signature for {@link ResourceOptions#getResourceTransformations()}.
     * A transformation is passed the same set of inputs provided to the {@link Resource} constructor,
     * and can optionally return alternate values for the "properties" and/or "options" prior to the resource
     * actually being created. The effect will be as though those "properties" and/or
     * "options" were passed in place of the original call to the {@link Resource} constructor.
     *
     * @return The new values to use for the "args" and "options" of the {@link Resource}
     * in place of the originally provided values.
     * Returns {@link Optional#empty()} if the resource will not be transformed.
     */
    Optional<ResourceTransformation.Result> apply(ResourceTransformation.Args args);

    /**
     * The argument bag passed to a {@link Resource} transformation.
     * @see #apply(Args)
     */
    @ParametersAreNonnullByDefault
    class Args {
        private final Resource resource;
        private final ResourceArgs args;
        private final ResourceOptions options;

        public Args(Resource resource, ResourceArgs args, ResourceOptions options) {
            this.resource = requireNonNull(resource);
            this.args = requireNonNull(args);
            this.options = requireNonNull(options);
        }

        /**
         * @return the {@link Resource} instance that is being transformed.
         */
        public Resource resource() {
            return this.resource;
        }

        /**
         * @return the original properties passed to the {@link Resource} constructor.
         */
        public ResourceArgs args() {
            return this.args;
        }

        /**
         * @return the original resource options passed to the {@link Resource} constructor.
         */
        public ResourceOptions options() {
            return this.options;
        }
    }

    /**
     * the result that must be returned by a resource transformation callback.
     * It includes new values to use for the "properties" and "options"
     * of the {@link Resource} in place of the originally provided values.
     * @see #apply(Args)
     */
    @ParametersAreNonnullByDefault
    class Result {
        private final ResourceArgs args;
        private final ResourceOptions options;

        public Result(ResourceArgs args, ResourceOptions options) {
            this.args = requireNonNull(args);
            this.options = requireNonNull(options);
        }

        /**
         * @return the original properties passed to the Resource constructor.
         */
        public ResourceArgs args() {
            return args;
        }

        /**
         * @return the original resource options passed to the Resource constructor.
         */
        public ResourceOptions options() {
            return options;
        }
    }
}