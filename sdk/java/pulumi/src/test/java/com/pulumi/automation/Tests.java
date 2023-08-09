package com.pulumi.automation;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class Tests {

    private static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";

    public static String randomStackName() {
        var letters = Stream.generate(() -> {
            var index = ThreadLocalRandom.current().nextInt(0, LOWERCASE_LETTERS.length());
            return LOWERCASE_LETTERS.charAt(index);
        });
        return letters.limit(8).collect(Collector.of(
                StringBuilder::new,
                StringBuilder::append,
                StringBuilder::append,
                StringBuilder::toString
        ));
    }

    public static String randomSuffix() {
        Integer integer = ThreadLocalRandom.current().nextInt();
        return String.format("%08x", integer); // 8 hex characters
    }
}
