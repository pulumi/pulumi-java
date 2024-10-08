// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.mypkg.outputs;

import com.pulumi.core.annotations.CustomType;
import java.lang.String;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

@CustomType
public final class Config {
    private @Nullable String foo;

    private Config() {}
    public Optional<String> foo() {
        return Optional.ofNullable(this.foo);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Config defaults) {
        return new Builder(defaults);
    }
    @CustomType.Builder
    public static final class Builder {
        private @Nullable String foo;
        public Builder() {}
        public Builder(Config defaults) {
    	      Objects.requireNonNull(defaults);
    	      this.foo = defaults.foo;
        }

        @CustomType.Setter
        public Builder foo(@Nullable String foo) {

            this.foo = foo;
            return this;
        }
        public Config build() {
            final var _resultValue = new Config();
            _resultValue.foo = foo;
            return _resultValue;
        }
    }
}
