// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.orbital;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.orbital.inputs.ListSpacecraftAvailableContactsArgs;
import io.pulumi.azurenative.orbital.outputs.ListSpacecraftAvailableContactsResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class ListSpacecraftAvailableContacts {
    private ListSpacecraftAvailableContacts() {}
    /**
         * Response for the ListAvailableContacts API service call.
     * API Version: 2021-04-04-preview.
     * 
     *
         * Response for the ListAvailableContacts API service call.
     * 
     */
    public static CompletableFuture<ListSpacecraftAvailableContactsResult> invokeAsync(ListSpacecraftAvailableContactsArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:orbital:listSpacecraftAvailableContacts", TypeShape.of(ListSpacecraftAvailableContactsResult.class), args == null ? ListSpacecraftAvailableContactsArgs.Empty : args, Utilities.withVersion(options));
    }
}