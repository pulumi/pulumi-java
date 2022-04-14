package io.pulumi.context.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pulumi.core.internal.Strings;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Level;

import static io.pulumi.core.internal.Objects.require;
import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class RuntimeContext {

    private final String monitorTarget;
    private final String engineTarget;
    private final String projectName;
    private final String stackName;
    private final boolean dryRun;
    private final ImmutableMap<String, String> config;
    private final ImmutableSet<String> configSecretKeys;
    private final Level globalLogLevel;
    private final boolean disableResourceReferences;
    private final boolean excessiveDebugOutput;
    private final int taskTimeoutInMillis;

    public RuntimeContext(
            String monitorTarget,
            String engineTarget,
            String projectName,
            String stackName,
            boolean dryRun,
            ImmutableMap<String, String> config,
            ImmutableSet<String> configSecretKeys,
            Level globalLogLevel,
            boolean disableResourceReferences,
            boolean excessiveDebugOutput,
            int taskTimeoutInMillis
    ) {
        this.monitorTarget = require(Strings::isNonEmptyOrNull, monitorTarget,
                () -> "expected a monitorTarget, got empty or null"
        );
        this.engineTarget = require(Strings::isNonEmptyOrNull, engineTarget,
                () -> "expected a engineTarget, got empty or null"
        );
        this.projectName = require(Strings::isNonEmptyOrNull, projectName,
                () -> "expected a projectName, got empty or null"
        );
        this.stackName = require(Strings::isNonEmptyOrNull, stackName,
                () -> "expected a stackName, got empty or null"
        );
        this.dryRun = dryRun; // primitive is never null
        this.config = requireNonNull(config);
        this.configSecretKeys = requireNonNull(configSecretKeys);
        this.globalLogLevel = requireNonNull(globalLogLevel);
        this.disableResourceReferences = disableResourceReferences; // primitive is never null
        this.excessiveDebugOutput = excessiveDebugOutput; // primitive is never null
        this.taskTimeoutInMillis = taskTimeoutInMillis; // primitive is never null
    }

    public String monitorTarget() {
        return this.monitorTarget;
    }

    public String engineTarget() {
        return this.engineTarget;
    }

    public String projectName() {
        return this.projectName;
    }

    public String stackName() {
        return this.stackName;
    }

    public boolean isDryRun() {
        return this.dryRun;
    }

    public ImmutableMap<String, String> config() {
        return config;
    }

    public ImmutableSet<String> configSecretKeys() {
        return configSecretKeys;
    }

    public Level globalLogLevel() {
        return globalLogLevel;
    }

    public boolean isDisableResourceReferences() {
        return disableResourceReferences;
    }

    public boolean isExcessiveDebugOutput() {
        return excessiveDebugOutput;
    }

    public int taskTimeoutInMillis() {
        return taskTimeoutInMillis;
    }
}
