package com.pulumi.deployment;

import com.pulumi.deployment.internal.Engine;
import com.pulumi.deployment.internal.Monitor;

/**
 * Builder interface for creating Engine and Monitor instances for inline deployments.
 */
public interface DeploymentBuilder {
    /**
     * Builds an Engine instance using the provided engine address.
     * @param engineAddr The engine address to connect to
     * @return A new Engine instance
     */
    Engine buildEngine(String engineAddr);

    /**
     * Builds a Monitor instance using the provided monitor address.
     * @param monitorAddr The monitor address to connect to
     * @return A new Monitor instance
     */
    Monitor buildMonitor(String monitorAddr);
}
