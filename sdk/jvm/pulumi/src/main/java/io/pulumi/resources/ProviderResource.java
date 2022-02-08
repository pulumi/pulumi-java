package io.pulumi.resources;

import io.grpc.Internal;
import io.pulumi.core.internal.Constants;
import io.pulumi.core.internal.TypedInputOutput;

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
    @Internal
    private CompletableFuture<String> registrationIdAsync() {
        var providerUrn = TypedInputOutput.cast(this.getUrn())
                .view(data -> data.getValueOrDefault(""));
        var providerId = TypedInputOutput.cast(this.getId())
                .view(data -> data.filter(String::isBlank).orElse(Constants.UnknownValue));

        return providerUrn.thenCompose(
                pUrn -> providerId.thenApply(
                        pId -> String.format("%s::%s", pUrn, pId)
                ));
    }

    @Internal
    public <T> T accept(ProviderResource.Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Internal
    public interface Visitor<T> {
        T visit(ProviderResource providerResource);
    }

    @Internal
    public static RegistrationIdVisitor registrationIdVisitor() {
        return new RegistrationIdVisitor();
    }

    @Internal
    public static PackageVisitor packageVisitor() {
        return new PackageVisitor();
    }

    @Internal
    public static class RegistrationIdVisitor implements Visitor<CompletableFuture<String>> {
        @Override
        public CompletableFuture<String> visit(ProviderResource providerResource) {
            return providerResource.registrationId;
        }
    }

    @Internal
    public static class PackageVisitor implements Visitor<String> {
        @Override
        public String visit(ProviderResource providerResource) {
            return providerResource.aPackage;
        }
    }
}
