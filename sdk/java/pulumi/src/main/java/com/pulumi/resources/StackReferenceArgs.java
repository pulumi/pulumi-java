package com.pulumi.resources;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The set of arguments for constructing a {@link StackReference} resource.
 */
public final class StackReferenceArgs extends ResourceArgs {

    /**
     * An empty {@link StackReferenceArgs} instance.
     */
    public static final StackReferenceArgs Empty = StackReferenceArgs.builder().build();

    @Import(name = "name", required = true)
    @Nullable
    private final Output<String> name;

    public StackReferenceArgs(@Nullable Output<String> name) {
        this.name = name;
    }

    /**
     * The name of the stack to reference.
     *
     * @deprecated use {@link #name()}
     */
    @Deprecated
    public Optional<Output<String>> getName() {
        return name();
    }

    /**
     * The name of the stack to reference.
     */
    public Optional<Output<String>> name() {
        return Optional.ofNullable(name);
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
        private Output<String> name;

        /**
         * @param name the name of the stack to reference.
         * @return the {@link Builder}
         * @see StackReferenceArgs#name()
         */
        public Builder name(@Nullable Output<String> name) {
            this.name = name;
            return this;
        }

        /**
         * @param name the name of the stack to reference.
         * @return the {@link Builder}
         * @see StackReferenceArgs#name()
         */
        public Builder name(String name) {
            this.name = Output.of(name);
            return this;
        }

        /**
         * @return a {@link StackReferenceArgs} instance created from this {@link Builder}
         */
        public StackReferenceArgs build() {
            return new StackReferenceArgs(this.name);
        }
    }
}
