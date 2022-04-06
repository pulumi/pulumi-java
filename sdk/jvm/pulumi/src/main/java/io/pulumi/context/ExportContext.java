package io.pulumi.context;

import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.InternalUse;

import java.util.Map;

/**
 * All exports associated with the current {@link StackContext}
 */
public interface ExportContext {

    /**
     * Used by Pulumi runtime.
     * @return Exports and {@link Output} from a Pulumi stack.
     */
    @InternalUse
    Map<String, Output<?>> exports();
}
