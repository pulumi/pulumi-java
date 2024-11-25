package com.pulumi.deployment;

import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.ProviderResource;
import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Options to help control the behavior of @see
 * {@link Deployment#invoke(String, TypeShape, InvokeArgs, InvokeOutputOptions)}.
 */
@ParametersAreNonnullByDefault
public class InvokeOutputOptions extends InvokeOptions {

    public static InvokeOutputOptions Empty = new InvokeOutputOptions();

    private final List<Resource> dependsOn;

    public InvokeOutputOptions() {
        this(null, null, null, null);
    }

    public InvokeOutputOptions(@Nullable Resource parent, @Nullable ProviderResource provider, @Nullable String version,
            @Nullable List<Resource> dependsOn) {
        super(parent, provider, version);
        this.dependsOn = dependsOn;
    }

    /**
     * Optional resources that this invoke depends on. The invoke will wait for
     * these
     * resources to be resolved before executing.
     */
    public List<Resource> getDependsOn() {
        return this.dependsOn == null ? List.of() : this.dependsOn;
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class InvokeOutputOptionsInternal {

        private final InvokeOutputOptions options;

        private InvokeOutputOptionsInternal(InvokeOutputOptions options) {
            this.options = requireNonNull(options);
        }

        public static InvokeOutputOptionsInternal from(InvokeOutputOptions options) {
            return new InvokeOutputOptionsInternal(options);
        }

        public Optional<ProviderResource> getNestedProvider(String token) {
            return this.options.getProvider().or(
                    () -> this.options.getParent()
                            .flatMap(p -> Internal.from(p).getProvider(token)));
        }
    }
}
