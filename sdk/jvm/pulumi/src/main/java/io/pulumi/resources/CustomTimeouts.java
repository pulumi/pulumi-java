package io.pulumi.resources;

import io.pulumi.core.internal.Copyable;

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

    public CustomTimeouts(@Nullable Duration create, @Nullable Duration update, @Nullable Duration delete) {
        this.create = create;
        this.update = update;
        this.delete = delete;
    }

    /**
     * The optional create timeout
     */
    public Optional<Duration> getCreate() {
        return Optional.ofNullable(create);
    }

    /**
     * The optional update timeout.
     */
    public Optional<Duration> getUpdate() {
        return Optional.ofNullable(update);
    }

    /* The optional delete timeout. */
    public Optional<Duration> getDelete() {
        return Optional.ofNullable(delete);
    }

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
}
