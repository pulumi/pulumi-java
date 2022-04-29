package com.pulumi.resources;

import com.pulumi.core.internal.Copyable;
import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional timeouts to supply in @see {@link ResourceOptions#getCustomTimeouts()}
 */
public final class CustomTimeouts implements Copyable<CustomTimeouts> {
    @Nullable
    private final Duration create;
    @Nullable
    private final Duration update;
    @Nullable
    private final Duration delete;

    /**
     * @param create the optional create timeout
     * @param update the optional update timeout
     * @param delete the optional delete timeout
     */
    public CustomTimeouts(@Nullable Duration create, @Nullable Duration update, @Nullable Duration delete) {
        this.create = create;
        this.update = update;
        this.delete = delete;
    }

    /**
     * @return the optional create timeout
     */
    public Optional<Duration> getCreate() {
        return Optional.ofNullable(create);
    }

    /**
     * @return the optional update timeout
     */
    public Optional<Duration> getUpdate() {
        return Optional.ofNullable(update);
    }

    /**
     * @return the optional delete timeout
     */
    public Optional<Duration> getDelete() {
        return Optional.ofNullable(delete);
    }

    /**
     * @return a copy of {@code this} {@link CustomTimeouts} instance
     */
    @Override
    public CustomTimeouts copy() {
        return new CustomTimeouts(this.create, this.update, this.delete);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomTimeouts that = (CustomTimeouts) o;
        return Objects.equals(create, that.create)
                && Objects.equals(update, that.update)
                && Objects.equals(delete, that.delete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(create, update, delete);
    }

    @InternalUse
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static String golangString(Optional<Duration> duration) {
        if (duration.isEmpty()) {
            return "";
        }

        // This will eventually be parsed by go's ParseDuration function here:
        // https://github.com/pulumi/pulumi/blob/06d4dde8898b2a0de2c3c7ff8e45f97495b89d82/pkg/resource/deploy/source_eval.go#L967
        //
        // So we generate a legal duration as allowed by
        // https://golang.org/pkg/time/#ParseDuration.
        //
        // Simply put, we simply convert our duration to the number of nanoseconds corresponding to it.
        // We also append "ns" to it, for the Golang parser.
        return duration.get().toNanos() + "ns";
    }

    /**
     * @return a {@link CustomTimeouts} builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The {@link CustomTimeouts} builder
     */
    public static final class Builder {
        @Nullable
        private Duration create;
        @Nullable
        private Duration update;
        @Nullable
        private Duration delete;

        public Builder() {
            // Empty
        }

        /**
         * @param create the optional create timeout
         * @return the {@link CustomTimeouts} builder
         * @see CustomTimeouts#create
         */
        public Builder create(Duration create) {
            this.create = create;
            return this;
        }

        /**
         * @param update the optional update timeout
         * @return the {@link CustomTimeouts} builder
         * @see CustomTimeouts#update
         */
        public Builder update(Duration update) {
            this.update = update;
            return this;
        }

        /**
         * @param delete the optional delete timeout
         * @return the {@link CustomTimeouts} builder
         * @see CustomTimeouts#delete
         */
        public Builder delete(Duration delete) {
            this.delete = delete;
            return this;
        }

        /**
         * @return a {@link CustomTimeouts} instance initialized using the {@link Builder}
         */
        public CustomTimeouts build() {
            return new CustomTimeouts(this.create, this.update, this.delete);
        }
    }
}
