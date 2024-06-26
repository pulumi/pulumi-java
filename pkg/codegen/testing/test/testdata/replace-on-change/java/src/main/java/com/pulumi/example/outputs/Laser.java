// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.example.outputs;

import com.pulumi.core.annotations.CustomType;
import com.pulumi.example.Cat;
import java.lang.Boolean;
import java.lang.Double;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

@CustomType
public final class Laser {
    private @Nullable Cat animal;
    private @Nullable Boolean batteries;
    private @Nullable Double light;

    private Laser() {}
    public Optional<Cat> animal() {
        return Optional.ofNullable(this.animal);
    }
    public Optional<Boolean> batteries() {
        return Optional.ofNullable(this.batteries);
    }
    public Optional<Double> light() {
        return Optional.ofNullable(this.light);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Laser defaults) {
        return new Builder(defaults);
    }
    @CustomType.Builder
    public static final class Builder {
        private @Nullable Cat animal;
        private @Nullable Boolean batteries;
        private @Nullable Double light;
        public Builder() {}
        public Builder(Laser defaults) {
    	      Objects.requireNonNull(defaults);
    	      this.animal = defaults.animal;
    	      this.batteries = defaults.batteries;
    	      this.light = defaults.light;
        }

        @CustomType.Setter
        public Builder animal(@Nullable Cat animal) {

            this.animal = animal;
            return this;
        }
        @CustomType.Setter
        public Builder batteries(@Nullable Boolean batteries) {

            this.batteries = batteries;
            return this;
        }
        @CustomType.Setter
        public Builder light(@Nullable Double light) {

            this.light = light;
            return this;
        }
        public Laser build() {
            final var _resultValue = new Laser();
            _resultValue.animal = animal;
            _resultValue.batteries = batteries;
            _resultValue.light = light;
            return _resultValue;
        }
    }
}
