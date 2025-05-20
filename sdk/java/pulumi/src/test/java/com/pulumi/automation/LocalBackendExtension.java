// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * JUnit extension that sets up a local Pulumi backend for tests in a temporary
 * directory. The temporary directory is created before each tests and deleted
 * after each test. Use the {@link EnvVars} annotation to inject the environment
 * variables that should be set for the test. These will include the
 * {@code PULUMI_BACKEND_URL} to the temporary directory if
 * {@code PULUMI_ACCESS_TOKEN} is not set.
 */
public class LocalBackendExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final Namespace NAMESPACE = Namespace.create(LocalBackendExtension.class);
    private static final String TEMP_DIR_KEY = "temporaryDirectory";
    private static final String EXTRA_ENV_VARS = "extraEnvVars";

    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        // If PULUMI_ACCESS_TOKEN is set we can use the service, but otherwise we need
        // to make a temporary folder and set PULUMI_BACKEND_URL.
        // If running in CI, always use the local backend.
        if (System.getenv("PULUMI_ACCESS_TOKEN") == null || System.getenv("PULUMI_ACCESS_TOKEN").isEmpty() ||
                "true".equals(System.getenv("GITHUB_ACTIONS"))) {
            String methodName = context.getTestMethod()
                    .map(method -> method.getName())
                    .orElse("unknown");
            var tempRootDir = Files.createTempDirectory("auto-tests-");
            var tempDir = Files.createDirectories(tempRootDir.resolve(methodName));

            var backendUrl = System.getProperty("os.name").toLowerCase().contains("windows")
                    ? "file://" + tempDir.toString().replace("\\", "/")
                    : "file:///" + tempDir.toString();

            var env = Map.of(
                    "PULUMI_BACKEND_URL", backendUrl,
                    "PULUMI_CONFIG_PASSPHRASE", "backup_password");

            context.getStore(NAMESPACE).put(TEMP_DIR_KEY, tempDir.toString());
            context.getStore(NAMESPACE).put(EXTRA_ENV_VARS, env);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws IOException {
        var tempDir = context.getStore(NAMESPACE)
                .get(TEMP_DIR_KEY, String.class);

        if (tempDir != null) {
            Files.walk(Path.of(tempDir))
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // Check if it's a Map parameter with appropriate type parameters and annotation
        if (parameterContext.getParameter().getType() == Map.class &&
                parameterContext.isAnnotated(EnvVars.class)) {
            return true;
        }

        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (parameterContext.isAnnotated(EnvVars.class)) {
            var result = extensionContext.getStore(NAMESPACE).get(EXTRA_ENV_VARS, Map.class);
            if (result == null) {
                return Collections.emptyMap();
            }
            return result;
        }

        throw new ParameterResolutionException("Unsupported parameter type");
    }
}
