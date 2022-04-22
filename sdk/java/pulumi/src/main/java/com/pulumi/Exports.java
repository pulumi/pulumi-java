package com.pulumi;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;

import java.util.Map;

/**
 * All exports associated with the current {@link Context}
 */
public interface Exports {

    /**
     * Used by Pulumi runtime.
     * @return Exports and {@link Output} from a Pulumi stack.
     */
    @InternalUse
    Map<String, Output<?>> exports();
}
