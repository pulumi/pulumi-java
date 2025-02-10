// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization.internal;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;

import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.experimental.automation.OperationType;
import com.pulumi.experimental.automation.PluginKind;
import com.pulumi.experimental.automation.ProjectSettings;
import com.pulumi.experimental.automation.StackSettings;
import com.pulumi.experimental.automation.UpdateKind;
import com.pulumi.experimental.automation.UpdateState;
import com.pulumi.experimental.automation.events.DiffKind;

/**
 * {@link LocalSerializer} provides serialization and deserialization utilities.
 */
@InternalUse
public class LocalSerializer {
    private final Gson gson = createGson();

    public <T> T deserializeJson(String content, Type type) {
        return gson.fromJson(content, type);
    }

    public <T> T deserializeJson(String content, Class<T> clazz) {
        // Consider supporting JSON deserialization of ProjectSettings
        // https://github.com/pulumi/pulumi-java/issues/1629
        if (ProjectSettings.class.equals(clazz)) {
            throw new UnsupportedOperationException(
                    "Reading ProjectSettings from JSON is not supported, please use YAML");
        }
        // Consider supporting JSON deserialization of StackSettings
        // https://github.com/pulumi/pulumi-java/issues/1631
        if (StackSettings.class.equals(clazz)) {
            throw new UnsupportedOperationException(
                    "Reading StackSettings from JSON is not supported, please use YAML");
        }

        return gson.fromJson(content, clazz);
    }

    public <T> T deserializeYaml(String content) {
        return new Yaml().load(content);
    }

    public <T> T deserializeYaml(String content, Class<T> clazz) {
        if (ProjectSettings.class.equals(clazz)) {
            return (T) ProjectSettingsYamlSerializer.deserialize(content);
        } else if (StackSettings.class.equals(clazz)) {
            return (T) StackSettingsYamlSerializer.deserialize(content);
        }

        throw new UnsupportedOperationException(
                String.format("Reading %s from YAML is not supported", clazz.getSimpleName()));
    }

    public <T> String serializeJson(T object) {
        // Consider supporting JSON serialization of ProjectSettings
        // https://github.com/pulumi/pulumi-java/issues/1629
        if (object instanceof ProjectSettings) {
            throw new UnsupportedOperationException(
                    "Saving ProjectSettings as JSON is not supported. Please use YAML");
        }
        // Consider supporting JSON serialization of StackSettings
        // https://github.com/pulumi/pulumi-java/issues/1631
        if (object instanceof StackSettings) {
            throw new UnsupportedOperationException(
                    "Saving StackSettings as JSON is not yet supported. Please use YAML");
        }

        return gson.toJson(object);
    }

    public <T> String serializeYaml(T object) {
        if (object instanceof ProjectSettings) {
            return ProjectSettingsYamlSerializer.serialize((ProjectSettings) object);
        } else if (object instanceof StackSettings) {
            return StackSettingsYamlSerializer.serialize((StackSettings) object);
        }

        throw new UnsupportedOperationException(
                String.format("Saving %s as YAML is not supported", object.getClass().getSimpleName()));
    }

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(OperationType.class, new OperationTypeDeserializer())
                .registerTypeAdapter(DiffKind.class, new DiffKindDeserializer())
                .registerTypeAdapter(UpdateKind.class, new UpdateKindDeserializer())
                .registerTypeAdapter(UpdateState.class, new UpdateStateDeserializer())
                .registerTypeAdapter(PluginKind.class, new PluginKindDeserializer())
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .create();
    }

    private static final class OperationTypeDeserializer implements JsonDeserializer<OperationType> {
        @Override
        public OperationType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = json.getAsString();

            // This is the only value that doesn't handle the typical pattern.
            if ("discard".equals(value)) {
                return OperationType.READ_DISCARD;
            }

            String enumValue = value.toUpperCase().replace('-', '_');

            try {
                return OperationType.valueOf(enumValue);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid UpdateKind: " + value);
            }
        }
    }

    private static final class InstantAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            out.value(value != null ? value.toString() : null);
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            String dateStr = in.nextString();
            return dateStr != null ? Instant.parse(dateStr) : null;
        }
    }

    private static final class UpdateKindDeserializer implements JsonDeserializer<UpdateKind> {
        @Override
        public UpdateKind deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = json.getAsString();
            String enumValue = value.toUpperCase().replace('-', '_');

            try {
                return UpdateKind.valueOf(enumValue);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid UpdateKind: " + value);
            }
        }
    }

    private static final class UpdateStateDeserializer implements JsonDeserializer<UpdateState> {
        @Override
        public UpdateState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = json.getAsString();
            String enumValue = value.toUpperCase().replace('-', '_');

            try {
                return UpdateState.valueOf(enumValue);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid UpdateKind: " + value);
            }
        }
    }

    private static final class DiffKindDeserializer implements JsonDeserializer<DiffKind> {
        @Override
        public DiffKind deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = json.getAsString();
            String enumValue = value.toUpperCase().replace('-', '_');

            try {
                return DiffKind.valueOf(enumValue);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid DiffKind: " + value);
            }
        }
    }

    private static final class PluginKindDeserializer implements JsonDeserializer<PluginKind> {
        @Override
        public PluginKind deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = json.getAsString();
            String enumValue = value.toUpperCase();

            try {
                return PluginKind.valueOf(enumValue);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid PluginKind: " + value);
            }
        }
    }
}
