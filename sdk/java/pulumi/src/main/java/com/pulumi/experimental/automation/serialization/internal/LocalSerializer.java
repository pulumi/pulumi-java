// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization.internal;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

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
import com.pulumi.experimental.automation.ProjectRuntime;
import com.pulumi.experimental.automation.ProjectRuntimeName;
import com.pulumi.experimental.automation.ProjectSettings;
import com.pulumi.experimental.automation.StackSettings;
import com.pulumi.experimental.automation.UpdateKind;
import com.pulumi.experimental.automation.UpdateState;
import com.pulumi.experimental.automation.events.DiffKind;

import org.yaml.snakeyaml.introspector.BeanAccess;

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
        // TODO support JSON deserialization of ProjectSettings
        if (ProjectSettings.class.equals(clazz)) {
            throw new UnsupportedOperationException("Reading ProjectSettings from JSON is not yet supported");
        }
        // TODO support JSON deserialization of StackSettings
        if (StackSettings.class.equals(clazz)) {
            throw new UnsupportedOperationException("Reading StackSettings from JSON is not yet supported");
        }

        return gson.fromJson(content, clazz);
    }

    public <T> T deserializeYaml(String content) {
        var yaml = createYaml(null);
        return yaml.load(content);
    }

    public <T> T deserializeYaml(String content, Class<T> clazz) {
        var yaml = createYaml(clazz);
        return yaml.load(content);
    }

    public <T> String serializeJson(T object) {
        // TODO support JSON serialization of ProjectSettings
        if (object instanceof ProjectSettings) {
            throw new UnsupportedOperationException("Saving ProjectSettings as JSON is not yet supported");
        }
        // TODO support JSON serialization of StackSettings
        if (object instanceof StackSettings) {
            throw new UnsupportedOperationException("Saving StackSettings as JSON is not yet supported");
        }

        return gson.toJson(object);
    }

    public <T> String serializeYaml(T object) {
        var yaml = createYaml(null);
        return yaml.dump(object);
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

    private static <T> Yaml createYaml(@Nullable Class<T> clazz) {
        var loaderOptions = new LoaderOptions();

        Constructor constructor = null;
        if (clazz != null && ProjectSettings.class.equals(clazz)) {
            constructor = new ProjectSettingsConstructor(loaderOptions);
        }

        var dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setAllowReadOnlyProperties(true);

        if (constructor != null) {
            return new Yaml(
                    constructor,
                    new CustomRepresenter(dumperOptions),
                    dumperOptions,
                    loaderOptions);
        }

        return new Yaml(new CustomRepresenter(dumperOptions), dumperOptions);
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

    private static class ProjectSettingsConstructor extends Constructor {
        public ProjectSettingsConstructor(LoaderOptions options) {
            super(ProjectSettings.class, options);
        }

        @Override
        protected Object constructObject(Node node) {
            if (node instanceof MappingNode && node.getTag().toString().contains("ProjectSettings")) {
                var mnode = (MappingNode) node;
                Node nameNode = mnode.getValue().stream()
                        .filter(t -> ((ScalarNode) t.getKeyNode()).getValue().equals("name"))
                        .findFirst()
                        .orElseThrow()
                        .getValueNode();
                String name = ((ScalarNode) nameNode).getValue();

                Node runtimeNode = mnode.getValue().stream()
                        .filter(t -> ((ScalarNode) t.getKeyNode()).getValue().equals("runtime"))
                        .findFirst()
                        .orElseThrow()
                        .getValueNode();
                ProjectRuntime runtime = runtimeNode instanceof ScalarNode
                        ? ProjectRuntime.builder(
                                ProjectRuntimeName.valueOf(((ScalarNode) runtimeNode).getValue().toUpperCase())).build()
                        : (ProjectRuntime) constructObject(runtimeNode);

                var builder = ProjectSettings.builder(name, runtime);

                // Process each property in the YAML
                mnode.getValue().forEach(tuple -> {
                    String key = ((ScalarNode) tuple.getKeyNode()).getValue();
                    Node valueNode = tuple.getValueNode();

                    switch (key) {
                        case "main":
                            builder.main(((ScalarNode) valueNode).getValue());
                            break;
                        case "description":
                            builder.description(((ScalarNode) valueNode).getValue());
                            break;

                        // TODO others
                    }
                });

                return builder.build();
            }

            return super.constructObject(node);
        }
    }

    private static class CustomRepresenter extends Representer {
        public CustomRepresenter(DumperOptions options) {
            super(options);
            this.addClassTag(ProjectSettings.class, Tag.MAP);
            this.addClassTag(ProjectRuntime.class, Tag.MAP);
            this.addClassTag(ProjectRuntimeName.class, Tag.STR);
            this.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
            this.getPropertyUtils().setSkipMissingProperties(true);

            representers.put(ProjectRuntimeName.class,
                    data -> represent(((ProjectRuntimeName) data).name().toLowerCase()));
        }

        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
                Object propertyValue, Tag customTag) {
            // Skip null values
            if (propertyValue == null) {
                return null;
            }
            // Skip empty collections
            if (propertyValue instanceof Collection && ((Collection<?>) propertyValue).isEmpty()) {
                return null;
            }
            // Skip empty maps
            if (propertyValue instanceof Map && ((Map<?, ?>) propertyValue).isEmpty()) {
                return null;
            }

            // Special handling of ProjectRuntime
            if (propertyValue instanceof ProjectRuntime) {
                var runtime = (ProjectRuntime) propertyValue;
                if (runtime.getOptions() == null) {
                    // If options is null, represent runtime as just the name
                    return new NodeTuple(
                            representScalar(Tag.STR, property.getName()),
                            representScalar(Tag.STR, runtime.getName().name().toLowerCase()));
                }
            }

            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
    }
}
