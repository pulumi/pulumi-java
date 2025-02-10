// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * The result of a {@link Workspace#whoAmI()} operation.
 */
public class WhoAmIResult {
    private final String user;
    @Nullable
    private final String url;
    private final List<String> organizations;

    /**
     * Creates a new instance of {@link WhoAmIResult}.
     *
     * @param user the identify of the current user
     * @param url the URL at which information about this backend may be seen
     * @param organizations organizations the user are in for the backend
     */
    public WhoAmIResult(String user, String url, List<String> organizations) {
        this.user = user;
        this.url = url;
        this.organizations = Collections.unmodifiableList(new ArrayList<>(organizations));
    }

    /*
     * The identify of the current user.
     *
     * @return the user
     */
    public String user() {
        return user;
    }

    /**
     * A URL at which information about this backend may be seen.
     *
     * @return the URL
     */
    @Nullable
    public String url() {
        return url;
    }

    /**
     * Organizations the user are in for the backend.
     *
     * @return the organizations
     */
    public List<String> organizations() {
        return organizations;
    }
}
