package io.pulumi.deployment;

import io.grpc.Internal;
import io.pulumi.resources.InvokeArgs;
import io.pulumi.resources.ProviderResource;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * Options to help control the behavior of @see {@link Deployment#invokeAsync(String, io.pulumi.core.internal.Reflection.TypeShape, InvokeArgs, InvokeOptions)}.
 */
public class InvokeOptions {

    public static InvokeOptions Empty = new InvokeOptions();

    @Nullable
    private final Resource parent;
    @Nullable
    private final ProviderResource provider;
    @Nullable
    private final String version;

    public InvokeOptions() {
        this(null, null, null);
    }

    public InvokeOptions(@Nullable Resource parent, @Nullable ProviderResource provider, @Nullable String version) {
        this.parent = parent;
        this.provider = provider;
        this.version = version;
    }

    /**
     * An optional parent to use for default options for this invoke (e.g. the default provider
     * to use).
     */
    public Optional<Resource> getParent() {
        return Optional.ofNullable(parent);
    }

    /*
     * An optional provider to use for this invocation. If no provider is supplied, the default
     * provider for the invoked function's package will be used.
     */
    public Optional<ProviderResource> getProvider() {
        return Optional.ofNullable(this.provider);
    }

    /**
     * An optional version, corresponding to the version of the provider plugin that should be
     * used when performing this invoke.
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    @Internal
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Internal
    public interface Visitor<T> {
        T visit(InvokeOptions options);
    }

    @Internal
    public static class NestedProviderVisitor implements Visitor<Optional<ProviderResource>> {

        private final String token;

        private NestedProviderVisitor(String token) {
            this.token = Objects.requireNonNull(token);
        }

        public static NestedProviderVisitor of(String token) {
            return new NestedProviderVisitor(token);
        }

        @Override
        public Optional<ProviderResource> visit(InvokeOptions options) {
            return options.getProvider().or(
                    () -> options.getParent().map(p -> Resource.internalGetProvider(p, token))
            );
        }
    }
}
