// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.akamai.inputs;

import com.pulumi.core.annotations.Import;
import java.lang.Integer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;


public final class GetImagingPolicyImagePolicyBreakpoints extends com.pulumi.resources.InvokeArgs {

    public static final GetImagingPolicyImagePolicyBreakpoints Empty = new GetImagingPolicyImagePolicyBreakpoints();

    @Import(name="widths")
    private @Nullable List<Integer> widths;

    public Optional<List<Integer>> widths() {
        return Optional.ofNullable(this.widths);
    }

    private GetImagingPolicyImagePolicyBreakpoints() {}

    private GetImagingPolicyImagePolicyBreakpoints(GetImagingPolicyImagePolicyBreakpoints $) {
        this.widths = $.widths;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static Builder builder(GetImagingPolicyImagePolicyBreakpoints defaults) {
        return new Builder(defaults);
    }

    public static final class Builder {
        private GetImagingPolicyImagePolicyBreakpoints $;

        public Builder() {
            $ = new GetImagingPolicyImagePolicyBreakpoints();
        }

        public Builder(GetImagingPolicyImagePolicyBreakpoints defaults) {
            $ = new GetImagingPolicyImagePolicyBreakpoints(Objects.requireNonNull(defaults));
        }

        public Builder widths(@Nullable List<Integer> widths) {
            $.widths = widths;
            return this;
        }

        public Builder widths(Integer... widths) {
            return widths(List.of(widths));
        }

        public GetImagingPolicyImagePolicyBreakpoints build() {
            return $;
        }
    }

}
