/**
 * Copyright (C) 2014 MountainBlade (http://mountainblade.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.mountainblade.modular;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a class that parses and stores version information using the extended
 * <a href="http://semver.org">semantic version format</a> (with pre-release and build labels).
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class Version implements Comparable<Version> {
    public static final Pattern SEMVER_DETECT = Pattern.compile("\\bv?(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\." +
            "(?:0|[1-9][0-9]*)(?:-[\\da-z\\-]+(?:\\.[\\da-z\\-]+)*)?(?:\\+[\\da-z\\-]+(?:\\.[\\da-z\\-]+)*)?\\b",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern SEMVER_FORMAT = Pattern.compile("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\." +
            "(0|[1-9][0-9]*))?(?:-([\\da-z\\-]+(?:\\.[\\da-z\\-]+)*))?(?:\\+([\\da-z\\-]+(?:\\.[\\da-z\\-]+)*))?",
            Pattern.CASE_INSENSITIVE);

    public static final String SNAPSHOT = "SNAPSHOT";

    private int major;
    private int minor;
    private int patch;
    private String preRelease;
    private String build;
    private boolean snapshot;

    /**
     * Creates a new version.
     *
     * @param major    The major version (for incompatible API changes)
     */
    public Version(int major) {
        this(major, 0);
    }

    /**
     * Creates a new version.
     *
     * @param major    The major version (for incompatible API changes)
     * @param minor    The minor version (functionality in a backwards-compatible manner)
     */
    public Version(int major, int minor) {
        this(major, minor, 0);
    }

    /**
     * Creates a new version.
     *
     * @param major    The major version (for incompatible API changes)
     * @param minor    The minor version (functionality in a backwards-compatible manner)
     * @param patch    The patch version (backwards-compatible bug fixes)
     */
    public Version(int major, int minor, int patch) {
        this(major, minor, patch, "", "");
    }

    /**
     * Creates a new version.
     *
     * @param major       The major version (for incompatible API changes)
     * @param minor       The minor version (functionality in a backwards-compatible manner)
     * @param patch       The patch version (backwards-compatible bug fixes)
     * @param snapshot    True if the version represents a snapshot, false if not
     */
    public Version(int major, int minor, int patch, boolean snapshot) {
        this(major, minor, patch, snapshot ? SNAPSHOT : "", "");
    }

    /**
     * Creates a new version.
     *
     * @param major         The major version (for incompatible API changes)
     * @param minor         The minor version (functionality in a backwards-compatible manner)
     * @param patch         The patch version (backwards-compatible bug fixes)
     * @param preRelease    The pre-release tag
     * @param build         The build identifier
     */
    public Version(int major, int minor, int patch, String preRelease, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease != null ? preRelease : "";
        this.build = build != null ? build : "";

        checkSnapshot();
    }

    /**
     * Creates a new version from the given text.
     *
     * @param text    The version text
     * @throws IllegalArgumentException When the given text does not contain a valid version tag
     */
    public Version(String text) throws IllegalArgumentException {
        final Matcher matcher = SEMVER_FORMAT.matcher(text.trim());
        final int i = matcher.groupCount();

        if (!matcher.matches() || 0 > i) {
            throw new IllegalArgumentException("Invalid version string");
        }

        String majorStr = matcher.group(1);
        String minorStr = matcher.group(2);
        String patchStr = matcher.group(3);

        major = Integer.parseInt(majorStr);
        minor = Integer.parseInt(minorStr);
        patch = patchStr == null ? 0 : Integer.parseInt(patchStr);

        // Extra information
        String preReleaseStr = matcher.group(4);
        preRelease = preReleaseStr != null ? preReleaseStr : "";

        String buildStr = matcher.group(5);
        build = buildStr != null ? buildStr : "";

        checkSnapshot();
    }

    private void checkSnapshot() {
        snapshot = preRelease.equalsIgnoreCase(SNAPSHOT);
    }

    /**
     * Gets the major version label representing incompatible API changes.
     *
     * @return The major version
     */
    public int getMajor() {
        return major;
    }

    /**
     * Gets the minor version label for functionality in a backwards-compatible manner.
     *
     * @return The minor version
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Gets the patch label representing backwards-compatible bug fixes.
     *
     * @return The patch version
     */
    public int getPatch() {
        return patch;
    }

    /**
     * Gets the pre-release label extension.
     *
     * @return The pre-release tag
     */
    public String getPreRelease() {
        return preRelease;
    }

    /**
     * Gets the build label extension.
     *
     * @return The pre-release tag
     */
    public String getBuild() {
        return build;
    }

    /**
     * Indicates whether or not the version represents a snapshot version
     * (indicated by the "SNAPSHOT" suffix in the pre-release label).
     *
     * @return True if the version represents a snapshot, false if not
     */
    public boolean isSnapshot() {
        return snapshot;
    }

    /**
     * Converts this version to a usable version tag.
     *
     * Its output is as follows:
     * <ul>
     *     <li>The MAJOR, MINOR and PATCH labels delimited by a dot</li>
     *     <li>If set, the Pre-Release label suffixed by a dash ("-")</li>
     *     <li>If set, the build label suffixed by a plus sign ("+")</li>
     * </ul>
     *
     * If the version describes a snapshot version the "SNAPSHOT" tag
     * will be written all uppercase. e.g: "1.0.2-SNAPSHOT+b10031".
     *
     * @return The full version tag
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(major).append('.').append(minor).append('.').append(patch);

        if (!preRelease.isEmpty()) {
            builder.append('-');

            if (isSnapshot()) {
                builder.append(SNAPSHOT);

            } else {
                builder.append(preRelease);
            }
        }

        if (!build.isEmpty()) {
            builder.append('+').append(build);
        }

        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Version version = (Version) o;
        return major == version.major && minor == version.minor && patch == version.patch &&
                preRelease.equalsIgnoreCase(version.preRelease) && build.equalsIgnoreCase(version.build);
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        result = 31 * result + preRelease.toLowerCase().hashCode();
        result = 31 * result + build.toLowerCase().hashCode();
        return result;
    }

    @Override
    public int compareTo(Version other) {
        int result = major - other.major;

        if (result == 0) {
            result = minor - other.minor;
        }
        if (result == 0) {
            result = patch - other.patch;
        }
        if (result == 0) {
            result = preRelease.compareTo(other.preRelease);
        }

        return result;
    }

    /**
     * Tries to find as many version tags within the given text and returns the result as an array.
     *
     * @param versions    The text to look through
     * @return All found version tags. Can be empty.
     */
    public static Version[] parseMulti(String... versions) {
        if (versions == null) {
            return new Version[0];
        }

        final Collection<Version> results = new LinkedList<>();
        for (String version : versions) {
            Matcher matcher = SEMVER_DETECT.matcher(version.toLowerCase());

            // Try to parse each version and add it to the list/array
            while (matcher.find()) {
                try {
                    String match = matcher.group();
                    results.add(new Version(match));

                } catch (IllegalArgumentException ignore) {
                    // fallthrough
                }
            }
        }

        return results.toArray(new Version[results.size()]);
    }

    /**
     * Parses the given string and tries to return only one version instance.
     *
     * @param version    The version text to parse
     * @return The parsed version, can be empty
     * @throws IllegalArgumentException when the given string contains no, or more than one version
     */
    public static Version parse(String version) throws IllegalArgumentException {
        final Version[] versions = Version.parseMulti(version);
        if (versions.length < 1) {
            throw new IllegalArgumentException("Cannot set empty / illegal version: " + version);
        }

        if (versions.length > 1) {
            throw new IllegalArgumentException("Invalid version tag; more than one version found: " + version);
        }

        return versions[0];
    }

}
