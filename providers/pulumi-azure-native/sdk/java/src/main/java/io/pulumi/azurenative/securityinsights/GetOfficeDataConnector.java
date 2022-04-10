// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.securityinsights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.securityinsights.inputs.GetOfficeDataConnectorArgs;
import io.pulumi.azurenative.securityinsights.outputs.GetOfficeDataConnectorResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetOfficeDataConnector {
    private GetOfficeDataConnector() {}
    /**
         * Represents office data connector.
     * API Version: 2020-01-01.
     * 
     *
         * Represents office data connector.
     * 
     */
    public static CompletableFuture<GetOfficeDataConnectorResult> invokeAsync(GetOfficeDataConnectorArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:securityinsights:getOfficeDataConnector", TypeShape.of(GetOfficeDataConnectorResult.class), args == null ? GetOfficeDataConnectorArgs.Empty : args, Utilities.withVersion(options));
    }
}