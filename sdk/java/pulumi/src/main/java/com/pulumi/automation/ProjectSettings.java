package com.pulumi.automation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A Pulumi project manifest.
 * It describes metadata applying to all sub-stacks created from the project.
 */
@ParametersAreNonnullByDefault
public class ProjectSettings {

    private final String name;
    private final String runtime;
    @Nullable
    private final ProjectBackend backend;

    /**
     * A new {@link ProjectSettings} with the given values
     *
     * @param name    the project name
     * @param runtime the language runtime
     * @param backend the optional {@link ProjectBackend} setting
     */
    public ProjectSettings(String name, String runtime, @Nullable ProjectBackend backend) {
        this.name = requireNonNull(name);
        this.runtime = requireNonNull(runtime);
        this.backend = backend;
    }

    /**
     * Name of the project containing alphanumeric characters, hyphens, underscores, and periods.
     * @return the project name
     */
    public String name() {
        return name;
    }

    /**
     * Installed language runtime of the project: nodejs, python, go, dotnet, java or yaml.
     * @return the language runtime
     */
    public String runtime() {
        return runtime;
    }

    public static ProjectSettings.Builder builder() {
        return new ProjectSettings.Builder();
    }

    public Optional<ProjectBackend> backend() {
        return Optional.ofNullable(backend);
    }

    public static class Builder {

        private String name;
        private String runtime;
        @Nullable
        private ProjectBackend backend;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder runtime(String runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder backend(String url) {
            this.backend = new ProjectBackend(url);
            return this;
        }

        public ProjectSettings build() {
            if (runtime == null) {
                runtime = "java";
            }
            return new ProjectSettings(
                    this.name,
                    this.runtime,
                    this.backend
            );
        }
    }
}
