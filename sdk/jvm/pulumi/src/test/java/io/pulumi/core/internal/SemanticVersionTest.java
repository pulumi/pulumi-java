package io.pulumi.core.internal;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticVersionTest {

    @Test
    void testHashCodeEqualsContract() {
        EqualsVerifier.forClass(SemanticVersion.class).verify();
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "0, 0.0",
            "0, 0.0.0",
            "0-alpha, 0.0.0-alpha",
            "0-alpha-1, 0.0.0-alpha-1",
            "1.2.3-rc1-ef25a70, 1.2.3-rc1-ef25a70"
    })
    void testComparableEquality(String first, String second) {
        var firstVer = SemanticVersion.parse(first);
        var secondVer = SemanticVersion.parse(second);

        //noinspection ConstantConditions
        var isComparableZero = firstVer.equals(secondVer) && firstVer instanceof Comparable && firstVer.compareTo(secondVer) == 0;
        assertThat(isComparableZero).isTrue();
        assertThat(firstVer.isEqualTo(secondVer)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "0.0.0, 0.0.1, -1",
            "0.0.1, 0.0.1, 0",
            "0.0.1, 0.0.0, 1",
            "0.0.2, 0.0.1-rc4-deadbeef, 1",
            "0.0.1-rc4-deadbeef, 0.0.2, -1",
            "0.0.1-rc5, 0.0.1-rc4-deadbeef, 1",
            "0.0.1-rc4-deadbeef, 0.0.1-rc5, -1",
            "1.0.0-beta.11, 1.0.0-rc.1, -1",
            // TODO: C# impl ignored leading zeros in pre, should we?
            // FIXME: our impl does not comply with https://semver.org/spec/v2.0.0.html
            "1.2.3-01, 1.2.3-1, -1",
            "1.2.3-a.01, 1.2.3-a.1, -1",
            "1.2.3-a.000001, 1.2.3-a.1, -1",
            "1.2.3+01, 1.2.3+1, -1",
            "1.2.3+a.01, 1.2.3+a.1, -1",
            "1.2.3+a.000001, 1.2.3+a.1, -1"
    })
    void testComparable(String first, String second, int expected) {
        var firstVer = SemanticVersion.parse(first);
        var secondVer = SemanticVersion.parse(second);

        assertThat(firstVer.compareTo(secondVer))
                .describedAs("Expected '%s'.compareTo('%s') to be: '%s'", firstVer, secondVer, expected)
                .isEqualTo(expected);
        if (expected == 0) {
            assertThat(firstVer.isEqualTo(secondVer)).isTrue();
            assertThat(firstVer.isGreaterOrEqualTo(secondVer)).isTrue();
            assertThat(firstVer.isLessOrEqualTo(secondVer)).isTrue();
            assertThat(firstVer.isGreaterThan(secondVer)).isFalse();
            assertThat(firstVer.isLessThan(secondVer)).isFalse();
        }
        if (expected > 0) {
            assertThat(firstVer.isEqualTo(secondVer)).isFalse();
            assertThat(firstVer.isGreaterOrEqualTo(secondVer)).isTrue();
            assertThat(firstVer.isLessOrEqualTo(secondVer)).isFalse();
            assertThat(firstVer.isGreaterThan(secondVer)).isTrue();
            assertThat(firstVer.isLessThan(secondVer)).isFalse();
        }
        if (expected < 0) {
            assertThat(firstVer.isEqualTo(secondVer)).isFalse();
            assertThat(firstVer.isGreaterOrEqualTo(secondVer)).isFalse();
            assertThat(firstVer.isLessOrEqualTo(secondVer)).isTrue();
            assertThat(firstVer.isGreaterThan(secondVer)).isFalse();
            assertThat(firstVer.isLessThan(secondVer)).isTrue();
        }
    }
}