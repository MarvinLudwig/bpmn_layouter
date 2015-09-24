/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

/**
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd></dd>
 *   <dt>Postcondition:</dt><dd></dd>
 *   <dt>Slots:</dt><dd>After phase 2.</dd>
 *   <dt>Same-slot dependencies:</dt><dd></dd>
 * </dl>
 * 
 */

package eu.ml82.bpmn_layouter.core.processors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.Utils;

/**
 * 
 * Assign original source node.
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd></dd>
 *   <dt>Postcondition:</dt><dd></dd>
 *   <dt>Slots:</dt><dd>Before phase 2.</dd>
 *   <dt>Same-slot dependencies:</dt><dd>Before BigNodesPreProcessor.</dd>
 * </dl>
 * 
 */

public class BpmnMessageFlowIntermediateProcessor implements ILayoutProcessor {

	public void process(LGraph lGraph, IKielerProgressMonitor progressMonitor) {
	        
	    Map<LEdge, LPort> edgeSources = new HashMap<LEdge, LPort>();
	    Map<LEdge, LPort> edgeTargets = new HashMap<LEdge, LPort>();
        List<LEdge> messageFlows = lGraph.getProperty(BpmnProperties.MESSAGE_FLOWS);
        if (messageFlows != null){
			for (LEdge edge : messageFlows){	    			
	    		LNode originalSourceNode = edge.getProperty(BpmnProperties.ORIGINAL_SOURCE_NODE);
	    		if (originalSourceNode != null){
	    			// The port side does not really matter here.
	    			// Though it shouldn't be the western or eastern one, 
	    			// in order to not get in conflict with big nodes processing.
	    			edgeSources.put(edge, Utils.getPort(originalSourceNode, PortSide.SOUTH));
	    			edgeTargets.put(edge, Utils.getPort(edge.getTarget().getNode(), PortSide.SOUTH));
	    		}
	    	}
        }
		    
		// set edge sources and targets
		for (Entry<LEdge, LPort> entry : edgeSources.entrySet()){
	    	LEdge edge = entry.getKey();
			edge.setSource(entry.getValue());
			
			// First we set a new source and then we remove
			// Looks stupid? It does, but we do it for a reason:
			// We want the edge to "know" its source, but we do not want the
			// source to "know" its edge. So we can avoid the edge to be handled
			// by long edge splitter. We don't need long edge processing for message flows
			// and it would cause an error during long edge joining.
    		Iterator<LEdge> edges = entry.getValue().getConnectedEdges().iterator();
    		while (edges.hasNext()){
    			LEdge tmpEdge = edges.next();
    			if (tmpEdge == edge) edges.remove();
    		}
	    }		
		for (Entry<LEdge, LPort> entry : edgeTargets.entrySet()){
	    	LEdge edge = entry.getKey();
			edge.setTarget(entry.getValue());
	    }
	    
	}

}
