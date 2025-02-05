// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * A set of plugins configured for a Pulumi project.
 */
public class ProjectPlugins {
    private final List<ProjectPluginOptions> providers;
    private final List<ProjectPluginOptions> languages;
    private final List<ProjectPluginOptions> analyzers;

    private ProjectPlugins(Builder builder) {
        providers = builder.providers == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(builder.providers);
        languages = builder.languages == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(builder.languages);
        analyzers = builder.analyzers == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(builder.analyzers);
    }

    /**
     * Returns a new builder for {@link ProjectPlugins}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The provider plugins to configure for the project.
     *
     * @return the provider plugins to configure for the project
     */
    public List<ProjectPluginOptions> getProviders() {
        return providers;
    }

    /**
     * The language plugins to configure for the project.
     *
     * @return the language plugins to configure for the project
     */
    public List<ProjectPluginOptions> getLanguages() {
        return languages;
    }

    /**
     * The analyzer plugins to configure for the project.
     *
     * @return the analyzer plugins to configure for the project
     */
    public List<ProjectPluginOptions> getAnalyzers() {
        return analyzers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectPlugins) o;
        return Objects.equals(providers, that.providers) &&
            Objects.equals(languages, that.languages) &&
            Objects.equals(analyzers, that.analyzers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providers, languages, analyzers);
    }

    /**
     * Builder for {@link ProjectPlugins}.
     */
    public static class Builder {
        @Nullable
        private List<ProjectPluginOptions> providers;
        @Nullable
        private List<ProjectPluginOptions> languages;
        @Nullable
        private List<ProjectPluginOptions> analyzers;

        private Builder() {
        }

        /**
         * The provider plugins to configure for the project.
         *
         * @param providers the providers to configure for the project
         * @return the builder
         */
        public Builder providers(List<ProjectPluginOptions> providers) {
            this.providers = providers;
            return this;
        }

        /**
         * The language plugins to configure for the project.
         *
         * @param languages the languages to configure for the project
         * @return the builder
         */
        public Builder languages(List<ProjectPluginOptions> languages) {
            this.languages = languages;
            return this;
        }

        /**
         * The analyzer plugins to configure for the project.
         *
         * @param analyzers the analyzers to configure for the project
         * @return the builder
         */
        public Builder analyzers(List<ProjectPluginOptions> analyzers) {
            this.analyzers = analyzers;
            return this;
        }

        /**
         * Builds the {@link ProjectPlugins}.
         *
         * @return the project backend
         */
        public ProjectPlugins build() {
            return new ProjectPlugins(this);
        }
    }
}
