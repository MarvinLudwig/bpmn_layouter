/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.kiml.options.PortConstraints;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.*;
import de.cau.cs.kieler.klay.layered.graph.LNode.NodeType;

/**
 * Create EAST and WEST ports, center them 
 * and assign all incoming edges to one port
 * and all outgoing edges to one port.
 * (important for BKNodePlacer)
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd></dd>
 *   <dt>Postcondition:</dt><dd></dd>
 *   <dt>Slots:</dt><dd>After phase 1.</dd>
 *   <dt>Same-slot dependencies:</dt><dd></dd>
 * </dl>
 * 
 */

public class BpmnPortPreProcessor implements ILayoutProcessor {

	public void process(LGraph layeredGraph,
			IKielerProgressMonitor progressMonitor) {
		
		Map<LEdge,LPort> edgeSources = new HashMap<LEdge,LPort>();
		Map<LEdge,LPort> edgeTargets = new HashMap<LEdge,LPort>();
		
		for (Layer layer : layeredGraph.getLayers()){
		for (LNode node : layer){
			
			if (node.getNodeType() == NodeType.LONG_EDGE)
				continue;
	        
	        double center = node.getSize().y / 2;
	        double nodeWidth = node.getSize().x;
	        LPort newPortWest = getPort(node,0,center,PortSide.WEST);
        	for (LEdge edge : node.getIncomingEdges()){
    	        edgeTargets.put(edge, newPortWest);
        	}    	        
	        LPort newPortEast = getPort(node,nodeWidth,center,PortSide.EAST);
        	for (LEdge edge : node.getOutgoingEdges()){
    	        edgeSources.put(edge, newPortEast);
        	}
        	
        	// apply port changes
        	for (Entry<LEdge, LPort> entry : edgeSources.entrySet()){
        		entry.getKey().setSource(entry.getValue());
        	}
        	for (Entry<LEdge, LPort> entry : edgeTargets.entrySet()){
        		entry.getKey().setTarget(entry.getValue());
        	}	     
        	
	        node.setProperty(LayoutOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_POS);
		} 
		}
		
	}
	
	LPort getPort(LNode node, double xPos, double yPos, PortSide side){
		Iterator<LPort> ports = node.getPorts(side).iterator();
		LPort newPort;
		if (ports.hasNext()) newPort = ports.next();
		else newPort = new LPort();
        newPort.getPosition().x = xPos;
        newPort.getPosition().y = yPos;
        newPort.setSide(side);
        newPort.setNode(node);
        return newPort;
	}

}
