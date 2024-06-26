// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.akamai.inputs;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.exceptions.MissingRequiredPropertyException;
import java.lang.String;
import java.util.Objects;


public final class GetImagingPolicyVideoPolicyVariableEnumOptionArgs extends com.pulumi.resources.ResourceArgs {

    public static final GetImagingPolicyVideoPolicyVariableEnumOptionArgs Empty = new GetImagingPolicyVideoPolicyVariableEnumOptionArgs();

    @Import(name="id", required=true)
    private Output<String> id;

    public Output<String> id() {
        return this.id;
    }

    @Import(name="value", required=true)
    private Output<String> value;

    public Output<String> value() {
        return this.value;
    }

    private GetImagingPolicyVideoPolicyVariableEnumOptionArgs() {}

    private GetImagingPolicyVideoPolicyVariableEnumOptionArgs(GetImagingPolicyVideoPolicyVariableEnumOptionArgs $) {
        this.id = $.id;
        this.value = $.value;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static Builder builder(GetImagingPolicyVideoPolicyVariableEnumOptionArgs defaults) {
        return new Builder(defaults);
    }

    public static final class Builder {
        private GetImagingPolicyVideoPolicyVariableEnumOptionArgs $;

        public Builder() {
            $ = new GetImagingPolicyVideoPolicyVariableEnumOptionArgs();
        }

        public Builder(GetImagingPolicyVideoPolicyVariableEnumOptionArgs defaults) {
            $ = new GetImagingPolicyVideoPolicyVariableEnumOptionArgs(Objects.requireNonNull(defaults));
        }

        public Builder id(Output<String> id) {
            $.id = id;
            return this;
        }

        public Builder id(String id) {
            return id(Output.of(id));
        }

        public Builder value(Output<String> value) {
            $.value = value;
            return this;
        }

        public Builder value(String value) {
            return value(Output.of(value));
        }

        public GetImagingPolicyVideoPolicyVariableEnumOptionArgs build() {
            if ($.id == null) {
                throw new MissingRequiredPropertyException("GetImagingPolicyVideoPolicyVariableEnumOptionArgs", "id");
            }
            if ($.value == null) {
                throw new MissingRequiredPropertyException("GetImagingPolicyVideoPolicyVariableEnumOptionArgs", "value");
            }
            return $;
        }
    }

}
