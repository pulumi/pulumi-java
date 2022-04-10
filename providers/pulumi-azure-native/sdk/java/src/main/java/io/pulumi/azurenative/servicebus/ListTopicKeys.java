// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.servicebus;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.servicebus.inputs.ListTopicKeysArgs;
import io.pulumi.azurenative.servicebus.outputs.ListTopicKeysResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class ListTopicKeys {
    private ListTopicKeys() {}
    /**
         * Namespace/ServiceBus Connection String
     * API Version: 2017-04-01.
     * 
     *
         * Namespace/ServiceBus Connection String
     * 
     */
    public static CompletableFuture<ListTopicKeysResult> invokeAsync(ListTopicKeysArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:servicebus:listTopicKeys", TypeShape.of(ListTopicKeysResult.class), args == null ? ListTopicKeysArgs.Empty : args, Utilities.withVersion(options));
    }
}