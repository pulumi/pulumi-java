package com.pulumi.core.internal;

import com.google.common.base.Functions;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.pulumi.Config;
import com.pulumi.core.Either;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.exceptions.RunException;
import com.pulumi.internal.ConfigInternal.ConfigMissingException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.pulumi.core.internal.Environment.getBooleanEnvironmentVariable;
import static com.pulumi.core.internal.Environment.getDoubleEnvironmentVariable;
import static com.pulumi.core.internal.Environment.getEnvironmentVariable;
import static com.pulumi.core.internal.Environment.getIntegerEnvironmentVariable;

/**
 * Helper functions that may be referenced by generated code but should not be used otherwise.
 */
// TODO: move to com.pulumi.codegen.internal
@InternalUse
public final class Codegen {

    public static <T> Output<Optional<T>> optional(Output</* Nullable */ T> out) {
        return out == null ? Output.of(Optional.empty()) : out.applyValue(Optional::ofNullable);
    }

    public static <T> Output<T> secret(@Nullable T value) {
        return Codegen.ofNullable(value).asSecret();
    }

    public static <T, O extends Output<T>> Output<T> secret(@Nullable O value) {
        return Codegen.ofNullable(value).asSecret();
    }

    public static <T> Output<T> empty() {
        return Output.ofNullable(null);
    }

    public static <T> Output<T> ofNullable(@Nullable T value) {
        return value == null ? Codegen.empty() : Output.ofNullable(value);
    }

    public static <T, O extends Output<T>> Output<T> ofNullable(@Nullable O value) {
        return value == null ? Codegen.empty() : value;
    }

    public static Config config(String name) {
        return Config.of(name);
    }

    /**
     * Helps generated code combine user-provided property values with schema or environment-based defaults.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static final class PropertyBuilder<T, R> {

        private final String propertyName;
        private final Function<T, R> convert;
        private final Function<String, T> readFromEnvVar;
        private final Function<String, T> parseJsonDefault;
        private final Function<Config, Optional<T>> tryReadFromConfig;
        private final Function<R, R> finalize;

        private final List<String> envVars = new ArrayList<>();
        private Optional<Config> config = Optional.empty();
        private Optional<R> arg = Optional.empty();
        private Optional<T> defaultValue = Optional.empty();
        private Optional<String> defaultValueJson = Optional.empty();

        public PropertyBuilder(
                String propertyName,
                Function<T, R> convert,
                Function<String, T> readFromEnvVar,
                Function<String, T> parseJsonDefault,
                Function<Config, Optional<T>> tryReadFromConfig,
                Function<R, R> finalize
        ) {
            this.propertyName = propertyName;
            this.readFromEnvVar = readFromEnvVar;
            this.parseJsonDefault = parseJsonDefault;
            this.tryReadFromConfig = tryReadFromConfig;
            this.convert = convert;
            this.finalize = finalize;
        }

        /**
         * Helper for Output-typed properties.
         */
        public PropertyBuilder<T, Output<R>> output() {
            return new PropertyBuilder<>(
                    this.propertyName,
                    x -> Output.of(this.finalize.apply(this.convert.apply(x))),
                    this.readFromEnvVar,
                    this.parseJsonDefault,
                    this.tryReadFromConfig,
                    Functions.identity()
            );
        }

        /**
         * Helper for secret Output-typed properties.
         */
        public PropertyBuilder<T, Output<R>> secret() {
            return new PropertyBuilder<>(
                    this.propertyName,
                    x -> Output.of(this.finalize.apply(this.convert.apply(x))),
                    this.readFromEnvVar,
                    this.parseJsonDefault,
                    this.tryReadFromConfig,
                    x -> x.asSecret()
            );
        }

        public <X> PropertyBuilder<T, Either<X, R>> right(Class<X> __) {
            return new PropertyBuilder<>(
                    this.propertyName,
                    x -> Either.ofRight(this.finalize.apply(this.convert.apply(x))),
                    this.readFromEnvVar,
                    this.parseJsonDefault,
                    this.tryReadFromConfig,
                    Functions.identity()
            );
        }

        public <X> PropertyBuilder<T, Either<R, X>> left(Class<X> __) {
            return new PropertyBuilder<>(
                    this.propertyName,
                    x -> Either.ofLeft(this.finalize.apply(this.convert.apply(x))),
                    this.readFromEnvVar,
                    this.parseJsonDefault,
                    this.tryReadFromConfig,
                    Functions.identity()
            );
        }

        /**
         * Registers a Config object as a source of possible default values.
         */
        public PropertyBuilder<T, R> config(Config config) {
            this.config = Optional.of(config);
            return this;
        }

        /**
         * Registers environment variables to consult for defaults.
         */
        public PropertyBuilder<T, R> env(String... environmentVariable) {
            this.envVars.addAll(List.of(environmentVariable));
            return this;
        }

        /**
         * Registers a default value specified in a provider schema.
         */
        public PropertyBuilder<T, R> def(T defaultValue) {
            this.defaultValue = Optional.of(defaultValue);
            return this;
        }

        /**
         * Registers a default value specified in a provider schema for object properties. This value is JSON-serialized
         * so that the true value needs to be recovered via known `TypeShape`.
         */
        public PropertyBuilder<T, R> defJson(String defaultValueJson) {
            this.defaultValueJson = Optional.of(defaultValueJson);
            return this;
        }

        /**
         * Registers an argument value passed by the user directly. If non-null, this value must be respected, disabling
         * any and all defaulting logic.
         */
        public PropertyBuilder<T, R> arg(@Nullable R argumentValue) {
            this.arg = Optional.ofNullable(argumentValue);
            return this;
        }

        /**
         * Retrieves the final value of the property after applying defaults.
         */
        public Optional<R> get() {
            return this.getRaw().map(this.finalize);
        }

        private Optional<R> getRaw() {
            // User-provided arguments disable any defaulting logic.
            if (this.arg.isPresent()) {
                return this.arg;
            }
            // If provided, give priority to Config as the source of defaults.
            if (this.config.isPresent()) {
                var fromConfig = this.tryReadFromConfig.apply(this.config.get());
                if (fromConfig.isPresent()) {
                    return fromConfig.map(this.convert);
                }
            }
            // Next, look in the environment variables.
            var envVar = this.envVars.stream()
                    .filter(Environment::hasEnvironmentVariable)
                    .map(this.readFromEnvVar)
                    .map(this.convert)
                    .findFirst();
            if (envVar.isPresent()) {
                return envVar;
            }
            // Finally, consider schema defaults.
            if (this.defaultValue.isPresent()) {
                return this.defaultValue.map(this.convert);
            }
            if (this.defaultValueJson.isPresent()) {
                return Optional.of(this.parseJsonDefault.apply(this.defaultValueJson.get())).map(this.convert);
            }
            return Optional.empty();
        }

        /**
         * Like get() but for contexts that accept null encoding of missing values.
         */
        @Nullable
        public R getNullable() {
            return this.get().orElse(null);
        }

        /**
         * Retrieves the final value of the property after applying defaults.
         *
         * @return the final resolved value
         * @throws NullPointerException if value is unknown.
         */
        public R require() {
            Supplier<? extends RuntimeException> exceptionSupplier = () -> {
                if (this.config.isPresent()) {
                    if (this.envVars.size() > 0) {
                        return new ConfigMissingException(this.propertyName, this.envVars);
                    } else {
                        return new ConfigMissingException(this.propertyName);
                    }
                }
                var baseMsg = String.format("Expected parameter '%s' to be non-null", this.propertyName);
                var envVarsMsg = this.envVars.isEmpty() ? "" : String.format(
                        " or else an environment variable to be set: '%s'", String.join(", ", this.envVars)
                );
                return new NullPointerException(baseMsg + envVarsMsg);
            };
            return this.get().orElseThrow(exceptionSupplier);
        }
    }

    /**
     * InvalidDefaultValueException is used when a provider schema specifies a malformed default value.
     */
    private static class InvalidDefaultValueException extends RunException {
        public InvalidDefaultValueException(String key,
                                            @Nullable Object v,
                                            String expectedType,
                                            @Nullable Exception cause) {
            super(String.format("Schema-provided '%s' value '%s' is not a valid %s", key, v, expectedType), cause);
        }
    }

    public static PropertyBuilder<Integer, Integer> integerProp(String propertyName) {
        Function<String, Integer> readFromEnvVar =
                envVar -> getIntegerEnvironmentVariable(envVar).orThrow(Function.identity());
        Function<Config, Optional<Integer>> tryReadFromConfig =
                config -> config.getInteger(propertyName);
        return new PropertyBuilder<>(
                propertyName, Codegen::identityConverter, readFromEnvVar, Codegen::throwingParseJson, tryReadFromConfig,
                Functions.identity()
        );
    }

    public static PropertyBuilder<Double, Double> doubleProp(String propertyName) {
        Function<String, Double> readFromEnvVar =
                envVar -> getDoubleEnvironmentVariable(envVar).orThrow(Function.identity());
        Function<Config, Optional<Double>> tryReadFromConfig =
                config -> config.getDouble(propertyName);
        return new PropertyBuilder<>(
                propertyName, Codegen::identityConverter, readFromEnvVar, Codegen::throwingParseJson, tryReadFromConfig,
                Functions.identity()
        );
    }

    public static PropertyBuilder<Boolean, Boolean> booleanProp(String propertyName) {
        Function<String, Boolean> readFromEnvVar =
                envVar -> getBooleanEnvironmentVariable(envVar).orThrow(Function.identity());
        Function<Config, Optional<Boolean>> tryReadFromConfig =
                config -> config.getBoolean(propertyName);
        return new PropertyBuilder<>(
                propertyName, Codegen::identityConverter, readFromEnvVar, Codegen::throwingParseJson, tryReadFromConfig,
                Functions.identity()
        );
    }

    public static PropertyBuilder<String, String> stringProp(String propertyName) {
        Function<String, String> readFromEnvVar =
                envVar -> getEnvironmentVariable(envVar).orThrow(Function.identity());
        Function<Config, Optional<String>> tryReadFromConfig =
                config -> config.get(propertyName);
        return new PropertyBuilder<>(
                propertyName, Codegen::identityConverter, readFromEnvVar, Codegen::throwingParseJson, tryReadFromConfig,
                Functions.identity()
        );
    }

    public static <T> PropertyBuilder<T, T> objectProp(String propertyName, Class<T> c) {
        return objectProp(propertyName, TypeShape.builder(c).build());
    }

    public static <T> PropertyBuilder<T, T> objectProp(String propertyName, TypeShape<T> typeShape) {
        Function<String, T> parseJson = json -> {
            // TODO should this be using Pulumi deserializers from protobuf Struct instead of GSON?
            // JSON can be converted to protobuf struct.
            try {
                var gson = new Gson();
                return gson.fromJson(json, typeShape.toGSON().getType());
            } catch (JsonParseException ex) {
                throw new InvalidDefaultValueException(propertyName, json, typeShape.getTypeName(), ex);
            }
        };
        Function<String, T> readFromEnvVar =
                envVar -> getEnvironmentVariable(envVar).mapOrThrow(Function.identity(), parseJson);
        Function<Config, Optional<T>> tryReadFromConfig =
                config -> config.getObject(propertyName, typeShape);
        return new PropertyBuilder<>(
                propertyName, Codegen::identityConverter, readFromEnvVar, parseJson, tryReadFromConfig,
                Functions.identity()
        );
    }

    private static <T> T identityConverter(T input) {
        return Functions.<T>identity().apply(input);
    }

    private static <T> T throwingParseJson(String __) throws IllegalStateException {
        throw new IllegalStateException("Should only be called for object properties");
    }
}
