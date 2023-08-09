package com.pulumi.automation;

import com.pulumi.Context;
import com.pulumi.Pulumi;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public interface PulumiAuto extends Pulumi {

    static API withProjectSettings(ProjectSettings projectSettings) {
        return 
    }

    static API withEnvironmentVariables(Map<String, String> environmentVariables);

    static API withInlineProgram(Consumer<Context> program);

    /**
     * Pulumi Automation entrypoint operations.
     */
    interface API {
        /**
         * The {@link ProjectSettings} object for the current project.
         *
         * @param projectSettings the project setting
         */
        API withProjectSettings(ProjectSettings projectSettings);

        API withEnvironmentVariables(Map<String, String> environmentVariables);

        API withInlineProgram(Consumer<Context> program);

        LocalWorkspace localWorkspace(LocalWorkspaceOptions options);
    }
}
