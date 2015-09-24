/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core;

import java.util.Set;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.math.KVectorChain;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.Utils;

/**
 * Adjust graph in order to have relative node positions.
 * (Relative to container)
 */

public class RelativePositions {

	/**
     * Adjust graph in order to have relative node positions.
     * (Relative to container)
	 * @param subProcesses 
	 * @param subProcesses 
     */
    public static void make(LGraph graph, Set<LNode> subProcesses){
    	Set<LNode> containers = graph.getProperty(BpmnProperties.CONTAINERS);
    	if (containers != null){
	        adjustEdges(graph);
	        adjustNodes(graph, new KVector(0,0));
    	}
    	else if (subProcesses != null){
      		for (LNode subProcess : subProcesses){
    			graph = subProcess.getProperty(InternalProperties.NESTED_LGRAPH);
    			if (graph != null){
    		       	adjustSubProcessChildNodes(subProcess);	
    			}
    		}
    	}
    }
    
    
	private static void adjustNodes(LGraph graph, KVector offset){
		for (LNode node : graph.getLayerlessNodes()){
			graph = node.getProperty(InternalProperties.NESTED_LGRAPH);
			if (graph != null) {
				KVector newOffset = node.getPosition();
				adjustNodes(graph, newOffset);
			}
			node.getPosition().sub(offset);
		}
	}
    
    /**
     * Adjust edges (port and bendpoint positions)
     */
    private static void adjustEdges(LGraph graph){
    	for (LNode node : graph.getLayerlessNodes()){
			graph = node.getProperty(InternalProperties.NESTED_LGRAPH);
			if (graph != null) {
				adjustEdges(graph);
			}
			for (LEdge edge : node.getOutgoingEdges()){
				adjustEdge(edge);
			}
		}    		
    }
    
    private static void adjustEdge(LEdge edge){
    	LPort source = edge.getSource();
		LPort target = edge.getTarget();
		LNode targetContainer = target.getNode().getProperty(InternalProperties.PARENT_LNODE);
		LNode sourceContainer = source.getNode().getProperty(InternalProperties.PARENT_LNODE);
		KVector adjustPos;
		KVector sourceContainerPos = sourceContainer.getPosition();
		if (sourceContainer	!= targetContainer){
			if (Utils.countEdges(target.getConnectedEdges().iterator())> 1){
				target = clonePort(target);
				edge.setTarget(target);
			}
			KVector targetContainerPos = targetContainer.getPosition();
			adjustPos = new KVector(sourceContainerPos).sub(targetContainerPos);
			target.getPosition().sub(adjustPos);
    	}
		
		// Adjust bendpoints
		adjustPos = sourceContainerPos;
		for (KVector bendpoint : edge.getBendPoints()){
			bendpoint.sub(adjustPos);
		}
		
		// Adjust edge labels
		for (LLabel edgeLabel : edge.getLabels()){
			LNode parent = edge.getSource().getNode().getProperty(InternalProperties.PARENT_LNODE);
			if (parent != null){
				edgeLabel.getPosition().sub(parent.getPosition());
			}
		}
    }
    
    private static void adjustSubProcessChildNodes(LNode subProcess) {
    	LGraph graph = subProcess.getProperty(InternalProperties.NESTED_LGRAPH);
    	if (graph == null) return;
    		
    	KVector subProcessPos = subProcess.getPosition();
		for (LNode node : graph.getLayerlessNodes()){
			KVector nodePos = node.getPosition();
			nodePos.sub(subProcessPos);
			for (LEdge edge : node.getOutgoingEdges()){
				// Adjust bendpoints
				KVectorChain bendpoints = edge.getBendPoints();
				for (KVector bendpoint : bendpoints){
					bendpoint.sub(subProcessPos);
				}
			}
		}
	}
    
    /** If a port has more than one connected edges and we move the port,
     * we need to clone it before moving.
     * @return 
     */
    private static LPort clonePort(LPort port){
    	LPort newPort = new LPort();
    	newPort.setNode(port.getNode());
    	newPort.setSide(port.getSide());
    	newPort.getPosition().add(port.getPosition());
    	newPort.getSize().add(port.getSize());
    	return newPort;
    }
}
