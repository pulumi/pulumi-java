package io.pulumi.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pulumi.context.internal.RuntimeContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Level;

@ParametersAreNonnullByDefault
public class TestRuntimeContext extends RuntimeContext {

    public TestRuntimeContext(
            String projectName,
            String stackName,
            boolean dryRun,
            ImmutableMap<String, String> config,
            ImmutableSet<String> configSecretKeys,
            Level globalLoggingLevel,
            boolean disableResourceReferences,
            boolean excessiveDebugOutput,
            int taskTimeoutInMillis
    ) {
        super(
                "unused",
                "unused",
                projectName,
                stackName,
                dryRun,
                config,
                configSecretKeys,
                globalLoggingLevel,
                disableResourceReferences,
                excessiveDebugOutput,
                taskTimeoutInMillis
        );
    }

    /**
     * Whether the test runs in Preview mode. Defaults to <b>true</b> if not specified.
     */
    public boolean isPreview() {
        return this.isDryRun();
    }

    public static TestRuntimeContextBuilder builder() {
        return new TestRuntimeContextBuilder();
    }
}
