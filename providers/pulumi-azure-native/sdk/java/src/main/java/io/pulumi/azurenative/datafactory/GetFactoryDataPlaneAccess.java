// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.datafactory;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.datafactory.inputs.GetFactoryDataPlaneAccessArgs;
import io.pulumi.azurenative.datafactory.outputs.GetFactoryDataPlaneAccessResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetFactoryDataPlaneAccess {
    private GetFactoryDataPlaneAccess() {}
    /**
         * Get Data Plane read only token response definition.
     * API Version: 2018-06-01.
     * 
     *
         * Get Data Plane read only token response definition.
     * 
     */
    public static CompletableFuture<GetFactoryDataPlaneAccessResult> invokeAsync(GetFactoryDataPlaneAccessArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:datafactory:getFactoryDataPlaneAccess", TypeShape.of(GetFactoryDataPlaneAccessResult.class), args == null ? GetFactoryDataPlaneAccessArgs.Empty : args, Utilities.withVersion(options));
    }
}