package net.mountainblade.modular.impl;

import gnu.trove.list.TLinkableAdapter;
import gnu.trove.list.linked.TLinkedList;
import gnu.trove.set.hash.THashSet;

import java.util.Iterator;

/**
 * Represents a topological sorted list.
 * <p/>
 * The topological sort algorithm sorts a list of nodes in an order in where all the dependencies are fulfilled.
 * <p/>
 * <a href="http://en.wikipedia.org/wiki/Topological_sorting">Read more at Wikipedia!</a>
 *
 * @author spaceemotion
 * @author p000ison
 */
public class TopologicalSortedList<E> extends TLinkedList<TopologicalSortedList.Node<E>> {

    /**
     * Adds a node to the list.
     *
     * @param element The element to add
     * @return The generated node
     */
    public Node<E> addNode(E element) {
        Node<E> node = new Node<>(element);
        super.add(node);

        return node;
    }

    /**
     * Sorts the list in topological order.
     *
     * @throws CycleException Gets thrown when a dependency cycle has been detected
     */
    public void sort() throws CycleException {
        // Define the "noEdges" set, a set of all nodes with no incoming edges
        THashSet<Node<E>> noEdges = new THashSet<>();

        Node[] allNodes = super.toArray(new Node[size()]);

        for (Node<E> n : this) {
            if (n.inEdges.size() == 0) {
                noEdges.add(n);
            }
        }

        // Clear ourselves
        clear();

        // While we have "no edges" nodes, iterate
        while (!noEdges.isEmpty()) {
            // Get next node and remove it from the set
            Node<E> n = noEdges.iterator().next();
            noEdges.remove(n);

            // Insert the node into our list
            this.add(n);

            // For each node m with an edge e from n to m do
            for (Iterator<Edge<E>> it = n.outerEdges.iterator(); it.hasNext(); ) {
                // Remove the edge "e" from the graph
                Edge<E> e = it.next();
                Node<E> m = e.to;

                // Remove edge from n
                it.remove();

                // Remove edge from m
                m.inEdges.remove(e);

                // If m has no other incoming edges then insert m into the "noEdges" set
                if (m.inEdges.isEmpty()) {
                    noEdges.add(m);
                }
            }
        }

        // Check to see if all edges are removed
        for (Node n : allNodes) {
            if (!n.inEdges.isEmpty() || !n.outerEdges.isEmpty()) {
                throw new CycleException("Cycle found in list: " + this);
            }
        }
    }

    /**
     * Represents a node in a topological sorted list.
     *
     * @param <E> The node type
     */
    public static class Node<E> extends TLinkableAdapter<Node<E>> {
        private final THashSet<Edge<E>> inEdges;
        private final THashSet<Edge<E>> outerEdges;
        private final E value;


        public Node(E value) {
            this.value = value;
            inEdges = new THashSet<>();
            outerEdges = new THashSet<>();
        }

        /**
         * Marks this node as a requirement for the given node.
         * This will place this node <b>before</b> the node.
         * (Formerly known as <code>addEdge()</code>).
         *
         * @param node The node this node is required for.
         * @return Ourselves, allowing method chaining
         */
        public Node<E> isRequiredBefore(Node<E> node) {
            Edge<E> e = new Edge<>(this, node);

            outerEdges.add(e);
            node.inEdges.add(e);

            return this;
        }

        /**
         * Marks this node as a requirement for the given element.
         * This will place this node <b>before</b> the element.
         * (Formerly known as <code>addEdge()</code>).
         *
         * @param element The element this node is required for
         * @return Ourselves, allowing method chaining
         */
        public Node<E> isRequiredBefore(E element) {
            return isRequiredBefore(new Node<>(element));
        }

        public E getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }

    }


    protected static class Edge<E> {
        private final Node<E> from;
        private final Node<E> to;


        public Edge(Node<E> from, Node<E> to) {
            this.from = from;
            this.to = to;
        }
    }

    public static class CycleException extends Exception {

        public CycleException(String message) {
            super(message);
        }

    }

}
