package net.mountainblade.modular.impl;

import com.google.common.collect.Sets;
import gnu.trove.map.hash.THashMap;
import net.mountainblade.modular.Module;

import java.lang.reflect.Array;
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
class HierarchicModuleRegistry extends ModuleRegistry {

    HierarchicModuleRegistry(ModuleRegistry parent) {
        super(new CombinedTHashMap<>(parent.getRegistry()), new CombinedCollection<Module>(parent));
    }

    Iterator<Module> getChildModules() {
        return ((CombinedCollection<Module>) getModuleCollection()).childIterator();
    }


    @SuppressWarnings("NullableProblems")
    private static class CombinedCollection<E> extends LinkedList<E> {
        //
        // This class has some trouble with exceptions, since we try to combine two collections while keeping
        // both intact.
        //
        // If something comes up and modules are acting weird this might be the place to look at.
        // Just saying.
        //

        /** The parent collection */
        private final Set<E> parent;


        public CombinedCollection(ModuleRegistry parent) {
            this.parent = ((Set<E>) parent.getModuleCollection());
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
            final Iterator<E> parentIterator = parent.iterator();
            final Iterator<E> childIterator = super.iterator();

            return new Iterator<E>() {
                @Override
                public boolean hasNext() {
                    return parentIterator.hasNext() || childIterator.hasNext();
                }

                @Override
                public E next() {
                    return getIterator().next();
                }

                @Override
                public void remove() {
                    getIterator().remove();
                }

                private Iterator<E> getIterator() {
                    return !childIterator.hasNext() ? parentIterator : childIterator;
                }
            };
        }

        public Iterator<E> childIterator() {
            return super.iterator();
        }

        @Override
        public int size() {
            return parent.size() + super.size();
        }

        @Override
        public boolean isEmpty() {
            return parent.isEmpty() && super.isEmpty();
        }

        @Override
        public E get(int index) {
            return index < parent.size() ? null : super.get(index);
        }

        @Override
        public Object[] toArray() {
            Object[] result = new Object[size()];
            int i = 0;

            for (E e : this) {
                result[i++] = e;
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            int size = size();
            if (a.length < size) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }

            Object[] result = a;
            int i = 0;

            for (E e : this) {
                result[i++] = e;
            }

            if (a.length > size) {
                a[size] = null;
            }

            return a;
        }

    }

    @SuppressWarnings("NullableProblems")
    private static class CombinedTHashMap<K, V> extends THashMap<K, V> {
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
            V value = parent.get(key);
            return value != null ? value : super.get(key);
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
