// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.pulumi.experimental.automation.serialization.internal.SkipIfFalse;

/**
 * A template used to seed new stacks created from this project.
 */
public class ProjectTemplate {
    @Nullable
    private final String displayName;
    @Nullable
    private final String description;
    @Nullable
    private final String quickstart;
    private final Map<String, ProjectTemplateConfigValue> config;
    @SkipIfFalse
    private final boolean important;
    private final Map<String, String> metadata;

    private ProjectTemplate(Builder builder) {
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.quickstart = builder.quickstart;
        this.config = builder.config == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.config);
        this.important = builder.important;
        this.metadata = builder.metadata == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.metadata);
    }

    /**
     * Returns a new builder for {@link ProjectTemplate}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Optional user friendly name of the template.
     *
     * @return the display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * The description of the template.
     *
     * @return the description
     */
    public String description() {
        return description;
    }

    /**
     * Optional text to be displayed after template creation.
     *
     * @return the quick start text
     */
    public String quickstart() {
        return quickstart;
    }

    /**
     * Optional template config.
     *
     * @return the template config
     */
    public Map<String, ProjectTemplateConfigValue> config() {
        return config;
    }

    /**
     * Indicates the template is important.
     *
     * @return true if the template is important
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * Additional metadata for the template.
     *
     * @return the metadata
     */
    public Map<String, String> metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProjectTemplate that = (ProjectTemplate) o;
        return important == that.important &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(description, that.description) &&
                Objects.equals(quickstart, that.quickstart) &&
                Objects.equals(config, that.config) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, description, quickstart, config, important, metadata);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .displayName(displayName)
                .description(description)
                .quickstart(quickstart)
                .config(config.isEmpty() ? null : config)
                .important(important)
                .metadata(metadata.isEmpty() ? null : metadata);
    }

    /**
     * Builder for {@link ProjectTemplate}.
     */
    public static class Builder {
        @Nullable
        private String displayName;
        @Nullable
        private String description;
        @Nullable
        private String quickstart;
        @Nullable
        private Map<String, ProjectTemplateConfigValue> config;
        private boolean important;
        @Nullable
        private Map<String, String> metadata;

        private Builder() {
        }

        /**
         * Optional user friendly name of the template.
         *
         * @param displayName the display name
         * @return the builder
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * The description of the template.
         *
         * @param description the description
         * @return the builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Optional text to be displayed after template creation.
         *
         * @param quickstart the quick start text
         * @return the builder
         */
        public Builder quickstart(String quickstart) {
            this.quickstart = quickstart;
            return this;
        }

        /**
         * Optional template config.
         *
         * @param config the template config
         * @return the builder
         */
        public Builder config(Map<String, ProjectTemplateConfigValue> config) {
            this.config = config;
            return this;
        }

        /**
         * Indicates the template is important.
         *
         * @param important true if the template is important
         * @return the builder
         */
        public Builder important(boolean important) {
            this.important = important;
            return this;
        }

        /**
         * Additional metadata for the template.
         *
         * @param metadata the metadata
         * @return the builder
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds the {@link ProjectTemplate}.
         *
         * @return the project template
         */
        public ProjectTemplate build() {
            return new ProjectTemplate(this);
        }
    }
}
