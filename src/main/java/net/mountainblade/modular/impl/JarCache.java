package net.mountainblade.modular.impl;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import lombok.Data;
import lombok.extern.java.Log;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Represents a cache for jar contents.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
class JarCache {
    private final Map<String, Entry> cache;


    public JarCache() {
        this.cache = new THashMap<>();
    }


    public Entry getEntry(URI uri) {
        File file = new File(uri);
        String hash = generateHash(file);

        if (hash == null) {
            log.warning("Could not calculate hash, JAR caching wont work: " + uri);
            return new Entry();
        }

        Entry entry = cache.get(hash);

        if (entry == null) {
            entry = new Entry();
            cache.put(hash, entry);
        }

        return entry;
    }

    private String generateHash(File file) {
        return "weak:" + file.getName() + "@" + file.length();
    }

    @Data
    public static class Entry {
        /** A set of all found classes by their name */
        private final Set<String> classes = new THashSet<>();

        /** List of found modules (by their class names) in the jar */
        private final Map<String, Collection<String>> subclasses = new THashMap<>();
    }

}
