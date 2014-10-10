package net.mountainblade.modular.impl;

import gnu.trove.set.hash.TLinkedHashSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;
import net.mountainblade.modular.UriHelper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Represents the ModuleLocation.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Getter
@EqualsAndHashCode
@ToString
@Log
public class ClasspathLocation {
    @Getter
    private static final Collection<String> nameBlacklist = new TLinkedHashSet<>();
    static {
        // Add maven stuff
        nameBlacklist.add("target.classes");
        nameBlacklist.add("target.test-classes");
    }

    private final URI uri;
    private URL url;

    private final String realm;
    private final boolean isJar;


    public ClasspathLocation(URI uri, String realm) {
        this.uri = uri;
        this.realm = realm;

        try {
            this.url = uri.toURL();

        } catch (MalformedURLException e) {
            log.log(Level.WARNING, "Could not convert URI to URL", e);
        }

        isJar = UriHelper.matchesScheme(uri, "file") && uri.getPath().endsWith(".jar");
    }


    // TODO move logic to separate class

    public Collection<String> listClassNames() {
        LinkedList<String> classNames = new LinkedList<>();

        if (isJarFile()) {
            // Jar file
            // TODO

        } else {
            // Plain 'ol class file
            File root = new File(uri);
            String rootPath = root.getAbsolutePath();

            for (File file : walkDirectory(root, new LinkedList<String>())) {
                // We only need class files
                if (!file.getName().endsWith(".class")) {
                    continue;
                }

                String path = Pattern.quote(rootPath);
                String name = file.getAbsolutePath()
                        .replaceAll(path, "")
                        .substring(1)
                        .replace("\\", "/")
                        .replace("/", ".");

                // Remove trailing .class
                if (name.endsWith("class")) {
                    name = name.substring(0, name.length() - (".class".length()));
                }

                // Only add if we got a name that is not on the blacklist for class names either
                if (!isBlacklisted(name)) {
                    classNames.add(name);
                }
            }
        }

        return classNames;
    }

    public boolean isJarFile() {
        return isJar;
    }

    private Collection<File> walkDirectory(File root, Collection<String> visited) {
        if (root == null) {
            return new ArrayList<>(0);
        }

        LinkedList<File> files = new LinkedList<>();
        File[] listFiles = root.listFiles();

        if (listFiles != null) {
            for (File file : listFiles) {
                files.add(file);

                try {
                    String canonicalPath = file.getCanonicalPath();

                    if (!visited.contains(canonicalPath)) {
                        visited.add(canonicalPath);
                    }

                } catch (IOException e) {
                    log.log(Level.WARNING, "Could not get canonical path of file: " + file, e);
                }

                if (file.isDirectory()) {
                    files.addAll(walkDirectory(file, visited));
                }
            }
        }

        return files;
    }


    protected final boolean isBlacklisted(String className) {
        for (String blackListed : nameBlacklist) {
            if (className.contains(blackListed)) {
                return true;
            }
        }

        return false;
    }

}
