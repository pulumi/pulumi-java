package com.pulumi.core.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.google.common.base.Functions;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.pulumi.Config;
import com.pulumi.core.Either;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.exceptions.RunException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Helper functions that may be referenced by generated code but should not be used otherwise.
 */
@InternalUse
public final class Codegen {
    public static <T> Output<T> secret(@Nullable T value) {
        return Codegen.ofNullable(value).asSecret();
    }

    public static <T, O extends Output<T>> Output<T> secret(@Nullable O value) {
        return Codegen.ofNullable(value).asSecret();
    }

    public static <T> Output<T> empty() {
        return Output.ofNullable((T)null);
    }

    public static <T> Output<T> ofNullable(@Nullable T value) {
        return value == null ? Codegen.empty() : Output.ofNullable(value);
    }

    public static <T, O extends Output<T>> Output<T> ofNullable(@Nullable O value) {
        return value == null ? Codegen.empty() : value;
    }

    /**
     * Helps generated code combine user-provided property values with schema or environment-based defaults.
     */
    public static final class PropertyBuilder<T, Result> {
        protected final String propertyName;
        protected final Function<T, Result> convert;
        protected final Function<String, T> readFromEnvVar;
        protected final Function<String, T> parseJsonDefault;
        protected final Function<Config, Optional<T>> tryReadFromConfig;

        private Optional<Config> config = Optional.empty();
        private List<String> envVars = new ArrayList<>();
        private Optional<Result> arg = Optional.empty();
        private Optional<T> defaultValue = Optional.empty();
        private Optional<String> defaultValueJson = Optional.empty();

        public PropertyBuilder(String propertyName,
                               Function<T, Result> convert,
                               Function<String, T> readFromEnvVar,
                               Function<String, T> parseJsonDefault,
                               Function<Config, Optional<T>> tryReadFromConfig) {
            this.propertyName = propertyName;
            this.readFromEnvVar = readFromEnvVar;
            this.parseJsonDefault = parseJsonDefault;
            this.tryReadFromConfig = tryReadFromConfig;
            this.convert = convert;
        }

        /**
         * Helper for Output-typed properties.
         */
        public PropertyBuilder<T, Output<Result>> output() {
            Function<T, Output<Result>> newConvert = x -> Output.of(this.convert.apply(x));
            return new PropertyBuilder<T, Output<Result>>(this.propertyName,
                    newConvert,
                    this.readFromEnvVar,
                    this.parseJsonDefault,
                    this.tryReadFromConfig);
        }

        /**
         * Helper for secret Output-typed properties.
         */
        public PropertyBuilder<T, Output<Result>> secret() {
            Function<T, Output<Result>> newConvert = x -> Output.of(this.convert.apply(x)).asSecret();
            return new PropertyBuilder<T, Output<Result>>(this.propertyName,
                    newConvert,
                    this.readFromEnvVar,
                    this.parseJsonDefault,
                    this.tryReadFromConfig);
        }

        public <X> PropertyBuilder<T, Either<X,Result>> right(Class<X> __) {
            return new PropertyBuilder<T, Either<X,Result>>(this.propertyName,
                    x -> Either.ofRight(this.convert.apply(x)),
                    this.readFromEnvVar,
                    this.parseJsonDefault,
                    this.tryReadFromConfig);
        }

        public <X> PropertyBuilder<T, Either<Result,X>> left(Class<X> __) {
            return new PropertyBuilder<T, Either<Result,X>>(this.propertyName,
                    x -> Either.ofLeft(this.convert.apply(x)),
                    this.readFromEnvVar,
                    this.parseJsonDefault,
                    this.tryReadFromConfig);
        }

        /**
         * Registers a Config object as a source of possible default values.
         */
        public PropertyBuilder<T, Result> config(Config config) {
            this.config = Optional.of(config);
            return this;
        }

        /**
         * Registers environment variables to consult for defaults.
         */
        public PropertyBuilder<T, Result> env(String ... environmentVariable) {
            this.envVars.addAll(List.of(environmentVariable));
            return this;
        }

        /**
         * Registers a default value specified in a provider schema.
         */
        public PropertyBuilder<T, Result> def(T defaultValue) {
            this.defaultValue = Optional.of(defaultValue);
            return this;
        }

        /**
         * Registers a default value specified in a provider schema for object properties. This value is JSON-serialized
         * so that the true value needs to be recovered via known `TypeShape`.
         */
        public PropertyBuilder<T, Result> defJson(String defaultValueJson) {
            this.defaultValueJson = Optional.of(defaultValueJson);
            return this;
        }

        /**
         * Registers an argument value passed by the user directly. If non-null, this value must be respected, disabling
         * any and all defaulting logic.
         */
        public PropertyBuilder<T, Result> arg(@Nullable Result argumentValue) {
            this.arg = Optional.ofNullable(argumentValue);
            return this;
        }

        /**
         * Retrieves the final value of the property after applying defaults.
         */
        public Optional<Result> get() {
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
            for (var envVar : this.envVars) {
                if (System.getenv(envVar) != null) {
                    return Optional.of(this.readFromEnvVar.apply(envVar)).map(this.convert);
                }
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
        public @Nullable Result getNullable() {
            return this.get().orElse(null);
        }

        /**
         * Retrieves the final value of the property after applying defaults. Throws an exception if unknown.
         */
        public Result require() {
            var v = this.get();
            if (v.isPresent()) {
                return v.get();
            }
            if (this.config.isPresent()) {
                if (this.envVars.size() > 0) {
                    throw new Config.ConfigMissingException(this.propertyName, this.envVars);
                } else {
                    throw new Config.ConfigMissingException(this.propertyName);
                }
            }
            var baseMsg = String.format("expected parameter '%s' to be non-null", this.propertyName);
            var msg = (this.envVars.size() == 0) ? baseMsg :
                    String.format("%s or else an environment variable to be set: %s",
                            baseMsg, String.join(", ", this.envVars));
            throw new NullPointerException(msg);
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
        Function<Integer, Integer> convert = Functions.identity();
        Function<String, Integer> parseJsonDefault = json -> {
            throw new IllegalStateException("Should only be called for object properties");
        };
        Function<String, Integer> readFromEnvVar = envVar -> {
            var result = Environment.getIntegerEnvironmentVariable(envVar);
            if (result.isRight()) {
                return result.right();
            }
            throw (RuntimeException)result.left();
        };
        Function<Config, Optional<Integer>> tryReadFromConfig = config -> config.getInteger(propertyName);
        return new PropertyBuilder<Integer, Integer>(propertyName,
            convert, readFromEnvVar, parseJsonDefault, tryReadFromConfig);
    }

    public static PropertyBuilder<Double, Double> doubleProp(String propertyName) {
        Function<Double, Double> convert = Functions.identity();
        Function<String, Double> parseJsonDefault = json -> {
            throw new IllegalStateException("Should only be called for object properties");
        };
        Function<String, Double> readFromEnvVar = envVar -> {
            var result = Environment.getDoubleEnvironmentVariable(envVar);
            if (result.isRight()) {
                return result.right();
            }
            throw (RuntimeException)result.left();
        };
        Function<Config, Optional<Double>> tryReadFromConfig = config -> config.getDouble(propertyName);
        return new PropertyBuilder<Double, Double>(propertyName,
                convert, readFromEnvVar, parseJsonDefault, tryReadFromConfig);
    }

    public static PropertyBuilder<Boolean, Boolean> booleanProp(String propertyName) {
        Function<Boolean, Boolean> convert = Functions.identity();
        Function<String, Boolean> parseJsonDefault = json -> {
            throw new IllegalStateException("Should only be called for object properties");
        };
        Function<String, Boolean> readFromEnvVar = envVar -> {
            var result = Environment.getBooleanEnvironmentVariable(envVar);
            if (result.isRight()) {
                return result.right();
            }
            throw (RuntimeException)result.left();
        };
        Function<Config, Optional<Boolean>> tryReadFromConfig = config -> config.getBoolean(propertyName);
        return new PropertyBuilder<Boolean, Boolean>(propertyName,
                convert, readFromEnvVar, parseJsonDefault, tryReadFromConfig);
    }

    public static PropertyBuilder<String, String> stringProp(String propertyName) {
        Function<String, String> convert = Functions.identity();
        Function<String, String> parseJsonDefault = json -> {
            throw new IllegalStateException("Should only be called for object properties");
        };
        Function<String, String> readFromEnvVar = envVar -> {
            var result = Environment.getEnvironmentVariable(envVar);
            if (result.isRight()) {
                return result.right();
            }
            throw (RuntimeException)result.left();
        };
        Function<Config, Optional<String>> tryReadFromConfig = config -> config.get(propertyName);
        return new PropertyBuilder<String, String>(propertyName,
                convert, readFromEnvVar, parseJsonDefault, tryReadFromConfig);
    }

    public static <T> PropertyBuilder<T, T> objectProp(String propertyName, Class<T> c) {
        return objectProp(propertyName, TypeShape.<T>builder(c).build());
    }

    public static <T> PropertyBuilder<T, T> objectProp(String propertyName, TypeShape<T> typeShape) {
        Function<T, T> convert = Functions.identity();
        Function<String, T> parseJsonDefault = json -> {
            // TODO should this be using Pulumi deserializers from protobuf Struct instead of GSON?
            // JSON can be converted to protobuf struct.
            try {
                var gson = new Gson();
                return gson.fromJson(json, typeShape.toGSON().getType());
            } catch (JsonParseException ex) {
                throw new InvalidDefaultValueException(propertyName, json, typeShape.getTypeName(), ex);
            }
        };
        Function<String, T> readFromEnvVar = envVar -> {
            var result = Environment.getEnvironmentVariable(envVar);
            if (result.isRight()) {
                return parseJsonDefault.apply(result.right());
            }
            throw (RuntimeException)result.left();
        };
        Function<Config, Optional<T>> tryReadFromConfig = config -> config.getObject(propertyName, typeShape);
        return new PropertyBuilder<T, T>(propertyName,
                convert, readFromEnvVar, parseJsonDefault, tryReadFromConfig);
    }
}
