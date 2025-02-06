// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.MissingProperty;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.pulumi.experimental.automation.*;

class ProjectSettingsYamlSerializer {
    public static String serialize(ProjectSettings settings) {
        var dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setAllowReadOnlyProperties(true);

        var yaml = new Yaml(new ProjectSettingsRepresenter(dumperOptions), dumperOptions);

        return yaml.dump(settings);
    }

    public static ProjectSettings deserialize(String yamlContent) {
        var loaderOptions = new LoaderOptions();
        var yaml = new Yaml(new ProjectSettingsModelConstructor(loaderOptions));

        ProjectSettingsModel model = yaml.load(yamlContent);
        if (model == null) {
            model = new ProjectSettingsModel();
        }
        return model.convert();
    }

    private static class ProjectSettingsRepresenter extends Representer {
        public ProjectSettingsRepresenter(DumperOptions options) {
            super(options);
            this.addClassTag(ProjectSettings.class, Tag.MAP);
            this.addClassTag(ProjectRuntimeName.class, Tag.STR);
            this.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
            this.getPropertyUtils().setSkipMissingProperties(true);

            representers.put(ProjectRuntimeName.class,
                    data -> represent(((ProjectRuntimeName) data).getValue()));
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

    public static class ProjectSettingsModel implements Model<ProjectSettings> {
        public String name;
        public ProjectRuntimeModel runtime;
        public String main;
        public String description;
        public String author;
        public String website;
        public String license;
        public Map<String, ProjectConfigTypeModel> config;
        public String stackConfigDir;
        public ProjectTemplateModel template;
        public ProjectBackendModel backend;
        public ProjectOptionsModel options;
        public ProjectPluginsModel plugins;

        @Override
        public ProjectSettings convert() {
            Objects.requireNonNull(name, "Missing name");
            Objects.requireNonNull(runtime, "Missing runtime");

            Map<String, ProjectConfigType> convertedConfig = null;
            if (config != null) {
                convertedConfig = new LinkedHashMap<>();
                for (var entry : config.entrySet()) {
                    convertedConfig.put(entry.getKey(), entry.getValue().convert());
                }
            }

            return ProjectSettings.builder(name, runtime.convert())
                    .main(main)
                    .description(description)
                    .author(author)
                    .website(website)
                    .license(license)
                    .config(convertedConfig)
                    .stackConfigDir(stackConfigDir)
                    .template(template != null ? template.convert() : null)
                    .backend(backend != null ? backend.convert() : null)
                    .options(options != null ? options.convert() : null)
                    .plugins(plugins != null ? plugins.convert() : null)
                    .build();
        }
    }

    public static class ProjectRuntimeModel implements Model<ProjectRuntime> {
        public ProjectRuntimeName name;
        public ProjectRuntimeOptionsModel options;

        @Override
        public ProjectRuntime convert() {
            Objects.requireNonNull(name, "Missing runtime name");

            return ProjectRuntime.builder(name)
                    .options(options != null ? options.convert() : null)
                    .build();
        }
    }

    public static class ProjectRuntimeOptionsModel implements Model<ProjectRuntimeOptions> {
        public Boolean typescript;
        public String nodeargs;
        public String packagemanager;
        public String buildTarget;
        public String binary;
        public String toolchain;
        public String virtualenv;
        public String typechecker;
        public String compiler;

        @Override
        public ProjectRuntimeOptions convert() {
            return ProjectRuntimeOptions.builder()
                    .typescript(typescript)
                    .nodeargs(nodeargs)
                    .packagemanager(packagemanager)
                    .buildTarget(buildTarget)
                    .binary(binary)
                    .toolchain(toolchain)
                    .virtualenv(virtualenv)
                    .typechecker(typechecker)
                    .compiler(compiler)
                    .build();
        }
    }

    public static class ProjectConfigTypeModel implements Model<ProjectConfigType> {
        public String type;
        public String description;
        public ProjectConfigItemsTypeModel items;
        public Object default_;
        public Object value;
        public boolean secret;

        @Override
        public ProjectConfigType convert() {
            return ProjectConfigType.builder()
                    .type(type)
                    .description(description)
                    .items(items != null ? items.convert() : null)
                    .default_(default_)
                    .value(value)
                    .secret(secret)
                    .build();
        }
    }

    public static class ProjectConfigItemsTypeModel implements Model<ProjectConfigItemsType> {
        public String type;
        public ProjectConfigItemsTypeModel items;

        @Override
        public ProjectConfigItemsType convert() {
            return ProjectConfigItemsType.builder()
                    .type(type)
                    .items(items != null ? items.convert() : null)
                    .build();
        }
    }

    public static class ProjectTemplateModel implements Model<ProjectTemplate> {
        public String displayName;
        public String description;
        public String quickstart;
        public Map<String, ProjectTemplateConfigValueModel> config;
        public boolean important;
        public Map<String, String> metadata;

        @Override
        public ProjectTemplate convert() {
            Map<String, ProjectTemplateConfigValue> convertedConfig = null;
            if (config != null) {
                convertedConfig = new LinkedHashMap<>();
                for (var entry : config.entrySet()) {
                    convertedConfig.put(entry.getKey(), entry.getValue().convert());
                }
            }

            return ProjectTemplate.builder()
                    .displayName(displayName)
                    .description(description)
                    .quickstart(quickstart)
                    .config(convertedConfig)
                    .important(important)
                    .metadata(metadata)
                    .build();
        }
    }

    public static class ProjectTemplateConfigValueModel implements Model<ProjectTemplateConfigValue> {
        public String description;
        public String default_;
        public boolean secret;

        @Override
        public ProjectTemplateConfigValue convert() {
            return new ProjectTemplateConfigValue(description, default_, secret);
        }
    }

    public static class ProjectBackendModel implements Model<ProjectBackend> {
        public String url;

        @Override
        public ProjectBackend convert() {
            return ProjectBackend.builder()
                    .url(url)
                    .build();
        }
    }

    public static class ProjectOptionsModel implements Model<ProjectOptions> {
        public String refresh;

        @Override
        public ProjectOptions convert() {
            return ProjectOptions.builder()
                    .refresh(refresh)
                    .build();
        }
    }

    public static class ProjectPluginsModel implements Model<ProjectPlugins> {
        public List<ProjectPluginOptionsModel> providers;
        public List<ProjectPluginOptionsModel> languages;
        public List<ProjectPluginOptionsModel> analyzers;

        @Override
        public ProjectPlugins convert() {
            return ProjectPlugins.builder()
                    .providers(convertList(providers))
                    .languages(convertList(languages))
                    .analyzers(convertList(analyzers))
                    .build();
        }

        private List<ProjectPluginOptions> convertList(List<ProjectPluginOptionsModel> plugins) {
            List<ProjectPluginOptions> result = null;
            if (plugins != null) {
                result = new ArrayList<>();
                for (var plugin : plugins) {
                    result.add(plugin.convert());
                }
            }
            return result;
        }
    }

    public static class ProjectPluginOptionsModel implements Model<ProjectPluginOptions> {
        public String name;
        public String version;
        public String path;

        @Override
        public ProjectPluginOptions convert() {
            return ProjectPluginOptions.builder()
                    .name(name)
                    .version(version)
                    .path(path)
                    .build();
        }
    }

    private static class ProjectSettingsModelConstructor extends Constructor {
        public ProjectSettingsModelConstructor(LoaderOptions loaderOptions) {
            super(ProjectSettingsModel.class, loaderOptions);
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

        @Override
        protected Object constructObject(Node node) {
            if (node instanceof ScalarNode) {
                var scalarNode = (ScalarNode) node;

                // Handle shorthand runtime syntax
                if (ProjectRuntimeModel.class.isAssignableFrom(node.getType())) {
                    var model = new ProjectRuntimeModel();
                    model.name = ProjectRuntimeName.fromString(scalarNode.getValue());
                    return model;
                }

                // Handle ProjectRuntimeName
                if (ProjectRuntimeName.class.isAssignableFrom(node.getType())) {
                    return ProjectRuntimeName.fromString(scalarNode.getValue());
                }

                // Handle enum values by converting to uppercase
                Class<?> type = node.getType();
                if (type != null && type.isEnum()) {
                    var value = scalarNode.getValue().toUpperCase();
                    return Enum.valueOf((Class<? extends Enum>) type, value);
                }
            }

            return super.constructObject(node);
        }
    }
}
