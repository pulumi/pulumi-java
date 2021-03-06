// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.aws;

import com.pulumi.aws.config_endpoints.inputs.Endpoints;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.Codegen;
import java.lang.String;
import java.util.List;
import java.util.Optional;

public final class Config {

    private static final com.pulumi.Config config = com.pulumi.Config.of("aws");
    public Optional<List<Endpoints>> endpoints() {
        return Codegen.objectProp("endpoints", TypeShape.<List<Endpoints>>builder(List.class).addParameter(Endpoints.class).build()).config(config).get();
    }
/**
 * The region where AWS operations will take place. Examples are us-east-1, us-west-2, etc.
 * 
 */
    public String region() {
        return Codegen.stringProp("region").config(config).env("AWS_REGION", "AWS_DEFAULT_REGION").require();
    }
}
