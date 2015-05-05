package net.mountainblade.modular.impl.location;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents a location for classes that are inside a .jar file.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class JarClassLocation extends ClassLocation {
    private final File jarFile;


    public JarClassLocation(File jarFile, String realm) {
        super(jarFile.toURI(), realm);
        this.jarFile = jarFile;
    }

    @Override
    public Collection<String> listClassNames() {
        final Collection<String> classNames = new LinkedList<>();

        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(jarFile))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    addProperClassName(entry.getName(), classNames);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Could not fetch JAR file contents: " + jarFile, e);
        }

        return classNames;
    }

}
