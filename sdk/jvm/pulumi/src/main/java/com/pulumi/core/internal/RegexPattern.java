package com.pulumi.core.internal;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Regular expressions for simple use cases with a sane API.
 * <p/>
 * Limitations:
 * <ul>
 *     <li>Does not support multiline patterns (Pattern.MULTILINE mode),
 *         would require implementation changes if needed in the future.</li>
 * </ul>
 */
public class RegexPattern {

    private final Pattern pattern;

    public RegexPattern(Pattern pattern) {
        this.pattern = requireNonNull(pattern);
    }

    public RegexMatcher matcher(CharSequence input) {
        return new RegexMatcher(pattern, input);
    }

    @Override
    public String toString() {
        return pattern.toString();
    }

    public static RegexPattern of(String pattern) {
        requireNonNull(pattern);
        return new RegexPattern(Pattern.compile(pattern));
    }

    public static class RegexMatcher {
        private final Matcher matcher;
        private final Pattern pattern;

        public RegexMatcher(Pattern pattern, CharSequence input) {
            requireNonNull(input);
            this.matcher = pattern.matcher(input);
            this.pattern = requireNonNull(pattern);
        }

        public boolean hasMatch() {
            return matcher.matches();
        }

        public Optional<String> namedMatch(String name) {
            if (matcher.matches()) {
                try {
                    return Optional.ofNullable(matcher.group(name));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format(
                            "Unexpected input for pattern: '%s', got error: %s", this.pattern, e.getMessage()
                    ), e);
                }
            }
            return Optional.empty();
        }
    }
}
