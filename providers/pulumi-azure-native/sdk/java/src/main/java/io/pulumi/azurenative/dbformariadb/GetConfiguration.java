// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.dbformariadb;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.dbformariadb.inputs.GetConfigurationArgs;
import io.pulumi.azurenative.dbformariadb.outputs.GetConfigurationResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetConfiguration {
    private GetConfiguration() {}
    /**
         * Represents a Configuration.
     * API Version: 2018-06-01.
     * 
     *
         * Represents a Configuration.
     * 
     */
    public static CompletableFuture<GetConfigurationResult> invokeAsync(GetConfigurationArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:dbformariadb:getConfiguration", TypeShape.of(GetConfigurationResult.class), args == null ? GetConfigurationArgs.Empty : args, Utilities.withVersion(options));
    }
}