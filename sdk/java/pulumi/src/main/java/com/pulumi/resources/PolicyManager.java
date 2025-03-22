package com.pulumi.resources;


import java.util.Optional;

public interface PolicyManager {
    void reportViolation(String description);

    void reportViolationWithContext(String description, PolicyResource... resourcesInvolved);

    Optional<PolicyResource> fetchResource(String urn);
}
