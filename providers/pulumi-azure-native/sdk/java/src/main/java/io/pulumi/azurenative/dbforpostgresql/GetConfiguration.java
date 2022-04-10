// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.dbforpostgresql;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.dbforpostgresql.inputs.GetConfigurationArgs;
import io.pulumi.azurenative.dbforpostgresql.outputs.GetConfigurationResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetConfiguration {
    private GetConfiguration() {}
    /**
         * Represents a Configuration.
     * API Version: 2017-12-01.
     * 
     *
         * Represents a Configuration.
     * 
     */
    public static CompletableFuture<GetConfigurationResult> invokeAsync(GetConfigurationArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:dbforpostgresql:getConfiguration", TypeShape.of(GetConfigurationResult.class), args == null ? GetConfigurationArgs.Empty : args, Utilities.withVersion(options));
    }
}