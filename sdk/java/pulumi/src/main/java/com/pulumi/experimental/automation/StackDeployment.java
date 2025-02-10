// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * Represents the state of a stack deployment as used by
 * {@link Workspace#importStack(String, StackDeployment)} and {@link Workspace#exportStack(String)}.
 * <p>
 * There is no strongly typed model for the contents yet, but you
 * can access the raw representation via {@link #deployment()}.
 * <p>
 * NOTE: instances may contain sensitive data (secrets).
 */
public final class StackDeployment {
    private final int version;
    private final Map<String, Object> deployment;

    private StackDeployment(int version, Map<String, Object> deployment) {
        this.version = version;
        this.deployment = deployment;
    }

    /**
     * Creates a new {@link StackDeployment} from the given JSON string.
     *
     * @param json the JSON string to parse
     * @return the stack deployment
     * @throws AutomationException if the deployment cannot be parsed
     */
    public static StackDeployment fromJson(String json) throws AutomationException {
        try {
            var jsonObject = JsonParser.parseString(json).getAsJsonObject();

            var version = jsonObject.get("version").getAsInt();

            var deploymentObject = jsonObject.get("deployment").getAsJsonObject();
            var gson = new Gson();
            var type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> deployment = gson.fromJson(deploymentObject, type);

            return new StackDeployment(version, deployment);
        } catch (Exception e) {
            throw new AutomationException("Failed to parse StackDeployment JSON", e);
        }
    }

    /**
     * Version indicates the schema of the encoded deployment.
     *
     * @return the version of the deployment
     */
    public int version() {
        return version;
    }

    /**
     * The opaque Pulumi deployment.
     *
     * @return the deployment
     */
    public Map<String, Object> deployment() {
        return deployment;
    }
}
