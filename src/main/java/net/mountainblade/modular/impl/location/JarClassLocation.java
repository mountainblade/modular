package net.mountainblade.modular.impl.location;

import net.mountainblade.modular.UriHelper;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents a location for classes that are inside a .jar file.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class JarClassLocation extends ClassLocation {
    private final boolean isJar;


    public JarClassLocation(URI uri, String realm) {
        super(uri, realm);

        isJar = UriHelper.matchesScheme(uri, "file") && uri.getPath().endsWith(".jar");
    }

    @Override
    public Collection<String> listClassNames() {
        if (!isJar) {
            return Collections.emptyList();
        }


        // TODO make this work for JAR files
        return null;
    }

}
