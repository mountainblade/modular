package net.mountainblade.modular.impl.location;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a location for local classes within our classpath.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class ClasspathLocation extends ClassLocation {
    private static final Logger LOG = Logger.getLogger(ClasspathLocation.class.getName());
    private static final String FILE_SUFFIX = ".class";


    public ClasspathLocation(URI uri, String realm) {
        super(uri, realm);
    }

    @Override
    public Collection<String> listClassNames() {
        LinkedList<String> classNames = new LinkedList<>();

        // Plain ol' class file
        File root = new File(uri);
        String rootPath = root.getAbsolutePath();

        for (File file : walkDirectory(root, new LinkedList<String>())) {
            // We only need class files
            if (!file.getName().endsWith(FILE_SUFFIX)) {
                continue;
            }

            String name = file.getAbsolutePath()
                    // Replace path prefix and ending slash
                    .substring(rootPath.length() + 1)

                    // Remove trailing .class
                    .replace(FILE_SUFFIX, "")

                    // Make the file name a proper class name
                    .replace("\\", "/")
                    .replace("/", ".");

            // Only add if we got a name that is not on the blacklist for class names either
            if (!isBlacklisted(name)) {
                classNames.add(name);
            }
        }

        return classNames;
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
                    LOG.log(Level.WARNING, "Could not get canonical path of file: " + file, e);
                }

                if (file.isDirectory()) {
                    files.addAll(walkDirectory(file, visited));
                }
            }
        }

        return files;
    }


}
