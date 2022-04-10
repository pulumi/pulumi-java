// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.powerplatform;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.powerplatform.inputs.GetAccountArgs;
import io.pulumi.azurenative.powerplatform.outputs.GetAccountResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetAccount {
    private GetAccount() {}
    /**
         * Definition of the account.
     * API Version: 2020-10-30-preview.
     * 
     *
         * Definition of the account.
     * 
     */
    public static CompletableFuture<GetAccountResult> invokeAsync(GetAccountArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:powerplatform:getAccount", TypeShape.of(GetAccountResult.class), args == null ? GetAccountArgs.Empty : args, Utilities.withVersion(options));
    }
}