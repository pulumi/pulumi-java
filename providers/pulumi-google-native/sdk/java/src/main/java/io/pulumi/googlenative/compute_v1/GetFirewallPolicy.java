// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.compute_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.compute_v1.inputs.GetFirewallPolicyArgs;
import io.pulumi.googlenative.compute_v1.outputs.GetFirewallPolicyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetFirewallPolicy {
    private GetFirewallPolicy() {}
    /**
         * Returns the specified firewall policy.
     * 
     */
    public static CompletableFuture<GetFirewallPolicyResult> invokeAsync(GetFirewallPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:compute/v1:getFirewallPolicy", TypeShape.of(GetFirewallPolicyResult.class), args == null ? GetFirewallPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}