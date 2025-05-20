// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import org.junit.jupiter.api.Test;

import com.pulumi.automation.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionTest {
    @Test
    public void testOfMajorMinor() {
        var version = Version.of(1, 2);
        assertThat(version.majorVersion()).isEqualTo(1);
        assertThat(version.minorVersion()).isEqualTo(2);
        assertThat(version.patchVersion()).isEqualTo(0);
    }

    @Test
    public void testOfMajorMinorPatch() {
        var version = Version.of(4, 5, 6);
        assertThat(version.majorVersion()).isEqualTo(4);
        assertThat(version.minorVersion()).isEqualTo(5);
        assertThat(version.patchVersion()).isEqualTo(6);
    }

    @Test
    public void testParse() {
        var version = Version.parse("1.2.3");
        assertThat(version.majorVersion()).isEqualTo(1);
        assertThat(version.minorVersion()).isEqualTo(2);
        assertThat(version.patchVersion()).isEqualTo(3);
    }

    @Test
    public void testTryParse() {
        var version = Version.tryParse("1.2.3");
        assertThat(version).isPresent();
        assertThat(version.get().majorVersion()).isEqualTo(1);
        assertThat(version.get().minorVersion()).isEqualTo(2);
        assertThat(version.get().patchVersion()).isEqualTo(3);

        var invalidVersion = Version.tryParse("invalid");
        assertThat(invalidVersion).isNotPresent();
    }

    @Test
    public void testIsHigherThan() {
        var version1 = Version.of(1, 2, 3);
        var version2 = Version.of(1, 2, 2);
        assertThat(version1.isHigherThan(version2)).isTrue();
    }

    @Test
    public void testIsHigherThanOrEquivalentTo() {
        var version1 = Version.of(1, 2, 3);
        var version2 = Version.of(1, 2, 3);
        assertThat(version1.isHigherThanOrEquivalentTo(version2)).isTrue();
    }

    @Test
    public void testIsLowerThan() {
        var version1 = Version.of(1, 2, 2);
        var version2 = Version.of(1, 2, 3);
        assertThat(version1.isLowerThan(version2)).isTrue();
    }

    @Test
    public void testIsLowerThanOrEquivalentTo() {
        var version1 = Version.of(1, 2, 3);
        var version2 = Version.of(1, 2, 3);
        assertThat(version1.isLowerThanOrEquivalentTo(version2)).isTrue();
    }

    @Test
    public void testIsEquivalentTo() {
        var version1 = Version.of(1, 2, 3);
        var version2 = Version.of(1, 2, 3);
        assertThat(version1.isEquivalentTo(version2)).isTrue();
    }

    @Test
    public void testEqualsAndHashCode() {
        var version1 = Version.of(1, 2, 3);
        var version2 = Version.of(1, 2, 3);
        assertThat(version1).isEqualTo(version2);
        assertThat(version1.hashCode()).isEqualTo(version2.hashCode());
    }

    @Test
    public void testCompareTo() {
        var version1 = Version.of(1, 2, 3);
        var version2 = Version.of(1, 2, 2);
        assertThat(version1.compareTo(version2)).isGreaterThan(0);
    }

    @Test
    public void testToString() {
        var version = Version.of(1, 2, 3);
        assertThat(version.toString()).isEqualTo("1.2.3");
    }
}
