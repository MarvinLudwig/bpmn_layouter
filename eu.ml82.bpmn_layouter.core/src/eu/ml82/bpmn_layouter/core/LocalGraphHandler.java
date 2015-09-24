/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core;

/**
 * Removes edges that cross pools or lanes
 * and set nodes that have incoming or outgoing cross-container edges 
 * as nodes that must be at top or bottom of the container
 * 
 */

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.Layer;
import de.cau.cs.kieler.klay.layered.properties.InLayerConstraint;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;


public final class LocalGraphHandler {
	
	List<LEdge> crossContainerEdges = new LinkedList<LEdge>();
	
    public List<LEdge> prepareGraph(final LGraph graph, final List<LNode> localNodes) {
    	
    	// prepare local graph
    	for (Layer layer : graph.getLayers()){
    		List<LNode> bottomNodes = new LinkedList<LNode>();
    		List<LNode> topNodes = new LinkedList<LNode>();
    		List<LNode> middleNodes = new LinkedList<LNode>();
    		Iterator<LNode> layerNodesIterator = layer.getNodes().iterator();
    		while (layerNodesIterator.hasNext()){
    			LNode node = layerNodesIterator.next();
    			if (!localNodes.contains(node)) layerNodesIterator.remove();
    			else { 
    				node.setGraph(graph);
    				
    				// remove non-local edges
    				checkAndRemoveEdges(node.getOutgoingEdges().iterator(),localNodes,"outgoing");
    				checkAndRemoveEdges(node.getIncomingEdges().iterator(),localNodes,"incoming");
    				
    				InLayerConstraint inLayerConstraint = node.getProperty(InternalProperties.IN_LAYER_CONSTRAINT);
    				if (inLayerConstraint != null){ 
    					if (inLayerConstraint == InLayerConstraint.TOP) topNodes.add(node);
    					else if (inLayerConstraint == InLayerConstraint.BOTTOM) bottomNodes.add(node);
        				else middleNodes.add(node);
    				}
    				else middleNodes.add(node);
    			}
    			
    		}
    		
    		if (bottomNodes.size() > 0)
	        for (LNode middleNode : middleNodes){
	        	middleNode.setProperty(InternalProperties.IN_LAYER_SUCCESSOR_CONSTRAINTS,new LinkedList<LNode>(bottomNodes));
    		}

    		bottomNodes.addAll(middleNodes);
	        for (LNode topNode : topNodes){
	        	topNode.setProperty(InternalProperties.IN_LAYER_SUCCESSOR_CONSTRAINTS,new LinkedList<LNode>(bottomNodes));
    		}
	        
    	}
    	
    	return crossContainerEdges;
    }

	private LNode getNode(LEdge edge, String type){
    	if (type.equals("incoming")) return edge.getSource().getNode();
    	else return edge.getTarget().getNode();
    }
    
    private void removeEdge(Iterator<LEdge> edgeIterator, LEdge edge){
    	while (edgeIterator.hasNext()){
    		if (edgeIterator.next() == edge) edgeIterator.remove();
    	}
    }
    
    // remove non-local edges
    private void checkAndRemoveEdges(Iterator<LEdge> edgeIterator, final List<LNode> localNodes, String type){
 		while (edgeIterator.hasNext()){
 			LEdge edge = edgeIterator.next();
     		if (!localNodes.contains(getNode(edge,type))) {
     			if (edge.getProperty(BpmnProperties.ELEMENT_TYPE) != BpmnElementType.MESSAGE_FLOW
     				&& !edge.getProperty(BpmnProperties.ARTIFACT_DUMMY_EDGE))
     				crossContainerEdges.add(edge);
 				edgeIterator.remove();				
 				
 				// Set constraints so that nodes with cross-container
 				// edges are put at the top or bottom of the container
 				// during crossing minimization.
 				LNode localNode; 				
 				LNode foreignNode;
 				LNode targetNode = edge.getTarget().getNode();
 				LNode sourceNode = edge.getSource().getNode();
				if (type.equals("incoming")) {
 					localNode = targetNode;
 					foreignNode = sourceNode; 
 					removeEdge(foreignNode.getOutgoingEdges().iterator(),edge);
 				}
 				else {
 					localNode = sourceNode;
 					foreignNode = targetNode;
 					removeEdge(foreignNode.getIncomingEdges().iterator(),edge);
 				}			
        		
				localNode.setProperty(InternalProperties.IN_LAYER_CONSTRAINT, InLayerConstraint.BOTTOM);
				foreignNode.setProperty(InternalProperties.IN_LAYER_CONSTRAINT, InLayerConstraint.TOP);
 				
 			}
 				
 		}
    }
 

}
