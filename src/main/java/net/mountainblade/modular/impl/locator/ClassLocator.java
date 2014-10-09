package net.mountainblade.modular.impl.locator;

import lombok.Getter;
import net.mountainblade.modular.impl.ClasspathLocation;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Represents the ClassLocator.
 *
 * @author spaceemotion
 * @version 1.0
 */
public abstract class ClassLocator {
    @Getter
    private final Collection<URI> uriBlacklist;

    @Getter
    private final Collection<String> nameBlacklist;


    public ClassLocator() {
        uriBlacklist = new LinkedList<>();
        nameBlacklist = new LinkedList<>();
    }


    public abstract boolean handles(URI uri);

    public abstract Collection<ClasspathLocation> discover(URI uri);


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
