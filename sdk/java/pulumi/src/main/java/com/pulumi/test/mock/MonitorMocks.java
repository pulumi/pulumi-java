package com.pulumi.test.mock;

import com.pulumi.core.Tuples;
import com.pulumi.deployment.MockCallArgs;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * Hooks to mock the engine that provide test doubles for offline unit testing of stacks.
 */
public interface MonitorMocks {

    /**
     * Invoked when a new resource is created by the program.
     * <p>
     *
     * @return A tuple of a resource identifier and resource state. State can be either a POJO
     * or a dictionary bag. The returned ID may be null for component resources.
     */
    CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args);

    /**
     * Invoked when the program needs to call a provider to load data (e.g., to retrieve an existing resource).
     * <p>
     *
     * @return Invocation result, can be either a POCO or a dictionary bag.
     */
    CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args);


    /**
     * ResourceArgs for use in {@link #newResourceAsync(ResourceArgs)}
     */
    final class ResourceArgs {

        /**
         * Resource type name.
         */
        @Nullable
        public final String type;

        /**
         * Resource name.
         */
        @Nullable
        public final String name;

        /**
         * Dictionary of resource input properties.
         */
        @Nullable
        public final Map<String, Object> inputs;

        @Nullable
        public final String provider;

        /**
         * Resource identifier.
         */
        @Nullable
        public final String id;

        public ResourceArgs(
                @Nullable String type,
                @Nullable String name,
                @Nullable Map<String, Object> inputs,
                @Nullable String provider,
                @Nullable String id
        ) {
            this.type = type;
            this.name = name;
            this.inputs = inputs;
            this.provider = provider;
            this.id = id;
        }
    }

    /**
     * A resource identifier and resource state.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    final class ResourceResult {
        public final Optional<String> id;
        public final Object state;

        /**
         * @param id    the ID may be empty for component resources
         * @param state the state can be either a POJO or a Map
         */
        public ResourceResult(Optional<String> id, Object state) {
            this.id = requireNonNull(id);
            this.state = requireNonNull(state);
        }
    }
}
