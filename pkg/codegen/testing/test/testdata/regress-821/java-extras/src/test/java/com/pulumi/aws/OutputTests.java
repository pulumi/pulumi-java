package com.pulumi.aws;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pulumi.aws.ec2.Ec2Functions;
import com.pulumi.aws.ec2.inputs.GetVpcArgs;
import com.pulumi.aws.ec2.outputs.GetVpcResult;
import com.pulumi.test.internal.PulumiTestInternal;
import com.pulumi.test.Mocks;
import com.pulumi.test.TestOptions;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static com.pulumi.test.PulumiTest.extractValue;

class OutputTests {

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testPreviewOutput() {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder()
                        .preview(true)
                        .build())
                .mocks(new Regress821Mocks())
                .build();

        var result = test.runTest(ctx -> {
            var state = Ec2Functions.getVpc(GetVpcArgs.builder().build())
                    .applyValue(GetVpcResult::state);
            ctx.export("vpcStateOutput", state);
        });

        var outputs = result.outputs();
        var resources = result.resources();
        var exceptions = result.exceptions();
        var errors = result.errors();

        assertThat(resources).hasSize(1);
        assertThat(exceptions).isEmpty();
        assertThat(errors).isEmpty();
        assertThat(extractValue(outputs.get("vpcStateOutput"))).isNull();
    }

    public static class Regress821Mocks implements Mocks {
        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            throw new RuntimeException("Not used");
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(CallArgs args) {
            return CompletableFuture.completedFuture(Map.of());
        }
    }
}