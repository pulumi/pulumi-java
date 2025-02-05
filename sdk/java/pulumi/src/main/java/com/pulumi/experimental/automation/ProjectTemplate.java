// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * A template used to seed new stacks created from this project.
 */
public class ProjectTemplate {
    @Nullable
    private final String description;
    @Nullable
    private final String quickStart;
    private final Map<String, ProjectTemplateConfigValue> config;
    private final boolean important;

    private ProjectTemplate(Builder builder) {
        this.description = builder.description;
        this.quickStart = builder.quickStart;
        this.config = builder.config == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.config);
        this.important = builder.important;
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
     * The description of the template.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Optional text to be displayed after template creation.
     *
     * @return the quick start text
     */
    public String getQuickStart() {
        return quickStart;
    }

    /**
     * Optional template config.
     *
     * @return the template config
     */
    public Map<String, ProjectTemplateConfigValue> getConfig() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProjectTemplate that = (ProjectTemplate) o;
        return important == that.important &&
                Objects.equals(description, that.description) &&
                Objects.equals(quickStart, that.quickStart) &&
                Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, quickStart, config, important);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .description(description)
                .quickStart(quickStart)
                .config(config.isEmpty() ? null : config)
                .important(important);
    }

    /**
     * Builder for {@link ProjectTemplate}.
     */
    public static class Builder {
        @Nullable
        private String description;
        @Nullable
        private String quickStart;
        @Nullable
        private Map<String, ProjectTemplateConfigValue> config;
        private boolean important;

        private Builder() {
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
         * @param quickStart the quick start text
         * @return the builder
         */
        public Builder quickStart(String quickStart) {
            this.quickStart = quickStart;
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
         * Builds the {@link ProjectTemplate}.
         *
         * @return the project template
         */
        public ProjectTemplate build() {
            return new ProjectTemplate(this);
        }
    }
}
