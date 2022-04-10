// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.dialogflow_v2;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.dialogflow_v2.inputs.GetConversationProfileArgs;
import io.pulumi.googlenative.dialogflow_v2.outputs.GetConversationProfileResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetConversationProfile {
    private GetConversationProfile() {}
    /**
         * Retrieves the specified conversation profile.
     * 
     */
    public static CompletableFuture<GetConversationProfileResult> invokeAsync(GetConversationProfileArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:dialogflow/v2:getConversationProfile", TypeShape.of(GetConversationProfileResult.class), args == null ? GetConversationProfileArgs.Empty : args, Utilities.withVersion(options));
    }
}