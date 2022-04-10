// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.retail_v2beta;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.retail_v2beta.inputs.GetControlArgs;
import io.pulumi.googlenative.retail_v2beta.outputs.GetControlResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetControl {
    private GetControl() {}
    /**
         * Gets a Control.
     * 
     */
    public static CompletableFuture<GetControlResult> invokeAsync(GetControlArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:retail/v2beta:getControl", TypeShape.of(GetControlResult.class), args == null ? GetControlArgs.Empty : args, Utilities.withVersion(options));
    }
}