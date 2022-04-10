// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.sql;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.sql.inputs.GetServerSecurityAlertPolicyArgs;
import io.pulumi.azurenative.sql.outputs.GetServerSecurityAlertPolicyResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetServerSecurityAlertPolicy {
    private GetServerSecurityAlertPolicy() {}
    /**
         * A server security alert policy.
     * API Version: 2020-11-01-preview.
     * 
     *
         * A server security alert policy.
     * 
     */
    public static CompletableFuture<GetServerSecurityAlertPolicyResult> invokeAsync(GetServerSecurityAlertPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:sql:getServerSecurityAlertPolicy", TypeShape.of(GetServerSecurityAlertPolicyResult.class), args == null ? GetServerSecurityAlertPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}