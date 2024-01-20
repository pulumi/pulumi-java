package com.pulumi.automation;

import com.pulumi.Pulumi;
import com.pulumi.automation.internal.PulumiAutoInternal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public interface PulumiAuto extends Pulumi {

    static API withProjectSettings(ProjectSettings projectSettings) {
        return new PulumiAutoInternal.APIInternal().withProjectSettings(projectSettings);
    }

    static API withEnvironmentVariables(Map<String, String> environmentVariables) {
        return new PulumiAutoInternal.APIInternal().withEnvironmentVariables(environmentVariables);
    }

    /**
     * Pulumi Automation entrypoint operations.
     */
    interface API {
        /**
         * The {@link ProjectSettings} object for the current project.
         *
         * @param projectSettings the project setting
         * @return the {@link API} instance
         */
        API withProjectSettings(ProjectSettings projectSettings);

        API withEnvironmentVariables(Map<String, String> environmentVariables);

        LocalWorkspace localWorkspace(LocalWorkspaceOptions options);
    }
}
