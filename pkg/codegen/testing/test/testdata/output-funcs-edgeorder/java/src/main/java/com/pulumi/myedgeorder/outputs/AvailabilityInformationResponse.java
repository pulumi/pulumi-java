// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.myedgeorder.outputs;

import com.pulumi.core.annotations.CustomType;
import java.lang.String;
import java.util.Objects;

@CustomType
public final class AvailabilityInformationResponse {
    /**
     * @return Current availability stage of the product. Availability stage
     * 
     */
    private String availabilityStage;
    /**
     * @return Reason why the product is disabled.
     * 
     */
    private String disabledReason;
    /**
     * @return Message for why the product is disabled.
     * 
     */
    private String disabledReasonMessage;

    private AvailabilityInformationResponse() {}
    /**
     * @return Current availability stage of the product. Availability stage
     * 
     */
    public String availabilityStage() {
        return this.availabilityStage;
    }
    /**
     * @return Reason why the product is disabled.
     * 
     */
    public String disabledReason() {
        return this.disabledReason;
    }
    /**
     * @return Message for why the product is disabled.
     * 
     */
    public String disabledReasonMessage() {
        return this.disabledReasonMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AvailabilityInformationResponse defaults) {
        return new Builder(defaults);
    }
    @CustomType.Builder
    public static final class Builder {
        private String availabilityStage;
        private String disabledReason;
        private String disabledReasonMessage;
        public Builder() {}
        public Builder(AvailabilityInformationResponse defaults) {
    	      Objects.requireNonNull(defaults);
    	      this.availabilityStage = defaults.availabilityStage;
    	      this.disabledReason = defaults.disabledReason;
    	      this.disabledReasonMessage = defaults.disabledReasonMessage;
        }

        @CustomType.Setter
        public Builder availabilityStage(String availabilityStage) {
            this.availabilityStage = Objects.requireNonNull(availabilityStage);
            return this;
        }
        @CustomType.Setter
        public Builder disabledReason(String disabledReason) {
            this.disabledReason = Objects.requireNonNull(disabledReason);
            return this;
        }
        @CustomType.Setter
        public Builder disabledReasonMessage(String disabledReasonMessage) {
            this.disabledReasonMessage = Objects.requireNonNull(disabledReasonMessage);
            return this;
        }
        public AvailabilityInformationResponse build() {
            final var o = new AvailabilityInformationResponse();
            o.availabilityStage = availabilityStage;
            o.disabledReason = disabledReason;
            o.disabledReasonMessage = disabledReasonMessage;
            return o;
        }
    }
}
