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
     * Given a string representing a duration, such as "300ms", "-1.5h" or "2h45m", parse it into
     * a {@link Duration}.
     *
     * @param timeout the string to parse
     * @return the parsed duration, or null if the input string was empty or null
     */
    public static Duration parseTimeoutString(String timeout) {
        if (timeout == null || timeout.isEmpty()) {
            return null;
        }

        // A duration string is a possibly signed sequence of decimal numbers, each with optional
        // fraction and a unit suffix, such as "300ms", "-1.5h" or "2h45m". Valid time units are "ns",
        // "us" (or "µs"), "ms", "s", "m", "h".

        String s = timeout;
        boolean neg = false;
        if (s.charAt(0) == '-' || s.charAt(0) == '+') {
            neg = s.charAt(0) == '-';
            s = s.substring(1);
        }
        if (s.equals("0")) {
            return Duration.ZERO;
        }
        if (s.isEmpty()) {
            throw new IllegalArgumentException("invalid duration " + timeout);
        }

        Duration duration = Duration.ZERO;
        while (!s.isEmpty()) {
            // find the next timeunit
            int i = 0;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                i++;
            }
            // parse the number
            double v = Double.parseDouble(s.substring(0, i));
            // parse the unit
            s = s.substring(i);
            if (s.isEmpty()) {
                throw new IllegalArgumentException("missing unit in duration " + timeout);
            }

            if (s.startsWith("ns")) {
                duration = duration.plusNanos((long)v);
                s = s.substring(2);
            } else if (s.startsWith("µs") || s.startsWith("us")) {
                duration = duration.plusNanos((long)(v * 1000));
                s = s.substring(2);
            } else if (s.startsWith("ms")) {
                duration = duration.plusNanos((long)(v * 1_000_000));
                s = s.substring(2);
            } else if (s.startsWith("s")) {
                duration = duration.plusNanos((long)(v * 1_000_000_000));
                s = s.substring(1);
            } else if (s.startsWith("m")) {
                duration = duration.plusSeconds((long)(v * 60));
                s = s.substring(1);
            } else if (s.startsWith("h")) {
                duration = duration.plusSeconds((long)(v * 3600));
                s = s.substring(1);
            } else if (s.startsWith("d")) {
                duration = duration.plusSeconds((long)(v * 86400));
                s = s.substring(1);
            } else {
                throw new IllegalArgumentException("invalid unit in duration " + timeout);
            }
        }
        return neg ? duration.negated() : duration;
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
