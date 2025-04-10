package com.pulumi.resources;


import java.util.Optional;

public interface AnalyzerManager {
    void reportViolation(String description);

    void reportViolationWithContext(String description, PolicyResource... resourcesInvolved);

    Optional<PolicyResource> fetchResource(String urn);
}
