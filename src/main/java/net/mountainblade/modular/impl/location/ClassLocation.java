package net.mountainblade.modular.impl.location;

import gnu.trove.set.hash.TLinkedHashSet;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a location (realm) for classes.
 *
 * @author spaceemotion
 * @version 1.0
 */
public abstract class ClassLocation {
    private static final Logger LOG = Logger.getLogger(ClassLocation.class.getName());
    private static final Collection<String> NAME_BLACKLIST = new TLinkedHashSet<>();
    private static final String FILE_SUFFIX = ".class";

    static {
        // Add maven stuff
        NAME_BLACKLIST.add("target.classes");
        NAME_BLACKLIST.add("target.test-classes");
    }

    protected final URI uri;
    protected final String realm;
    protected URL url;


    public ClassLocation(URI uri, String realm) {
        this.uri = uri;
        this.realm = realm;

        // Resolve URL so we can cache the value
        try {
            url = uri.toURL();

        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Could not convert URI to URL", e);
            throw new IllegalArgumentException("Invalid URI given", e);
        }
    }

    public URI getUri() {
        return uri;
    }

    public String getRealm() {
        return realm;
    }

    public URL getUrl() {
        return url;
    }

    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    public abstract Collection<String> listClassNames();

    protected void addProperClassName(String classPath, Collection<String> classNames) {
        // We only need class files
        if (!classPath.endsWith(FILE_SUFFIX)) {
            return;
        }

        String name = classPath
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

    protected final boolean isBlacklisted(String className) {
        for (String blackListed : NAME_BLACKLIST) {
            if (className.contains(blackListed)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassLocation)) return false;

        ClassLocation that = (ClassLocation) o;
        return !(realm != null ? !realm.equals(that.realm) : that.realm != null) && uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + (realm != null ? realm.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ClassLocation{uri=" + uri + ", realm='" + realm + "'}";
    }

}
