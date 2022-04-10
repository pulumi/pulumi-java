// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.apimanagement;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.apimanagement.inputs.ListNamedValueArgs;
import io.pulumi.azurenative.apimanagement.outputs.ListNamedValueResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class ListNamedValue {
    private ListNamedValue() {}
    /**
         * Client or app secret used in IdentityProviders, Aad, OpenID or OAuth.
     * API Version: 2020-12-01.
     * 
     *
         * Client or app secret used in IdentityProviders, Aad, OpenID or OAuth.
     * 
     */
    public static CompletableFuture<ListNamedValueResult> invokeAsync(ListNamedValueArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:apimanagement:listNamedValue", TypeShape.of(ListNamedValueResult.class), args == null ? ListNamedValueArgs.Empty : args, Utilities.withVersion(options));
    }
}