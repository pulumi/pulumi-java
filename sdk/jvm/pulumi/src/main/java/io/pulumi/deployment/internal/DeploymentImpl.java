package io.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.Internal;
import io.pulumi.Log;
import io.pulumi.Stack;
import io.pulumi.core.Input;
import io.pulumi.core.Output;
import io.pulumi.core.OutputDefault;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple4;
import io.pulumi.core.internal.Maps;
import io.pulumi.core.internal.*;
import io.pulumi.core.internal.Reflection.TypeShape;
import io.pulumi.core.internal.annotations.InputImport;
import io.pulumi.deployment.CallOptions;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.exceptions.LogException;
import io.pulumi.exceptions.ResourceException;
import io.pulumi.exceptions.RunException;
import io.pulumi.resources.*;
import io.pulumi.serialization.internal.*;
import pulumirpc.EngineOuterClass;
import pulumirpc.EngineOuterClass.LogRequest;
import pulumirpc.EngineOuterClass.LogSeverity;
import pulumirpc.Provider.CallRequest;
import pulumirpc.Provider.InvokeRequest;
import pulumirpc.Resource.ReadResourceRequest;
import pulumirpc.Resource.RegisterResourceRequest;
import pulumirpc.Resource.SupportsFeatureRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.time.Duration;
import java.util.Objects;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.pulumi.core.internal.CompletableFutures.ignoreNullMapValues;
import static io.pulumi.core.internal.Environment.getBooleanEnvironmentVariable;
import static io.pulumi.core.internal.Environment.getEnvironmentVariable;
import static io.pulumi.core.internal.Exceptions.getStackTrace;
import static io.pulumi.core.internal.PulumiCollectors.toTupleOfMaps2;
import static io.pulumi.core.internal.Strings.isNonEmptyOrNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class DeploymentImpl extends DeploymentInstanceHolder implements Deployment, DeploymentInternal {

    private final DeploymentState state;
    private final FeatureSupport featureSupport;
    private final Invoke invoke;
    private final Call call;
    private final Prepare prepare;
    private final ReadOrRegisterResource readOrRegisterResource;
    private final ReadResource readResource;
    private final RegisterResource registerResource;
    private final RegisterResourceOutputs registerResourceOutputs;
    private final RootResource rootResource;

    DeploymentImpl() {
        this(fromEnvironment());
    }

    // TODO private Deployment(InlineDeploymentSettings settings)

    @Internal
    @VisibleForTesting
    DeploymentImpl(
            DeploymentState state
    ) {
        this.state = Objects.requireNonNull(state);
        this.featureSupport = new FeatureSupport(state.monitor);
        this.invoke = new Invoke(state.monitor, this.featureSupport);
        this.rootResource = new RootResource(state.engine);
        this.prepare = new Prepare(this.featureSupport, this.rootResource);
        this.call = new Call(state.monitor, this.prepare);
        this.readResource = new ReadResource(prepare, state.monitor);
        this.registerResource = new RegisterResource(prepare, state.monitor);
        this.readOrRegisterResource = new ReadOrRegisterResource(state.runner, this.invoke, this.readResource, this.registerResource, state.isDryRun);
        this.registerResourceOutputs = new RegisterResourceOutputs(state.runner, state.monitor, featureSupport);

        this.state.standardLogger.log(Level.INFO, "Deployment initialized.");
    }

    /**
     * @throws IllegalArgumentException if an environment variable is not found
     */
    private static DeploymentState fromEnvironment() {
        var standardLogger = Logger.getLogger(DeploymentImpl.class.getName());
        standardLogger.log(Level.FINEST, "ENV: " + System.getenv());

        Function<Exception, RuntimeException> startErrorSupplier = (Exception e) ->
                new IllegalArgumentException("Program run without the Pulumi engine available; re-run using the `pulumi` CLI", e);

        try {
            var monitorTarget = getEnvironmentVariable("PULUMI_MONITOR").orThrow(startErrorSupplier);
            var engineTarget = getEnvironmentVariable("PULUMI_ENGINE").orThrow(startErrorSupplier);
            var project = getEnvironmentVariable("PULUMI_PROJECT").orThrow(startErrorSupplier);
            var stack = getEnvironmentVariable("PULUMI_STACK").orThrow(startErrorSupplier);
//            var pwd = getEnvironmentVariable("PULUMI_PWD");
            var dryRun = getBooleanEnvironmentVariable("PULUMI_DRY_RUN").orThrow(startErrorSupplier);
//            var queryMode = getBooleanEnvironmentVariable("PULUMI_QUERY_MODE");
//            var parallel = getIntegerEnvironmentVariable("PULUMI_PARALLEL");
//            var tracing = getEnvironmentVariable("PULUMI_TRACING");
            // TODO what to do with all the unused envvars?

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

    @Internal
    public EngineLogger getLogger() {
        return this.state.logger;
    }

    @Internal
    public Runner getRunner() {
        return this.state.runner;
    }

    public Optional<String> getConfig(String fullKey) {
        return this.state.config.getConfig(fullKey);
    }

    public boolean isConfigSecret(String fullKey) {
        return this.state.config.isConfigSecret(fullKey);
    }

    @Nullable
    private Stack stack; // TODO: get rid of mutability, somehow

    @Internal
    public Stack getStack() {
        if (this.stack == null) {
            throw new IllegalStateException("Trying to acquire Deployment#getStack before 'run' was called.");
        }
        return this.stack;
    }

    @Internal
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

        @Internal
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

        @Internal
        CompletableFuture<Boolean> monitorSupportsResourceReferences() {
            return monitorSupportsFeature("resourceReferences");
        }
    }

    @ParametersAreNonnullByDefault
    @Internal
    @VisibleForTesting
    static class Config {

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
        Config(ImmutableMap<String, String> allConfig, ImmutableSet<String> configSecretKeys) {
            this.allConfig = Objects.requireNonNull(allConfig);
            this.configSecretKeys = Objects.requireNonNull(configSecretKeys);
        }

        private static Config parse() {
            return new Config(parseConfig(), parseConfigSecretKeys());
        }

        /**
         * Returns a copy of the full config map.
         */
        @Internal
        private ImmutableMap<String, String> getAllConfig() {
            return allConfig;
        }

        /**
         * Returns a copy of the config secret keys.
         */
        @Internal
        private ImmutableSet<String> configSecretKeys() {
            return configSecretKeys;
        }

        /**
         * Sets a configuration variable.
         */
        @Internal
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
        @Internal
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

        @Internal
        @VisibleForTesting
        static ImmutableMap<String, String> parseConfig(String envConfigJson) {
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

        @Internal
        @VisibleForTesting
        static ImmutableSet<String> parseConfigSecretKeys(String envConfigSecretKeysJson) {
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
            var idx = key.indexOf(":");
            if (idx > 0 && key.substring(idx + 1).startsWith("config:")) {
                return key.substring(0, idx) + ":" + key.substring(idx + 1 + "config:".length());
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

        private final Monitor monitor;
        private final FeatureSupport featureSupport;

        private Invoke(Monitor monitor, FeatureSupport featureSupport) {
            this.monitor = Objects.requireNonNull(monitor);
            this.featureSupport = Objects.requireNonNull(featureSupport);
        }

        public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args) {
            return invoke(token, targetType, args, InvokeOptions.Empty);
        }

        public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
            return OutputDefault.of(rawInvoke(token, targetType, args, options));
        }

        private <T> CompletableFuture<InputOutputData<T>> rawInvoke(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
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
                    .thenApply(result -> Converter.convertValue(
                            String.format("%s result", token),
                            Value.newBuilder()
                                    .setStructValue(result.serialized)
                                    .build(),
                            targetType,
                            result.propertyToDependentResources.values().stream()
                                    .flatMap(Collection::stream)
                                    .collect(toImmutableSet()))
                    );
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
            return invokeRawAsync(token, args, options).thenApply(
                    result -> Converter.convertValue(
                            String.format("%s result", token),
                            Value.newBuilder()
                                    .setStructValue(result.serialized)
                                    .build(),
                            targetType
                    ))
                    .thenApply(InputOutputData::getValueNullable);
        }

        private CompletableFuture<Serialization.SerializationResult> invokeRawAsync(String token, InvokeArgs args, InvokeOptions options) {
            Objects.requireNonNull(token);
            Objects.requireNonNull(args);
            Objects.requireNonNull(options);

            Log.debug(String.format("Invoking function: token='%s' asynchronously", token));

            // Wait for all values to be available, and then perform the RPC.
            var serializedFuture = args.internalToOptionalMapAsync()
                    .thenCompose(argsDict ->
                            this.featureSupport.monitorSupportsResourceReferences()
                                    .thenCompose(keepResources ->
                                            Serialization.serializeFilteredPropertiesAsync(
                                                    String.format("invoke:%s", token), argsDict, ignore -> true, keepResources
                                            ))
                    );

            CompletableFuture<Optional<String>> providerFuture = CompletableFutures.flipOptional(
                    () -> getProviderFrom(token, options).map(p -> p.accept(ProviderResource.registrationIdVisitor()))
            );

            return CompletableFuture.allOf(serializedFuture, providerFuture)
                    .thenCompose(unused -> {
                        var serialized = serializedFuture.join();
                        var provider = providerFuture.join();
                        var version = options.getVersion();

                        Log.debug(String.format("Invoke RPC prepared: token='%s'", token) +
                                (DeploymentState.ExcessiveDebugOutput ? String.format(", obj='%s'", serialized) : ""));
                        return this.monitor.invokeAsync(InvokeRequest.newBuilder()
                                .setTok(token)
                                .setProvider(provider.orElse(""))
                                .setVersion(version.orElse(""))
                                .setArgs(serialized.serialized)
                                .setAcceptResources(!DeploymentState.DisableResourceReferences)
                                .build()
                        ).thenApply(response -> {
                            // Handle failures.
                            if (response.getFailuresCount() > 0) {
                                var reasons = response.getFailuresList().stream()
                                        .map(reason -> String.format("%s (%s)", reason.getReason(), reason.getProperty()))
                                        .collect(Collectors.joining("; "));

                                throw new InvokeException(String.format("Invoke of '%s' failed: %s", token, reasons));
                            }
                            return new Serialization.SerializationResult(response.getReturn(), serialized.propertyToDependentResources);
                        });
                    });
        }

        private static Optional<ProviderResource> getProviderFrom(String token, InvokeOptions options) {
            return options.accept(InvokeOptions.NestedProviderVisitor.of(token));
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
        private final Monitor monitor;
        private final Prepare prepare;

        public Call(Monitor monitor, Prepare prepare) {
            this.monitor = Objects.requireNonNull(monitor);
            this.prepare = Objects.requireNonNull(prepare);
        }

        void call(String token, CallArgs args) {
            call(token, args, null, CallOptions.Empty);
        }

        void call(String token, CallArgs args, @Nullable Resource self) {
            call(token, args, self, CallOptions.Empty);
        }

        void call(String token, CallArgs args, @Nullable Resource self, CallOptions options) {
            OutputDefault.of(callRawAsync(token, args, self, options).thenApply(unused -> null));
        }

        <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args) {
            return call(token, targetType, args, null, CallOptions.Empty);
        }

        <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self) {
            return call(token, targetType, args, self, CallOptions.Empty);
        }

        <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, CallOptions options) {
            return OutputDefault.of(callAsync(token, targetType, args, self, options));
        }

        private <T> CompletableFuture<InputOutputData<T>> callAsync(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, CallOptions options) {
            Objects.requireNonNull(token);
            Objects.requireNonNull(targetType);
            Objects.requireNonNull(args);
            Objects.requireNonNull(options);

            return callRawAsync(token, args, self, options).thenApply(
                    r -> Converter.convertValue(
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

            Log.debug(String.format("Calling function: token='%s' asynchronously", token));

            // Wait for all values to be available, and then perform the RPC.
            var serializedFuture = args.internalToOptionalMapAsync()
                    .thenApply(argsDict -> self == null
                            ? argsDict
                            : ImmutableMap.<String, Optional<Object>>builder()
                            .putAll(argsDict)
                            .put("__self__", Optional.of(self))
                            .build()
                    )
                    .thenCompose(
                            argsDict -> Serialization.serializeFilteredPropertiesAsync(
                                    String.format("call:%s", token), argsDict, ignore -> true, true)
                    );


            // Determine the provider and version to use.
            var provider = self == null ? getProviderFrom(token, options) : self.internalGetProvider();
            var version = self == null ? options.getVersion() : self.internalGetVersion();

            CompletableFuture<Optional<String>> providerFuture = CompletableFutures.flipOptional(
                    () -> provider.map(p -> p.accept(ProviderResource.registrationIdVisitor()))
            );

            return CompletableFuture.allOf(serializedFuture, providerFuture)
                    .thenCompose(unused -> {
                        var serialized = serializedFuture.join();
                        var providerReference = providerFuture.join();

                        // Add arg dependencies to the request.
                        var argDependencies = CompletableFutures.allOf(
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

                        Log.debug(String.format("Call RPC prepared: token='%s'", token) +
                                (DeploymentState.ExcessiveDebugOutput ? String.format(", obj='%s'", serialized) : ""));

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

        private static Optional<ProviderResource> getProviderFrom(String token, CallOptions options) {
            return options.accept(CallOptions.NestedProviderVisitor.of(token));
        }
    }

    private static class CallException extends RuntimeException {
        public CallException(String message) {
            super(message);
        }
    }

    private static final class Prepare {

        private final FeatureSupport featureSupport;
        private final RootResource rootResource;

        private Prepare(FeatureSupport featureSupport, RootResource rootResource) {
            this.featureSupport = Objects.requireNonNull(featureSupport);
            this.rootResource = Objects.requireNonNull(rootResource);
        }

        private CompletableFuture<PrepareResult> prepareResourceAsync(
                String label, Resource res, boolean custom, boolean remote,
                ResourceArgs args, ResourceOptions options) {

            var type = res.getResourceType();
            var name = res.getResourceName();

            // Before we can proceed, all our dependencies must be finished.
            logExcessive("Gathering explicit dependencies: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);
            return gatherExplicitDependenciesAsync(options.getDependsOn())
                    .thenApply(ImmutableSet::copyOf)
                    .thenCompose(explicitDirectDependencies -> {
                        logExcessive("Gathered explicit dependencies: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);

                        // Serialize out all our props to their final values. In doing so, we'll also collect all
                        // the Resources pointed to by any Dependency objects we encounter, adding them to 'propertyDependencies'.
                        logExcessive("Serializing properties: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);
                        return args.internalToOptionalMapAsync().thenCompose(
                                map -> this.featureSupport.monitorSupportsResourceReferences().thenCompose(
                                        supportsResourceReferences -> Serialization.serializeResourcePropertiesAsync(label, map, supportsResourceReferences).thenCompose(
                                                serializationResult -> {
                                                    var serializedProps = serializationResult.serialized;
                                                    var propertyToDirectDependencies = serializationResult.propertyToDependentResources;
                                                    logExcessive("Serialized properties: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);

                                                    // Wait for the parent to complete.
                                                    // If no parent was provided, parent to the root resource.
                                                    logExcessive("Getting parent urn: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);

                                                    var parentUrn = options.getParent().isPresent()
                                                            ? TypedInputOutput.cast(options.getParent().get().getUrn()).view(InputOutputData::getValueOptional)
                                                            : this.rootResource.getRootResourceAsync(type);
                                                    return parentUrn.thenCompose(
                                                            (Optional<String> pUrn) -> {
                                                                logExcessive("Got parent urn: t=%s, name=%s, custom=%s, remote=%s", type, name, custom, remote);
                                                                var providerRef = custom
                                                                        ? CompletableFutures.flipOptional(options.getProvider().map(p -> p.accept(ProviderResource.registrationIdVisitor())))
                                                                        : CompletableFuture.completedFuture(Optional.<String>empty());

                                                                return providerRef.thenCompose(
                                                                        (Optional<String> pRef) -> {
                                                                            final CompletableFuture<ImmutableMap<String, String>> providerFutures;
                                                                            if (remote && options instanceof ComponentResourceOptions) {
                                                                                var componentOpts = (ComponentResourceOptions) options;

                                                                                // TODO: C# had the following logic here:
                                                                                //          "If only the Provider opt is set, move it to the Providers list for further processing."
                                                                                //       But our ComponentResourceOptions should guarantee the desired semantics.
                                                                                //       It would be great to add more tests and maybe remove 'provider' in favour of 'providers' only

                                                                                providerFutures = CompletableFutures.allOf(
                                                                                        componentOpts.getProviders().stream()
                                                                                                .collect(toMap(
                                                                                                        p -> p.accept(ProviderResource.packageVisitor()),
                                                                                                        p -> p.accept(ProviderResource.registrationIdVisitor())
                                                                                                )))
                                                                                        .thenApply(ImmutableMap::copyOf);
                                                                            } else {
                                                                                providerFutures = CompletableFuture.completedFuture(ImmutableMap.of());
                                                                            }

                                                                            return providerFutures.thenCompose(providerRefs -> {
                                                                                // Collect the URNs for explicit/implicit dependencies for the engine so that it can understand
                                                                                // the dependency graph and optimize operations accordingly.

                                                                                // The list of all dependencies (implicit or explicit).
                                                                                var allDirectDependencies = new HashSet<>(explicitDirectDependencies);

                                                                                var allDirectDependencyUrnsFuture =
                                                                                        CompletableFutures.builder(getAllTransitivelyReferencedResourceUrnsAsync(explicitDirectDependencies));
                                                                                var propertyToDirectDependencyUrnFutures = new HashMap<String, CompletableFuture<ImmutableSet<String>>>();

                                                                                for (var entry : propertyToDirectDependencies.entrySet()) {
                                                                                    var propertyName = entry.getKey();
                                                                                    var directDependencies = entry.getValue();

                                                                                    allDirectDependencies.addAll(directDependencies);

                                                                                    var urns = getAllTransitivelyReferencedResourceUrnsAsync(
                                                                                            ImmutableSet.copyOf(directDependencies)
                                                                                    );
                                                                                    allDirectDependencyUrnsFuture.accumulate(urns, (s1, s2) -> Sets.union(s1, s2).immutableCopy());
                                                                                    propertyToDirectDependencyUrnFutures.put(propertyName, urns);
                                                                                }

                                                                                var propertyToDirectDependencyUrnsFuture =
                                                                                        CompletableFutures.allOf(propertyToDirectDependencyUrnFutures)
                                                                                                .thenApply(ImmutableMap::copyOf);

                                                                                return allDirectDependencyUrnsFuture.build().thenCompose(
                                                                                        allDirectDependencyUrns -> propertyToDirectDependencyUrnsFuture.thenCompose(
                                                                                                propertyToDirectDependencyUrns -> {

                                                                                                    // Wait for all aliases. Note that we use 'res.aliases' instead of 'options.aliases' as
                                                                                                    // the former has been processed in the Resource constructor prior to calling
                                                                                                    // 'registerResource' - both adding new inherited aliases and simplifying aliases down to URNs.
                                                                                                    var aliasesFuture = CompletableFutures.allOf(
                                                                                                            res.internalGetAliases().stream()
                                                                                                                    .map(alias -> TypedInputOutput.cast(alias).view(v -> v.getValueOrDefault("")))
                                                                                                                    .collect(toSet()))
                                                                                                            .thenApply(completed -> completed.stream()
                                                                                                                    .filter(Strings::isNonEmptyOrNull)
                                                                                                                    .collect(toImmutableSet())); // the Set will make sure the aliases de-duplicated

                                                                                                    return aliasesFuture.thenApply(aliases -> new PrepareResult(
                                                                                                            serializedProps,
                                                                                                            pUrn.orElse(""),
                                                                                                            pRef.orElse(""),
                                                                                                            providerRefs,
                                                                                                            allDirectDependencyUrns,
                                                                                                            propertyToDirectDependencyUrns,
                                                                                                            aliases.asList()
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

        private CompletableFuture<List<Resource>> gatherExplicitDependenciesAsync(Input<List<Resource>> resources) {
            return TypedInputOutput.cast(resources).view(d -> d.getValueOrDefault(List.of()));
        }

        private CompletableFuture<ImmutableSet<String>> getAllTransitivelyReferencedResourceUrnsAsync(
                ImmutableSet<Resource> resources
        ) {
            // Go through 'resources', but transitively walk through **Component** resources, collecting any
            // of their child resources.  This way, a Component acts as an aggregation really of all the
            // reachable resources it parents.  This walking will stop when it hits custom resources.
            //
            // This function also terminates at remote components, whose children are not known to the Node SDK directly.
            // Remote components will always wait on all of their children, so ensuring we return the remote component
            // itself here and waiting on it will accomplish waiting on all of it's children regardless of whether they
            // are returned explicitly here.
            //
            // In other words, if we had:
            //
            //                  Comp1
            //              /     |     \
            //          Cust1   Comp2  Remote1
            //                  /   \       \
            //              Cust2   Cust3  Comp3
            //              /                 \
            //          Cust4                Cust5
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
                            return resource.internalGetRemote();
                        }
                        return false; // Unreachable
                    })
                    .map(resource -> TypedInputOutput.cast(resource.getUrn()).view(d -> d.getValueOrDefault("")))
                    .collect(toImmutableSet());
            return CompletableFutures.allOf(transitivelyReachableCustomResources)
                    .thenApply(ts -> ts.stream()
                            .filter(Strings::isNonEmptyOrNull)
                            .collect(toImmutableSet())
                    );
        }

        /**
         * Recursively walk the resources passed in, returning them and all resources reachable
         * from @see {@link Resource#getChildResources()} through any **Component** resources we encounter.
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
                        synchronized (resource.getChildResources()) {
                            childResources.addAll(resource.getChildResources());
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
        public final ImmutableList<String> aliases;

        public PrepareResult(
                Struct serializedProps,
                String parentUrn,
                String providerRef,
                ImmutableMap<String, String> providerRefs,
                ImmutableSet<String> allDirectDependencyUrns,
                ImmutableMap<String, ImmutableSet<String>> propertyToDirectDependencyUrns,
                ImmutableList<String> aliases
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
    public void readOrRegisterResource(Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args, ResourceOptions options) {
        this.readOrRegisterResource.readOrRegisterResource(resource, remote, newDependency, args, options);
    }

    private static final class ReadOrRegisterResource {

        private final Runner runner;
        private final Invoke invoke;
        private final ReadResource readResource;
        private final RegisterResource registerResource;
        private final boolean isDryRun;

        private ReadOrRegisterResource(Runner runner, Invoke invoke, ReadResource readResource, RegisterResource registerResource, boolean isDryRun) {
            this.runner = Objects.requireNonNull(runner);
            this.invoke = Objects.requireNonNull(invoke);
            this.readResource = Objects.requireNonNull(readResource);
            this.registerResource = Objects.requireNonNull(registerResource);
            this.isDryRun = isDryRun;
        }

        public void readOrRegisterResource(Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args, ResourceOptions options) {
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
            // the `@OutputExport(...) Output<T>` properties. We need those properties assigned by the
            // time the base 'Resource' constructor finishes so that both derived classes and
            // external consumers can use the Output properties of `resource`.
            var completionSources = OutputCompletionSource.initializeOutputs(resource);

            this.runner.registerTask(
                    String.format("readOrRegisterResource: %s-%s", resource.getResourceType(), resource.getResourceName()),
                    completeResourceAsync(resource, remote, newDependency, args, options, completionSources));
        }

        /**
         * Calls @see {@link #readOrRegisterResource(Resource, boolean, Function, ResourceArgs, ResourceOptions)}"
         * then completes all the @see {@link OutputCompletionSource} sources on the {@code resource}
         * with the results of it.
         */
        private CompletableFuture<Void> completeResourceAsync(
                Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args,
                ResourceOptions options, ImmutableMap<String, OutputCompletionSource<?>> completionSources
        ) {
            return readOrRegisterResourceAsync(resource, remote, newDependency, args, options)
                    .thenCompose(response ->
                            CompletableFuture.supplyAsync(() -> {
                                var urn = response.t1;
                                var id = response.t2;
                                var data = response.t3;
                                var dependencies = response.t4;


                                // Run in a try/catch/finally so that we always resolve all the outputs of the resource
                                // regardless of whether we encounter an errors computing the action.
                                try {
                                    completionSources.get(Constants.UrnPropertyName).setStringValue(urn, true);

                                    if (resource instanceof CustomResource) {
                                        completionSources.get(Constants.IdPropertyName)
                                                .setStringValue(id == null ? "" : id, isNonEmptyOrNull(id));
                                    }

                                    // Go through all our output fields and lookup a corresponding value in the response
                                    // object.  Allow the output field to deserialize the response.
                                    for (var entry : completionSources.entrySet()) {
                                        var fieldName = entry.getKey();
                                        OutputCompletionSource completionSource = entry.getValue();
                                        if (Constants.UrnPropertyName.equals(fieldName) || Constants.IdPropertyName.equals(fieldName)) {
                                            // Already handled specially above.
                                            continue;
                                        }

                                        // We process and deserialize each field (instead of bulk processing
                                        // 'response.data' so that each field can have independent isKnown/isSecret values.
                                        // We do not want to bubble up isKnown/isSecret from one field to the rest.
                                        var value = Structs.tryGetValue(data, fieldName);
                                        if (value.isPresent()) {
                                            var contextInfo = String.format("%s.%s", resource.getClass().getTypeName(), fieldName);
                                            var depsOrEmpty = Maps.tryGetValue(dependencies, fieldName).orElse(ImmutableSet.of());
                                            completionSource.setValue(Converter.convertValue(
                                                    contextInfo,
                                                    value.get(),
                                                    completionSource.getTypeShape(),
                                                    depsOrEmpty
                                            ));
                                        }
                                    }
                                } catch (Exception e) {
                                    // Mark any unresolved output properties with this exception. That way we don't
                                    // leave any outstanding tasks sitting around which might cause hangs.
                                    for (var source : completionSources.values()) {
                                        source.trySetException(e);
                                    }

                                    throw e;
                                } finally {
                                    // Ensure that we've at least resolved all our completion sources. That way we
                                    // don't leave any outstanding tasks sitting around which might cause hangs.
                                    for (var source : completionSources.values()) {
                                        // Didn't get a value for this field. Resolve it with a default value.
                                        // If we're in preview, we'll consider this unknown and in a normal
                                        // update we'll consider it known.
                                        source.trySetDefaultResult(!this.isDryRun);
                                    }
                                }

                                //noinspection RedundantCast
                                return (Void) null;
                            }));
        }

        private CompletableFuture<Tuple4<String /* urn */, String /* id */, Struct, ImmutableMap<String, ImmutableSet<Resource>>>> readOrRegisterResourceAsync(
                Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args,
                ResourceOptions options
        ) {
            if (options.getUrn().isPresent()) {
                // This is a resource that already exists. Read its state from the engine.
                return this.invoke.invokeRawAsync(
                        "pulumi:pulumi:getResource",
                        new GetResourceInvokeArgs(options.getUrn().get()),
                        InvokeOptions.Empty
                ).thenApply(invokeResult -> {
                    var result = invokeResult.serialized;
                    var urn = result.getFieldsMap().get(Constants.UrnPropertyName).getStringValue();
                    var id = result.getFieldsMap().get(Constants.IdPropertyName).getStringValue();
                    var state = result.getFieldsMap().get(Constants.StatePropertyName).getStructValue();
                    return Tuples.of(urn, id, state, ImmutableMap.of());
                });
            }

            if (options.getId().isPresent()) {
                return TypedInputOutput.cast(options.getId().get())
                        .view(d -> d.getValueOrDefault(""))
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
        @InputImport(name = Constants.UrnPropertyName, required = true)
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

        private final Prepare prepare;
        private final Monitor monitor;

        private ReadResource(Prepare prepare, Monitor monitor) {
            this.prepare = Objects.requireNonNull(prepare);
            this.monitor = Objects.requireNonNull(monitor);
        }

        private CompletableFuture<Tuple4<String /* urn */, String /* id */, Struct, ImmutableMap<String, ImmutableSet<Resource>>>> readResourceAsync(
                Resource resource, String id, ResourceArgs args, ResourceOptions options
        ) {
            var name = resource.getResourceName();
            var type = resource.getResourceType();
            var label = String.format("resource:%s[%s]#...", name, type);
            Log.debug(String.format("Reading resource: id=%s, type=%s, name=%s", id, type, name));

            return this.prepare.prepareResourceAsync(label, resource, /* custom */ true, /* remote */ false, args, options)
                    .thenCompose(prepareResult -> {
                        Log.debug(String.format(
                                "ReadResource RPC prepared: id=%s, type=%s, name=%s", id, type, name) +
                                (DeploymentState.ExcessiveDebugOutput ? String.format(", obj=%s", prepareResult.serializedProps) : "")
                        );

                        // Create a resource request and do the RPC.
                        var request = ReadResourceRequest.newBuilder()
                                .setType(type)
                                .setName(name)
                                .setId(id)
                                .setParent(prepareResult.parentUrn)
                                .setProvider(prepareResult.providerRef)
                                .setVersion(options.getVersion().orElse(""))
                                .setAcceptSecrets(true)
                                .setAcceptResources(!DeploymentState.DisableResourceReferences);

                        for (int i = 0; i < prepareResult.allDirectDependencyUrns.size(); i++) {
                            request.setDependencies(i, prepareResult.allDirectDependencyUrns.asList().get(i));
                        }

                        // Now run the operation, serializing the invocation if necessary.
                        return this.monitor.readResourceAsync(resource, request.build())
                                .thenApply(response -> Tuples.of(
                                        response.getUrn(), id, response.getProperties(), ImmutableMap.of()
                                ));

                    });
        }
    }

    private static final class RegisterResource {

        private final Prepare prepare;
        private final Monitor monitor;

        private RegisterResource(Prepare prepare, Monitor monitor) {
            this.prepare = Objects.requireNonNull(prepare);
            this.monitor = Objects.requireNonNull(monitor);
        }

        private CompletableFuture<Tuple4<String /* urn */, String /* id */, Struct, ImmutableMap<String, ImmutableSet<Resource>>>> registerResourceAsync(
                Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args,
                ResourceOptions options) {
            var name = resource.getResourceName();
            var type = resource.getResourceType();
            var custom = resource instanceof CustomResource;

            Log.debug(String.format(
                    "Preparing resource: t=%s, name=%s, custom=%s, remote=%s",
                    type, name, custom, remote
            ));
            var label = String.format("resource:%s[%s]", name, type);
            return this.prepare.prepareResourceAsync(label, resource, custom, remote, args, options)
                    .thenCompose(prepareResult -> {
                        Log.debug(String.format(
                                "Prepared resource: t=%s, name=%s, custom=%s, remote=%s",
                                type, name, custom, remote
                        ));

                        var request = createRegisterResourceRequest(
                                type, name, custom, remote, options, prepareResult
                        );

                        Log.debug(String.format(
                                "Registering resource monitor start: t=%s, name=%s, custom=%s, remote=%s",
                                type, name, custom, remote
                        ));
                        return this.monitor.registerResourceAsync(resource, request)
                                .thenApply(result -> {
                                    Log.debug(String.format(
                                            "Registering resource monitor end: t=%s, name=%s, custom=%s, remote=%s",
                                            type, name, custom, remote
                                    ));

                                    var dependencies = result.getPropertyDependenciesMap().entrySet().stream()
                                            .collect(toImmutableMap(
                                                    Map.Entry::getKey,
                                                    entry -> entry.getValue().getUrnsList().stream()
                                                            .map(newDependency)
                                                            .collect(toImmutableSet())
                                            ));

                                    return Tuples.of(result.getUrn(), result.getId(), result.getObject(), dependencies);
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
                    .setImportId(customOpts ? ((CustomResourceOptions) options).getImportId().orElse("") : "")
                    .setAcceptSecrets(true)
                    .setAcceptResources(!DeploymentState.DisableResourceReferences)
                    .setDeleteBeforeReplace(customOpts && ((CustomResourceOptions) options).getDeleteBeforeReplace())
                    .setDeleteBeforeReplaceDefined(true) // FIXME: WTF is an undefined boolean?!
                    .setCustomTimeouts(
                            RegisterResourceRequest.CustomTimeouts.newBuilder()
                                    .setCreate(customTimeoutToGolangString.apply(CustomTimeouts::getCreate))
                                    .setDelete(customTimeoutToGolangString.apply(CustomTimeouts::getDelete))
                                    .setUpdate(customTimeoutToGolangString.apply(CustomTimeouts::getUpdate))
                                    .build()
                    )
                    .setRemote(remote);

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
    public void registerResourceOutputs(Resource resource, Output<Map<String, Optional<Object>>> outputs) {
        this.registerResourceOutputs.registerResourceOutputs(resource, outputs);
    }

    private static final class RegisterResourceOutputs {

        private final Runner runner;
        private final Monitor monitor;
        private final FeatureSupport featureSupport;

        private RegisterResourceOutputs(Runner runner, Monitor monitor, FeatureSupport featureSupport) {
            this.runner = Objects.requireNonNull(runner);
            this.monitor = Objects.requireNonNull(monitor);
            this.featureSupport = Objects.requireNonNull(featureSupport);
        }

        public void registerResourceOutputs(Resource resource, Output<Map<String, Optional<Object>>> outputs) {
            // RegisterResourceOutputs is called in a fire-and-forget manner.  Make sure we keep track of
            // this task so that the application will not quit until this async work completes.
            this.runner.registerTask(
                    String.format("DeploymentInternalInternal.registerResourceOutputs: %s-%s", resource.getResourceType(), resource.getResourceName()),
                    registerResourceOutputsAsync(resource, outputs));
        }

        private CompletableFuture<Void> registerResourceOutputsAsync(
                Resource resource, Output<Map<String, Optional<Object>>> outputs
        ) {
            var opLabel = "monitor.registerResourceOutputs(...)";

            var urnFuture = TypedInputOutput.cast(resource.getUrn())
                    .view(d -> d.getValueOrDefault(""));
            var propsFuture = TypedInputOutput.cast(outputs)
                    .view(d -> d.getValueOrDefault(Map.of()));

            BiFunction<String, Struct, CompletableFuture<Void>> registerResourceOutputsAsync = (urn, serialized) -> {
                if (Strings.isEmptyOrNull(urn)) {
                    throw new IllegalStateException(String.format("Expected a urn at this point, got: '%s'", urn));
                }
                Log.debug(String.format("RegisterResourceOutputs RPC prepared: urn='%s'", urn) +
                        (DeploymentState.ExcessiveDebugOutput ?
                                String.format(", outputs=%s", JsonFormatter
                                        .format(serialized)
                                        .orThrow(Function.identity()))
                                : ""
                        ));

                return this.monitor.registerResourceOutputsAsync(
                        pulumirpc.Resource.RegisterResourceOutputsRequest.newBuilder()
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
                                    keepResources -> Serialization.serializeAllPropertiesAsync(opLabel, props, keepResources).thenCompose(
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
            if (Stack.InternalRootPulumiStackTypeName.equals(type)) {
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
            return TypedInputOutput.cast(stack.getUrn())
                    .view(InputOutputData::getValueNullable)
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

    private final static class Serialization {

        private Serialization() {
            throw new UnsupportedOperationException("static class");
        }

        /**
         * Walks the props object passed in, awaiting all interior promises besides those
         * for @see {@link Resource#getUrn()} and @see {@link CustomResource#getId()},
         * creating a reasonable POJO object that can be remoted over to registerResource.
         */
        private static CompletableFuture<SerializationResult> serializeResourcePropertiesAsync(
                String label, Map<String, Optional<Object>> args, boolean keepResources
        ) {
            Predicate<String> filter = key -> !Constants.IdPropertyName.equals(key) && !Constants.UrnPropertyName.equals(key);
            return serializeFilteredPropertiesAsync(label, args, filter, keepResources);
        }

        private static CompletableFuture<Struct> serializeAllPropertiesAsync(
                String label, Map<String, Optional<Object>> args, boolean keepResources
        ) {
            return serializeFilteredPropertiesAsync(label, args, unused -> true, keepResources)
                    .thenApply(result -> result.serialized);
        }

        /**
         * walks the props object passed in, awaiting all interior promises for properties
         * with keys that match the provided filter, creating a reasonable POJO object that
         * can be remoted over to registerResource.
         */
        private static CompletableFuture<SerializationResult> serializeFilteredPropertiesAsync(
                String label, Map<String, Optional<Object>> args, Predicate<String> acceptKey, boolean keepResources) {
            var resultFutures = new HashMap<String, CompletableFuture</* @Nullable */ Object>>();
            var temporaryResources = new HashMap<String, Set<Resource>>();

            // FIXME: this is ugly, try to factor out a method with named tuple as result type
            for (var arg : args.entrySet()) {
                var key = arg.getKey();
                var value = arg.getValue();
                if (acceptKey.test(key)) {
                    // We treat properties with null values as if they do not exist, CompletableFutures.allOf does it for us
                    var serializer = new Serializer(DeploymentState.ExcessiveDebugOutput); // serializer is mutable, that's why it's inside the loop
                    var v = serializer.serializeAsync(String.format("%s.%s", label, key), value, keepResources);
                    resultFutures.put(key, v);
                    temporaryResources.put(key, serializer.dependentResources);
                }
            }

            return CompletableFutures.flatAllOf(resultFutures)
                    .thenApply(results -> results.entrySet().stream()
                            .filter(ignoreNullMapValues())
                            .collect(toTupleOfMaps2(
                                    Map.Entry::getKey,
                                    Map.Entry::getKey,
                                    e -> e.getValue().join(),
                                    e -> temporaryResources.get(e.getKey())
                            ))
                    ).thenApply(
                            results -> new SerializationResult(
                                    Serializer.createStruct(results.t1),
                                    results.t2
                            )
                    );
        }

        @ParametersAreNonnullByDefault
        private static class SerializationResult {
            public final Struct serialized;
            public final ImmutableMap<String, Set<Resource>> propertyToDependentResources;

            public SerializationResult(
                    Struct result,
                    ImmutableMap<String, Set<Resource>> propertyToDependentResources) {
                this.serialized = result;
                this.propertyToDependentResources = propertyToDependentResources;
            }
        }
    }

    @ParametersAreNonnullByDefault
    @Internal
    @VisibleForTesting
    static class DeploymentState {
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

        @Internal
        @VisibleForTesting
        DeploymentState(
                DeploymentImpl.Config config,
                Logger standardLogger,
                String projectName,
                String stackName,
                boolean isDryRun,
                Engine engine,
                Monitor monitor) {
            this.standardLogger = Objects.requireNonNull(standardLogger);
            this.config = Objects.requireNonNull(config);
            this.projectName = Objects.requireNonNull(projectName);
            this.stackName = Objects.requireNonNull(stackName);
            this.isDryRun = isDryRun;
            this.engine = Objects.requireNonNull(engine);
            this.monitor = Objects.requireNonNull(monitor);
            postInit();
        }

        private void postInit() {
            this.logger = new DefaultEngineLogger(this, standardLogger);
            this.runner = new DefaultRunner(this, standardLogger);
        }
    }

    @ParametersAreNonnullByDefault
    @Internal
    @VisibleForTesting
    static class DefaultRunner implements Runner {
        private static final int ProcessExitedSuccessfully = 0;
        private static final int ProcessExitedBeforeLoggingUserActionableMessage = 1;
        // Keep track if we already logged the information about an unhandled error to the user.
        // If so, we end with a different exit code. The language host recognizes this and will not print
        // any further messages to the user since we already took care of it.
        //
        // 32 was picked so as to be very unlikely to collide with any other error codes.
        private static final int ProcessExitedAfterLoggingUserActionableMessage = 32;

        private final EngineLogger engineLogger;
        private final Logger standardLogger;

        /**
         * The set of tasks (futures) that we have fired off. We issue futures in a Fire-and-Forget manner
         * to be able to expose a Synchronous @see {@link io.pulumi.resources.Resource} model for users.
         * i.e. a user just synchronously creates a resource, and we asynchronously kick off the work
         * to populate it.
         * This works well, however we have to make sure the console app
         * doesn't exit because it thinks there is no work to do.
         * <p/>
         * To ensure that doesn't happen, we have the main entrypoint of the app just
         * continuously, asynchronously loop, waiting for these tasks to complete, and only
         * exiting once the set becomes empty.
         */
        private final Map<CompletableFuture<Void>, List<String>> inFlightTasks = new ConcurrentHashMap<>();
        private final Queue<Exception> swallowedExceptions = new ConcurrentLinkedQueue<>();

        public DefaultRunner(DeploymentState deployment, Logger standardLogger) {
            this.engineLogger = Objects.requireNonNull(Objects.requireNonNull(deployment).logger);
            this.standardLogger = Objects.requireNonNull(standardLogger);
        }

        public List<Exception> getSwallowedExceptions() {
            return ImmutableList.copyOf(this.swallowedExceptions);
        }

        /**
         * @param stackType the Stack type class instance, if class is nested it must be static
         * @param <T>       the Stack type, if class is nested it must be static
         */
        @Override
        public <T extends Stack> CompletableFuture<Integer> runAsync(Class<T> stackType) {
            Objects.requireNonNull(stackType);
            if (Reflection.isNestedClass(stackType)) {
                throw new IllegalArgumentException(String.format(
                        "runAsync(Class<T>) cannot be used with nested classes, make class '%s' static, standalone or use runAsync(Supplier<T extends Stack>)",
                        stackType.getTypeName()
                ));
            }
            return runAsync(() -> {
                try {
                    return stackType.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException(String.format(
                            "Couldn't create an instance of the stack type: '%s', error: %s",
                            stackType.getTypeName(), e
                    ), e);
                }
            });
        }

        @Override
        public <T extends Stack> CompletableFuture<Integer> runAsync(Supplier<T> stackFactory) {
            try {
                var stack = stackFactory.get();
                // Stack doesn't call RegisterOutputs, so we register them on its behalf.
                stack.internalRegisterPropertyOutputs();
                registerTask(String.format("runAsync: %s, %s", stack.getResourceType(), stack.getResourceName()),
                        TypedInputOutput.cast(stack.internalGetOutputs()).internalGetDataAsync());
            } catch (Exception ex) {
                return handleExceptionAsync(ex);
            }

            return whileRunningAsync();
        }

        @Override
        public CompletableFuture<Integer> runAsyncFuture(Supplier<CompletableFuture<Map<String, Optional<Object>>>> callback, @Nullable StackOptions options) {
            var stack = new Stack(callback, options);
            registerTask(String.format("runAsyncFuture: %s, %s", stack.getResourceType(), stack.getResourceName()),
                    TypedInputOutput.cast(stack.internalGetOutputs()).internalGetDataAsync());
            return whileRunningAsync();
        }

        @Override
        public <T> void registerTask(String description, CompletableFuture<T> task) {
            Objects.requireNonNull(description);
            Objects.requireNonNull(task);
            standardLogger.log(Level.FINEST, String.format("Registering task: '%s', %s", description, task));

            // we don't need the result here, just the future itself
            CompletableFuture<Void> key = task.thenApply(ignore -> null);
            // TODO: should we enforce a timeout (with .orTimeout()) on the task (and make it configurable)?

            // We may get several of the same tasks with different descriptions. That can
            // happen when the runtime reuses cached tasks that it knows are value-identical
            // (for example a completed future). In that case, we just store all the descriptions.
            // We'll print them all out as done once this task actually finishes.
            inFlightTasks.compute(key,
                    (ignore, descriptions) -> {
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
        private CompletableFuture<Integer> whileRunningAsync() {
            // Getting error information from a logger is slightly ugly, but that's what C# implementation does
            Supplier<Integer> exitCode = () -> this.engineLogger.hasLoggedErrors()
                    ? ProcessExitedBeforeLoggingUserActionableMessage
                    : ProcessExitedSuccessfully;

            // Wait for every task and remove from inFlightTasks when completed
            Consumer<CompletableFuture<Void>> handleCompletion = (task) -> {
                try {
                    // Wait for the task completion (non-blocking).
                    // At this point it is guaranteed by the execution loop bellow, that the task is already complete.
                    if (!task.isDone()) {
                        throw new IllegalStateException(
                                String.format("expected task to be done at this point, it was not: %s, %s",
                                        inFlightTasks.get(task),
                                        task
                                )
                        );
                    }
                    task.join();

                    // Log the descriptions of completed tasks.
                    if (standardLogger.isLoggable(Level.FINEST)) {
                        List<String> descriptions = inFlightTasks.getOrDefault(task, List.of());
                        // getOrDefault should never return null, but it does for whatever reason, so just to be sure
                        if (descriptions == null) {
                            descriptions = List.of();
                        }
                        standardLogger.log(Level.FINEST, String.format("Completed task: '%s', %s", String.join(",", descriptions), task));
                    }
                } catch (Exception e) {
                    standardLogger.log(Level.FINEST, String.format("Failed task: '%s', exception: %s", inFlightTasks.get(task), e));
                    throw e;
                } finally {
                    // Once finished, remove the task from the set of tasks that are running.
                    this.inFlightTasks.remove(task);
                }
            };

            // Keep looping as long as there are outstanding tasks that are still running.
            while (inFlightTasks.size() > 0) {
                standardLogger.log(Level.FINEST, String.format("Remaining tasks [%s]: %s", inFlightTasks.size(), inFlightTasks));

                // Grab all the tasks we currently have running.
                for (var task : inFlightTasks.keySet()) {
                    try {
                        if (task.isDone()) {
                            // at this point the future is guaranteed to be solved
                            // so there won't be any blocking here
                            handleCompletion.accept(task); // will remove from inFlightTasks
                        } else {
                            standardLogger.log(Level.FINEST, String.format("Tasks not done: %s", task));
                            // will attempt again in the next iteration
                        }
                    } catch (Exception e) {
                        return handleExceptionAsync(e);
                    }
                }
            }

            // There were no more tasks we were waiting on.
            // Quit out, reporting if we had any errors or not.
            return CompletableFuture.completedFuture(exitCode.get());
        }

        private CompletableFuture<Integer> handleExceptionAsync(Exception exception) {
            this.swallowedExceptions.add(exception);

            Function<Void, Integer> exitMessageAndCode = unused -> {
                standardLogger.log(Level.FINE, "Returning from program after last error");
                return ProcessExitedAfterLoggingUserActionableMessage;
            };

            if (exception instanceof LogException) {
                // We got an error while logging itself.
                // Nothing to do here but print some errors and abort.
                standardLogger.log(Level.SEVERE, String.format(
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
            // end up bubbling up an exception that will be caught
            // by nothing. This will tear down the actual process
            // with a non-zero error which our host will handle
            // properly.
            if (exception instanceof RunException) {
                // Always hide the stack for RunErrors.
                return engineLogger
                        .errorAsync(exception.getMessage())
                        .thenApply(exitMessageAndCode);
            } else if (exception instanceof ResourceException) {
                var resourceEx = (ResourceException) exception;
                var message = resourceEx.isHideStack() ? resourceEx.getMessage() : getStackTrace(resourceEx);
                return engineLogger
                        .errorAsync(message, resourceEx.getResource().orElse(null))
                        .thenApply(exitMessageAndCode);
            } else {
                var pid = ProcessHandle.current().pid();
                var command = ProcessHandle.current().info().commandLine().orElse("unknown");
                return engineLogger
                        .errorAsync(String.format(
                                "Running program [PID: %d](%s) failed with an unhandled exception:\n%s",
                                pid, command, Exceptions.getStackTrace(exception)))
                        .thenApply(exitMessageAndCode);
            }
        }
    }

    @ParametersAreNonnullByDefault
    @Internal
    @VisibleForTesting
    static class DefaultEngineLogger implements EngineLogger {
        private final DeploymentState state;
        private final Logger standardLogger;
        private final AtomicInteger errorCount;

        // We serialize all logging tasks so that the engine doesn't hear about them out of order.
        // This is necessary for streaming logs to be maintained in the right order.
        private CompletableFuture<Void> lastLogTask = CompletableFuture.allOf();
        private final Object logGate = new Object(); // lock target

        public DefaultEngineLogger(DeploymentState state, Logger standardLogger) {
            this.state = Objects.requireNonNull(state);
            this.standardLogger = Objects.requireNonNull(standardLogger);
            this.errorCount = new AtomicInteger(0);
        }

        private Runner getRunner() {
            return Objects.requireNonNull(this.state.runner);
        }

        private Engine getEngine() {
            return Objects.requireNonNull(this.state.engine);
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
            // Serialize our logging tasks so that streaming logs appear in order.
            CompletableFuture<Void> task;
            synchronized (logGate) {
                if (severity == LogSeverity.ERROR) {
                    this.errorCount.incrementAndGet();
                }

                // TODO: C# uses a 'Task.Run' here (like CompletableFuture.runAsync/supplyAsync?)
                //       so that "we don't end up aggressively running the actual logging while holding this lock."
                //       Is something similar required in Java or thenComposeAsync is enough?
                this.lastLogTask = this.lastLogTask.thenComposeAsync(
                        ignore -> logAsync(severity, message, resource, streamId, ephemeral)
                );
                task = this.lastLogTask;
            }

            getRunner().registerTask(message, task);
            return task;
        }

        private CompletableFuture<Void> logAsync(LogSeverity severity, String message,
                                                 @Nullable Resource resource, @Nullable Integer streamId,
                                                 @Nullable Boolean ephemeral) {
            try {
                return tryGetResourceUrnAsync(resource)
                        .thenCompose(
                                urn -> getEngine().logAsync(
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
                synchronized (logGate) {
                    // mark that we had an error so that our top level process quits with an error
                    // code.
                    errorCount.incrementAndGet();
                }

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
        private static CompletableFuture<String> tryGetResourceUrnAsync(@Nullable Resource resource) {
            if (resource != null) {
                try {
                    return TypedInputOutput.cast(resource.getUrn()).view(v -> v.getValueOrDefault(""));
                } catch (Throwable ignore) {
                    // getting the urn for a resource may itself fail, in that case we don't want to
                    // fail to send an logging message. we'll just send the logging message unassociated
                    // with any resource.
                }
            }

            return CompletableFuture.completedFuture("");
        }
    }

    private static void logExcessive(String message, Object... args) {
        if (DeploymentState.ExcessiveDebugOutput) {
            Log.debug(String.format(message, args));
        }
    }
}
