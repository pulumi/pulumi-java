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
    private final String nodeargs;
    @Nullable
    private final String packagemanager;
    @Nullable
    private final String buildTarget;
    @Nullable
    private final String binary;
    @Nullable
    private final String toolchain;
    @Nullable
    private final String virtualenv;
    @Nullable
    private final String typechecker;
    @Nullable
    private final String compiler;

    private ProjectRuntimeOptions(Builder builder) {
        typescript = builder.typescript;
        nodeargs = builder.nodeargs;
        packagemanager = builder.packagemanager;
        buildTarget = builder.buildTarget;
        binary = builder.binary;
        toolchain = builder.toolchain;
        virtualenv = builder.virtualenv;
        typechecker = builder.typechecker;
        compiler = builder.compiler;
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
    public Boolean typescript() {
        return typescript;
    }

    /**
     * Applies to NodeJS projects only. Arguments to pass to {@code node}.
     *
     * @return the arguments to pass to {@code node}
     */
    public String nodeargs() {
        return nodeargs;
    }

    /**
     * Applies to NodeJS projects only. The package manager to use for installing
     * dependencies, either "npm" (default), "pnpm", or "yarn".
     *
     * @return the package manager
     */
    public String packagemanager() {
        return packagemanager;
    }

    /**
     * Applies to Go projects only. Path to save the compiled go binary to.
     *
     * @return the build target
     */
    public String buildTarget() {
        return buildTarget;
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
    public String binary() {
        return binary;
    }

    /**
     * Applies to Python projects only. The toolchain to use for managing virtual
     * environments, either "pip" (default) or "poetry", or "uv".
     *
     * @return the toolchain
     */
    public String toolchain() {
        return toolchain;
    }

    /**
     * Applies to Python projects only. A string that specifies the path to a
     * virtual environment to use when running the program.
     *
     * @return the virtual environment
     */
    public String virtualenv() {
        return virtualenv;
    }

    /**
     * Applies to Python projects only. The type checker library to use.
     *
     * @return the type checker
     */
    public String typechecker() {
        return typechecker;
    }

    /**
     * Applies to YAML projects only. Executable and arguments issued to standard
     * out.
     *
     * @return the compiler
     */
    public String compiler() {
        return compiler;
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
                Objects.equals(nodeargs, that.nodeargs) &&
                Objects.equals(packagemanager, that.packagemanager) &&
                Objects.equals(buildTarget, that.buildTarget) &&
                Objects.equals(binary, that.binary) &&
                Objects.equals(toolchain, that.toolchain) &&
                Objects.equals(virtualenv, that.virtualenv) &&
                Objects.equals(typechecker, that.typechecker) &&
                Objects.equals(compiler, that.compiler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typescript, nodeargs, packagemanager, buildTarget, binary, toolchain, virtualenv,
                typechecker, compiler);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .typescript(typescript)
                .nodeargs(nodeargs)
                .packagemanager(packagemanager)
                .buildTarget(buildTarget)
                .binary(binary)
                .toolchain(toolchain)
                .virtualenv(virtualenv)
                .typechecker(typechecker)
                .compiler(compiler);
    }

    /**
     * Builder for {@link ProjectRuntimeOptions}.
     */
    public static class Builder {
        @Nullable
        private Boolean typescript;
        @Nullable
        private String nodeargs;
        @Nullable
        private String packagemanager;
        @Nullable
        private String buildTarget;
        @Nullable
        private String binary;
        @Nullable
        private String toolchain;
        @Nullable
        private String virtualenv;
        @Nullable
        private String typechecker;
        @Nullable
        private String compiler;

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
         * Applies to NodeJS projects only. Arguments to pass to {@code node}.
         *
         * @param nodeargs arguments to pass to {@code node}
         * @return the builder
         */
        public Builder nodeargs(String nodeargs) {
            this.nodeargs = nodeargs;
            return this;
        }

        /**
         * Applies to NodeJS projects only. The package manager to use for installing
         * dependencies, either "npm" (default), "pnpm", or "yarn".
         *
         * @param packagemanager the package manager
         * @return the builder
         */
        public Builder packagemanager(String packagemanager) {
            this.packagemanager = packagemanager;
            return this;
        }

        /**
         * Applies to Go projects only. Path to save the compiled go binary to.
         *
         * @param buildTarget the build target
         * @return the builder
         */
        public Builder buildTarget(String buildTarget) {
            this.buildTarget = buildTarget;
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
         * Applies to Python projects only. The toolchain to use for managing virtual
         * environments, either "pip" (default) or "poetry", or "uv".
         *
         * @param toolchain the toolchain
         * @return the builder
         */
        public Builder toolchain(String toolchain) {
            this.toolchain = toolchain;
            return this;
        }

        /**
         * Applies to Python projects only. A string that specifies the path to a
         * virtual environment to use when running the program.
         *
         * @param virtualenv the virtual environment
         * @return the builder
         */
        public Builder virtualenv(String virtualenv) {
            this.virtualenv = virtualenv;
            return this;
        }

        /**
         * Applies to Python projects only. The type checker library to use.
         *
         * @param typechecker the type checker
         * @return the builder
         */
        public Builder typechecker(String typechecker) {
            this.typechecker = typechecker;
            return this;
        }

        /**
         * Applies to YAML projects only. Executable and arguments issued to standard
         * out.
         *
         * @param compiler executable and arguments
         * @return the builder
         */
        public Builder compiler(String compiler) {
            this.compiler = compiler;
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
