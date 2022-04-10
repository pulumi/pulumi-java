// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.eventgrid;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.eventgrid.inputs.GetSystemTopicEventSubscriptionFullUrlArgs;
import io.pulumi.azurenative.eventgrid.outputs.GetSystemTopicEventSubscriptionFullUrlResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSystemTopicEventSubscriptionFullUrl {
    private GetSystemTopicEventSubscriptionFullUrl() {}
    /**
         * Full endpoint url of an event subscription
     * API Version: 2021-06-01-preview.
     * 
     *
         * Full endpoint url of an event subscription
     * 
     */
    public static CompletableFuture<GetSystemTopicEventSubscriptionFullUrlResult> invokeAsync(GetSystemTopicEventSubscriptionFullUrlArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:eventgrid:getSystemTopicEventSubscriptionFullUrl", TypeShape.of(GetSystemTopicEventSubscriptionFullUrlResult.class), args == null ? GetSystemTopicEventSubscriptionFullUrlArgs.Empty : args, Utilities.withVersion(options));
    }
}