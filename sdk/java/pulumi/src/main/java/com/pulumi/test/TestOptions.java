package com.pulumi.test;

import com.pulumi.resources.ResourceTransformation;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Optional settings for tests.
 */
public class TestOptions {

    public static final TestOptions Empty = builder().build();

    private final String projectName;
    private final String stackName;
    private final boolean preview;
    private final List<ResourceTransformation> resourceTransformations;

    /**
     * @param projectName             the test project name to use
     * @param stackName               the test stack name to use
     * @param preview                 is the test a preview or a normal execution
     * @param resourceTransformations the test stack resource transformations
     */
    public TestOptions(
            String projectName,
            String stackName,
            boolean preview,
            List<ResourceTransformation> resourceTransformations
    ) {
        this.projectName = requireNonNull(projectName);
        this.stackName = requireNonNull(stackName);
        this.preview = preview;
        this.resourceTransformations = requireNonNull(resourceTransformations);
    }

    /**
     * @return the test project name
     */
    public String projectName() {
        return this.projectName;
    }

    /**
     * @return the test stack name
     */
    public String stackName() {
        return this.stackName;
    }

    /**
     * Whether the test runs in Preview mode. Defaults to <b>true</b> if not specified.
     */
    public boolean preview() {
        return this.preview;
    }

    /**
     * @return the stack resource transformations
     * @see com.pulumi.resources.StackOptions#resourceTransformations()
     */
    public List<ResourceTransformation> resourceTransformations() {
        return this.resourceTransformations;
    }

    /**
     * @return a new {@link Builder} for {@link TestOptions}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for {@link TestOptions}
     */
    public static class Builder {
        private String projectName;
        private String stackName;
        private boolean preview;
        private List<ResourceTransformation> resourceTransformations;

        public Builder() {
            this.projectName = "project";
            this.stackName = "stack";
            this.preview = false;
            this.resourceTransformations = List.of();
        }

        /**
         * The project name. Defaults to <b>"project"</b> if not specified.
         *
         * @param projectName the project name to use in the test
         * @return this {@link Builder}
         */
        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        /**
         * The stack name. Defaults to <b>"stack"</b> if not specified.
         *
         * @param stackName the stack name to use in the test
         * @return this {@link Builder}
         */
        public Builder stackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        /**
         * The preview mode. Defaults to <b>false</b> if not specified.
         *
         * @param preview set true if test is a preview or false on normal execution
         * @return this {@link Builder}
         */
        public Builder preview(boolean preview) {
            this.preview = preview;
            return this;
        }

        /**
         * The stack transformations. Defaults to <b>empty</b> if not specified.
         *
         * @param resourceTransformations the transformations to use in test
         * @return this {@link Builder}
         * @see com.pulumi.resources.StackOptions#resourceTransformations()
         */
        public Builder resourceTransformations(List<ResourceTransformation> resourceTransformations) {
            this.resourceTransformations = resourceTransformations;
            return this;
        }

        /**
         * @return a new {@link TestOptions} from this {@link Builder}.
         */
        public TestOptions build() {
            return new TestOptions(
                    this.projectName, this.stackName, this.preview, this.resourceTransformations
            );
        }
    }
}
