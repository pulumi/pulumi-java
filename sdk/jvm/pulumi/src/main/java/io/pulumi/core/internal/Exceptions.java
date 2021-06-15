package io.pulumi.core.internal;

import io.grpc.Internal;

import java.io.PrintWriter;
import java.io.StringWriter;

@Internal
public class Exceptions {

    private Exceptions() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * @return the stack trace from a Throwable as a String
     */
    public static String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        throwable.printStackTrace(printWriter);
        return stringWriter.getBuffer().toString();
    }
}
