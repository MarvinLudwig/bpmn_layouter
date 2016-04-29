/*
 * Camunda BPMN model wrapper for the BPMN auto-layouter
 *
 * Copyright 2015 by Christophe Bertoncini
 *
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.camunda_model;

import java.util.TreeMap;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.math.KVectorChain;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.graph.LShape;

/**
 * Keep track of generated nodes for a Process
 */
public class ProcessGraphGeneration {

    //
    // KIELER Objects
    //

    // Pool graph parent
    LGraph poolGraph;

    // Lanes
    TreeMap<String, LNode> laneNodes;

    // Elements
    TreeMap<String, LNode> elementNodes;

    // Sequences
    TreeMap<String, LEdge> sequenceEdges;

    /**
     * @param poolGraph
     *            Parent Pool Graph node
     */
    public ProcessGraphGeneration(LGraph poolGraph) {
        this.poolGraph = poolGraph;
        this.laneNodes = new TreeMap<String, LNode>();
        this.elementNodes = new TreeMap<String, LNode>();
        this.sequenceEdges = new TreeMap<String, LEdge>();
    }

    /**
     * Add a laneNode to poolGraph
     * 
     * @param laneId
     * @param laneName
     */
    public void addLane(final String laneId, final String laneName) {

        LNode laneNode = BpmnGraphGeneration.createNode(poolGraph, laneName, 1, 1);
        BpmnGraphGeneration.setNodeProperties(laneNode, AutoLayoutNodeType.LANE);
        laneNode.getInsets().set(10, 10, 5, 5);

        // LGraph laneGraph = BpmnGraphGeneration.addNestedGraph(laneNode);
        BpmnGraphGeneration.addNestedGraph(laneNode);

        laneNodes.put(laneId, laneNode);
    }

    private LGraph getNestedGraph(final String laneId) throws Exception {

        // Get parent graph for node
        LGraph nestedGraph = null;

        if (laneId == null) {
            // No lane => use pool
            nestedGraph = this.poolGraph;
        } else {
            LNode laneNode = this.laneNodes.get(laneId);
            if (laneNode == null) {
                throw new Exception("No lane with id " + laneId + " !");
            }

            nestedGraph = BpmnGraphGeneration.getNestedGraph(laneNode);
        }

        return nestedGraph;
    }

    /**
     * Add node to lane
     *
     * @param elementId
     * @param name
     * @return
     */
    public LNode addNode(final String elementId, final String name, final String laneId) throws Exception {

        // Get parent graph for node
        LGraph nestedGraph = getNestedGraph(laneId);
        if (nestedGraph == null) {
            return null;
        }

        LNode elementNode = BpmnGraphGeneration.createNode(nestedGraph, name, 0, 0);
        // elementNode.getInsets().set(5, 5, 5, 5);

        elementNodes.put(elementId, elementNode);

        return elementNode;
    }

    public LEdge createEdge(final String sourceId, final String targetId, final String sequenceId, final String label) {

        LEdge edge;

        LNode sourceNode = this.elementNodes.get(sourceId);
        LNode targetNode = this.elementNodes.get(targetId);

        if ((sourceNode != null) && (targetNode != null)) {
            edge = BpmnGraphGeneration.createEdge(sourceNode, targetNode, label);
            sequenceEdges.put(sequenceId, edge);
        } else {
            edge = null;
        }

        return edge;
    }

    /**
     * Retrieve box for process
     * 
     * @return
     */
    public LShape getProcessShape() {
        KVector offset = this.poolGraph.getOffset();

        return new ElementBox(offset, this.poolGraph.getSize());
    }

    /**
     * Retrieve box for lane
     * 
     * @return
     */
    public LShape getLaneShape(final String laneId) {
        LShape laneNode = laneNodes.get(laneId);

        KVector offset = laneNode.getProperty(BpmnAutoLayoutProperties.OFFSET);
        KVector position = KVector.sum(laneNode.getPosition(), offset);

        return new ElementBox(position, laneNode.getSize());
    }

    /**
     * Retrieve box for element
     * 
     * @return
     */
    public LShape getElementShape(final String elementId) {
        LShape elementNode = elementNodes.get(elementId);

        KVector offset = elementNode.getProperty(BpmnAutoLayoutProperties.OFFSET);
        KVector position = KVector.sum(elementNode.getPosition(), offset);

        return new ElementBox(position, elementNode.getSize());
    }

    /**
     * Retrieve points for edge
     * 
     * @return
     */
    public KVectorChain getSequenceEdge(final String sequenceId) {

        KVectorChain vectorChain = new KVectorChain();

        LEdge edge = sequenceEdges.get(sequenceId);
        if (edge != null) {

            LPort sourcePort = edge.getSource();
            KVector offset = sourcePort.getNode().getProperty(BpmnAutoLayoutProperties.OFFSET);

            KVector start = sourcePort.getAbsoluteAnchor();
            vectorChain.add(start.clone().add(offset));

            for (KVector benPoint : edge.getBendPoints())
                vectorChain.add(benPoint.clone().add(offset));

            LPort targetPort = edge.getTarget();
            KVector end = targetPort.getAbsoluteAnchor();
            vectorChain.add(end.clone().add(offset));
        }

        return vectorChain;
    }

}
