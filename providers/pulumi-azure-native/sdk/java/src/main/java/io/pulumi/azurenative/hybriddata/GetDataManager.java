// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.hybriddata;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.hybriddata.inputs.GetDataManagerArgs;
import io.pulumi.azurenative.hybriddata.outputs.GetDataManagerResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDataManager {
    private GetDataManager() {}
    /**
         * The DataManager resource.
     * API Version: 2019-06-01.
     * 
     *
         * The DataManager resource.
     * 
     */
    public static CompletableFuture<GetDataManagerResult> invokeAsync(GetDataManagerArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:hybriddata:getDataManager", TypeShape.of(GetDataManagerResult.class), args == null ? GetDataManagerArgs.Empty : args, Utilities.withVersion(options));
    }
}