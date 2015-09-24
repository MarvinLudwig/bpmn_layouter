/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors.boundary_events;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;

/**
 * Change the boundary event edge so
 * the boundary event successor is connected directly 
 * to the node the boundary event is attached to
 * 
 * This is done to have the boundary event's successor
 * placed into the correct layer during layering (phase 2)
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd>None.</dd>
 *   <dt>Postcondition:</dt><dd>Boundary events are prepared for phase 2.</dd>
 *   <dt>Slots:</dt><dd>Before phase 2.</dd>
 *   <dt>Same-slot dependencies:</dt><dd>Before BigNodesPreProcessor.</dd>
 * </dl>
 * 
 */

public final class BpmnBoundaryEventPreProcessor implements ILayoutProcessor {

	/**
	 * 
	 * <dl>
	 *   <dt>Precondition:</dt><dd>an acyclic graph, where every port has an assigned side.</dd>
	 *   <dt>Postcondition:</dt><dd>TBD</dd>
	 *   <dt>Slots:</dt><dd>Before phase 2.</dd>
	 *   <dt>Same-slot dependencies:</dt><dd>Before BigNodesPreProcessor.</dd>
	 * </dl>
	 * 
	 */
	
    public void process(final LGraph lGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("Boundary event processor", 1);
        
        Map<LEdge,LPort> edges = new HashMap<LEdge,LPort>();
        List<LNode> boundaryEvents = new LinkedList<LNode>();
        
        for (LNode node : lGraph.getLayerlessNodes()){
        		for (LEdge edge : node.getOutgoingEdges()){	
	        		if (edge.getProperty(BpmnProperties.BOUNDARY_EVENT_DUMMY_EDGE) == true){ 
	        			
	        			LPort boundaryEventPort = edge.getTarget();	        		
	        			LNode boundaryEvent = boundaryEventPort.getNode();
	        			boundaryEvents.add(boundaryEvent);
	        			
	        			LPort attachedTo = edge.getSource();
	        			LNode attachedToNode = edge.getSource().getNode();
	        			
	        			boundaryEvent.setProperty(BpmnProperties.ATTACHED_TO, attachedToNode);       			
	        				        			
	        			// set size to 0 in order to avoid wasting of 
	        			// space during layouting
	        			KVector originalSize = new KVector(boundaryEvent.getSize());
	        			boundaryEvent.setProperty(BpmnProperties.ORIGINAL_SIZE, originalSize);
	        			boundaryEvent.getSize().y = 0;
	        			boundaryEvent.getSize().x = 0;
	        			
	        			boundaryEventPort.getPosition().y = originalSize.y / 2;

	        			// Change port of the "attached to" and the boundary event
	        			// to port side EAST. 
	        			// They have to be east ports to work with big nodes, and
	        			// they have to have the same port side in order to have
	        			// no nodes between them.
	        			// (countInLayerEdgeCrossings in LayerSweepCrossingMinimizer).
	        			boundaryEventPort.setSide(PortSide.EAST);
	        			if (attachedTo == null || attachedTo.getSide() != boundaryEventPort.getSide()){
	        				for (LPort port : attachedToNode.getPorts()){
	        					if (port.getSide() == boundaryEventPort.getSide()) {
	        						edges.put(edge, port);
	        						attachedTo = port;
	        					}
	        				}
	        			}
	        				        			
	        			// put edge on the list of edges to be changed later
	        			for (LEdge outgoingEdge : boundaryEvent.getOutgoingEdges()){
	        				edges.put(outgoingEdge, attachedTo);
		        			// store boundary event as original predecessor of its successor
	        				outgoingEdge.getTarget().getNode().setProperty(BpmnProperties.ORIGINAL_SOURCE_NODE, boundaryEvent);
	        			}
	        		}
	        	}
        }
        
        lGraph.setProperty(BpmnProperties.BOUNDARY_EVENTS, boundaryEvents);
        
        // finally: change edges' source ports
        for(Entry<LEdge,LPort> entry : edges.entrySet()) {
        	LEdge edge = entry.getKey();
        	LPort port = entry.getValue();
        	edge.setSource(port);
        }
        
    }
 

}
