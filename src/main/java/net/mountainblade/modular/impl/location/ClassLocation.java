package net.mountainblade.modular.impl.location;

import gnu.trove.set.hash.TLinkedHashSet;
import lombok.Getter;
import lombok.extern.java.Log;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;

/**
 * Represents a location (realm) for classes.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
@Getter
public abstract class ClassLocation {
    protected static final Collection<String> nameBlacklist = new TLinkedHashSet<>();
    static {
        // Add maven stuff
        nameBlacklist.add("target.classes");
        nameBlacklist.add("target.test-classes");
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
            log.log(Level.WARNING, "Could not convert URI to URL", e);
        }
    }

    public abstract Collection<String> listClassNames();

    protected final boolean isBlacklisted(String className) {
        for (String blackListed : nameBlacklist) {
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
