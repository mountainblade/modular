package net.mountainblade.modular.impl.resolver;

import net.mountainblade.modular.impl.location.ClassLocation;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Represents a resolver for classes inside specific locations.
 *
 * @author spaceemotion
 * @version 1.0
 */
public abstract class ClassResolver {
    private final Collection<URI> uriBlacklist;
    private final Collection<String> nameBlacklist;


    protected ClassResolver() {
        uriBlacklist = new LinkedList<>();
        nameBlacklist = new LinkedList<>();
    }

    public final Collection<URI> getUriBlacklist() {
        return uriBlacklist;
    }

    public final Collection<String> getNameBlacklist() {
        return nameBlacklist;
    }

    public abstract boolean handles(URI uri);

    public abstract Collection<ClassLocation> resolve(URI uri);

    protected final boolean isBlacklisted(URI uri) {
        // Use our name blacklist first
        String urlString = uri.toString();
        for (String blackListed : nameBlacklist) {
            if (urlString.contains(blackListed)) {
                return true;
            }
        }

        // Then use the URI blacklist
        return this.getUriBlacklist().contains(uri);
    }

}
