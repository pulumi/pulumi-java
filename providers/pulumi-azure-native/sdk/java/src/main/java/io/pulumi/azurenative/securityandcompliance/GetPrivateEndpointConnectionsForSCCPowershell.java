// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.securityandcompliance;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.securityandcompliance.inputs.GetPrivateEndpointConnectionsForSCCPowershellArgs;
import io.pulumi.azurenative.securityandcompliance.outputs.GetPrivateEndpointConnectionsForSCCPowershellResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetPrivateEndpointConnectionsForSCCPowershell {
    private GetPrivateEndpointConnectionsForSCCPowershell() {}
    /**
         * The Private Endpoint Connection resource.
     * API Version: 2021-03-08.
     * 
     *
         * The Private Endpoint Connection resource.
     * 
     */
    public static CompletableFuture<GetPrivateEndpointConnectionsForSCCPowershellResult> invokeAsync(GetPrivateEndpointConnectionsForSCCPowershellArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:securityandcompliance:getPrivateEndpointConnectionsForSCCPowershell", TypeShape.of(GetPrivateEndpointConnectionsForSCCPowershellResult.class), args == null ? GetPrivateEndpointConnectionsForSCCPowershellArgs.Empty : args, Utilities.withVersion(options));
    }
}