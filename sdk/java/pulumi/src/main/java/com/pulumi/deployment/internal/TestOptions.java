package com.pulumi.deployment.internal;

import com.pulumi.resources.ResourceTransformation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Optional settings for tests.
 */
public class TestOptions {

    private final String projectName;
    private final String stackName;
    private final boolean preview;
    private final List<ResourceTransformation> resourceTransformations;

    public TestOptions() {
        this(null, null, true);
    }

    public TestOptions(boolean preview) {
        this(null, null, preview);
    }

    public TestOptions(@Nullable String projectName, @Nullable String stackName) {
        this(projectName, stackName, true);
    }

    /**
     * @param projectName the test project name to use
     * @param stackName the test stack name to use
     * @param preview is the test a preview or a normal execution
     */
    public TestOptions(@Nullable String projectName, @Nullable String stackName, boolean preview) {
        this.projectName = projectName != null ? projectName : "project";
        this.stackName = stackName != null ? stackName : "stack";
        this.preview = preview;
        this.resourceTransformations = List.of();
    }

    /**
     * Project name. Defaults to <b>"project"</b> if not specified.
     */
    public String getProjectName() {
        return this.projectName;
    }

    /**
     * Stack name. Defaults to <b>"stack"</b> if not specified.
     */
    public String getStackName() {
        return this.stackName;
    }

    /**
     * Whether the test runs in Preview mode. Defaults to <b>true</b> if not specified.
     */
    public boolean isPreview() {
        return this.preview;
    }

    /**
     * @return the stack resource transformations
     * @see com.pulumi.resources.StackOptions#resourceTransformations()
     */
    public List<ResourceTransformation> resourceTransformations() {
        return this.resourceTransformations;
    }

    // TODO: add a builder
}
