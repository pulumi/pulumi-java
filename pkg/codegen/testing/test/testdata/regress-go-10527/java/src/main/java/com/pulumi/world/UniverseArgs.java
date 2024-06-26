// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.world;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.world.inputs.WorldArgs;
import java.lang.String;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;


public final class UniverseArgs extends com.pulumi.resources.ResourceArgs {

    public static final UniverseArgs Empty = new UniverseArgs();

    @Import(name="worlds")
    private @Nullable Output<Map<String,WorldArgs>> worlds;

    public Optional<Output<Map<String,WorldArgs>>> worlds() {
        return Optional.ofNullable(this.worlds);
    }

    private UniverseArgs() {}

    private UniverseArgs(UniverseArgs $) {
        this.worlds = $.worlds;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static Builder builder(UniverseArgs defaults) {
        return new Builder(defaults);
    }

    public static final class Builder {
        private UniverseArgs $;

        public Builder() {
            $ = new UniverseArgs();
        }

        public Builder(UniverseArgs defaults) {
            $ = new UniverseArgs(Objects.requireNonNull(defaults));
        }

        public Builder worlds(@Nullable Output<Map<String,WorldArgs>> worlds) {
            $.worlds = worlds;
            return this;
        }

        public Builder worlds(Map<String,WorldArgs> worlds) {
            return worlds(Output.of(worlds));
        }

        public UniverseArgs build() {
            return $;
        }
    }

}
