// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.subscription;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.subscription.inputs.GetAliasArgs;
import io.pulumi.azurenative.subscription.outputs.GetAliasResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetAlias {
    private GetAlias() {}
    /**
         * Subscription Information with the alias.
     * API Version: 2020-09-01.
     * 
     *
         * Subscription Information with the alias.
     * 
     */
    public static CompletableFuture<GetAliasResult> invokeAsync(GetAliasArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:subscription:getAlias", TypeShape.of(GetAliasResult.class), args == null ? GetAliasArgs.Empty : args, Utilities.withVersion(options));
    }
}