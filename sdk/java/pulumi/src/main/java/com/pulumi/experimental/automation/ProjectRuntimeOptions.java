package com.pulumi.experimental.automation;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Various configuration options that apply to different language runtimes.
 */
public class ProjectRuntimeOptions {
    @Nullable
    private final Boolean typescript;
    @Nullable
    private final String binary;
    @Nullable
    private final String virtualEnv;

    private ProjectRuntimeOptions(Builder builder) {
        this.typescript = builder.typescript;
        this.binary = builder.binary;
        this.virtualEnv = builder.virtualEnv;
    }

    /**
     * Returns a new builder for {@link ProjectRuntimeOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Applies to NodeJS projects only. A boolean that controls whether to use
     * ts-node to execute sources.
     *
     * @return the typescript flag
     */
    public Boolean getTypescript() {
        return typescript;
    }

    /**
     * Applies to Go, .NET, and Java projects only.
     * <ul>
     * <li>Go: A string that specifies the name of a pre-build executable to look
     * for on
     * your path.</li>
     * <li>.NET: A string that specifies the path of a pre-build .NET assembly.</li>
     * <li>Java: A string that specifies the path of a pre-build Java JAR file or a
     * JBang entry-point file to execute.</li>
     * </ul>
     *
     * @return the binary
     */
    public String getBinary() {
        return binary;
    }

    /**
     * Applies to Python projects only. A string that specifies the path to a
     * virtual environment to use when running the program.
     *
     * @return the virtual environment
     */
    public String getVirtualEnv() {
        return virtualEnv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectRuntimeOptions) o;
        return Objects.equals(typescript, that.typescript) &&
                Objects.equals(binary, that.binary) &&
                Objects.equals(virtualEnv, that.virtualEnv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typescript, binary, virtualEnv);
    }

    /**
     * Builder for {@link ProjectRuntimeOptions}.
     */
    public static class Builder {
        @Nullable
        private Boolean typescript;
        @Nullable
        private String binary;
        @Nullable
        private String virtualEnv;

        private Builder() {
        }

        /**
         * Applies to NodeJS projects only. A boolean that controls whether to use
         * ts-node to execute sources.
         *
         * @param typescript the typescript flag
         * @return the builder
         */
        public Builder typescript(Boolean typescript) {
            this.typescript = typescript;
            return this;
        }

        /**
         * Applies to Go, .NET, and Java projects only.
         * <ul>
         * <li>Go: A string that specifies the name of a pre-build executable to look
         * for on
         * your path.</li>
         * <li>.NET: A string that specifies the path of a pre-build .NET assembly.</li>
         * <li>Java: A string that specifies the path of a pre-build Java JAR file or a
         * JBang entry-point file to execute.</li>
         * </ul>
         *
         * @param binary the binary
         * @return the builder
         */
        public Builder binary(String binary) {
            this.binary = binary;
            return this;
        }

        /**
         * Applies to Python projects only. A string that specifies the path to a
         * virtual environment to use when running the program.
         *
         * @param virtualEnv the virtual environment
         * @return the builder
         */
        public Builder virtualEnv(String virtualEnv) {
            this.virtualEnv = virtualEnv;
            return this;
        }

        /**
         * Builds the {@link ProjectRuntimeOptions}.
         *
         * @return the project runtime options
         */
        public ProjectRuntimeOptions build() {
            return new ProjectRuntimeOptions(this);
        }
    }
}
