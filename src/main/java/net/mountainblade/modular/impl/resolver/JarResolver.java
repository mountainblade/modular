package net.mountainblade.modular.impl.resolver;

import net.mountainblade.modular.UriHelper;
import net.mountainblade.modular.impl.location.ClassLocation;
import net.mountainblade.modular.impl.location.JarClassLocation;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Represents a resolver for JAR module locations.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class JarResolver extends ClassResolver {

    @Override
    public boolean handles(URI uri) {
        return UriHelper.matchesScheme(uri, "file");
    }

    @Override
    public Collection<ClassLocation> resolve(URI uri) {
        final LinkedList<ClassLocation> locations = new LinkedList<>();
        final File[] files = new File(uri.getSchemeSpecificPart()).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory() && file.getName().toLowerCase().endsWith(".jar");
            }
        });

        if (files != null) {
            for (File file : files) {
                locations.add(new JarClassLocation(file));
            }
        }

        return locations;
    }

}
