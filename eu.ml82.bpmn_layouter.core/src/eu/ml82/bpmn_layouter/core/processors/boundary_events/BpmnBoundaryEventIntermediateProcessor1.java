/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors.boundary_events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.graph.Layer;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.Utils;

/**
 * Set constraints so that the boundary event node
 * is placed below the node it is attached to 
 * during crossing minimization.
 * 
 * Re-attach edge to boundary event
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd>Layers are assigned.</dd>
 *   <dt>Postcondition:</dt><dd>Constraints are set.</dd>
 *   <dt>Slots:</dt><dd>Between phase 2 and 3.</dd>
 *   <dt>Same-slot dependencies:</dt><dd>None.</dd>
 * </dl>
 * 
 */

public final class BpmnBoundaryEventIntermediateProcessor1 implements ILayoutProcessor {

    /**
     * {@inheritDoc}
     */
    public void process(final LGraph lGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("Boundary event processor", 1);
        
        Map<LNode,Layer> boundaryEventNodes = new HashMap<LNode,Layer>();
        Map<LEdge,LPort> edges = new HashMap<LEdge,LPort>();  
        
        List<LNode> boundaryEvents = lGraph.getProperty(BpmnProperties.BOUNDARY_EVENTS);
        if (boundaryEvents == null) return;
        
    	for (LNode boundaryEventNode : boundaryEvents){
    		for (LEdge edge : boundaryEventNode.getIncomingEdges()){	
        		if (edge.getProperty(BpmnProperties.BOUNDARY_EVENT_DUMMY_EDGE) == true){
        				
        			// Move the boundary event to the layer
        			// of the node it is attached to
        			LNode node = edge.getSource().getNode();
        			boundaryEventNodes.put(boundaryEventNode, node.getLayer());
        			
        			// Set constraints
        			List<LNode> nodeList = node.getProperty(InternalProperties.IN_LAYER_SUCCESSOR_CONSTRAINTS);
        			if (nodeList == null ) nodeList = new ArrayList<LNode>();
        			nodeList.add(boundaryEventNode);
        			node.setProperty(InternalProperties.IN_LAYER_SUCCESSOR_CONSTRAINTS,nodeList);
        			
        			// Restore original edge from boundary event to its successor    			
        			LNode attachedToNode = edge.getSource().getNode();
        			for (LEdge outEdge : attachedToNode.getOutgoingEdges()){
        				LNode target = outEdge.getTarget().getNode();
        				LNode boundaryEvent = target.getProperty(BpmnProperties.ORIGINAL_SOURCE_NODE);
        				if (boundaryEvent != null){
        					edges.put(outEdge, Utils.getPort(boundaryEvent,PortSide.WEST));
        				}
        			}
        		}
        	}
        }
        
        for(Entry<LEdge,LPort> entry : edges.entrySet()) {
        	LEdge edge = entry.getKey();
        	LPort port = entry.getValue();
        	edge.setSource(port);
        }
        
        for(Entry<LNode, Layer> entry : boundaryEventNodes.entrySet()) {
        	LNode boundaryEventNode = entry.getKey();
        	Layer layer = entry.getValue();
        	boundaryEventNode.setLayer(layer);
        }
        
    }
 

}
