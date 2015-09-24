/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors.boundary_events;

import java.util.Iterator;
import java.util.List;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;

/**
 * Remove boundary event dummy edges
 * (Connections between boundary events and their "attached to" nodes)
 * in order to avoid unneeded space during edge routing
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd></dd>
 *   <dt>Postcondition:</dt><dd></dd>
 *   <dt>Slots:</dt><dd>Before phase 5.</dd>
 *   <dt>Same-slot dependencies:</dt><dd>Before After BpmnPortPostProcessor</dd>
 * </dl>
 * 
 */

public final class BpmnBoundaryEventIntermediateProcessor2 implements ILayoutProcessor {

    /**
     * {@inheritDoc}
     */
    public void process(final LGraph lGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("Boundary event processor", 1);
     
        List<LNode> boundaryEvents = lGraph.getProperty(BpmnProperties.BOUNDARY_EVENTS);
        for (LNode boundaryEventNode : boundaryEvents){
        	LPort node = boundaryEventNode.getIncomingEdges().iterator().next().getSource();
        	Iterator<LEdge> edgeIterator = node.getOutgoingEdges().iterator();
    		while (edgeIterator.hasNext()){	
    			LEdge edge = edgeIterator.next();
        		if (edge.getProperty(BpmnProperties.BOUNDARY_EVENT_DUMMY_EDGE) == true){	        				
        			edgeIterator.remove();
        		}
        	}
        }
        
    }
 

}
