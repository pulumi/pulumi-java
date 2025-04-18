// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.akamai.outputs;

import com.pulumi.akamai.outputs.GetCloudletsPhasedReleaseMatchRuleMatchRuleForwardSettings;
import com.pulumi.akamai.outputs.GetCloudletsPhasedReleaseMatchRuleMatchRuleMatch;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.exceptions.MissingRequiredPropertyException;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

@CustomType
public final class GetCloudletsPhasedReleaseMatchRuleMatchRule {
    private @Nullable Boolean disabled;
    private @Nullable Integer end;
    private GetCloudletsPhasedReleaseMatchRuleMatchRuleForwardSettings forwardSettings;
    private @Nullable String matchUrl;
    private @Nullable List<GetCloudletsPhasedReleaseMatchRuleMatchRuleMatch> matches;
    private @Nullable Boolean matchesAlways;
    private @Nullable String name;
    private @Nullable Integer start;
    private String type;

    private GetCloudletsPhasedReleaseMatchRuleMatchRule() {}
    public Optional<Boolean> disabled() {
        return Optional.ofNullable(this.disabled);
    }
    public Optional<Integer> end() {
        return Optional.ofNullable(this.end);
    }
    public GetCloudletsPhasedReleaseMatchRuleMatchRuleForwardSettings forwardSettings() {
        return this.forwardSettings;
    }
    public Optional<String> matchUrl() {
        return Optional.ofNullable(this.matchUrl);
    }
    public List<GetCloudletsPhasedReleaseMatchRuleMatchRuleMatch> matches() {
        return this.matches == null ? List.of() : this.matches;
    }
    public Optional<Boolean> matchesAlways() {
        return Optional.ofNullable(this.matchesAlways);
    }
    public Optional<String> name() {
        return Optional.ofNullable(this.name);
    }
    public Optional<Integer> start() {
        return Optional.ofNullable(this.start);
    }
    public String type() {
        return this.type;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(GetCloudletsPhasedReleaseMatchRuleMatchRule defaults) {
        return new Builder(defaults);
    }
    @CustomType.Builder
    public static final class Builder {
        private @Nullable Boolean disabled;
        private @Nullable Integer end;
        private GetCloudletsPhasedReleaseMatchRuleMatchRuleForwardSettings forwardSettings;
        private @Nullable String matchUrl;
        private @Nullable List<GetCloudletsPhasedReleaseMatchRuleMatchRuleMatch> matches;
        private @Nullable Boolean matchesAlways;
        private @Nullable String name;
        private @Nullable Integer start;
        private String type;
        public Builder() {}
        public Builder(GetCloudletsPhasedReleaseMatchRuleMatchRule defaults) {
    	      Objects.requireNonNull(defaults);
    	      this.disabled = defaults.disabled;
    	      this.end = defaults.end;
    	      this.forwardSettings = defaults.forwardSettings;
    	      this.matchUrl = defaults.matchUrl;
    	      this.matches = defaults.matches;
    	      this.matchesAlways = defaults.matchesAlways;
    	      this.name = defaults.name;
    	      this.start = defaults.start;
    	      this.type = defaults.type;
        }

        @CustomType.Setter
        public Builder disabled(@Nullable Boolean disabled) {

            this.disabled = disabled;
            return this;
        }
        @CustomType.Setter
        public Builder end(@Nullable Integer end) {

            this.end = end;
            return this;
        }
        @CustomType.Setter
        public Builder forwardSettings(GetCloudletsPhasedReleaseMatchRuleMatchRuleForwardSettings forwardSettings) {
            if (forwardSettings == null) {
              throw new MissingRequiredPropertyException("GetCloudletsPhasedReleaseMatchRuleMatchRule", "forwardSettings");
            }
            this.forwardSettings = forwardSettings;
            return this;
        }
        @CustomType.Setter
        public Builder matchUrl(@Nullable String matchUrl) {

            this.matchUrl = matchUrl;
            return this;
        }
        @CustomType.Setter
        public Builder matches(@Nullable List<GetCloudletsPhasedReleaseMatchRuleMatchRuleMatch> matches) {

            this.matches = matches;
            return this;
        }
        public Builder matches(GetCloudletsPhasedReleaseMatchRuleMatchRuleMatch... matches) {
            return matches(List.of(matches));
        }
        @CustomType.Setter
        public Builder matchesAlways(@Nullable Boolean matchesAlways) {

            this.matchesAlways = matchesAlways;
            return this;
        }
        @CustomType.Setter
        public Builder name(@Nullable String name) {

            this.name = name;
            return this;
        }
        @CustomType.Setter
        public Builder start(@Nullable Integer start) {

            this.start = start;
            return this;
        }
        @CustomType.Setter
        public Builder type(String type) {
            if (type == null) {
              throw new MissingRequiredPropertyException("GetCloudletsPhasedReleaseMatchRuleMatchRule", "type");
            }
            this.type = type;
            return this;
        }
        public GetCloudletsPhasedReleaseMatchRuleMatchRule build() {
            final var _resultValue = new GetCloudletsPhasedReleaseMatchRuleMatchRule();
            _resultValue.disabled = disabled;
            _resultValue.end = end;
            _resultValue.forwardSettings = forwardSettings;
            _resultValue.matchUrl = matchUrl;
            _resultValue.matches = matches;
            _resultValue.matchesAlways = matchesAlways;
            _resultValue.name = name;
            _resultValue.start = start;
            _resultValue.type = type;
            return _resultValue;
        }
    }
}
