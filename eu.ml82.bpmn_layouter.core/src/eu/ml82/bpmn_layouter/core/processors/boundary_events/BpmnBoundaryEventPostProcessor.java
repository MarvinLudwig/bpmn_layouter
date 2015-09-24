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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.graph.LShape;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.Maps;

/**
 * Position boundary event and layout boundary event edges.
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd>Edges are routed. BigNodes have been merged.</dd>
 *   <dt>Postcondition:</dt><dd>Boundary event is on .</dd>
 *   <dt>Slots:</dt><dd>After phase 5.</dd>
 *   <dt>Same-slot dependencies:</dt><dd>After BigNodesPostProcessor.</dd>
 * </dl>
 * 
 */

public final class BpmnBoundaryEventPostProcessor implements ILayoutProcessor {

    /**
     * {@inheritDoc}
     */
    public void process(final LGraph lGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("Boundary event processor", 1); 
        
        Map<LNode, LNode> boundaryEventsAttachedTo = new HashMap<LNode, LNode>();
        Set<LNode> attachedTos = new HashSet<LNode>();
        
        List<LNode> boundaryEvents = lGraph.getProperty(BpmnProperties.BOUNDARY_EVENTS);
        
    	for (LNode node : boundaryEvents){
    		LNode attachedTo = node.getProperty(BpmnProperties.ATTACHED_TO);
    		if (attachedTo != null){
    			boundaryEventsAttachedTo.put(node, attachedTo);
    			attachedTos.add(attachedTo);        				
        	}
    	}
        		    
    	Map<LShape, Double> be = new HashMap<LShape, Double>();
        for (LNode attachedTo : attachedTos){    		
        	// get boundary events for the "attached to" activity
        	be.clear();
        	for (Entry<LNode, LNode> entry : boundaryEventsAttachedTo.entrySet()){
        		LNode boundaryEvent = entry.getKey();
        		if (entry.getValue().equals(attachedTo)){
        			Iterator<LEdge> edgeIterator = boundaryEvent.getOutgoingEdges().iterator();
        			// If the boundary event has an outgoing edge, we put the port to the list.
        			// If not, we put the boundary event 
    				if (edgeIterator.hasNext()){
    					LEdge edge = edgeIterator.next();
    					be.put(edge.getSource(), edge.getTarget().getNode().getPosition().y);
    				}
    				else be.put(boundaryEvent,0.0);
        		}
        	}
            // If there is more than one boundary event attached to an activity,
    		// we place the boundary events sorted by the y-pos of their successors
            // in order to avoid edge crossings
        	int offsetX = 0;
    		for (LShape shape : Maps.getKeysSortedByValue(be)){
    			LNode boundaryEvent;
    			if (shape instanceof LPort){
    				boundaryEvent = ((LPort) shape).getNode();    				
    			}
    			else boundaryEvent = (LNode) shape;
    			
    			KVector originalSize = boundaryEvent.getProperty(BpmnProperties.ORIGINAL_SIZE);
    			if (originalSize != null) {
    				boundaryEvent.getSize().x = originalSize.x;
    				boundaryEvent.getSize().y = originalSize.y;
    			}
    			// position boundary event on the node it is attached to
    			offsetX = offsetX + (int) (boundaryEvent.getSize().x + 15);
    			boundaryEvent.getPosition().x = attachedTo.getPosition().x
    											+ attachedTo.getSize().x 
    											- offsetX;
    			double offsetY = attachedTo.getSize().y;;
    			boolean bottomEvent = true;
    			if (boundaryEvent.getPosition().y < attachedTo.getPosition().y){
    				// there are some rare cases were the layout needs the boundary
    				// event to be on top of its parent node
    				offsetY = 0;
    				bottomEvent = false;
    			}
    			boundaryEvent.getPosition().y = attachedTo.getPosition().y 
    											+ offsetY
    											- boundaryEvent.getSize().y / 2; 
    			// if boundary event has outgoing edge -> position port
    			if (shape instanceof LPort){
    				shape.getPosition().x = boundaryEvent.getSize().x / 2;
    				if (bottomEvent) shape.getPosition().y = boundaryEvent.getSize().y;
    				else shape.getPosition().y = 0;
    			}
    			// edge routing
    			for (LEdge outEdge : boundaryEvent.getOutgoingEdges()){
    				LNode targetNode = outEdge.getTarget().getNode();
    				LPort target = outEdge.getTarget();
        			double bendpointX = boundaryEvent.getPosition().x + boundaryEvent.getSize().x / 2;
        			double bendpointY = targetNode.getPosition().y + target.getPosition().y; 
        			KVector newBendpoint = new KVector(bendpointX, bendpointY);
    				outEdge.getBendPoints().clear();
        			outEdge.getBendPoints().add(newBendpoint);
    			}
    		}
        		
        }
        
                
    }

}

