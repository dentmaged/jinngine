package jinngine.util;

import java.util.Iterator;

/**
 * An undirected graph that keeps track of connected components (groups). Each time an edge is added or removed 
 * from the graph, data structures are maintained, reflecting connected components in the graph. This means, 
 * that adding edges are roughly an O(k) operation, while removing an edge could i result in a total traversal of the graph, 
 * visiting all present edges, worst case O((n-1)^2) where n is the number of nodes. Usually this will be much 
 * cheaper, given that the graph has a low density, and is fragmented into separated components. 
 *
 * @param <T> Type that stores in nodes
 * @param <U> Type that stores in edges
 */
public interface ComponentGraph<T,U> {

	/**
	 * Dummy interface for identifying components in the graph
	 */
	public interface Component {}
	
	/**
	 * Node classifier for the ContactGraph
	 *
	 * @param <T> Type that stores in nodes
	 */
	public interface NodeClassifier<T> {
		/**
		 * @param node Node to classify
		 * @return true if the node is to be considered as a delimiting node, such that two
		 * components in some graph, would not be merged if connected through such a node. Returns false otherwise.
		 */
		public boolean isDelimitor(final T node);
	}
	
	/**
	 * Add an edge to the graph, and implicitly add included end-nodes if not already present in the graph.
	 * This is roughly an O(k) and sometimes O(nodes) operation, depending on whether components are to be merged or not.
	 * @param pair A pair of nodes, where an edge is to be added between them.
	 * @param edgeelement An element of type U to store in the new edge
	 */
	public void addEdge( Pair<T> pair, U edgeelement);

	/**
	 * Remove an edge. If the removal results in one or more isolated nodes, these will be removed 
	 * from the graph implicitly. 
 	 * 
	 * For non-dense and relatively fragmented graphs, this operation will be cheap. Otherwise, for
	 * dense and strongly connected graphs, the operation could include a full traversal of the graph visiting all
	 * present edges, resulting in an O((n-1)^2) operation, where n is the number of nodes in the graph.
	 * @param pair edge to be removed
	 * @return true if the edge was actually removed, false if the edge did not exists before call.
	 */
	public boolean removeEdge( Pair<T> pair);
	
	/**
	 * Get the edge element of type U that is stored in the edge defined by
	 * a pair of node types T. If no such edge exist, the return value is null.
	 * @param pair A pair of T type objects defining an edge in the graph
	 * @return The U type object stored in the edge. Return value is null if no such 
	 * edge is present in the graph
	 */
	public U getEdge( Pair<T> pair);

	/**
	 * Return an iterator yielding the edges in the specified component. 
	 * @param c Component to iterate
	 * @return Iterator giving the edge elements in the component
	 */
	public Iterator<U> getEdgesInComponent(Component c);

	/**
	 * Returns an iterator yielding the nodes present in the given component
	 * @param c Any component of this graph
	 * @return An iterator yielding the nodes present in the component c
	 */
	public Iterator<T> getNodesInComponent(Component c);
	
	/**
	 * Return an iterator that yields the components in the graph
	 * @return 
	 */
	public Iterator<Component> getComponents();
	
	/**
	 * Return the number of components in this graph
	 */
	public int getNumberOfComponents();

}
