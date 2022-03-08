package io.pulumi.deployment;

import io.pulumi.core.Tuples;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Hooks to mock the engine that provide test doubles for offline unit testing of stacks.
 */
public interface Mocks {

    /**
     * Invoked when a new resource is created by the program.
     * <p/>
     *
     * @return A tuple of a resource identifier and resource state. State can be either a POJO
     * or a dictionary bag. The returned ID may be null for component resources.
     */
    CompletableFuture<Tuples.Tuple2<Optional<String> /* id */, Object /* state */>> newResourceAsync(MockResourceArgs args);

    /**
     * Invoked when the program needs to call a provider to load data (e.g., to retrieve an existing resource).
     * <p/>
     *
     * @return Invocation result, can be either a POCO or a dictionary bag.
     */
    CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args);
}
