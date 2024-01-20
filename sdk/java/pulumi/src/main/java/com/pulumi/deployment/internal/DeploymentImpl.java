package com.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.CompletableFutures;
import com.pulumi.core.internal.Constants;
import com.pulumi.core.internal.Environment;
import com.pulumi.core.internal.GlobalLogging;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.Maps;
import com.pulumi.core.internal.OutputCompletionSource;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.OutputInternal;
import com.pulumi.core.internal.Strings;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.CallOptions;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.InvokeOptions;
import com.pulumi.exceptions.LogException;
import com.pulumi.exceptions.ResourceException;
import com.pulumi.exceptions.RunException;
import com.pulumi.resources.CallArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.CustomTimeouts;
import com.pulumi.resources.DependencyResource;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.ProviderResource;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.ResourceOptions;
import com.pulumi.resources.internal.Stack;
import com.pulumi.serialization.internal.Converter;
import com.pulumi.serialization.internal.Deserializer;
import com.pulumi.serialization.internal.JsonFormatter;
import com.pulumi.serialization.internal.PropertiesSerializer;
import com.pulumi.serialization.internal.PropertiesSerializer.SerializationResult;
import com.pulumi.serialization.internal.Structs;
import pulumirpc.AliasOuterClass.Alias;
import pulumirpc.EngineOuterClass;
import pulumirpc.EngineOuterClass.LogRequest;
import pulumirpc.EngineOuterClass.LogSeverity;
import pulumirpc.Provider.CallRequest;
import pulumirpc.Resource.ReadResourceRequest;
import pulumirpc.Resource.RegisterResourceOutputsRequest;
import pulumirpc.Resource.RegisterResourceRequest;
import pulumirpc.Resource.SupportsFeatureRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.pulumi.core.internal.Environment.getBooleanEnvironmentVariable;
import static com.pulumi.core.internal.Environment.getEnvironmentVariable;
import static com.pulumi.core.internal.Exceptions.getStackTrace;
import static com.pulumi.core.internal.Strings.isNonEmptyOrNull;
import static com.pulumi.resources.internal.Stack.RootPulumiStackTypeName;
import static java.util.stream.Collectors.toMap;

@InternalUse
public class DeploymentImpl extends DeploymentInstanceHolder implements Deployment, DeploymentInternal {

    private final DeploymentState state;
    private final Log log;
    private final FeatureSupport featureSupport;
    private final PropertiesSerializer serialization;
    private final Deserializer deserializer;
    private final Converter converter;
    private final Invoke invoke;
    private final Call call;
    private final Prepare prepare;
    private final ReadOrRegisterResource readOrRegisterResource;
    private final ReadResource readResource;
    private final RegisterResource registerResource;
    private final RegisterResourceOutputs registerResourceOutputs;
    private final RootResource rootResource;

    @InternalUse
    @VisibleForTesting
    public DeploymentImpl(
            DeploymentState state
    ) {
        this.state = Objects.requireNonNull(state);
        this.log = new Log(state.logger, DeploymentState.ExcessiveDebugOutput);
        this.featureSupport = new FeatureSupport(state.monitor);
        this.serialization = new PropertiesSerializer(this.log);
        this.deserializer = new Deserializer(this.log);
        this.converter = new Converter(this.log, this.deserializer);
        this.invoke = new Invoke(
                this.log, state.monitor, this.featureSupport, this.serialization, this.converter,
                DeploymentState.DisableResourceReferences
        );
        this.rootResource = new RootResource(state.engine);
        this.prepare = new Prepare(this.log, this.featureSupport, this.rootResource, this.serialization);
        this.call = new Call(this.log, state.monitor, this.prepare, this.serialization, this.converter);
        this.readResource = new ReadResource(
                this.log, this.prepare, state.monitor,
                DeploymentState.DisableResourceReferences
        );
        this.registerResource = new RegisterResource(
                this.log, this.prepare, state.monitor,
                DeploymentState.DisableResourceReferences
        );
        this.readOrRegisterResource = new ReadOrRegisterResourceInternal(
                this.log, state.runner, this.invoke, this.readResource,
                this.registerResource, this.converter, state.isDryRun
        );
        this.registerResourceOutputs = new RegisterResourceOutputsInternal(
                this.log, state.runner, state.monitor, this.featureSupport, this.serialization
        );
    }

    @InternalUse
    @VisibleForTesting
    public static DeploymentImpl fromEnvironment() {
        var state = DeploymentState.fromEnvironment();
        var impl = new DeploymentImpl(state);
        DeploymentInstanceHolder.setInstance(new DeploymentInstanceInternal(impl));
        return impl;
    }

    @Override
    @Nonnull
    public String getStackName() {
        return this.state.stackName;
    }

    @Override
    @Nonnull
    public String getProjectName() {
        return this.state.projectName;
    }

    @Override
    public boolean isDryRun() {
        return this.state.isDryRun;
    }

    @Override
    @InternalUse
    public Runner getRunner() {
        return this.state.runner;
    }

    public Log getLog() {
        return this.log;
    }

    @Override
    @InternalUse
    public Config getConfig() {
        return this.state.config;
    }

    @Override
    public Optional<String> getConfig(String fullKey) {
        return this.state.config.getConfig(fullKey);
    }

    @Override
    public boolean isConfigSecret(String fullKey) {
        return this.state.config.isConfigSecret(fullKey);
    }

    @Nullable
    private Stack stack;

    @InternalUse
    @Override
    public Stack getStack() {
        if (this.stack == null) {
            throw new IllegalStateException("Trying to acquire Deployment#getStack before 'run' was called.");
        }
        return this.stack;
    }

    // TODO: remove when refactoring deployment initialization
    @InternalUse
    @Override
    public void setStack(Stack stack) {
        Objects.requireNonNull(stack);
        this.stack = stack;
    }

    private final static class FeatureSupport {

        private final Monitor monitor;
        private final Map<String, Boolean> featureSupport;

        private FeatureSupport(Monitor monitor) {
            this.monitor = Objects.requireNonNull(monitor);
            this.featureSupport = Collections.synchronizedMap(new HashMap<>());
        }

        @InternalUse
        private CompletableFuture<Boolean> monitorSupportsFeature(String feature) {
            if (!this.featureSupport.containsKey(feature)) {
                var request = SupportsFeatureRequest.newBuilder().setId(feature).build();
                return this.monitor.supportsFeatureAsync(request)
                        .thenApply(r -> {
                            var hasSupport = r.getHasSupport();
                            this.featureSupport.put(feature, hasSupport);
                            return hasSupport;
                        });
            }
            return CompletableFuture.completedFuture(this.featureSupport.get(feature));
        }

        @InternalUse
        CompletableFuture<Boolean> monitorSupportsResourceReferences() {
            return monitorSupportsFeature("resourceReferences");
        }
    }

    @ParametersAreNonnullByDefault
    @InternalUse
    public static class Config {

        /**
         * The environment variable key that the language plugin uses to set configuration values.
         */
        private static final String ConfigEnvKey = "PULUMI_CONFIG";

        /**
         * The environment variable key that the language plugin uses to set the list of secret configuration keys.
         */
        private static final String ConfigSecretKeysEnvKey = "PULUMI_CONFIG_SECRET_KEYS";

        private ImmutableMap<String, String> allConfig;

        private ImmutableSet<String> configSecretKeys;

        @VisibleForTesting
        public Config(ImmutableMap<String, String> allConfig, ImmutableSet<String> configSecretKeys) {
            this.allConfig = Objects.requireNonNull(allConfig);
            this.configSecretKeys = Objects.requireNonNull(configSecretKeys);
        }

        public static Config parse() {
            return new Config(parseConfig(), parseConfigSecretKeys());
        }

        /**
         * Returns a copy of the full config map.
         */
        @InternalUse
        private ImmutableMap<String, String> getAllConfig() {
            return allConfig;
        }

        /**
         * Returns a copy of the config secret keys.
         */
        @InternalUse
        private ImmutableSet<String> configSecretKeys() {
            return configSecretKeys;
        }

        /**
         * Sets a configuration variable.
         */
        @InternalUse
        @VisibleForTesting
        void setConfig(String key, String value) { // TODO: can the setter be avoided?
            this.allConfig = new ImmutableMap.Builder<String, String>()
                    .putAll(this.allConfig)
                    .put(key, value)
                    .build();
        }

        /**
         * Appends all provided configuration.
         */
        @InternalUse
        @VisibleForTesting
        void setAllConfig(ImmutableMap<String, String> config, @Nullable Iterable<String> secretKeys) { // TODO: can the setter be avoided?
            this.allConfig = new ImmutableMap.Builder<String, String>()
                    .putAll(this.allConfig)
                    .putAll(config)
                    .build();
            if (secretKeys != null) {
                this.configSecretKeys = new ImmutableSet.Builder<String>()
                        .addAll(this.configSecretKeys)
                        .addAll(secretKeys)
                        .build();
            }
        }

        public Optional<String> getConfig(String fullKey) {
            return Optional.ofNullable(this.allConfig.getOrDefault(fullKey, null));
        }

        public boolean isConfigSecret(String fullKey) {
            return this.configSecretKeys.contains(fullKey);
        }

        private static ImmutableMap<String, String> parseConfig() {
            var envConfig = Environment.getEnvironmentVariable(ConfigEnvKey);
            if (envConfig.isValue()) {
                return parseConfig(envConfig.value());
            }
            return ImmutableMap.of();
        }

        @InternalUse
        @VisibleForTesting
        public static ImmutableMap<String, String> parseConfig(String envConfigJson) {
            var parsedConfig = ImmutableMap.<String, String>builder();

            var gson = new Gson();
            var envObject = gson.fromJson(envConfigJson, JsonElement.class);
            for (var prop : envObject.getAsJsonObject().entrySet()) {
                parsedConfig.put(cleanKey(prop.getKey()), prop.getValue().getAsString());
            }

            return parsedConfig.build();
        }

        private static ImmutableSet<String> parseConfigSecretKeys() {
            var envConfigSecretKeys = Environment.getEnvironmentVariable(ConfigSecretKeysEnvKey);
            if (envConfigSecretKeys.isValue()) {
                return parseConfigSecretKeys(envConfigSecretKeys.value());
            }

            return ImmutableSet.of();
        }

        @InternalUse
        @VisibleForTesting
        public static ImmutableSet<String> parseConfigSecretKeys(String envConfigSecretKeysJson) {
            var parsedConfigSecretKeys = ImmutableSet.<String>builder();

            var gson = new Gson();
            var envObject = gson.fromJson(envConfigSecretKeysJson, JsonElement.class);
            for (var element : envObject.getAsJsonArray()) {
                parsedConfigSecretKeys.add(element.getAsString());
            }

            return parsedConfigSecretKeys.build();
        }

        /**
         * CleanKey takes a configuration key, and if it is of the form "(string):config:(string)"
         * removes the ":config:" portion. Previously, our keys always had the string ":config:" in
         * them, and we'd like to remove it. However, the language host needs to continue to set it
         * so we can be compatible with older versions of our packages. Once we stop supporting
         * older packages, we can change the language host to not add this :config: thing and
         * remove this function.
         */
        private static String cleanKey(String key) {
            final var prefix = "config:";
            var idx = key.indexOf(":");
            if (idx > 0 && key.substring(idx + 1).startsWith(prefix)) {
                return key.substring(0, idx) + ":" + key.substring(idx + 1 + prefix.length());
            }
            return key;
        }
    }

    @Override
    public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args) {
        return this.invoke.invoke(token, targetType, args, InvokeOptions.Empty);
    }

    @Override
    public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
        return this.invoke.invoke(token, targetType, args, options);
    }

    @Override
    public CompletableFuture<Void> invokeAsync(String token, InvokeArgs args, InvokeOptions options) {
        return this.invoke.invokeAsync(token, args, options);
    }

    @Override
    public CompletableFuture<Void> invokeAsync(String token, InvokeArgs args) {
        return this.invoke.invokeAsync(token, args);
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
        return this.invoke.invokeAsync(token, targetType, args, options);
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args) {
        return this.invoke.invokeAsync(token, targetType, args);
    }

    @ParametersAreNonnullByDefault
    private final static class Invoke {

        private final Log log;
        private final Monitor monitor;
        private final FeatureSupport featureSupport;
        private final PropertiesSerializer serialization;
        private final Converter converter;
        private final boolean disableResourceReferences;

        private Invoke(
                Log log,
                Monitor monitor,
                FeatureSupport featureSupport,
                PropertiesSerializer serialization,
                Converter converter,
                boolean disableResourceReferences
        ) {
            this.log = Objects.requireNonNull(log);
            this.monitor = Objects.requireNonNull(monitor);
            this.featureSupport = Objects.requireNonNull(featureSupport);
            this.serialization = Objects.requireNonNull(serialization);
            this.converter = Objects.requireNonNull(converter);
            this.disableResourceReferences = disableResourceReferences;
        }

        public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args) {
            return invoke(token, targetType, args, InvokeOptions.Empty);
        }

        public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
            Objects.requireNonNull(token);
            Objects.requireNonNull(targetType);
            Objects.requireNonNull(args);
            Objects.requireNonNull(options);

            log.debug(String.format("Invoking function: token='%s' asynchronously", token));

            // Wait for all values to be available, and then perform the RPC.
            return new OutputInternal<>(this.featureSupport.monitorSupportsResourceReferences()
                    .thenCompose(keepResources -> this.serializeInvokeArgs(token, args, keepResources))
                    .thenCompose(serializedArgs -> {
                        if (!serializedArgs.containsUnknowns) {
                            return this.invokeRawAsync(token, serializedArgs, options)
                                    .thenApply(result -> parseInvokeResponse(token, targetType, result));
                        } else {
                            return CompletableFuture.completedFuture(OutputData.unknown());
                        }
                    }));
        }

        private <T> OutputData<T> parseInvokeResponse(
                String token, TypeShape<T> targetType, SerializationResult result) {
            return this.converter.convertValue(
                    String.format("%s result", token),
                    Value.newBuilder()
                            .setStructValue(result.serialized)
                            .build(),
                    targetType,
                    result.propertyToDependentResources.values().stream()
                            .flatMap(Collection::stream)
                            .collect(toImmutableSet()));
        }

        private <T> CompletableFuture<OutputData<T>> rawInvoke(
                String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
            Objects.requireNonNull(token);
            Objects.requireNonNull(targetType);
            Objects.requireNonNull(args);
            Objects.requireNonNull(options);

            // This method backs all calls that generate
            // `Output<T>` and may include `Input<T>` values in the
            // `args`. It needs to decide which control-flow tracking
            // features are supported in the SDK and which ones in the
            // provider implementing the invoke logic.
            //
            // Current choices are:
            //
            // - any resource dependency found by a recursive
            //   traversal of `args` that awaits and inspects every
            //   `Input<T>` will always be propagated into the
            //   `Output<T>`; the provider cannot "swallow"
            //   dependencies
            //
            // - the provider is responsible for deciding whether the
            //   `Output<T>` is secret and known, and may add
            //   additional dependencies
            //
            // This means that presence of secrets or unknowns in the
            // `args` does not guarantee the result is secret or
            // unknown, which differs from Pulumi SDKs that choose to
            // implement these invokes via `apply` (currently Go and
            // Python) and is the same as C# SDK.
            //
            // Differences from `call`: the `invoke` gRPC protocol
            // does not yet support passing or returning out-of-band
            // dependencies to the provider, and in-band `Resource`
            // value support is subject to feature negotiation (see
            // `monitorSupportsResourceReferences`). So `call` makes
            // the provider fully responsible for dependency
            // tracking, which is a good future direction also for
            // `invoke`.

            return invokeRawAsync(token, args, options)
                    .thenApply(result -> parseInvokeResponse(token, targetType, result));
        }

        public CompletableFuture<Void> invokeAsync(String token, InvokeArgs args) {
            return invokeAsync(token, args, InvokeOptions.Empty);
        }

        public CompletableFuture<Void> invokeAsync(String token, InvokeArgs args, InvokeOptions options) {
            return invokeRawAsync(token, args, options).thenApply(unused -> null);
        }

        public <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args) {
            return invokeAsync(token, targetType, args, InvokeOptions.Empty);
        }

        public <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
            return this.rawInvoke(token, targetType, args, options).thenApply(OutputData::getValueNullable);
        }

        private CompletableFuture<SerializationResult> invokeRawAsync(
                String token, InvokeArgs args, InvokeOptions options) {
            Objects.requireNonNull(token);
            Objects.requireNonNull(args);
            Objects.requireNonNull(options);
            log.debug(String.format("Invoking function: token='%s' asynchronously", token));
            // Wait for all values to be available, and then perform the RPC.
            return this.featureSupport.monitorSupportsResourceReferences()
                    .thenCompose(keepResources -> this.serializeInvokeArgs(token, args, keepResources))
                    .thenCompose(serializedArgs -> this.invokeRawAsync(token, serializedArgs, options));
        }

        private CompletableFuture<SerializationResult> invokeRawAsync(
                String token, SerializationResult invokeArgs, InvokeOptions options) {
            CompletableFuture<Optional<String>> providerFuture = CompletableFutures.flipOptional(
                    () -> {
                        var provider = Internal.from(options).getNestedProvider(token);
                        return provider.map(p -> Internal.from(p).getRegistrationId());
                    }
            );
            return providerFuture.thenCompose(provider -> {
                var version = options.getVersion();
                log.debugOrExcessive(
                        String.format("Invoke RPC prepared: token='%s'", token),
                        String.format(", obj='%s'", invokeArgs)
                );
                return this.monitor.invokeAsync(pulumirpc.Resource.ResourceInvokeRequest.newBuilder()
                        .setTok(token)
                        .setProvider(provider.orElse(""))
                        .setVersion(version.orElse(""))
                        .setArgs(invokeArgs.serialized)
                        .setAcceptResources(!this.disableResourceReferences)
                        .build()
                ).thenApply(response -> {
                    // Handle failures.
                    if (response.getFailuresCount() > 0) {
                        var reasons = response.getFailuresList().stream()
                                .map(reason -> String.format("%s (%s)", reason.getReason(), reason.getProperty()))
                                .collect(Collectors.joining("; "));
                        throw new InvokeException(String.format("Invoke of '%s' failed: %s", token, reasons));
                    }
                    return new SerializationResult(response.getReturn(),
                            invokeArgs.propertyToDependentResources);
                });
            });
        }

        private CompletableFuture<SerializationResult> serializeInvokeArgs(
                String token, InvokeArgs args, boolean keepResources) {
            return Internal.from(args).toMapAsync(this.log).thenCompose(argsDict ->
                    serialization.serializeFilteredPropertiesAsync(
                            String.format("invoke:%s", token), argsDict, ignore -> true,
                            keepResources));
        }
    }

    private static class InvokeException extends RuntimeException {
        public InvokeException(String message) {
            super(message);
        }
    }

    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, CallOptions options) {
        return this.call.call(token, targetType, args, self, options);
    }

    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self) {
        return this.call.call(token, targetType, args, self);
    }

    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args) {
        return this.call.call(token, targetType, args);
    }

    public void call(String token, CallArgs args, @Nullable Resource self, CallOptions options) {
        this.call.call(token, args, self, options);
    }

    public void call(String token, CallArgs args, @Nullable Resource self) {
        this.call.call(token, args, self);
    }

    public void call(String token, CallArgs args) {
        this.call.call(token, args);
    }

    @ParametersAreNonnullByDefault
    private final static class Call {

        private final Log log;
        private final Monitor monitor;
        private final Prepare prepare;
        private final PropertiesSerializer serialization;
        private final Converter converter;

        public Call(
                Log log,
                Monitor monitor,
                Prepare prepare,
                PropertiesSerializer serialization,
                Converter converter
        ) {
            this.log = Objects.requireNonNull(log);
            this.monitor = Objects.requireNonNull(monitor);
            this.prepare = Objects.requireNonNull(prepare);
            this.serialization = Objects.requireNonNull(serialization);
            this.converter = Objects.requireNonNull(converter);
        }

        void call(String token, CallArgs args) {
            call(token, args, null, CallOptions.Empty);
        }

        void call(String token, CallArgs args, @Nullable Resource self) {
            call(token, args, self, CallOptions.Empty);
        }

        void call(String token, CallArgs args, @Nullable Resource self, CallOptions options) {
            new OutputInternal<>(callRawAsync(token, args, self, options).thenApply(unused -> null));
        }

        <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args) {
            return call(token, targetType, args, null, CallOptions.Empty);
        }

        <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self) {
            return call(token, targetType, args, self, CallOptions.Empty);
        }

        <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, CallOptions options) {
            return new OutputInternal<>(callAsync(token, targetType, args, self, options));
        }

        private <T> CompletableFuture<OutputData<T>> callAsync(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, CallOptions options) {
            Objects.requireNonNull(token);
            Objects.requireNonNull(targetType);
            Objects.requireNonNull(args);
            Objects.requireNonNull(options);

            return callRawAsync(token, args, self, options).thenApply(
                    r -> this.converter.convertValue(
                            String.format("%s result", token),
                            Value.newBuilder()
                                    .setStructValue(r.result)
                                    .build(),
                            targetType,
                            r.dependencies
                    )
            );
        }

        private CompletableFuture<CallRawAsyncResult> callRawAsync(
                String token, CallArgs args, @Nullable Resource self, CallOptions options) {
            Objects.requireNonNull(token);
            Objects.requireNonNull(args);
            Objects.requireNonNull(options);

            log.debug(String.format("Calling function: token='%s' asynchronously", token));

            // Wait for all values to be available, and then perform the RPC.
            var serializedFuture = Internal.from(args).toMapAsync(this.log)
                    .thenApply(argsDict -> self == null
                            ? argsDict
                            : ImmutableMap.<String, Output<?>>builder()
                            .putAll(argsDict)
                            .put("__self__", Output.of(self))
                            .build()
                    )
                    .thenCompose(
                            argsDict -> serialization.serializeFilteredPropertiesAsync(
                                    String.format("call:%s", token), argsDict, ignore -> true, true)
                    );


            // Determine the provider and version to use.
            var provider = self == null ? Internal.from(options).getNestedProvider(token) : Internal.from(self).getProvider();
            var version = self == null ? options.getVersion() : Internal.from(self).getVersion();

            CompletableFuture<Optional<String>> providerFuture = CompletableFutures.flipOptional(
                    () -> provider.map(p -> Internal.from(p).getRegistrationId())
            );

            return CompletableFuture.allOf(serializedFuture, providerFuture)
                    .thenCompose(unused -> {
                        var serialized = serializedFuture.join();
                        var providerReference = providerFuture.join();

                        // Add arg dependencies to the request.
                        CompletableFuture<Map<String, CallRequest.ArgumentDependencies>> argDependencies = CompletableFutures.allOf(
                                serialized.propertyToDependentResources.entrySet().stream().collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> {
                                            var directDependencies = ImmutableSet.copyOf(e.getValue());
                                            return prepare.getAllTransitivelyReferencedResourceUrnsAsync(directDependencies)
                                                    .thenApply(
                                                            urns -> CallRequest.ArgumentDependencies.newBuilder()
                                                                    .addAllUrns(urns)
                                                                    .build()
                                                    );
                                        }
                                ))
                        );

                        log.debugOrExcessive(
                                String.format("Call RPC prepared: token='%s'", token),
                                String.format(", obj='%s'", serialized)
                        );

                        // Kick off the call.
                        return argDependencies.thenCompose(deps ->
                                this.monitor.callAsync(CallRequest.newBuilder()
                                        .setTok(token)
                                        .setProvider(providerReference.orElse(""))
                                        .setVersion(version.orElse(""))
                                        .setArgs(serialized.serialized)
                                        .putAllArgDependencies(deps)
                                        .build()
                                ));
                    }).thenApply(response -> {
                        // Handle failures.
                        if (response.getFailuresCount() > 0) {
                            var reasons = response.getFailuresList().stream()
                                    .map(reason -> String.format("%s (%s)", reason.getReason(), reason.getProperty()))
                                    .collect(Collectors.joining("; "));

                            throw new CallException(String.format("Call of '%s' failed: %s", token, reasons));
                        }

                        // Unmarshal return dependencies.
                        var dependencies = response.getReturnDependenciesMap().values().stream()
                                .flatMap(deps -> deps.getUrnsList().stream().map(DependencyResource::new))
                                .map(r -> (Resource) r)
                                .collect(toImmutableSet());
                        return new CallRawAsyncResult(response.getReturn(), dependencies);
                    });
        }

        private static class CallRawAsyncResult {
            public final Struct result;
            public final ImmutableSet<Resource> dependencies;

            private CallRawAsyncResult(Struct result, ImmutableSet<Resource> dependencies) {
                this.result = Objects.requireNonNull(result);
                this.dependencies = Objects.requireNonNull(dependencies);
            }
        }
    }

    private static class CallException extends RuntimeException {
        public CallException(String message) {
            super(message);
        }
    }

    private static final class Prepare {

        private final Log log;
        private final FeatureSupport featureSupport;
        private final RootResource rootResource;
        private final PropertiesSerializer serialization;

        private Prepare(Log log, FeatureSupport featureSupport, RootResource rootResource, PropertiesSerializer serialization) {
            this.log = Objects.requireNonNull(log);
            this.featureSupport = Objects.requireNonNull(featureSupport);
            this.rootResource = Objects.requireNonNull(rootResource);
            this.serialization = Objects.requireNonNull(serialization);
        }

        private CompletableFuture<PrepareResult> prepareResourceAsync(
                String label, Resource res, boolean custom, boolean remote,
                ResourceArgs args, ResourceOptions options) {

            var type = res.pulumiResourceType();
            var name = res.pulumiResourceName();

            // Before we can proceed, all our dependencies must be finished.
            log.excessive("Gathering explicit dependencies: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);
            return gatherExplicitDependenciesAsync(options.getDependsOn())
                    .thenApply(ImmutableSet::copyOf)
                    .thenCompose(explicitDirectDependencies -> {
                        log.excessive(
                                "Gathered explicit dependencies: t=%s, name=%s, custom=%s, remote=%s, explicitDirectDependencies=%S",
                                type, name, custom, remote, explicitDirectDependencies
                        );

                        // Serialize out all our props to their final values. In doing so, we'll also collect all
                        // the Resources pointed to by any Dependency objects we encounter, adding them to 'propertyDependencies'.
                        log.excessive("Serializing properties: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);
                        return Internal.from(args).toMapAsync(this.log).thenCompose(
                                props -> this.featureSupport.monitorSupportsResourceReferences().thenCompose(
                                        supportsResourceReferences -> serialization.serializeResourcePropertiesAsync(label, props, supportsResourceReferences).thenCompose(
                                                serializationResult -> {
                                                    var serializedProps = serializationResult.serialized;
                                                    var propertyToDirectDependencies = serializationResult.propertyToDependentResources;
                                                    log.excessive("Serialized properties: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);

                                                    // Wait for the parent to complete.
                                                    // If no parent was provided, parent to the root resource.
                                                    log.excessive("Getting parent urn: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);

                                                    // If no parent was provided, parent to the root resource.
                                                    var parentUrn = options.getParent().isPresent()
                                                            ? Internal.of(options.getParent().get().urn()).getValueOptional()
                                                            : this.rootResource.getRootResourceAsync(type);
                                                    return parentUrn.thenCompose(
                                                            (Optional<String> pUrn) -> {
                                                                log.excessive("Got parent urn: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);

                                                                // Construct the provider reference, if we were given a provider to use.
                                                                final CompletableFuture<Optional<String>> providerRef;
                                                                if (custom) {
                                                                    providerRef = CompletableFutures.flipOptional(options.getProvider().map(p -> Internal.from(p).getRegistrationId()));
                                                                } else {
                                                                    providerRef = CompletableFuture.completedFuture(Optional.<String>empty());
                                                                }

                                                                return providerRef.thenCompose(
                                                                        (Optional<String> pRef) -> {
                                                                            // For remote resources, merge any provider opts into a single dict,
                                                                            // and then create a new dict with all the resolved provider refs.
                                                                            final CompletableFuture<ImmutableMap<String, String>> providerFutures;
                                                                            if (remote && options instanceof ComponentResourceOptions) {
                                                                                var componentOpts = (ComponentResourceOptions) options;

                                                                                log.excessive("Processing a remote ComponentResource: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);

                                                                                Function<List<ProviderResource>, Map<String, CompletableFuture<String>>> convertProviders =
                                                                                        (providers) -> providers.stream()
                                                                                                .map(Internal::from)
                                                                                                .collect(toMap(
                                                                                                        ProviderResource.ProviderResourceInternal::getPackage,
                                                                                                        ProviderResource.ProviderResourceInternal::getRegistrationId
                                                                                                ));

                                                                                providerFutures = CompletableFutures.allOf(
                                                                                        convertProviders.apply(componentOpts.getProviders())
                                                                                ).thenApply(ImmutableMap::copyOf);
                                                                            } else {
                                                                                providerFutures = CompletableFuture.completedFuture(ImmutableMap.of());
                                                                            }

                                                                            return providerFutures.thenCompose(providerRefs -> {
                                                                                // Collect the URNs for explicit/implicit dependencies for the engine so that it can understand
                                                                                // the dependency graph and optimize operations accordingly.

                                                                                // The list of all dependencies (implicit or explicit).
                                                                                var allTransitiveDependencyUrns =
                                                                                        CompletableFutures.builder(getAllTransitivelyReferencedResourceUrnsAsync(explicitDirectDependencies));

                                                                                var propertyToDirectDependencyUrnFutures = new HashMap<String, CompletableFuture<ImmutableSet<String>>>();
                                                                                for (var entry : propertyToDirectDependencies.entrySet()) {
                                                                                    var propertyName = entry.getKey();
                                                                                    var directDependencies = entry.getValue();

                                                                                    var urns = getAllTransitivelyReferencedResourceUrnsAsync(
                                                                                            ImmutableSet.copyOf(directDependencies)
                                                                                    );
                                                                                    allTransitiveDependencyUrns.accumulate(urns, (s1, s2) -> Sets.union(s1, s2).immutableCopy());
                                                                                    propertyToDirectDependencyUrnFutures.put(propertyName, urns);
                                                                                }

                                                                                var propertyToDirectDependencyUrnsFuture =
                                                                                        CompletableFutures.allOf(propertyToDirectDependencyUrnFutures)
                                                                                                .thenApply(ImmutableMap::copyOf);

                                                                                return allTransitiveDependencyUrns.build().thenCompose(
                                                                                        allDirectDependencyUrns -> propertyToDirectDependencyUrnsFuture.thenCompose(
                                                                                                propertyToDirectDependencyUrns -> {

                                                                                                    // Wait for all aliases.
                                                                                                    var aliasesFuture = AliasSerializer.serializeAliases(options.getAliases());

                                                                                                    return aliasesFuture.thenApply(aliases -> new PrepareResult(
                                                                                                            serializedProps,
                                                                                                            pUrn.orElse(""),
                                                                                                            pRef.orElse(""),
                                                                                                            providerRefs,
                                                                                                            allDirectDependencyUrns,
                                                                                                            propertyToDirectDependencyUrns,
                                                                                                            aliases
                                                                                                    ));
                                                                                                })
                                                                                );
                                                                            });
                                                                        }
                                                                );
                                                            });
                                                }
                                        )
                                )
                        );
                    });
        }

        private CompletableFuture<List<Resource>> gatherExplicitDependenciesAsync(Output<List<Resource>> resources) {
            return Internal.of(resources).getValueOrDefault(List.of());
        }

        private CompletableFuture<ImmutableSet<String>> getAllTransitivelyReferencedResourceUrnsAsync(
                ImmutableSet<Resource> resources
        ) {
            // Go through 'resources', but transitively walk through **Component** resources, collecting any
            // of their child resources.  This way, a Component acts as an aggregation really of all the
            // reachable resources it parents.  This walking will stop when it hits custom resources.
            //
            // This function also terminates at remote components, whose children are not known to the Java SDK directly.
            // Remote components will always wait on all of their children, so ensuring we return the remote component
            // itself here and waiting on it will accomplish waiting on all of its children regardless of whether they
            // are returned explicitly here.
            //
            // In other words, if we had:
            //
            //                  Comp1
            //               /    |    \
            //          Cust1   Comp2  Remote1
            //                  /   \      \
            //             Cust2   Cust3  Comp3
            //              /                \
            //         Cust4                 Cust5
            //
            // Then the transitively reachable resources of Comp1 will be [Cust1, Cust2, Cust3, Remote1]. It
            // will *not* include:
            // * Cust4 because it is a child of a custom resource
            // * Comp2 because it is a non-remote component resoruce
            // * Comp3 and Cust5 because Comp3 is a child of a remote component resource
            var transitivelyReachableResources =
                    getTransitivelyReferencedChildResourcesOfComponentResources(resources);

            var transitivelyReachableCustomResources = transitivelyReachableResources.stream()
                    .filter(resource -> {
                        if (resource instanceof CustomResource) {
                            return true;
                        }
                        if (resource instanceof ComponentResource) {
                            return Internal.from(resource).getRemote();
                        }
                        return false; // Unreachable
                    })
                    .map(resource -> Internal.of(resource.urn()).getValueOrDefault(""))
                    .collect(toImmutableSet());
            return CompletableFutures.allOf(transitivelyReachableCustomResources)
                    .thenApply(ts -> ts.stream()
                            .filter(Strings::isNonEmptyOrNull)
                            .collect(toImmutableSet())
                    );
        }

        /**
         * Recursively walk the resources passed in, returning them and all resources reachable
         * from @see {@link Resource#pulumiChildResources()} through any **Component** resources we encounter.
         */
        private ImmutableSet<Resource> getTransitivelyReferencedChildResourcesOfComponentResources(
                ImmutableSet<Resource> resources
        ) {
            // Recursively walk the dependent resources through their children, adding them to the result set.
            var result = new HashSet<Resource>();
            addTransitivelyReferencedChildResourcesOfComponentResources(resources, result);
            return ImmutableSet.copyOf(result);
        }

        private void addTransitivelyReferencedChildResourcesOfComponentResources(
                ImmutableSet<Resource> resources, Set<Resource> builder
        ) {
            for (var resource : resources) {
                if (builder.add(resource)) {
                    if (resource instanceof ComponentResource) {
                        var childResources = ImmutableSet.<Resource>builder();
                        synchronized (resource.pulumiChildResources()) {
                            childResources.addAll(resource.pulumiChildResources());
                        }
                        addTransitivelyReferencedChildResourcesOfComponentResources(childResources.build(), builder);
                    }
                }
            }
        }
    }

    @ParametersAreNonnullByDefault
    private static class PrepareResult {
        public final Struct serializedProps;
        public final String parentUrn;
        public final String providerRef;
        public final ImmutableMap<String, String> providerRefs;
        public final ImmutableSet<String> allDirectDependencyUrns;
        public final ImmutableMap<String, ImmutableSet<String>> propertyToDirectDependencyUrns;
        public final ImmutableList<Alias> aliases;

        public PrepareResult(
                Struct serializedProps,
                String parentUrn,
                String providerRef,
                ImmutableMap<String, String> providerRefs,
                ImmutableSet<String> allDirectDependencyUrns,
                ImmutableMap<String, ImmutableSet<String>> propertyToDirectDependencyUrns,
                ImmutableList<Alias> aliases
        ) {
            this.serializedProps = Objects.requireNonNull(serializedProps);
            this.parentUrn = Objects.requireNonNull(parentUrn);
            this.providerRef = Objects.requireNonNull(providerRef);
            this.providerRefs = Objects.requireNonNull(providerRefs);
            this.allDirectDependencyUrns = Objects.requireNonNull(allDirectDependencyUrns);
            this.propertyToDirectDependencyUrns = Objects.requireNonNull(propertyToDirectDependencyUrns);
            this.aliases = Objects.requireNonNull(aliases);
        }
    }

    @Override
    public void readOrRegisterResource(
            Resource resource, boolean remote, Function<String, Resource> newDependency,
            ResourceArgs args, ResourceOptions options, Resource.LazyFields lazy
    ) {
        this.readOrRegisterResource.readOrRegisterResource(resource, remote, newDependency, args, options, lazy);
    }

    private static final class RawResourceResult {
        public final String urn;
        public final String id;
        public final Struct data;
        public final ImmutableMap<String, ImmutableSet<Resource>> dependencies;

        private RawResourceResult(String urn, String id, Struct data, ImmutableMap<String, ImmutableSet<Resource>> dependencies) {
            this.urn = Objects.requireNonNull(urn, "RegisterResourceAsyncResult expected non-null 'urn'");
            this.id = Objects.requireNonNull(id, "RegisterResourceAsyncResult expected non-null 'id'");
            this.data = Objects.requireNonNull(data, "RegisterResourceAsyncResult expected non-null 'data'");
            this.dependencies = Objects.requireNonNull(dependencies, "RegisterResourceAsyncResult expected non-null 'dependencies'");
        }
    }

    private static final class ReadOrRegisterResourceInternal implements ReadOrRegisterResource {

        private final Log log;
        private final Runner runner;
        private final Invoke invoke;
        private final ReadResource readResource;
        private final RegisterResource registerResource;
        private final Converter converter;
        private final boolean isDryRun;

        private ReadOrRegisterResourceInternal(
                Log log,
                Runner runner,
                Invoke invoke,
                ReadResource readResource,
                RegisterResource registerResource,
                Converter converter,
                boolean isDryRun
        ) {
            this.log = Objects.requireNonNull(log);
            this.runner = Objects.requireNonNull(runner);
            this.invoke = Objects.requireNonNull(invoke);
            this.readResource = Objects.requireNonNull(readResource);
            this.registerResource = Objects.requireNonNull(registerResource);
            this.converter = Objects.requireNonNull(converter);
            this.isDryRun = isDryRun;
        }

        @Override
        public void readOrRegisterResource(
                Resource resource, boolean remote, Function<String, Resource> newDependency,
                ResourceArgs args, ResourceOptions options, Resource.LazyFields lazy
        ) {
            // readOrRegisterResource is called in a fire-and-forget manner. Make sure we keep
            // track of this task so that the application will not quit until this async work
            // completes.
            //
            // Also, we can only do our work once the constructor for the resource has actually
            // finished. Otherwise, we might actually read and get the result back *prior* to the
            // object finishing initializing. Note: this is not a speculative concern. This is
            // something that does happen and has to be accounted for.
            //
            // IMPORTANT! We have to make sure we run 'OutputCompletionSource#initializeOutputs'
            // synchronously directly when `resource`'s constructor runs since this will set all of
            // the `@Export(...) Output<T>` properties. We need those properties assigned by the
            // time the base 'Resource' constructor finishes so that both derived classes and
            // external consumers can use the Output properties of `resource`.
            var completionSources = OutputCompletionSource.from(resource);

            this.runner.registerTask(
                    String.format("readOrRegisterResource: %s-%s", resource.pulumiResourceType(), resource.pulumiResourceName()),
                    completeResourceAsync(resource, remote, newDependency, args, options, completionSources, lazy)
            );
        }

        /**
         * Calls @see {@link #readOrRegisterResource(Resource, boolean, Function, ResourceArgs, ResourceOptions, Resource.LazyFields)}"
         * then completes all the @see {@link OutputCompletionSource} sources on the {@code resource}
         * with the results of it.
         */
        private CompletableFuture<Void> completeResourceAsync(
                Resource resource, boolean remote, Function<String, Resource> newDependency,
                ResourceArgs args, ResourceOptions options,
                ImmutableMap<String, OutputCompletionSource<?>> completionSources,
                Resource.LazyFields lazy
        ) {
            return readOrRegisterResourceAsync(resource, remote, newDependency, args, options)
                    .thenApplyAsync(response -> {
                        var urn = response.urn;
                        var id = response.id;
                        var data = response.data;
                        var dependencies = response.dependencies;
                        log.excessive(
                                "Read response for resource: t=%s, name=%s, urn=%s, id=%s, remote=%s, data=%s",
                                resource.pulumiResourceType(), resource.pulumiResourceName(), urn, id, remote, data
                        );

                        lazy.urn().completeOrThrow(new OutputInternal(
                                OutputData.of(urn).withDependency(resource)));

                        if (resource instanceof CustomResource) {
                            var isKnown = isNonEmptyOrNull(id);
                            lazy.id().orElseThrow().completeOrThrow(isKnown
                                    ? new OutputInternal(OutputData.of(id).withDependency(resource))
                                    : new OutputInternal(OutputData.<String>unknown().withDependency(resource)));
                        }

                        // Go through all our output fields and lookup a corresponding value in the response
                        // object.  Allow the output field to deserialize the response.
                        for (var entry : completionSources.entrySet()) {
                            var fieldName = entry.getKey();
                            OutputCompletionSource<?> completionSource = entry.getValue();

                            // We process and deserialize each field instead of bulk processing
                            // 'response.data' so that each field can have independent isKnown/isSecret values.
                            // We do not want to bubble up isKnown/isSecret from one field to the rest.
                            var value = Structs.tryGetValue(data, fieldName);
                            log.excessive(String.format(
                                    "Setting OutputCompletionSource for field=%s shape=%s value=%s",
                                    fieldName, completionSource.getTypeShape().asString(), value
                            ));
                            if (value.isPresent()) {
                                var contextInfo = String.format("%s.%s", resource.getClass().getTypeName(), fieldName);
                                var depsOrEmpty = Maps.tryGetValue(dependencies, fieldName).orElse(ImmutableSet.of());
                                completionSource.setValue(
                                        this.converter,
                                        contextInfo,
                                        value.get(),
                                        depsOrEmpty
                                );
                            }
                        }
                        return (Void) null;
                    })
                    // Wrap with `whenComplete` so that we always resolve all the outputs of the resource
                    // regardless of whether we encounter an errors computing the action.
                    .whenComplete((__, throwable) -> {
                        if (throwable instanceof Exception) {
                            var e = (Exception) throwable;
                            // Mark any unresolved output properties with this exception. That way we don't
                            // leave any outstanding tasks sitting around which might cause hangs.
                            for (var source : completionSources.values()) {
                                source.trySetException(e);
                            }
                        }
                        if (throwable != null) {
                            lazy.urn().fail(throwable);
                            if (resource instanceof CustomResource) {
                                lazy.id().orElseThrow().fail(throwable);
                            }
                        }
                        // Ensure that we've at least resolved all our completion sources. That way we
                        // don't leave any outstanding tasks sitting around which might cause hangs.
                        for (var source : completionSources.values()) {
                            // Didn't get a value for this field. Resolve it with a default value.
                            // If we're in preview, we'll consider this unknown and in a normal
                            // update we'll consider it known.
                            source.trySetDefaultResult(!this.isDryRun);
                        }
                        Output<String> defaultValue = this.isDryRun
                                ? new OutputInternal<>(OutputData.unknown())
                                : Output.of("");
                        lazy.urn().complete(defaultValue);
                        if (resource instanceof CustomResource) {
                            lazy.id().orElseThrow().complete(defaultValue);
                        }
                    });
        }

        private CompletableFuture<RawResourceResult> readOrRegisterResourceAsync(
                Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args,
                ResourceOptions options
        ) {
            if (options.getUrn().isPresent()) {
                // This is a resource that already exists. Read its state from the engine.

                // Before we can proceed, all our dependencies must be finished.
                log.excessive(
                        "Reading existing resource: t=%s, name=%s, urn=%s, remote=%s",
                        resource.pulumiResourceType(), resource.pulumiResourceName(), options.getUrn().get(), remote
                );
                return this.invoke.invokeRawAsync(
                        "pulumi:pulumi:getResource",
                        new GetResourceInvokeArgs(options.getUrn().get()),
                        InvokeOptions.Empty
                ).thenApply(invokeResult -> {
                    var result = invokeResult.serialized;
                    var urn = result.getFieldsMap().get(Constants.UrnPropertyName).getStringValue();
                    var id = result.getFieldsMap().get(Constants.IdPropertyName).getStringValue();
                    var state = result.getFieldsMap().get(Constants.StatePropertyName).getStructValue();
                    return new RawResourceResult(urn, id, state, ImmutableMap.of());
                });
            }

            if (options.getId().isPresent()) {
                return Internal.of(options.getId().get())
                        .getValueOrDefault("")
                        .thenCompose(id -> {
                            if (isNonEmptyOrNull(id)) {
                                if (!(resource instanceof CustomResource)) {
                                    throw new IllegalArgumentException("ResourceOptions.id is only valid for a CustomResource");
                                }

                                // If this resource already exists, read its state rather than registering it anew.
                                return this.readResource.readResourceAsync(resource, id, args, options);
                            }
                            // see comment at the end of the method below
                            return this.registerResource.registerResourceAsync(resource, remote, newDependency, args, options);
                        });
            }

            // Kick off the resource registration. If we are actually performing a deployment,
            // this resource's properties will be resolved asynchronously after the operation completes,
            // so that dependent computations resolve normally.
            // If we are just planning, on the other hand, values will never resolve.
            return this.registerResource.registerResourceAsync(resource, remote, newDependency, args, options);
        }
    }

    // Arguments type for the `getResource` invoke.
    private static class GetResourceInvokeArgs extends InvokeArgs {
        @Import(name = Constants.UrnPropertyName, required = true)
        @Nullable
        private final String urn;

        public GetResourceInvokeArgs(@Nullable String urn) {
            this.urn = urn;
        }

        public Optional<String> getUrn() {
            return Optional.ofNullable(urn);
        }
    }

    private static final class ReadResource {

        private final Log log;
        private final Prepare prepare;
        private final Monitor monitor;
        private final boolean disableResourceReferences;

        private ReadResource(Log log, Prepare prepare, Monitor monitor, boolean disableResourceReferences) {
            this.log = Objects.requireNonNull(log);
            this.prepare = Objects.requireNonNull(prepare);
            this.monitor = Objects.requireNonNull(monitor);
            this.disableResourceReferences = disableResourceReferences;
        }

        private CompletableFuture<RawResourceResult> readResourceAsync(
                Resource resource, String id, ResourceArgs args, ResourceOptions options
        ) {
            var name = resource.pulumiResourceName();
            var type = resource.pulumiResourceType();
            var label = String.format("resource:%s[%s]#...", name, type);
            log.debug(String.format("Reading resource: id=%s, type=%s, name=%s", id, type, name));

            return this.prepare.prepareResourceAsync(label, resource, /* custom */ true, /* remote */ false, args, options)
                    .thenCompose(prepareResult -> {
                        log.debugOrExcessive(String.format(
                                "ReadResource RPC prepared: id=%s, type=%s, name=%s", id, type, name),
                                String.format(", obj=%s", prepareResult.serializedProps)
                        );

                        // Create a resource request and do the RPC.
                        var request = ReadResourceRequest.newBuilder()
                                .setType(type)
                                .setName(name)
                                .setId(id)
                                .setParent(prepareResult.parentUrn)
                                .setProvider(prepareResult.providerRef)
                                .setProperties(prepareResult.serializedProps)
                                .setVersion(options.getVersion().orElse(""))
                                .setAcceptSecrets(true)
                                .setAcceptResources(!this.disableResourceReferences);

                        for (int i = 0; i < prepareResult.allDirectDependencyUrns.size(); i++) {
                            request.setDependencies(i, prepareResult.allDirectDependencyUrns.asList().get(i));
                        }

                        // Now run the operation, serializing the invocation if necessary.
                        return this.monitor.readResourceAsync(resource, request.build())
                                .thenApply(response -> new RawResourceResult(
                                        response.getUrn(), id, response.getProperties(), ImmutableMap.of()
                                ));

                    });
        }
    }

    private static final class RegisterResource {

        private final Log log;
        private final Prepare prepare;
        private final Monitor monitor;
        private final boolean disableResourceReferences;

        private RegisterResource(Log log, Prepare prepare, Monitor monitor, boolean disableResourceReferences) {
            this.log = Objects.requireNonNull(log);
            this.prepare = Objects.requireNonNull(prepare);
            this.monitor = Objects.requireNonNull(monitor);
            this.disableResourceReferences = disableResourceReferences;
        }

        private CompletableFuture<RawResourceResult> registerResourceAsync(
                Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args,
                ResourceOptions options) {
            var name = resource.pulumiResourceName();
            var type = resource.pulumiResourceType();
            var custom = resource instanceof CustomResource;

            log.debug(String.format(
                    "Preparing resource: t=%s, name=%s, custom=%s, remote=%s",
                    type, name, custom, remote
            ));
            var label = String.format("resource:%s[%s]", name, type);
            return this.prepare.prepareResourceAsync(label, resource, custom, remote, args, options)
                    .thenCompose(prepareResult -> {
                        log.debug(String.format(
                                "Prepared resource: t=%s, name=%s, custom=%s, remote=%s",
                                type, name, custom, remote
                        ));

                        var request = createRegisterResourceRequest(
                                type, name, custom, remote, options, prepareResult
                        );

                        log.debug(String.format(
                                "Registering resource monitor start: t=%s, name=%s, custom=%s, remote=%s, request=%s",
                                type, name, custom, remote, request
                        ));
                        return this.monitor.registerResourceAsync(resource, request)
                                .thenApply(result -> {
                                    log.debug(String.format(
                                            "Registering resource monitor end: t=%s, name=%s, custom=%s, remote=%s, result=%s",
                                            type, name, custom, remote, result
                                    ));

                                    var dependencies = result.getPropertyDependenciesMap().entrySet().stream()
                                            .collect(toImmutableMap(
                                                    Map.Entry::getKey,
                                                    entry -> entry.getValue().getUrnsList().stream()
                                                            .map(newDependency)
                                                            .collect(toImmutableSet())
                                            ));

                                    return new RawResourceResult(result.getUrn(), result.getId(), result.getObject(), dependencies);
                                });
                    });
        }

        private RegisterResourceRequest createRegisterResourceRequest(
                String type, String name, boolean custom, boolean remote, ResourceOptions options, PrepareResult prepareResult
        ) {
            var customOpts = options instanceof CustomResourceOptions;

            Function<Function<CustomTimeouts, Optional<Duration>>, String> customTimeoutToGolangString =
                    (Function<CustomTimeouts, Optional<Duration>> f) ->
                            options.getCustomTimeouts()
                                    .map(f)
                                    .map(CustomTimeouts::golangString)
                                    .orElse("");

            var request = RegisterResourceRequest.newBuilder()
                    .setType(type)
                    .setName(name)
                    .setCustom(custom)
                    .setProtect(options.isProtect())
                    .setVersion(options.getVersion().orElse(""))
                    .setPluginDownloadURL(options.getPluginDownloadURL().orElse(""))
                    .setImportId(customOpts ? ((CustomResourceOptions) options).getImportId().orElse("") : "")
                    .setAcceptSecrets(true)
                    .setAcceptResources(!this.disableResourceReferences)
                    .setDeleteBeforeReplace(customOpts && ((CustomResourceOptions) options).getDeleteBeforeReplace())
                    .setDeleteBeforeReplaceDefined(true)
                    .setCustomTimeouts(
                            RegisterResourceRequest.CustomTimeouts.newBuilder()
                                    .setCreate(customTimeoutToGolangString.apply(CustomTimeouts::getCreate))
                                    .setDelete(customTimeoutToGolangString.apply(CustomTimeouts::getDelete))
                                    .setUpdate(customTimeoutToGolangString.apply(CustomTimeouts::getUpdate))
                                    .build()
                    )
                    .setRemote(remote)
                    .setRetainOnDelete(options.isRetainOnDelete());

            if (customOpts) {
                request.addAllAdditionalSecretOutputs(((CustomResourceOptions) options).getAdditionalSecretOutputs());
                request.addAllReplaceOnChanges(options.getReplaceOnChanges());
            }

            request.addAllIgnoreChanges(options.getIgnoreChanges());

            // populateRequest

            request.setObject(prepareResult.serializedProps);
            request.setParent(prepareResult.parentUrn);
            request.setProvider(prepareResult.providerRef);
            request.putAllProviders(prepareResult.providerRefs);
            request.addAllAliases(prepareResult.aliases);
            request.addAllDependencies(prepareResult.allDirectDependencyUrns);

            request.putAllPropertyDependencies(prepareResult.propertyToDirectDependencyUrns.entrySet().stream()
                    .collect(toImmutableMap(
                            Map.Entry::getKey,
                            entry -> RegisterResourceRequest.PropertyDependencies.newBuilder()
                                    .addAllUrns(entry.getValue())
                                    .build()
                    ))
            );

            return request.build();
        }
    }

    @Override
    public void registerResourceOutputs(Resource resource, Output<Map<String, Output<?>>> outputs) {
        this.registerResourceOutputs.registerResourceOutputs(resource, outputs);
    }

    private static final class RegisterResourceOutputsInternal implements RegisterResourceOutputs {

        private final Log log;
        private final Runner runner;
        private final Monitor monitor;
        private final FeatureSupport featureSupport;
        private final PropertiesSerializer serialization;

        private RegisterResourceOutputsInternal(
                Log log,
                Runner runner,
                Monitor monitor,
                FeatureSupport featureSupport,
                PropertiesSerializer serialization
        ) {
            this.log = Objects.requireNonNull(log);
            this.runner = Objects.requireNonNull(runner);
            this.monitor = Objects.requireNonNull(monitor);
            this.featureSupport = Objects.requireNonNull(featureSupport);
            this.serialization = Objects.requireNonNull(serialization);
        }

        public void registerResourceOutputs(Resource resource, Output<Map<String, Output<?>>> outputs) {
            // RegisterResourceOutputs is called in a fire-and-forget manner.  Make sure we keep track of
            // this task so that the application will not quit until this async work completes.
            this.runner.registerTask(
                    String.format(
                            "DeploymentInternalInternal.registerResourceOutputs: %s-%s",
                            resource.pulumiResourceType(), resource.pulumiResourceName()
                    ),
                    registerResourceOutputsAsync(resource, outputs));
        }

        private CompletableFuture<Void> registerResourceOutputsAsync(
                Resource resource, Output<Map<String, Output<?>>> outputs
        ) {
            var opLabel = "monitor.registerResourceOutputs(...)";

            var urnFuture = Internal.of(resource.urn())
                    .getValueOrDefault("");
            var propsFuture = Internal.of(outputs)
                    .getValueOrDefault(Map.of());

            BiFunction<String, Struct, CompletableFuture<Void>> registerResourceOutputsAsync = (urn, serialized) -> {
                if (Strings.isEmptyOrNull(urn)) {
                    throw new IllegalStateException(String.format("Expected a urn at this point, got: '%s'", urn));
                }
                log.debugOrExcessive(
                        String.format("RegisterResourceOutputs RPC prepared: urn='%s'", urn),
                        String.format(", outputs=%s", JsonFormatter
                                        .format(serialized)
                                        .orThrow(Function.identity())
                        ));

                return this.monitor.registerResourceOutputsAsync(
                        RegisterResourceOutputsRequest.newBuilder()
                                .setUrn(urn)
                                .setOutputs(serialized)
                                .build()
                );
            };

            // The registration could very well still be taking place, so we will need to wait for its URN.
            // Additionally, the output properties might have come from other resources, so we must await those too.
            return urnFuture.thenCompose(
                    urn -> propsFuture.thenCompose(
                            props -> this.featureSupport.monitorSupportsResourceReferences().thenCompose(
                                    keepResources -> serialization.serializeAllPropertiesAsync(opLabel, props, keepResources).thenCompose(
                                            serialized -> registerResourceOutputsAsync.apply(urn, serialized)
                                    )
                            )
                    )
            );
        }
    }

    private static final class RootResource {

        @Nullable
        private CompletableFuture<Optional<String>> rootResource;
        private final Object rootResourceLock = new Object();
        private final Engine engine;

        private RootResource(Engine engine) {
            this.engine = Objects.requireNonNull(engine);
        }

        /**
         * Returns a root resource URN that will automatically become the default parent of all
         * resources. This can be used to ensure that all resources without explicit parents are
         * parented to a common parent resource.
         */
        CompletableFuture<Optional<String>> getRootResourceAsync(String type) {
            // If we're calling this while creating the stack itself. No way to know its urn at this point.
            if (RootPulumiStackTypeName.equals(type)) {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            synchronized (rootResourceLock) {
                if (rootResource == null) {
                    try {
                        var stack = DeploymentInternal.getInstance().getStack();
                        rootResource = setRootResourceWorkerAsync(stack);
                    } catch (IllegalStateException ex) {
                        throw new IllegalStateException("Calling getRootResourceAsync before the stack was registered!");
                    }
                }
            }

            return this.rootResource;
        }

        private CompletableFuture<Optional<String>> setRootResourceWorkerAsync(Stack stack) {
            return Internal.of(stack.urn())
                    .getValueNullable()
                    .thenCompose(
                            resUrn -> this.engine.setRootResourceAsync(
                                    EngineOuterClass.SetRootResourceRequest.newBuilder()
                                            .setUrn(resUrn)
                                            .build()
                            ).thenCompose(
                                    ignore -> this.engine.getRootResourceAsync(
                                            EngineOuterClass.GetRootResourceRequest.newBuilder().build()
                                    ).thenApply(EngineOuterClass.GetRootResourceResponse::getUrn)
                            )
                    ).thenApply(Optional::ofNullable);
        }
    }

    @ParametersAreNonnullByDefault
    @InternalUse
    public static class DeploymentState {
        public static final boolean DisableResourceReferences = getBooleanEnvironmentVariable("PULUMI_DISABLE_RESOURCE_REFERENCES").or(false);
        public static final boolean ExcessiveDebugOutput = getBooleanEnvironmentVariable("PULUMI_EXCESSIVE_DEBUG_OUTPUT").or(false);

        public final DeploymentImpl.Config config;
        public final String projectName;
        public final String stackName;
        public final boolean isDryRun;
        public final Engine engine;
        public final Monitor monitor;
        public Runner runner; // late init
        public EngineLogger logger; // late init

        private final Logger standardLogger;

        @InternalUse
        @VisibleForTesting
        public DeploymentState(
                DeploymentImpl.Config config,
                Logger standardLogger,
                String projectName,
                String stackName,
                boolean isDryRun,
                Engine engine,
                Monitor monitor) {
            this.config = Objects.requireNonNull(config);
            this.standardLogger = Objects.requireNonNull(standardLogger);
            this.projectName = Objects.requireNonNull(projectName);
            this.stackName = Objects.requireNonNull(stackName);
            this.isDryRun = isDryRun;
            this.engine = Objects.requireNonNull(engine);
            this.monitor = Objects.requireNonNull(monitor);
            // Use Suppliers to avoid problems with cyclic dependencies
            this.logger = new DefaultEngineLogger(standardLogger, () -> this.runner, () -> this.engine);
            this.runner = new DefaultRunner(standardLogger, this.logger);
        }

        /**
         * @throws IllegalArgumentException if an environment variable is not found
         */
        public static DeploymentState fromEnvironment() {
            var standardLogger = Logger.getLogger(DeploymentImpl.class.getName());
            standardLogger.log(Level.FINEST, "ENV: " + System.getenv());

            Function<RuntimeException, RuntimeException> startErrorSupplier =
                    e -> new IllegalArgumentException(
                            "Program run without the Pulumi engine available; re-run using the `pulumi` CLI", e
                    );

            try {
                var monitorTarget = getEnvironmentVariable("PULUMI_MONITOR").orThrow(startErrorSupplier);
                var engineTarget = getEnvironmentVariable("PULUMI_ENGINE").orThrow(startErrorSupplier);
                var project = getEnvironmentVariable("PULUMI_PROJECT").orThrow(startErrorSupplier);
                var stack = getEnvironmentVariable("PULUMI_STACK").orThrow(startErrorSupplier);
                var dryRun = getBooleanEnvironmentVariable("PULUMI_DRY_RUN").orThrow(startErrorSupplier);

                var config = Config.parse();
                standardLogger.setLevel(GlobalLogging.GlobalLevel);

                standardLogger.log(Level.FINEST, "Creating deployment engine");
                var engine = new GrpcEngine(engineTarget);
                standardLogger.log(Level.FINEST, "Created deployment engine");

                standardLogger.log(Level.FINEST, "Creating deployment monitor");
                var monitor = new GrpcMonitor(monitorTarget);
                standardLogger.log(Level.FINEST, "Created deployment monitor");

                return new DeploymentState(config, standardLogger, project, stack, dryRun, engine, monitor);
            } catch (NullPointerException ex) {
                throw new IllegalStateException(
                        "Program run without the Pulumi engine available; re-run using the `pulumi` CLI", ex);
            }
        }
    }

    @ParametersAreNonnullByDefault
    @InternalUse
    @VisibleForTesting
    static class DefaultRunner implements Runner {
        private final Logger standardLogger;
        private final EngineLogger engineLogger;

        /**
         * The set of tasks (futures) that we have fired off. We issue futures in a Fire-and-Forget manner
         * to be able to expose a Synchronous @see {@link com.pulumi.resources.Resource} model for users.
         * i.e. a user just synchronously creates a resource, and we asynchronously kick off the work
         * to populate it.
         * This works well, however we have to make sure the console app
         * doesn't exit because it thinks there is no work to do.
         * <p>
         * To ensure that doesn't happen, we have the main entrypoint of the app just
         * continuously, asynchronously loop, waiting for these tasks to complete, and only
         * exiting once the set becomes empty.
         */
        private final Map<CompletableFuture<Void>, List<String>> inFlightTasks = new ConcurrentHashMap<>();
        private final Queue<Exception> swallowedExceptions = new ConcurrentLinkedQueue<>();

        public DefaultRunner(Logger standardLogger, EngineLogger engineLogger) {
            this.standardLogger = Objects.requireNonNull(standardLogger);
            this.engineLogger = Objects.requireNonNull(engineLogger);
        }

        @Override
        public <T> CompletableFuture<Result<T>> runAsync(Supplier<T> callback) {
            var valueFuture = CompletableFuture.supplyAsync(callback);
            // run the callback asynchronously in the context of the error handler
            registerTask("DefaultRunner#runAsync", valueFuture);
            // loop starts after the callback
            return valueFuture
                    .thenCompose(value -> whileRunningAsync().thenApply(__ -> value))
                    .handle((value, throwable) -> {
                        if (throwable != null) {
                            return handleExceptionAsync(throwable).thenApply(errorCode ->
                                    new Result<>(
                                            errorCode,
                                            ImmutableList.copyOf(this.swallowedExceptions),
                                            Optional.ofNullable(value)
                                    )
                            );
                        }
                        // Getting error information from a logger is slightly ugly, but that's what C# implementation does
                        var code = this.engineLogger.hasLoggedErrors()
                                ? ProcessExitedBeforeLoggingUserActionableMessage
                                : ProcessExitedSuccessfully;
                        return CompletableFuture.completedFuture(new Result<>(
                                code,
                                ImmutableList.copyOf(this.swallowedExceptions),
                                Optional.ofNullable(value)
                        ));
                    })
                    .thenCompose(Function.identity()); // we return a future from logging, and we need to flat-map here
        }

        @Override
        public <T> void registerTask(String description, CompletableFuture<T> task) {
            Objects.requireNonNull(description);
            Objects.requireNonNull(task);
            this.standardLogger.log(Level.FINEST, String.format("Registering task: '%s', %s", description, task));

            // we don't need the result here, just the future itself
            CompletableFuture<Void> key = task.thenApply(__ -> null);

            // We may get several of the same tasks with different descriptions. That can
            // happen when the runtime reuses cached tasks that it knows are value-identical
            // (for example a completed future). In that case, we just store all the descriptions.
            // We'll print them all out as done once this task actually finishes.
            this.inFlightTasks.compute(key,
                    (__, descriptions) -> {
                        if (descriptions == null) {
                            return Lists.newArrayList(description);
                        } else {
                            descriptions.add(description);
                            return descriptions;
                        }
                    });
        }

        // Wait for one of the two events to happen:
        // 1. All tasks in the list complete successfully, or
        // 2. Any task throws an exception.
        // So the resulting semantics is that we complete
        // when remaining count is zero, or when an exception is thrown.
        private CompletableFuture<Void> whileRunningAsync() {
            // Wait for every task and remove from inFlightTasks when completed
            Consumer<CompletableFuture<Void>> handleCompletion = (task) -> {
                try {
                    // Wait for the task completion (non-blocking).
                    // At this point it is guaranteed by the execution loop bellow, that the task is already complete.
                    if (!task.isDone()) {
                        throw new IllegalStateException(
                                String.format("expected task to be done at this point, it was not: %s, %s",
                                        this.inFlightTasks.get(task),
                                        task
                                )
                        );
                    }
                    task.join();

                    // Log the descriptions of completed tasks.
                    if (this.standardLogger.isLoggable(Level.FINEST)) {
                        List<String> descriptions = inFlightTasks.getOrDefault(task, List.of());
                        // getOrDefault should never return null, but it does for whatever reason, so just to be sure
                        if (descriptions == null) {
                            descriptions = List.of();
                        }
                        this.standardLogger.log(Level.FINEST, String.format("Completed task: '%s', %s", String.join(",", descriptions), task));
                    }
                } catch (Exception e) {
                    this.standardLogger.log(Level.FINEST, String.format("Failed task: '%s', exception: %s", inFlightTasks.get(task), e));
                    throw e;
                } finally {
                    // Once finished, remove the task from the set of tasks that are running.
                    this.inFlightTasks.remove(task);
                }
            };

            Supplier<CompletableFuture<Void>> loopUntilDone = () -> {
                // Keep looping as long as there are outstanding tasks that are still running.
                while (inFlightTasks.size() > 0) {
                    this.standardLogger.log(Level.FINEST, String.format("Remaining tasks [%s]: %s", inFlightTasks.size(), inFlightTasks));

                    // Grab all the tasks we currently have running.
                    for (var task : inFlightTasks.keySet()) {
                        try {
                            if (task.isDone()) {
                                // at this point the future is guaranteed to be solved
                                // so there won't be any blocking here
                                handleCompletion.accept(task); // will remove from inFlightTasks
                            } else {
                                this.standardLogger.log(Level.FINEST, String.format("Tasks not done: %s", task));
                                // will attempt again in the next iteration
                            }
                        } catch (Exception e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    }
                }

                // There were no more tasks we were waiting on. Quit out.
                return CompletableFuture.completedFuture((Void) null);
            };
            return CompletableFuture.supplyAsync(loopUntilDone).thenCompose(f -> f);
        }

        private CompletableFuture<Integer> handleExceptionAsync(Throwable throwable) {
            if (throwable instanceof Exception) {
                return handleExceptionAsync((Exception) throwable);
            }
            return handleExceptionAsync(new RunException("unexpected throwable", throwable));
        }

        private CompletableFuture<Integer> handleExceptionAsync(Exception exception) {
            this.swallowedExceptions.add(exception);

            Function<Void, Integer> exitMessageAndCode = unused -> {
                this.standardLogger.log(Level.FINE, "Returning from program after last error");
                return ProcessExitedAfterLoggingUserActionableMessage;
            };

            if (exception instanceof LogException) {
                // We got an error while logging itself.
                // Nothing to do here but print some errors and abort.
                this.standardLogger.log(Level.SEVERE, String.format(
                        "Error occurred trying to send logging message to engine: %s", exception.getMessage()));
                return CompletableFuture.supplyAsync(() -> {
                    System.err.printf("Error occurred trying to send logging message to engine: %s%n", exception);
                    exception.printStackTrace();
                    return ProcessExitedBeforeLoggingUserActionableMessage;
                });
            }

            // unwrap the CompletionException (used by CompletableFuture)
            if (exception instanceof CompletionException
                    && exception.getCause() != null
                    && exception.getCause() instanceof Exception) {
                return handleExceptionAsync((Exception) exception.getCause());
            }

            // For all other issues we encounter we log the
            // problem to the error stream.
            //
            // Note: if these logging calls fail, they will just
            // end up bubbling an exception that will be caught
            // by nothing. This will tear down the actual process
            // with a non-zero error which our host will handle
            // properly.
            if (exception instanceof RunException) {
                // Always hide the stack for RunException.
                return this.engineLogger
                        .errorAsync(exception.getMessage())
                        .thenApply(exitMessageAndCode);
            }
            if (exception instanceof ResourceException) {
                var resourceEx = (ResourceException) exception;
                var message = resourceEx.isHideStack() ? resourceEx.getMessage() : getStackTrace(resourceEx);
                return this.engineLogger
                        .errorAsync(message, resourceEx.getResource().orElse(null))
                        .thenApply(exitMessageAndCode);
            }

            var pid = ProcessHandle.current().pid();
            var command = ProcessHandle.current().info().commandLine().orElse("unknown");
            return this.engineLogger
                    .errorAsync(String.format(
                            "Running program [PID: %d](%s) failed with an unhandled exception:\n%s",
                            pid, command, getStackTrace(exception)))
                    .thenApply(exitMessageAndCode);

        }
    }

    @ParametersAreNonnullByDefault
    @InternalUse
    @VisibleForTesting
    public static final class DefaultEngineLogger implements EngineLogger {
        private final Supplier<Runner> runner;
        private final Supplier<Engine> engine;
        private final Logger standardLogger;
        private final AtomicInteger errorCount;

        // We serialize all logging tasks so that the engine doesn't hear about them out of order.
        // This is necessary for streaming logs to be maintained in the right order.
        private CompletableFuture<Void> lastLogTask = CompletableFuture.allOf();
        private final Object logGate = new Object(); // lock target

        public DefaultEngineLogger(Logger standardLogger, Supplier<Runner> runner, Supplier<Engine> engine) {
            this.standardLogger = Objects.requireNonNull(standardLogger);
            this.runner = Objects.requireNonNull(runner);
            this.engine = Objects.requireNonNull(engine);
            this.errorCount = new AtomicInteger(0);
        }

        @Override
        public boolean hasLoggedErrors() {
            return errorCount.get() > 0;
        }

        @Override
        public int getErrorCount() {
            return errorCount.get();
        }

        @Override
        public CompletableFuture<Void> logAsync(Level level,
                                                String message,
                                                @Nullable Resource resource,
                                                @Nullable Integer streamId,
                                                @Nullable Boolean ephemeral) {
            standardLogger.log(level, message);
            return logImplAsync(toLogSeverity(level), message, resource, streamId, ephemeral);
        }

        private static LogSeverity toLogSeverity(Level level) {

            if (level == Level.FINEST) {
                return LogSeverity.DEBUG;
            }

            if (level == Level.INFO) {
                return LogSeverity.INFO;
            }

            if (level == Level.WARNING) {
                return LogSeverity.WARNING;
            }

            if (level == Level.SEVERE) {
                return LogSeverity.ERROR;
            }

            throw new IllegalArgumentException("Invalid level: " + level.getName());
        }

        @Override
        public CompletableFuture<Void> debugAsync(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
            standardLogger.log(Level.FINEST, message);
            return logImplAsync(LogSeverity.DEBUG, message, resource, streamId, ephemeral);
        }

        @Override
        public CompletableFuture<Void> infoAsync(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
            standardLogger.log(Level.INFO, message);
            return logImplAsync(LogSeverity.INFO, message, resource, streamId, ephemeral);
        }

        @Override
        public CompletableFuture<Void> warnAsync(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
            standardLogger.log(Level.WARNING, message);
            return logImplAsync(LogSeverity.WARNING, message, resource, streamId, ephemeral);
        }

        @Override
        public CompletableFuture<Void> errorAsync(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
            standardLogger.log(Level.SEVERE, message);
            return logImplAsync(LogSeverity.ERROR, message, resource, streamId, ephemeral);
        }

        private CompletableFuture<Void> logImplAsync(LogSeverity severity, String message,
                                                     @Nullable Resource resource, @Nullable Integer streamId,
                                                     @Nullable Boolean ephemeral
        ) {
            if (severity == LogSeverity.ERROR) {
                this.errorCount.incrementAndGet();
            }

            var runner = this.runner.get();
            if (runner == null) {
                // Graceful degradation of logging
                standardLogger.warning("Degraded functionality [DefaultEngineLogger]: async logging is unavailable because of no Runner");
                return CompletableFuture.completedFuture(null);
            }

            // Serialize our logging tasks so that streaming logs appear in order.
            // TODO: this implementation will compose CompletableFuture's infinitely and this may cause issues at some point
            CompletableFuture<Void> task;
            synchronized (logGate) {
                // TODO: C# uses a 'Task.Run' here (like CompletableFuture.runAsync/supplyAsync?)
                //       so that "we don't end up aggressively running the actual logging while holding this lock."
                //       Is something similar required in Java or thenComposeAsync is enough?
                this.lastLogTask = this.lastLogTask.thenComposeAsync(
                        ignore -> logAsync(severity, message, resource, streamId, ephemeral)
                );
                task = this.lastLogTask;
            }

            runner.registerTask(message, task);
            return task;
        }

        private CompletableFuture<Void> logAsync(LogSeverity severity, String message,
                                                 @Nullable Resource resource, @Nullable Integer streamId,
                                                 @Nullable Boolean ephemeral) {
            var engine = this.engine.get();
            if (engine == null) {
                // Graceful degradation of logging
                standardLogger.warning("Degraded functionality [DefaultEngineLogger]: async logging is unavailable because of no Engine");
                return CompletableFuture.completedFuture(null);
            }
            try {
                return resourceUrnOrEmpty(resource)
                        .thenCompose(
                                urn -> engine.logAsync(
                                        LogRequest.newBuilder()
                                                .setSeverity(severity)
                                                .setMessage(message)
                                                .setUrn(urn)
                                                .setStreamId(streamId == null ? 0 : streamId)
                                                .setEphemeral(ephemeral != null && ephemeral)
                                                .build()
                                )
                        );
            } catch (Exception e) {
                // mark that we had an error so that our top level process quits with an error code.
                this.errorCount.incrementAndGet();

                // We have a potential pathological case with logging. Consider if logging a
                // message itself throws an error.  If we then allow the error to bubble up, our top
                // level handler will try to log that error, which can potentially lead to an error
                // repeating unendingly. So, to prevent that from happening, we report a very specific
                // exception that the top level can know about and handle specially.
                throw new LogException(e);
            }
        }

        /**
         * @return a URN or empty String for the given @see {@link Resource}
         */
        private static CompletableFuture<String> resourceUrnOrEmpty(@Nullable Resource resource) {
            if (resource != null) {
                try {
                    return Internal.of(resource.urn()).getValueOrDefault("");
                } catch (Throwable ignore) {
                    // getting the urn for a resource may itself fail, in that case we don't want to
                    // fail to send a logging message. we'll just send the logging message unassociated
                    // with any resource.
                }
            }

            return CompletableFuture.completedFuture("");
        }
    }
}
