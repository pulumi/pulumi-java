package com.pulumi.test;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * Hooks to mock the engine that provide test doubles for offline unit testing of stacks.
 */
public interface Mocks {

    /**
     * Invoked when a new resource is created by the program.
     * <p>
     *
     * @param args arguments containing resource information
     * @return A resource identifier and resource state. State can be either a POJO
     * or a dictionary bag. The returned ID may be null for component resources.
     */
    CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args);

    /**
     * Invoked when the program needs to call a provider to load data (e.g., to retrieve an existing resource).
     * <p>
     * Default implementation returns an empty map.
     *
     * @param args arguments containing call information
     * @return the call invocation result, can be either a POCO or a dictionary bag.
     */
    default CompletableFuture<Map<String, Object>> callAsync(CallArgs args) {
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * ResourceArgs for use in {@link #newResourceAsync(ResourceArgs)}
     */
    class ResourceArgs {

        /**
         * Resource type name
         */
        @Nullable
        public final String type;

        /**
         * Resource name
         */
        @Nullable
        public final String name;

        /**
         * Resource input properties
         */
        @Nullable
        public final Map<String, Object> inputs;

        /**
         * A provider name
         */
        @Nullable
        public final String provider;

        /**
         * Resource identifier
         */
        @Nullable
        public final String id;

        /**
         * A mock resource arguments
         *
         * @param type     resource type name
         * @param name     resource name
         * @param inputs   resource input properties
         * @param provider a provider name
         * @param id       a resource identifier
         */
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
    class ResourceResult {
        /**
         * A resource identifier
         */
        public final Optional<String> id;
        /**
         * A resource state
         */
        public final Object state;

        /**
         * @param id    the ID may be empty for component resources
         * @param state the state can be either a POJO or a Map
         */
        public ResourceResult(Optional<String> id, Object state) {
            this.id = requireNonNull(id);
            this.state = requireNonNull(state);
        }

        public static ResourceResult of(Optional<String> id, Object state) {
            return new ResourceResult(id, state);
        }
    }

    /**
     * MockCallArgs for use in CallAsync
     */
    class CallArgs {

        /**
         * Resource identifier
         */
        @Nullable
        public final String token;

        /**
         * Call arguments
         */
        @Nullable
        public final ImmutableMap<String, Object> args;

        /**
         * Provider name
         */
        @Nullable
        public final String provider;

        /**
         * A mock call arguments.
         *
         * @param token    a resource identifier
         * @param args     call arguments
         * @param provider a provider name
         */
        public CallArgs(@Nullable String token, @Nullable ImmutableMap<String, Object> args, @Nullable String provider) {
            this.token = token;
            this.args = args;
            this.provider = provider;
        }
    }

}
