// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.timeseriesinsights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.timeseriesinsights.inputs.GetGen1EnvironmentArgs;
import io.pulumi.azurenative.timeseriesinsights.outputs.GetGen1EnvironmentResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetGen1Environment {
    private GetGen1Environment() {}
    /**
         * An environment is a set of time-series data available for query, and is the top level Azure Time Series Insights resource. Gen1 environments have data retention limits.
     * API Version: 2020-05-15.
     * 
     *
         * An environment is a set of time-series data available for query, and is the top level Azure Time Series Insights resource. Gen1 environments have data retention limits.
     * 
     */
    public static CompletableFuture<GetGen1EnvironmentResult> invokeAsync(GetGen1EnvironmentArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:timeseriesinsights:getGen1Environment", TypeShape.of(GetGen1EnvironmentResult.class), args == null ? GetGen1EnvironmentArgs.Empty : args, Utilities.withVersion(options));
    }
}