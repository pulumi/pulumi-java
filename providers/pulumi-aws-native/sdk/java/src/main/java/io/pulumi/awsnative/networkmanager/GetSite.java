// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.networkmanager;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.networkmanager.inputs.GetSiteArgs;
import io.pulumi.awsnative.networkmanager.outputs.GetSiteResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSite {
    private GetSite() {}
    /**
         * The AWS::NetworkManager::Site type describes a site.
     * 
     */
    public static CompletableFuture<GetSiteResult> invokeAsync(GetSiteArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:networkmanager:getSite", TypeShape.of(GetSiteResult.class), args == null ? GetSiteArgs.Empty : args, Utilities.withVersion(options));
    }
}