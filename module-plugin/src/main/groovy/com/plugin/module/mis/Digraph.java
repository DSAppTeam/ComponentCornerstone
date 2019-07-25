package com.plugin.module.mis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

/**
 * An example class for directed graphs.  The vertex type can be specified.
 * There are no edge costs/weights.
 * <p>
 * Written for CS211, Nov 2006.
 *
 * @author Paul Chew
 */
public class Digraph<V> {

    /**
     * The implementation here is basically an adjacency list, but instead
     * of an array of lists, a Map is used to map each vertex to its list of
     * adjacent vertices.
     */
    private Map<V, List<V>> neighbors = new HashMap<V, List<V>>();

    /**
     * String representation of dependencyGraph.
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        for (V v : neighbors.keySet()) s.append("\n    " + v + " -> " + neighbors.get(v));
        return s.toString();
    }

    /**
     * Add a vertex to the dependencyGraph.  Nothing happens if vertex is already in dependencyGraph.
     */
    public void add(V vertex) {
        if (neighbors.containsKey(vertex)) return;
        neighbors.put(vertex, new ArrayList<V>());
    }

    /**
     * True iff dependencyGraph contains vertex.
     */
    public boolean contains(V vertex) {
        return neighbors.containsKey(vertex);
    }

    /**
     * Add an edge to the dependencyGraph; if either vertex does not exist, it's added.
     * This implementation allows the creation of multi-edges and self-loops.
     */
    public void add(V from, V to) {
        this.add(from);
        this.add(to);
        neighbors.get(from).add(to);
    }

    /**
     * Remove an edge from the dependencyGraph.  Nothing happens if no such edge.
     *
     * @throws IllegalArgumentException if either vertex doesn't exist.
     */
    public void remove(V from, V to) {
        if (!(this.contains(from) && this.contains(to)))
            throw new IllegalArgumentException("Nonexistent vertex");
        neighbors.get(from).remove(to);
    }

    /**
     * Report (as a Map) the out-degree of each vertex.
     */
    public Map<V, Integer> outDegree() {
        Map<V, Integer> result = new HashMap<V, Integer>();
        for (V v : neighbors.keySet()) result.put(v, neighbors.get(v).size());
        return result;
    }

    /**
     * Report (as a Map) the in-degree of each vertex.
     */
    public Map<V, Integer> inDegree() {
        Map<V, Integer> result = new HashMap<V, Integer>();
        for (V v : neighbors.keySet()) result.put(v, 0);       // All in-degrees are 0
        for (V from : neighbors.keySet()) {
            for (V to : neighbors.get(from)) {
                result.put(to, result.get(to) + 1);           // Increment in-degree
            }
        }
        return result;
    }

    /**
     * Report (as a List) the topological sort of the vertices; null for no such sort.
     */
    public List<V> topSort() {
        Map<V, Integer> degree = inDegree();
        // Determine all vertices with zero in-degree
        Stack<V> zeroVerts = new Stack<V>();        // Stack as good as any here
        for (V v : degree.keySet()) {
            if (degree.get(v) == 0) zeroVerts.push(v);
        }
        // Determine the topological order
        List<V> result = new ArrayList<V>();
        while (!zeroVerts.isEmpty()) {
            V v = zeroVerts.pop();                  // Choose a vertex with zero in-degree
            result.add(v);                          // Vertex v is next in topol order
            // "Remove" vertex v by updating its neighbors
            for (V neighbor : neighbors.get(v)) {
                degree.put(neighbor, degree.get(neighbor) - 1);
                // Remember any vertices that now have zero in-degree
                if (degree.get(neighbor) == 0) zeroVerts.push(neighbor);
            }
        }
        // Check that we have used the entire dependencyGraph (if not, there was a cycle)
        if (result.size() != neighbors.size()) return null;
        return result;
    }

    /**
     * True iff dependencyGraph is a dag (directed acyclic dependencyGraph).
     */
    public boolean isDag() {
        return topSort() != null;
    }

    /**
     * Report (as a Map) the bfs distance to each vertex from the start vertex.
     * The distance is an Integer; the value null is used to represent infinity
     * (implying that the corresponding node cannot be reached).
     */
    public Map bfsDistance(V start) {
        Map<V, Integer> distance = new HashMap<V, Integer>();
        // Initially, all distance are infinity, except start node
        for (V v : neighbors.keySet()) distance.put(v, null);
        distance.put(start, 0);
        // Process nodes in queue order
        Queue<V> queue = new LinkedList<V>();
        queue.offer(start);                                    // Place start node in queue
        while (!queue.isEmpty()) {
            V v = queue.remove();
            int vDist = distance.get(v);
            // Update neighbors
            for (V neighbor : neighbors.get(v)) {
                if (distance.get(neighbor) != null) continue;  // Ignore if already done
                distance.put(neighbor, vDist + 1);
                queue.offer(neighbor);
            }
        }
        return distance;
    }

//    /**
//     * Main program (for testing).
//     */
//    public static void main (String[] args) {
//        // Create a Graph with Integer nodes
//        Digraph<Integer> graph = new Digraph<Integer>();
//        graph.add(0, 1); graph.add(0, 2); graph.add(0, 3);
//        graph.add(1, 2); graph.add(1, 3); graph.add(2, 3);
//        graph.add(2, 4); graph.add(4, 5); graph.add(5, 6);    // Tetrahedron with tail
//        System.out.println("The current dependencyGraph: " + graph);
//        System.out.println("In-degrees: " + graph.inDegree());
//        System.out.println("Out-degrees: " + graph.outDegree());
//        System.out.println("A topological sort of the vertices: " + graph.topSort());
//        System.out.println("The dependencyGraph " + (graph.isDag()?"is":"is not") + " a dag");
//        System.out.println("BFS distances starting from " + 0 + ": " + graph.bfsDistance(0));
//        System.out.println("BFS distances starting from " + 1 + ": " + graph.bfsDistance(1));
//        System.out.println("BFS distances starting from " + 2 + ": " + graph.bfsDistance(2));
//        graph.add(4, 1);                                     // Create a cycle
//        System.out.println("Cycle created");
//        System.out.println("The current dependencyGraph: " + graph);
//        System.out.println("In-degrees: " + graph.inDegree());
//        System.out.println("Out-degrees: " + graph.outDegree());
//        System.out.println("A topological sort of the vertices: " + graph.topSort());
//        System.out.println("The dependencyGraph " + (graph.isDag()?"is":"is not") + " a dag");
//        System.out.println("BFS distances starting from " + 2 + ": " + graph.bfsDistance(2));
//    }
}