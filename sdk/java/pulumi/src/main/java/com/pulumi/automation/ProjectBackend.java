package com.pulumi.automation;

public class ProjectBackend {
    private final String url;

    public ProjectBackend(String url) {
        this.url = url;
    }

    public String url() {
        return url;
    }
}
