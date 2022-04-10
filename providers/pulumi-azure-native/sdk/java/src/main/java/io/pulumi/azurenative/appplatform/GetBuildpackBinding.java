// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.appplatform;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.appplatform.inputs.GetBuildpackBindingArgs;
import io.pulumi.azurenative.appplatform.outputs.GetBuildpackBindingResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetBuildpackBinding {
    private GetBuildpackBinding() {}
    /**
         * Buildpack Binding Resource object
     * API Version: 2022-01-01-preview.
     * 
     *
         * Buildpack Binding Resource object
     * 
     */
    public static CompletableFuture<GetBuildpackBindingResult> invokeAsync(GetBuildpackBindingArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:appplatform:getBuildpackBinding", TypeShape.of(GetBuildpackBindingResult.class), args == null ? GetBuildpackBindingArgs.Empty : args, Utilities.withVersion(options));
    }
}