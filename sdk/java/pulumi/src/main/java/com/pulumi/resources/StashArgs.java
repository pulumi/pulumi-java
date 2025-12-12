package com.pulumi.resources;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;

import javax.annotation.Nullable;

/**
 * The set of arguments for constructing a {@link Stash} resource.
 */
public final class StashArgs extends ResourceArgs {

    /**
     * An empty {@link StashArgs} instance.
     */
    public static final StashArgs Empty = StashArgs.builder().build();

    @Import(name = "output", required = true)
    @Nullable
    private final Output<Object> input;

    public StashArgs(@Nullable Output<Object> input) {
        this.input = input;
    }

    /**
     * The value to store in the stash resource.
     */
    public Output<Object> input() {
        return input;
    }

    /**
     * @return a {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for {@link StackReferenceArgs}.
     */
    public static final class Builder {
        @Nullable
        private Output<Object> input;

        /**
         * @param input the value to store in the stash resource.
         * @return the {@link Builder}
         * @see StashArgs#input()
         */
        public Builder input(@Nullable Output<Object> input) {
            this.input = input;
            return this;
        }

        /**
         * @param input the value to store in the stash resource.
         * @return the {@link Builder}
         * @see StashArgs#input()
         */
        public Builder input(Object input) {
            this.input = Output.of(input);
            return this;
        }

        /**
         * @return a {@link StashArgs} instance created from this {@link Builder}
         */
        public StashArgs build() {
            return new StashArgs(this.input);
        }
    }
}