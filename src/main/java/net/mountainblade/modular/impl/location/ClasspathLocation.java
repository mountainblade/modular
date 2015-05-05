package net.mountainblade.modular.impl.location;

import gnu.trove.set.hash.TLinkedHashSet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
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


    public ClasspathLocation(URI uri, String realm) {
        super(uri, realm);
    }

    @Override
    public Collection<String> listClassNames() {
        LinkedList<String> classNames = new LinkedList<>();

        // Plain ol' class file
        File root = new File(uri);
        String rootPath = root.getAbsolutePath();

        for (File file : walkDirectory(root, new TLinkedHashSet<String>())) {
            addProperClassName(file.getAbsolutePath().substring(rootPath.length() + 1), classNames);
        }

        return classNames;
    }

    private Collection<File> walkDirectory(File root, Collection<String> visited) {
        if (root == null) {
            return Collections.emptyList();
        }

        LinkedList<File> files = new LinkedList<>();
        File[] listFiles = root.listFiles();

        if (listFiles != null) {
            for (File file : listFiles) {
                files.add(file);

                try {
                    String canonicalPath = file.getCanonicalPath();
                    visited.add(canonicalPath);

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
