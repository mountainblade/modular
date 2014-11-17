package net.mountainblade.modular.impl;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Represents a cache for jar contents.
 *
 * @author spaceemotion
 * @version 1.0
 */
class JarCache {
    private static final Logger LOG = Logger.getLogger(JarCache.class.getName());
    private final Map<String, Entry> cache;


    public JarCache() {
        this.cache = new THashMap<>();
    }

    public Entry getEntry(URI uri) {
        File file = new File(uri);
        String hash = generateHash(file);

        if (hash == null) {
            LOG.warning("Could not calculate hash, JAR caching wont work: " + uri);
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


    public static final class Entry {
        /** A set of all found classes by their name */
        private final Set<String> classes = new THashSet<>();

        /** List of found modules (by their class names) in the jar */
        private final Map<String, Collection<String>> subclasses = new THashMap<>();


        public Set<String> getClasses() {
            return classes;
        }

        public Map<String, Collection<String>> getSubclasses() {
            return subclasses;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            return classes.equals(entry.classes) && subclasses.equals(entry.subclasses);
        }

        @Override
        public int hashCode() {
            return 31 * classes.hashCode() + subclasses.hashCode();
        }

        @Override
        public String toString() {
            return "Entry{" + "classes=" + classes + ", subclasses=" + subclasses + '}';
        }

    }

}
