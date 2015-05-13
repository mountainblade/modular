/**
 * Copyright (c) 2012-2015 MountainBlade.
 * All rights reserved.
 */
package net.mountainblade.modular;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a class that parses and stores version information using the <a href="http://semver.org">semantic version
 * format</a>.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class Version {
    public static final Pattern SEMVER_DETECT = Pattern.compile("\\bv?(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\." +
            "(?:0|[1-9][0-9]*)(?:-[\\da-z\\-]+(?:\\.[\\da-z\\-]+)*)?(?:\\+[\\da-z\\-]+(?:\\.[\\da-z\\-]+)*)?\\b",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern SEMVER_FORMAT = Pattern.compile("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\." +
            "(0|[1-9][0-9]*))?(?:-([\\da-z\\-]+(?:\\.[\\da-z\\-]+)*))?(?:\\+([\\da-z\\-]+(?:\\.[\\da-z\\-]+)*))?",
            Pattern.CASE_INSENSITIVE);

    private int major;
    private int minor;
    private int patch;
    private String preRelease;
    private String build;
    private boolean snapshot;


    public Version(int major) {
        this(major, 0);
    }

    public Version(int major, int minor) {
        this(major, minor, 0);
    }

    public Version(int major, int minor, int patch) {
        this(major, minor, patch, "", "");
    }

    public Version(int major, int minor, int patch, String preRelease, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.build = build;

        checkSnapshot();
    }

    public Version(String text) throws IllegalArgumentException {
        final Matcher matcher = SEMVER_FORMAT.matcher(text);
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

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getPreRelease() {
        return preRelease;
    }

    public String getBuild() {
        return build;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    private void checkSnapshot() {
        snapshot = preRelease.equalsIgnoreCase("snapshot");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(major).append('.').append(minor).append('.').append(patch);

        if (!preRelease.isEmpty()) {
            builder.append('-').append(preRelease);
        }

        if (!build.isEmpty()) {
            builder.append('+').append(build);
        }

        return builder.toString();
    }

    /**
     * Tries to find as many version tags within the given text and returns the result as an array.
     *
     * @param text    The text to look through
     * @return All found version tags. Can be empty.
     */
    public static Version[] parse(String text) {
        if (text == null) {
            return new Version[0];
        }

        Collection<Version> results = new LinkedList<>();
        Matcher matcher = SEMVER_DETECT.matcher(text.toLowerCase());

        // Try to parse each version and add it to the list/array
        while (matcher.find()) {
            try {
                String match = matcher.group();
                results.add(new Version(match));

            } catch (IllegalArgumentException ignore) {
                // fallthrough
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
    public static Version parseSingle(String version) throws IllegalArgumentException {
        final Version[] versions = Version.parse(version);
        if (versions.length < 1) {
            throw new IllegalArgumentException("Cannot set empty / illegal version: " + version);
        }

        if (versions.length > 1) {
            throw new IllegalArgumentException("Invalid version tag; more than one version found: " + version);
        }

        return versions[0];
    }

}
