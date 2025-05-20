package com.pulumi.provider.internal;

import com.pulumi.provider.internal.models.*;
import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.provider.internal.testdata.TestComponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class ComponentProviderTests {

    private ComponentProvider provider;
    private Metadata metadata;


    @BeforeEach
    void setUp() {
        metadata = new Metadata("test-package", "1.0.0", "Test Package");
        provider = new ComponentProvider(metadata, TestComponent.class.getPackage());
    }

    @Test
    void getSchema_ShouldReturnValidSchema() {
        var request = new GetSchemaRequest(1, null, null);
        CompletableFuture<GetSchemaResponse> futureResponse = provider.getSchema(request);
        GetSchemaResponse response = futureResponse.join();

        assertNotNull(response);
        assertNotNull(response.getSchema());
        assertTrue(response.getSchema().contains("TestComponent"));
        assertTrue(response.getSchema().contains("testProperty"));
    }

    @Test
    void construct_ValidComponent_ShouldCreateInstance() {
        String name = "test-component";
        Map<String, PropertyValue> inputs = Map.of(
            "testProperty", PropertyValue.of("test-value")
        );
        ComponentResourceOptions options = ComponentResourceOptions.builder().build();
        ConstructRequest request = new ConstructRequest(
            "test-package:index:TestComponent",
            name,
            inputs,
            options
        );

        // We haven't initiated the deployment, so component construction will fail. Expect that specific exception,
        // since it will indicate that the rest of the provider is working. The checks are somewhat brittle and may fail
        // if we change the exception message or stack, but hopefully that will not happen too often.
        RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.construct(request).join());
        assertEquals("Failed to construct component: test-package:index:TestComponent", exception.getMessage());
        assertTrue(exception.getCause() instanceof InvocationTargetException);
        assertTrue(exception.getCause().getCause() instanceof IllegalStateException);
        assertEquals("Trying to acquire Deployment#instance before 'run' was called.", 
            exception.getCause().getCause().getMessage());
    }

    @Test
    void construct_InvalidPackageName_ShouldThrowException() {
        ConstructRequest request = new ConstructRequest(
            "wrong:index:TestComponent",
            "test",
            Map.of(),
            ComponentResourceOptions.builder().build()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            provider.construct(request).join();
        });
    }

    @Test
    void construct_InvalidModuleName_ShouldThrowException() {
        ConstructRequest request = new ConstructRequest(
            "test-package:wrong:TestComponent",
            "test",
            Map.of(),
            ComponentResourceOptions.builder().build()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            provider.construct(request).join();
        });
    }

    @Test
    void construct_NonExistentComponent_ShouldThrowException() {
        ConstructRequest request = new ConstructRequest(
            "test-package:index:NonExistentComponent",
            "test",
            Map.of(),
            ComponentResourceOptions.builder().build()
        );

        assertThrows(RuntimeException.class, () -> {
            provider.construct(request).join();
        });
    }
}
