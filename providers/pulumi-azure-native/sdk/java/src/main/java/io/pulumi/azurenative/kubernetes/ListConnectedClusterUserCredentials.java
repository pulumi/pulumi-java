// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.kubernetes;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.kubernetes.inputs.ListConnectedClusterUserCredentialsArgs;
import io.pulumi.azurenative.kubernetes.outputs.ListConnectedClusterUserCredentialsResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class ListConnectedClusterUserCredentials {
    private ListConnectedClusterUserCredentials() {}
    /**
         * The list of credential result response.
     * API Version: 2021-04-01-preview.
     * 
     *
         * The list of credential result response.
     * 
     */
    public static CompletableFuture<ListConnectedClusterUserCredentialsResult> invokeAsync(ListConnectedClusterUserCredentialsArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:kubernetes:listConnectedClusterUserCredentials", TypeShape.of(ListConnectedClusterUserCredentialsResult.class), args == null ? ListConnectedClusterUserCredentialsArgs.Empty : args, Utilities.withVersion(options));
    }
}