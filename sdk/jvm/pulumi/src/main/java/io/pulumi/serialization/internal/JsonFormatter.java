package com.pulumi.serialization.internal;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.pulumi.core.Either;

public class JsonFormatter {

    private static final JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

    public static Either<RuntimeException, String> format(MessageOrBuilder value) {
        try {
            return Either.valueOf(printer.print(value));
        } catch (InvalidProtocolBufferException e) {
            return Either.errorOf(new IllegalStateException(e));
        }
    }
}
