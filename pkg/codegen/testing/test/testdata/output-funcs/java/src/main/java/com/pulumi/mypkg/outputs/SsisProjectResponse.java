// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.mypkg.outputs;

import com.pulumi.core.annotations.CustomType;
import com.pulumi.exceptions.MissingRequiredPropertyException;
import com.pulumi.mypkg.outputs.SsisEnvironmentReferenceResponse;
import com.pulumi.mypkg.outputs.SsisParameterResponse;
import java.lang.Double;
import java.lang.String;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

@CustomType
public final class SsisProjectResponse {
    /**
     * @return Metadata description.
     * 
     */
    private @Nullable String description;
    /**
     * @return Environment reference in project
     * 
     */
    private @Nullable List<SsisEnvironmentReferenceResponse> environmentRefs;
    /**
     * @return Folder id which contains project.
     * 
     */
    private @Nullable Double folderId;
    /**
     * @return Metadata id.
     * 
     */
    private @Nullable Double id;
    /**
     * @return Metadata name.
     * 
     */
    private @Nullable String name;
    /**
     * @return Parameters in project
     * 
     */
    private @Nullable List<SsisParameterResponse> parameters;
    /**
     * @return The type of SSIS object metadata.
     * Expected value is &#39;Project&#39;.
     * 
     */
    private String type;
    /**
     * @return Project version.
     * 
     */
    private @Nullable Double version;

    private SsisProjectResponse() {}
    /**
     * @return Metadata description.
     * 
     */
    public Optional<String> description() {
        return Optional.ofNullable(this.description);
    }
    /**
     * @return Environment reference in project
     * 
     */
    public List<SsisEnvironmentReferenceResponse> environmentRefs() {
        return this.environmentRefs == null ? List.of() : this.environmentRefs;
    }
    /**
     * @return Folder id which contains project.
     * 
     */
    public Optional<Double> folderId() {
        return Optional.ofNullable(this.folderId);
    }
    /**
     * @return Metadata id.
     * 
     */
    public Optional<Double> id() {
        return Optional.ofNullable(this.id);
    }
    /**
     * @return Metadata name.
     * 
     */
    public Optional<String> name() {
        return Optional.ofNullable(this.name);
    }
    /**
     * @return Parameters in project
     * 
     */
    public List<SsisParameterResponse> parameters() {
        return this.parameters == null ? List.of() : this.parameters;
    }
    /**
     * @return The type of SSIS object metadata.
     * Expected value is &#39;Project&#39;.
     * 
     */
    public String type() {
        return this.type;
    }
    /**
     * @return Project version.
     * 
     */
    public Optional<Double> version() {
        return Optional.ofNullable(this.version);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(SsisProjectResponse defaults) {
        return new Builder(defaults);
    }
    @CustomType.Builder
    public static final class Builder {
        private @Nullable String description;
        private @Nullable List<SsisEnvironmentReferenceResponse> environmentRefs;
        private @Nullable Double folderId;
        private @Nullable Double id;
        private @Nullable String name;
        private @Nullable List<SsisParameterResponse> parameters;
        private String type;
        private @Nullable Double version;
        public Builder() {}
        public Builder(SsisProjectResponse defaults) {
    	      Objects.requireNonNull(defaults);
    	      this.description = defaults.description;
    	      this.environmentRefs = defaults.environmentRefs;
    	      this.folderId = defaults.folderId;
    	      this.id = defaults.id;
    	      this.name = defaults.name;
    	      this.parameters = defaults.parameters;
    	      this.type = defaults.type;
    	      this.version = defaults.version;
        }

        @CustomType.Setter
        public Builder description(@Nullable String description) {

            this.description = description;
            return this;
        }
        @CustomType.Setter
        public Builder environmentRefs(@Nullable List<SsisEnvironmentReferenceResponse> environmentRefs) {

            this.environmentRefs = environmentRefs;
            return this;
        }
        public Builder environmentRefs(SsisEnvironmentReferenceResponse... environmentRefs) {
            return environmentRefs(List.of(environmentRefs));
        }
        @CustomType.Setter
        public Builder folderId(@Nullable Double folderId) {

            this.folderId = folderId;
            return this;
        }
        @CustomType.Setter
        public Builder id(@Nullable Double id) {

            this.id = id;
            return this;
        }
        @CustomType.Setter
        public Builder name(@Nullable String name) {

            this.name = name;
            return this;
        }
        @CustomType.Setter
        public Builder parameters(@Nullable List<SsisParameterResponse> parameters) {

            this.parameters = parameters;
            return this;
        }
        public Builder parameters(SsisParameterResponse... parameters) {
            return parameters(List.of(parameters));
        }
        @CustomType.Setter
        public Builder type(String type) {
            if (type == null) {
              throw new MissingRequiredPropertyException("SsisProjectResponse", "type");
            }
            this.type = type;
            return this;
        }
        @CustomType.Setter
        public Builder version(@Nullable Double version) {

            this.version = version;
            return this;
        }
        public SsisProjectResponse build() {
            final var _resultValue = new SsisProjectResponse();
            _resultValue.description = description;
            _resultValue.environmentRefs = environmentRefs;
            _resultValue.folderId = folderId;
            _resultValue.id = id;
            _resultValue.name = name;
            _resultValue.parameters = parameters;
            _resultValue.type = type;
            _resultValue.version = version;
            return _resultValue;
        }
    }
}
