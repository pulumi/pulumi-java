// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.mypkg.outputs;

import com.pulumi.core.annotations.CustomType;
import java.lang.Double;
import java.lang.String;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

@CustomType
public final class SsisEnvironmentReferenceResponse {
    /**
     * @return Environment folder name.
     * 
     */
    private @Nullable String environmentFolderName;
    /**
     * @return Environment name.
     * 
     */
    private @Nullable String environmentName;
    /**
     * @return Environment reference id.
     * 
     */
    private @Nullable Double id;
    /**
     * @return Reference type
     * 
     */
    private @Nullable String referenceType;

    private SsisEnvironmentReferenceResponse() {}
    /**
     * @return Environment folder name.
     * 
     */
    public Optional<String> environmentFolderName() {
        return Optional.ofNullable(this.environmentFolderName);
    }
    /**
     * @return Environment name.
     * 
     */
    public Optional<String> environmentName() {
        return Optional.ofNullable(this.environmentName);
    }
    /**
     * @return Environment reference id.
     * 
     */
    public Optional<Double> id() {
        return Optional.ofNullable(this.id);
    }
    /**
     * @return Reference type
     * 
     */
    public Optional<String> referenceType() {
        return Optional.ofNullable(this.referenceType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(SsisEnvironmentReferenceResponse defaults) {
        return new Builder(defaults);
    }
    @CustomType.Builder
    public static final class Builder {
        private @Nullable String environmentFolderName;
        private @Nullable String environmentName;
        private @Nullable Double id;
        private @Nullable String referenceType;
        public Builder() {}
        public Builder(SsisEnvironmentReferenceResponse defaults) {
    	      Objects.requireNonNull(defaults);
    	      this.environmentFolderName = defaults.environmentFolderName;
    	      this.environmentName = defaults.environmentName;
    	      this.id = defaults.id;
    	      this.referenceType = defaults.referenceType;
        }

        @CustomType.Setter
        public Builder environmentFolderName(@Nullable String environmentFolderName) {

            this.environmentFolderName = environmentFolderName;
            return this;
        }
        @CustomType.Setter
        public Builder environmentName(@Nullable String environmentName) {

            this.environmentName = environmentName;
            return this;
        }
        @CustomType.Setter
        public Builder id(@Nullable Double id) {

            this.id = id;
            return this;
        }
        @CustomType.Setter
        public Builder referenceType(@Nullable String referenceType) {

            this.referenceType = referenceType;
            return this;
        }
        public SsisEnvironmentReferenceResponse build() {
            final var _resultValue = new SsisEnvironmentReferenceResponse();
            _resultValue.environmentFolderName = environmentFolderName;
            _resultValue.environmentName = environmentName;
            _resultValue.id = id;
            _resultValue.referenceType = referenceType;
            return _resultValue;
        }
    }
}
