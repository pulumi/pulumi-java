package com.pulumi.core.internal;

import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;

@InternalUse
public class Exceptions {

    private Exceptions() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * @return the stack trace from a Throwable as a String
     */
    public static String getStackTrace(@Nullable Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        if (throwable != null) {
            throwable.printStackTrace(printWriter);
        }
        return stringWriter.getBuffer().toString();
    }
}
