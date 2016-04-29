/*
 * Camunda BPMN model wrapper for the BPMN auto-layouter
 *
 * Copyright 2015 by Christophe Bertoncini
 *
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.camunda_model;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Logical graph
 *
 * @param <TKey>
 *            Key identifying node
 * @param <TNode>
 *            Node
 */
public class LogicalGraph<TKey extends Comparable<TKey>, TNode> {

    // Nodes for graph
    protected TreeMap<TKey, TNode> nodes;

    // Predecessor of node
    protected Multimap<TKey, TKey> predecessors;

    // Successors of node
    protected Multimap<TKey, TKey> successors;

    public LogicalGraph() {
        nodes = new TreeMap<TKey, TNode>();
        predecessors = TreeMultimap.create();
        successors = TreeMultimap.create();
    }

    /**
     * Clear all info
     */
    public void clear() {
        this.successors.clear();
        this.predecessors.clear();
        this.nodes.clear();
    }

    public void addNode(TKey nodeId, TNode node) {
        nodes.put(nodeId, node);
    }

    public Set<TKey> getKeys() {
        return nodes.keySet();
    }

    public Collection<TNode> getNodes() {
        return nodes.values();
    }

    public TNode getNode(TKey nodeId) {
        return nodes.get(nodeId);
    }

    public void addLink(TKey sourceId, TKey targetId) {
        predecessors.put(targetId, sourceId);
        successors.put(sourceId, targetId);
    }

    public int getPredecessorsSize(TKey targetId) {
        Collection<TKey> previousNodes = predecessors.get(targetId);
        return (previousNodes != null) ? previousNodes.size() : 0;
    }

    public int getSuccessorsSize(TKey sourceId) {
        Collection<TKey> nextNodes = successors.get(sourceId);
        return (nextNodes != null) ? nextNodes.size() : 0;
    }

}
