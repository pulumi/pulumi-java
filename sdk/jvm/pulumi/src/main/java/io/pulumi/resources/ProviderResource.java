package io.pulumi.resources;

import io.pulumi.core.internal.Constants;
import io.pulumi.core.internal.Internal.Field;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

/**
 * A @see {@link Resource} that implements CRUD operations
 * for other custom resources. These resources are managed similarly to other resources,
 * including the usual diffing and update semantics.
 */
@ParametersAreNonnullByDefault
public class ProviderResource extends CustomResource {

    private static final String ProviderResourceTypePrefix = "pulumi:providers:";
    private final String aPackage;
    private final CompletableFuture<String> registrationId;

    @SuppressWarnings("unused")
    @Field
    private final Internal internal = new Internal();

    /**
     * Creates and registers a new provider resource for a particular package.
     *
     * @param aPackage The package associated with this provider
     * @param name     The unique name of the provider
     * @param args     The configuration to use for this provider
     * @param options  A bag of options that control this provider's behavior
     */
    public ProviderResource(String aPackage, String name, ResourceArgs args, @Nullable CustomResourceOptions options) {
        this(aPackage, name, args, options, false);
    }

    /**
     * Creates and registers a new provider resource for a particular package.
     *
     * @param aPackage   The package associated with this provider
     * @param name       The unique name of the provider
     * @param args       The configuration to use for this provider
     * @param options    A bag of options that control this provider's behavior
     * @param dependency True if this is a synthetic resource used internally for dependency tracking
     */
    protected ProviderResource(String aPackage, String name,
                               ResourceArgs args, @Nullable CustomResourceOptions options, boolean dependency) {
        super(providerResourceType(aPackage), name, args, options, dependency);
        this.aPackage = aPackage;
        this.registrationId = registrationIdAsync();
    }

    private static String providerResourceType(String aPackage) {
        return ProviderResourceTypePrefix + aPackage;
    }

    /**
     * Fields urn and id can be set late, with reflection, so we need lazy init here
     */
    private CompletableFuture<String> registrationIdAsync() {
        var providerUrn = io.pulumi.core.internal.Internal.of(this.getUrn()).getValueOrDefault("");
        var providerId = io.pulumi.core.internal.Internal.of(this.getId()).getDataAsync()
                .thenApply(data -> data.filter(String::isBlank).orElse(Constants.UnknownValue));

        return providerUrn.thenCompose(
                pUrn -> providerId.thenApply(
                        pId -> String.format("%s::%s", pUrn, pId)
                ));
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public final class Internal {

        private Internal() {
            /* Empty */
        }

        @InternalUse
        public CompletableFuture<String> getRegistrationId() {
            return ProviderResource.this.registrationId;
        }

        @InternalUse
        public String getPackage() {
            return ProviderResource.this.aPackage;
        }
    }
}
