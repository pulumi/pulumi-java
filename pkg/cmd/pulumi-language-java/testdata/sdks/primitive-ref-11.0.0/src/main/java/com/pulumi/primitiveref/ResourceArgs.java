// *** WARNING: this file was generated by pulumi-language-java. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.primitiveref;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.exceptions.MissingRequiredPropertyException;
import com.pulumi.primitiveref.inputs.DataArgs;
import java.util.Objects;


public final class ResourceArgs extends com.pulumi.resources.ResourceArgs {

    public static final ResourceArgs Empty = new ResourceArgs();

    @Import(name="data", required=true)
    private Output<DataArgs> data;

    public Output<DataArgs> data() {
        return this.data;
    }

    private ResourceArgs() {}

    private ResourceArgs(ResourceArgs $) {
        this.data = $.data;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static Builder builder(ResourceArgs defaults) {
        return new Builder(defaults);
    }

    public static final class Builder {
        private ResourceArgs $;

        public Builder() {
            $ = new ResourceArgs();
        }

        public Builder(ResourceArgs defaults) {
            $ = new ResourceArgs(Objects.requireNonNull(defaults));
        }

        public Builder data(Output<DataArgs> data) {
            $.data = data;
            return this;
        }

        public Builder data(DataArgs data) {
            return data(Output.of(data));
        }

        public ResourceArgs build() {
            if ($.data == null) {
                throw new MissingRequiredPropertyException("ResourceArgs", "data");
            }
            return $;
        }
    }

}
