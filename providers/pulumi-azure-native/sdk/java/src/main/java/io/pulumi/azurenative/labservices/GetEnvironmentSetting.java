// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.labservices;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.labservices.inputs.GetEnvironmentSettingArgs;
import io.pulumi.azurenative.labservices.outputs.GetEnvironmentSettingResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetEnvironmentSetting {
    private GetEnvironmentSetting() {}
    /**
         * Represents settings of an environment, from which environment instances would be created
     * API Version: 2018-10-15.
     * 
     *
         * Represents settings of an environment, from which environment instances would be created
     * 
     */
    public static CompletableFuture<GetEnvironmentSettingResult> invokeAsync(GetEnvironmentSettingArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:labservices:getEnvironmentSetting", TypeShape.of(GetEnvironmentSettingResult.class), args == null ? GetEnvironmentSettingArgs.Empty : args, Utilities.withVersion(options));
    }
}