package net.mountainblade.modular.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import gnu.trove.map.hash.THashMap;
import net.mountainblade.modular.Module;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Represents a hierarchic module registry.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class HierarchicModuleRegistry extends ModuleRegistry {
    private final Map<Class<? extends Module>, Entry> registry;
    private final Collection<Module> modules;


    HierarchicModuleRegistry(ModuleRegistry parent) {
        super(parent.getRegistry(), parent.getModuleCollection());

        registry = new CombinedTHashMap<>(parent.getRegistry());
        modules = new CombinedCollection<>(parent.getModuleCollection());
    }

    @Override
    public Map<Class<? extends Module>, Entry> getRegistry() {
        return registry;
    }

    @Override
    protected Collection<Module> getModuleCollection() {
        return modules;
    }


    @SuppressWarnings("NullableProblems")
    private class CombinedCollection<E> extends LinkedList<E> {
        private final Collection<E> parent;


        public CombinedCollection(Collection<E> parent) {
            this.parent = parent;
        }

        @Override
        public boolean contains(final Object o) {
            return parent.contains(o) || super.contains(o);
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            return parent.containsAll(c) || super.containsAll(c);
        }

        @Override
        public Iterator<E> iterator() {
            return Iterables.concat(parent, this).iterator();
        }

        @Override
        public int size() {
            return parent.size() + super.size();
        }

        @Override
        public Object[] toArray() {
            Object[] result = new Object[size()];
            Iterator<E> iterator = iterator();

            int i = 0;
            while (iterator.hasNext()) {
                result[i++] = iterator.next();
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            int size = size();
            if (a.length < size) {
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            }

            Iterator<E> iterator = iterator();
            Object[] result = a;

            int i = 0;
            while (iterator.hasNext()) {
                result[i++] = iterator.next();
            }

            if (a.length > size) {
                a[size] = null;
            }

            return a;
        }

    }

    private class CombinedTHashMap<K, V> extends THashMap<K, V> {
        private final Map<K, V> parent;


        public CombinedTHashMap(Map<K, V> parent) {
            super();
            this.parent = parent;
        }

        public CombinedTHashMap() {
            super();
            parent = Collections.emptyMap();
        }

        @Override
        public int size() {
            return super.size() + parent.size();
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty() && parent.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return super.containsKey(key) || parent.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return super.containsValue(value) || parent.containsValue(value);
        }

        @Override
        public V get(Object key) {
            V value = super.get(key);
            return value != null ? value : parent.get(key);
        }

        @Override
        public Set<K> keySet() {
            return Sets.union(parent.keySet(), super.keySet());
        }

        @Override
        public Collection<V> values() {
            LinkedList<V> values = new LinkedList<>(parent.values());
            values.addAll(super.values());

            return values;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return Sets.union(parent.entrySet(), super.entrySet());
        }

    }

}
