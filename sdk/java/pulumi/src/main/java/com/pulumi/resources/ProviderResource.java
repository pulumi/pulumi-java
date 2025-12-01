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
        this(aPackage, name, args, options, dependency, null);
    }

    /**
     * Creates and registers a new provider resource for a particular package.
     *
     * @param aPackage   The package associated with this provider
     * @param name       The unique name of the provider
     * @param args       The configuration to use for this provider
     * @param options    A bag of options that control this provider's behavior
     * @param dependency True if this is a synthetic resource used internally for dependency tracking
     * @param packageRef The package reference to use for this provider
     */
    protected ProviderResource(String aPackage, String name,
                               ResourceArgs args, @Nullable CustomResourceOptions options, boolean dependency,
                               @Nullable CompletableFuture<String> packageRef) {
        super(providerResourceType(aPackage), name, args, options, dependency, packageRef);
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
        // If we were given providers, wait for them to resolve and construct provider references from them.
        // A provider reference is a well-known string (two ::-separated values) that the engine interprets.
        var providerUrn = Internal.of(this.urn()).getValueOrDefault("");
        var providerId = Internal.of(this.id()).getDataAsync()
                .thenApply(data -> data.filter(String::isBlank).orElse(Constants.UnknownValue));

        return providerUrn.thenCompose(
                pUrn -> providerId.thenApply(
                        pId -> String.format("%s::%s", pUrn, pId)
                ));
    }

    /**
     * Internal implementation class for {@link ProviderResource}
     * responsible for managing the lifecycle, registration, and state of provider resources within the Pulumi engine.
     *
     * @see ProviderResource
     * @see CustomResourceInternal
     * @see com.pulumi.resources.Resource
     */
    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class ProviderResourceInternal extends CustomResourceInternal {

        /**
         * The associated {@link ProviderResource} instance that this internal class manages.
         */
        private final ProviderResource resource;

        /**
         * Constructs a new internal provider resource manager for the given {@link ProviderResource}.
         *
         * @param resource the provider resource to be managed; must not be null
         */
        private ProviderResourceInternal(ProviderResource resource) {
            super(resource);
            this.resource = requireNonNull(resource);
        }

        /**
         * Creates a new {@link ProviderResourceInternal} instance from the given {@link ProviderResource}.
         *
         * @param r the provider resource to wrap; must not be null
         * @return a new internal provider resource instance managing the given provider
         */
        public static ProviderResourceInternal from(ProviderResource r) {
            return new ProviderResourceInternal(r);
        }

        /**
         * Retrieves the registration ID associated with the managed provider resource.
         *
         * @return a future that completes with the registration ID string
         */
        @InternalUse
        public CompletableFuture<String> getRegistrationId() {
            return this.resource.registrationId;
        }

        /**
         * Gets the package name associated with the managed provider resource.
         *
         * @return the provider package name
         */
        @InternalUse
        public String getPackage() {
            return this.resource.aPackage;
        }
    }
}