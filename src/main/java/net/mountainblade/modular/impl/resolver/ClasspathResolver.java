package net.mountainblade.modular.impl.resolver;

import com.google.common.base.Splitter;
import net.mountainblade.modular.UriHelper;
import net.mountainblade.modular.impl.location.ClasspathLocation;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a locator for classes inside our own classpath.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class ClasspathResolver extends ClassResolver {
    private static final Logger LOG = Logger.getLogger(ClasspathResolver.class.getName());
    private static final String CLASS_PATH_SEPARATOR = System.getProperty("path.separator");
    private static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");
    private static final List<String> CLASSES = Splitter.on(CLASS_PATH_SEPARATOR).splitToList(JAVA_CLASS_PATH);


    public ClasspathResolver() {
        // Get root class loader
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        while (loader != null && loader.getParent() != null) {
            loader = loader.getParent();
        }

        // Add URLs to our blacklist
        if (loader != null && loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) loader).getURLs()) {
                try {
                    getUriBlacklist().add(url.toURI());

                } catch (URISyntaxException e) {
                    LOG.log(Level.WARNING, "Could not convert URL to URI", e);
                }
            }
        }

        // Add defaults to our name black list
        Collection<String> nameBlacklist = getNameBlacklist();
        nameBlacklist.add("/jre/lib/");
        nameBlacklist.add("/jdk/lib/");
        nameBlacklist.add("/lib/rt.jar");
    }

    @Override
    public boolean handles(URI uri) {
        return UriHelper.matchesScheme(uri, "classpath");
    }

    @Override
    public Collection<ClasspathLocation> resolve(URI uri) {
        // Create general list of sources
        // We do not do this in a static context since we can always add filters at runtime later on
        Collection<ClasspathLocation> locations = new LinkedList<>();

        for (String aClass : CLASSES) {
            URI fileUri = new File(aClass).toURI();

            if (!isBlacklisted(fileUri)) {
                locations.add(new ClasspathLocation(fileUri, "#classpath"));
            }
        }

        return locations;
    }

}
