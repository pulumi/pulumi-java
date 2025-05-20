// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * A Pulumi project manifest. It describes metadata applying to all sub-stacks
 * created from the project.
 */
public class ProjectSettings {
    private final String name;
    private final ProjectRuntime runtime;
    @Nullable
    private final String main;
    @Nullable
    private final String description;
    @Nullable
    private final String author;
    @Nullable
    private final String website;
    @Nullable
    private final String license;
    private Map<String, ProjectConfigType> config;
    @Nullable
    private final String stackConfigDir;
    @Nullable
    private final ProjectTemplate template;
    @Nullable
    private final ProjectBackend backend;
    @Nullable
    private final ProjectOptions options;
    @Nullable
    private final ProjectPlugins plugins;

    private ProjectSettings(Builder builder) {
        name = builder.name;
        runtime = builder.runtime;
        main = builder.main;
        description = builder.description;
        author = builder.author;
        website = builder.website;
        license = builder.license;
        config = builder.config == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.config);
        stackConfigDir = builder.stackConfigDir;
        template = builder.template;
        backend = builder.backend;
        options = builder.options;
        plugins = builder.plugins;
    }

    /**
     * Returns a new builder for {@link ProjectSettings}.
     *
     * @param name    the name of the project
     * @param runtime the runtime name of the project
     * @return the builder
     */
    public static Builder builder(String name, ProjectRuntimeName runtime) {
        return new Builder().name(name).runtime(runtime);
    }

    /**
     * Returns a new builder for {@link ProjectSettings}.
     *
     * @param name    the name of the project
     * @param runtime the runtime of the project
     * @return the builder
     */
    public static Builder builder(String name, ProjectRuntime runtime) {
        return new Builder().name(name).runtime(runtime);
    }

    /**
     * Creates a new default project settings.
     *
     * @param name the name of the project
     * @return the project settings
     */
    static ProjectSettings createDefault(String name) {
        return builder(name, ProjectRuntimeName.JAVA).build();
    }

    /**
     * Returns whether this is the default project settings.
     *
     * @return true if this is the default project settings
     */
    boolean isDefault() {
        return Objects.equals(this, createDefault(name));
    }

    /**
     * The name of the project.
     *
     * @return the name of the project
     */
    public String name() {
        return name;
    }

    /**
     * The runtime of the project.
     *
     * @return the runtime of the project
     */
    public ProjectRuntime runtime() {
        return runtime;
    }

    /**
     * An optional override for the program's main entry-point location.
     *
     * @return the main entry point of the program
     */
    @Nullable
    public String main() {
        return main;
    }

    /**
     * The description of the project.
     *
     * @return the description of the project
     */
    @Nullable
    public String description() {
        return description;
    }

    /**
     * The author of the project.
     *
     * @return the author of the project
     */
    @Nullable
    public String author() {
        return author;
    }

    /**
     * The website of the project.
     *
     * @return the website of the project
     */
    @Nullable
    public String website() {
        return website;
    }

    /**
     * The license of the project.
     *
     * @return the license of the project
     */
    @Nullable
    public String license() {
        return license;
    }

    /**
     * The config of the project.
     *
     * @return the config of the project
     */
    public Map<String, ProjectConfigType> config() {
        return config;
    }

    /**
     * Indicates where to store the Pulumi.&lt;stack-name&gt;.yaml files, combined with
     * the folder Pulumi.yaml is in.
     *
     * @return where to store the Pulumi.&lt;stack-name&gt;.yaml files
     */
    @Nullable
    public String stackConfigDir() {
        return stackConfigDir;
    }

    /**
     * The template of the project.
     *
     * @return the template of the project
     */
    @Nullable
    public ProjectTemplate template() {
        return template;
    }

    /**
     * The backend of the project.
     *
     * @return the backend of the project
     */
    @Nullable
    public ProjectBackend backend() {
        return backend;
    }

    /**
     * An optional set of project options.
     *
     * @return the options of the project
     */
    @Nullable
    public ProjectOptions options() {
        return options;
    }

    /**
     * An optional set of plugins of the project.
     *
     * @return the plugins of the project
     */
    @Nullable
    public ProjectPlugins plugins() {
        return plugins;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectSettings) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(runtime, that.runtime) &&
                Objects.equals(main, that.main) &&
                Objects.equals(description, that.description) &&
                Objects.equals(author, that.author) &&
                Objects.equals(website, that.website) &&
                Objects.equals(license, that.license) &&
                Objects.equals(config, that.config) &&
                Objects.equals(stackConfigDir, that.stackConfigDir) &&
                Objects.equals(template, that.template) &&
                Objects.equals(backend, that.backend) &&
                Objects.equals(options, that.options) &&
                Objects.equals(plugins, that.plugins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, runtime, main, description, author, website, license, config, stackConfigDir,
                template, backend, options, plugins);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .runtime(runtime)
                .main(main)
                .description(description)
                .author(author)
                .website(website)
                .license(license)
                .config(config.isEmpty() ? null : config)
                .stackConfigDir(stackConfigDir)
                .template(template)
                .backend(backend)
                .options(options)
                .plugins(plugins);
    }

    /**
     * Builder for {@link ProjectSettings}.
     */
    public static class Builder {
        private String name;
        private ProjectRuntime runtime;
        @Nullable
        private String main;
        @Nullable
        private String description;
        @Nullable
        private String author;
        @Nullable
        private String website;
        @Nullable
        private String license;
        @Nullable
        private Map<String, ProjectConfigType> config;
        @Nullable
        private String stackConfigDir;
        @Nullable
        private ProjectTemplate template;
        @Nullable
        private ProjectBackend backend;
        @Nullable
        private ProjectOptions options;
        @Nullable
        private ProjectPlugins plugins;

        private Builder() {
        }

        /**
         * The name of the project.
         *
         * @param name the name of the project
         * @return the builder
         */
        public Builder name(String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * The runtime name of the project.
         *
         * @param runtime the runtime name of the project
         * @return the builder
         */
        public Builder runtime(ProjectRuntimeName runtime) {
            this.runtime = ProjectRuntime.builder(Objects.requireNonNull(runtime)).build();
            return this;
        }

        /**
         * The runtime of the project.
         *
         * @param runtime the runtime of the project
         * @return the builder
         */
        public Builder runtime(ProjectRuntime runtime) {
            this.runtime = Objects.requireNonNull(runtime);
            return this;
        }

        /**
         * An optional override for the program's main entry-point location.
         *
         * @param main the main entry point of the program
         * @return the builder
         */
        public Builder main(@Nullable String main) {
            this.main = main;
            return this;
        }

        /**
         * The description of the project.
         *
         * @param description the description of the project
         * @return the builder
         */
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * The author of the project.
         *
         * @param author the author of the project
         * @return the builder
         */
        public Builder author(@Nullable String author) {
            this.author = author;
            return this;
        }

        /**
         * The website of the project.
         *
         * @param website the website of the project
         * @return the builder
         */
        public Builder website(@Nullable String website) {
            this.website = website;
            return this;
        }

        /**
         * The license of the project.
         *
         * @param license the license of the project
         * @return the builder
         */
        public Builder license(@Nullable String license) {
            this.license = license;
            return this;
        }

        /**
         * The config of the project.
         *
         * @param config the config of the project
         * @return the builder
         */
        public Builder config(@Nullable Map<String, ProjectConfigType> config) {
            this.config = config;
            return this;
        }

        /**
         * Indicates where to store the Pulumi.&lt;stack-name&gt;.yaml files, combined with
         * the folder Pulumi.yaml is in.
         *
         * @param stackConfigDir where to store the Pulumi.&lt;stack-name&gt;.yaml files
         * @return the builder
         */
        public Builder stackConfigDir(@Nullable String stackConfigDir) {
            this.stackConfigDir = stackConfigDir;
            return this;
        }

        /**
         * The template of the project.
         *
         * @param template the template of the project
         * @return the builder
         */
        public Builder template(@Nullable ProjectTemplate template) {
            this.template = template;
            return this;
        }

        /**
         * The backend of the project.
         *
         * @param backend the backend of the project
         * @return the builder
         */
        public Builder backend(@Nullable ProjectBackend backend) {
            this.backend = backend;
            return this;
        }

        /**
         * The options of the project.
         *
         * @param options the options of the project
         * @return the builder
         */
        public Builder options(@Nullable ProjectOptions options) {
            this.options = options;
            return this;
        }

        /**
         * The plugins of the project.
         *
         * @param plugins the plugins of the project
         * @return the builder
         */
        public Builder plugins(@Nullable ProjectPlugins plugins) {
            this.plugins = plugins;
            return this;
        }

        /**
         * Builds the {@link ProjectSettings}.
         *
         * @return the project settings
         */
        public ProjectSettings build() {
            return new ProjectSettings(this);
        }
    }
}
