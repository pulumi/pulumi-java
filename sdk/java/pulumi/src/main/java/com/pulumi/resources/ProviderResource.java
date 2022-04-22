package com.pulumi.resources;

import com.pulumi.core.internal.Constants;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

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
        var providerUrn = Internal.of(this.getUrn()).getValueOrDefault("");
        var providerId = Internal.of(this.getId()).getDataAsync()
                .thenApply(data -> data.filter(String::isBlank).orElse(Constants.UnknownValue));

        return providerUrn.thenCompose(
                pUrn -> providerId.thenApply(
                        pId -> String.format("%s::%s", pUrn, pId)
                ));
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class ProviderResourceInternal extends CustomResourceInternal {

        private final ProviderResource resource;

        private ProviderResourceInternal(ProviderResource resource) {
            super(resource);
            this.resource = requireNonNull(resource);
        }

        public static ProviderResourceInternal from(ProviderResource r) {
            return new ProviderResourceInternal(r);
        }

        @InternalUse
        public CompletableFuture<String> getRegistrationId() {
            return this.resource.registrationId;
        }

        @InternalUse
        public String getPackage() {
            return this.resource.aPackage;
        }
    }
}
