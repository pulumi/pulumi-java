// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.network;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.network.inputs.GetFirewallPolicyRuleCollectionGroupArgs;
import io.pulumi.azurenative.network.outputs.GetFirewallPolicyRuleCollectionGroupResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetFirewallPolicyRuleCollectionGroup {
    private GetFirewallPolicyRuleCollectionGroup() {}
    /**
         * Rule Collection Group resource.
     * API Version: 2020-11-01.
     * 
     *
         * Rule Collection Group resource.
     * 
     */
    public static CompletableFuture<GetFirewallPolicyRuleCollectionGroupResult> invokeAsync(GetFirewallPolicyRuleCollectionGroupArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:network:getFirewallPolicyRuleCollectionGroup", TypeShape.of(GetFirewallPolicyRuleCollectionGroupResult.class), args == null ? GetFirewallPolicyRuleCollectionGroupArgs.Empty : args, Utilities.withVersion(options));
    }
}