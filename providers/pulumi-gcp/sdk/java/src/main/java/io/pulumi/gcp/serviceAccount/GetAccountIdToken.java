// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.gcp.serviceAccount;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.gcp.Utilities;
import io.pulumi.gcp.serviceAccount.inputs.GetAccountIdTokenArgs;
import io.pulumi.gcp.serviceAccount.outputs.GetAccountIdTokenResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetAccountIdToken {
    private GetAccountIdToken() {}
    /**
         * This data source provides a Google OpenID Connect (`oidc`) `id_token`.  Tokens issued from this data source are typically used to call external services that accept OIDC tokens for authentication (e.g. [Google Cloud Run](https://cloud.google.com/run/docs/authenticating/service-to-service)).
     * 
     * For more information see
     * [OpenID Connect](https://openid.net/specs/openid-connect-core-1_0.html#IDToken).
     * 
     * ## Example Usage
     * 
     * ### ServiceAccount JSON Credential File.
     *   `gcp.serviceAccount.getAccountIdToken` will use the configured provider credentials
     * 
     * ### Service Account Impersonation.
     *   `gcp.serviceAccount.getAccountAccessToken` will use background impersonated credentials provided by `gcp.serviceAccount.getAccountAccessToken`.
     * 
     *   Note: to use the following, you must grant `target_service_account` the
     *   `roles/iam.serviceAccountTokenCreator` role on itself.
     * 
     *
         * A collection of arguments for invoking getAccountIdToken.
     * 
     *
         * A collection of values returned by getAccountIdToken.
     * 
     */
    public static CompletableFuture<GetAccountIdTokenResult> invokeAsync(GetAccountIdTokenArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("gcp:serviceAccount/getAccountIdToken:getAccountIdToken", TypeShape.of(GetAccountIdTokenResult.class), args == null ? GetAccountIdTokenArgs.Empty : args, Utilities.withVersion(options));
    }
}