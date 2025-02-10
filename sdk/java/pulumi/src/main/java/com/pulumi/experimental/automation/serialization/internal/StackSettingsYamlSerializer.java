// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.MissingProperty;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.pulumi.experimental.automation.*;

class StackSettingsYamlSerializer {
    public static String serialize(StackSettings settings) {
        var dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setAllowReadOnlyProperties(true);

        var yaml = new Yaml(new StackSettingsRepresenter(dumperOptions), dumperOptions);
        return yaml.dump(settings);
    }

    public static StackSettings deserialize(String yamlContent) {
        var loaderOptions = new LoaderOptions();
        var yaml = new Yaml(new StackSettingsModelConstructor(loaderOptions));

        StackSettingsModel model = yaml.load(yamlContent);
        if (model == null) {
            model = new StackSettingsModel();
        }
        return model.convert();
    }

    private static class StackSettingsRepresenter extends Representer {
        public StackSettingsRepresenter(DumperOptions options) {
            super(options);
            this.addClassTag(StackSettings.class, Tag.MAP);
            this.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
            this.getPropertyUtils().setSkipMissingProperties(true);
            this.representers.put(StackSettingsConfigValue.class, data -> {
                var value = (StackSettingsConfigValue) data;
                if (!value.isSecure()) {
                    return represent(value.getValue());
                }

                Node valueNode = represent(value.getValue());
                Node secureNode = new ScalarNode(Tag.STR, "secure", null, null, DumperOptions.ScalarStyle.PLAIN);
                return new MappingNode(Tag.MAP,
                        Collections.singletonList(new NodeTuple(secureNode, valueNode)),
                        DumperOptions.FlowStyle.AUTO);
            });
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

            // Skip Boolean values that are false and annotated with @SkipIfFalse
            if (propertyValue instanceof Boolean && !(Boolean) propertyValue) {
                try {
                    var field = javaBean.getClass().getDeclaredField(property.getName());
                    if (field.isAnnotationPresent(SkipIfFalse.class)) {
                        return null;
                    }
                } catch (NoSuchFieldException e) {
                    // ignore
                }
            }

            // Serialize properties with a trailing underscore as the name without the
            // underscore.
            // For example "default_" will be serialized as "default".
            var name = property.getName();
            if (name.endsWith("_")) {
                var result = super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                return new NodeTuple(
                        representScalar(Tag.STR, name.substring(0, name.length() - 1)),
                        result.getValueNode());
            }

            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
    }

    public static class StackSettingsModel implements Model<StackSettings> {
        public String secretsprovider;
        public String encryptedkey;
        public String encryptionsalt;
        public Map<String, Object> config;

        @Override
        public StackSettings convert() {
            Map<String, StackSettingsConfigValue> convertedConfig = null;
            if (config != null) {
                convertedConfig = new LinkedHashMap<>();
                for (var entry : config.entrySet()) {
                    var value = convertConfigValue(entry.getValue());
                    convertedConfig.put(entry.getKey(), value);
                }
            }

            return StackSettings.builder()
                    .secretsProvider(secretsprovider)
                    .encryptedKey(encryptedkey)
                    .encryptionSalt(encryptionsalt)
                    .config(convertedConfig)
                    .build();
        }

        private static StackSettingsConfigValue convertConfigValue(Object value) {
            if (value instanceof Map<?, ?>) {
                var map = (Map<?, ?>) value;
                if (map.size() == 1 && map.containsKey("secure")) {
                    return new StackSettingsConfigValue(map.get("secure"), true);
                }
            }
            return new StackSettingsConfigValue(value);
        }
    }

    private static class StackSettingsModelConstructor extends Constructor {
        public StackSettingsModelConstructor(LoaderOptions loaderOptions) {
            super(StackSettingsModel.class, loaderOptions);
            setPropertyUtils(new PropertyUtils() {
                @Override
                public Property getProperty(Class<?> type, String name) {
                    // First look for the property with the exact name.
                    var property = super.getProperty(type, name);
                    if (property instanceof MissingProperty) {
                        // If the property is missing, try with an underscore at the end.
                        property = super.getProperty(type, name + "_");
                    }
                    return property;
                }
            });
            this.getPropertyUtils().setSkipMissingProperties(true);
        }
    }
}
