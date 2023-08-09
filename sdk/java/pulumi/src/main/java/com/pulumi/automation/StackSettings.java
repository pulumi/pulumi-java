package com.pulumi.automation;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class StackSettings {

    private final ImmutableMap<String, ValueOrSecret> config;

    public StackSettings(Map<String, ValueOrSecret> config) {
        this.config = ImmutableMap.copyOf(config);
    }

    /**
     * This is an optional configuration bag.
     * @return stack configuration
     */
    public Map<String, ValueOrSecret> config() {
        return config;
    }

    public static StackSettings.Builder builder() {
        return new StackSettings.Builder();
    }

    public static class Builder {

        private String name;
        private Map<String, ValueOrSecret> config;

        public Builder config(Map<String, ValueOrSecret> config) {
            this.config = config;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public StackSettings build() {
            return new StackSettings(
                    this.config
            );
        }
    }
}
