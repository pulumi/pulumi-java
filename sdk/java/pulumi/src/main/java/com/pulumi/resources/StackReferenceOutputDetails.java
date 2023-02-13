package com.pulumi.resources;

import java.util.Optional;

/**
 * StackReferenceOutputDetails records an output from a StackReference.
 * <p>
 * At most one of value and secretValue will be set.
 */
public class StackReferenceOutputDetails {
    private final Optional<Object> value;
    private final Optional<Object> secretValue;

    protected StackReferenceOutputDetails(Optional<Object> value, Optional<Object> secretValue) {
        this.value = value;
        this.secretValue = secretValue;
    }

    /**
     * Returns the value of the output.
     * <p>
     * Returns an absent value if the output is a secret or if it does not exist.
     */
    public Optional<Object> getValue() {
        return value;
    }

    /**
     * Returns the secret value of the output.
     * <p>
     * Returns an absent value if the output is not a secret or if it does not
     * exist.
     */
    public Optional<Object> getSecretValue() {
        return secretValue;
    }

    /**
     * Creates a builder for a {@link StackReferenceOutputDetails}.
     *
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds instances of type {@link StackReferenceOutputDetails}.
     * <p>
     * Use {@link #builder()} to create a new builder.
     */
    public static class Builder {
        private Optional<Object> value = Optional.empty();
        private Optional<Object> secretValue = Optional.empty();

        private Builder() {
        }

        /**
         * Specifies an output value returned by the StackReference.
         * <p>
         * The value must not be a secret.
         */
        public Builder value(Object value) {
            this.value = Optional.of(value);
            return this;
        }

        /**
         * Specifies a secret output value returned by the StackReference.
         * <p>
         * The value *must* be a secret.
         */
        public Builder secretValue(Object secretValue) {
            this.secretValue = Optional.of(secretValue);
            return this;
        }

        /**
         * Builds a StackReferenceOutputDetails instance.
         * <p>
         * 
         * @throws IllegalStateException if both value and secretValue are set
         */
        public StackReferenceOutputDetails build() {
            if (value.isPresent() && secretValue.isPresent()) {
                throw new IllegalStateException("Cannot set both value and secretValue");
            }
            return new StackReferenceOutputDetails(value, secretValue);
        }
    }
}