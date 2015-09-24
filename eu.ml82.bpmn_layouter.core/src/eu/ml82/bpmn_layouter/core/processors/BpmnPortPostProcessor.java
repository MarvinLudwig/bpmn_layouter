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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LNode.NodeType;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.graph.Layer;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.Maps;
import eu.ml82.bpmn_layouter.core.utils.Utils;

/**
 * 
 * In the BpmnPortPreProcessor we merged all edges of one side into
 * one port. That was needed for good node placing results.
 * Here we split the ports for good edge routing results.
 * 
 * This is only done for western and eastern ports, we do not touch
 * northern and southern ports here (e. g. message flows).
 * 
* <dl>
 *   <dt>Precondition:</dt><dd>none</dd>
 *   <dt>Postcondition:</dt><dd>none</dd>
 *   <dt>Slots:</dt><dd>Before phase 5.</dd>
 *   <dt>Same-slot dependencies:</dt><dd></dd>
 * </dl>
 * 
 */

public class BpmnPortPostProcessor implements ILayoutProcessor {
	
	List<LEdge> singleInEdges = new LinkedList<LEdge>();
	List<LEdge> singleOutEdges = new LinkedList<LEdge>();
	class NewPort{PortSide side; LNode node;}
	List<NewPort> newPorts = new LinkedList<NewPort>();
    int portCounter;
	
	public void process(LGraph layeredGraph,
			IKielerProgressMonitor progressMonitor) {
		
		Map<LPort,Double> incomingPorts = new HashMap<LPort,Double>();
		Map<LPort,Double> outgoingPorts = new HashMap<LPort,Double>();
		Map<LEdge,LPort> sources = new HashMap<LEdge,LPort>();
		Map<LEdge,LPort> targets = new HashMap<LEdge,LPort>();
		
		for (Layer layer : layeredGraph.getLayers()){
		for (LNode node : layer.getNodes()){
			BpmnElementType elementType = node.getProperty(BpmnProperties.ELEMENT_TYPE);
			if (node.getNodeType() != NodeType.LONG_EDGE
				&& elementType != BpmnElementType.GATEWAY 
				&& elementType != BpmnElementType.EVENT){
				// prepare
				int outgoingCount = 0, incomingCount = 0;
	        	LNode nodeContainer = node.getProperty(InternalProperties.PARENT_LNODE);
				incomingPorts.clear();
				outgoingPorts.clear();
				targets.clear();
				sources.clear();
				List<LPort> ports = new LinkedList<LPort>();
		        for (LPort port : node.getPorts()){
		        	if (port.getSide() == PortSide.EAST
		        		|| port.getSide() == PortSide.WEST)
		        		ports.add(port);
		        }
		        Iterator<LPort> portsIterator = ports.iterator();
				
				// incoming edges
		        LNode targetContainer = nodeContainer;
		        for (LEdge edge : node.getIncomingEdges()){
		        	LPort port = edge.getTarget();
		        	if (port.getSide() == PortSide.WEST
		        		|| port.getSide() == PortSide.EAST){
			        	incomingCount++;
			        	port = getPort(PortSide.WEST,node,portsIterator);
			        	targets.put(edge,port);
			        	incomingPorts.put(port, Utils.calcAbsolutePos(edge.getSource().getNode()).y);
		        	}
		        }
		        
		        // TODO: Why are there western outgoing ports? 
		        // Check with "Neuer Prozess_2.bpmn"
		        
		        // outgoing edges
		        for (LEdge edge : node.getOutgoingEdges()) {
		        	LPort port = edge.getSource();
		        	if (port.getSide() == PortSide.WEST
		        		|| port.getSide() == PortSide.EAST){
			        	outgoingCount++;
			        	port = getPort(PortSide.EAST,node, portsIterator);
			        	sources.put(edge,port);
			        	outgoingPorts.put(port, Utils.calcAbsolutePos(edge.getTarget().getNode()).y);
			        }	 
		        }
		  	  
		        adjustSources(sources);
		        adjustTargets(targets);
		        
		        double nodeHeight = node.getSize().y;
		        double offsetEast = nodeHeight / (outgoingCount+1);
		        double offsetWest = nodeHeight / (incomingCount+1);
		        	   
		        // Only one incoming or only one outgoing edge
		        // incoming
		        if (incomingCount == 1){
		        	LEdge edge = node.getIncomingEdges().iterator().next();
		        	singleInEdges.add(edge);
		        }
		        else adjustPorts (node,incomingPorts,offsetWest,PortSide.WEST);
		        
		    	// outgoing
		        if (outgoingCount == 1){
		        	LEdge edge = node.getOutgoingEdges().iterator().next();
		        	singleOutEdges.add(edge);
		        }
		        else adjustPorts (node,outgoingPorts,offsetEast,PortSide.EAST);
		        
			}
		} 	
		}		
        
		// Adjust ports of nodes that have only one incoming or only one outgoing edge.
		// This needs to be done after all other port adjustments.
        adjustSingleInEdges(singleInEdges);
        adjustSingleOutEdges(singleOutEdges);
	}
	
	// Get port uses existing ports
	private LPort getPort (PortSide side, LNode node, Iterator<LPort> portsIterator){
		LPort port;
		if (portsIterator.hasNext()){ 
			port = portsIterator.next();
		}
		else {
			port = new LPort();
			port.setSide(side);
			port.setNode(node);
		}
		port.setSide(side);
		if (side == PortSide.WEST) port.getPosition().x = 0;
		else if (side == PortSide.EAST) port.getPosition().x = node.getSize().x;
		port.getPosition().y = node.getSize().y / 2;
		return port;
	}
	
	private void adjustSources(Map<LEdge,LPort> edges){
		for (Entry<LEdge,LPort> entry : edges.entrySet()){
			entry.getKey().setSource(entry.getValue());
		}
	}
	
	private void adjustTargets(Map<LEdge,LPort> edges){
		for (Entry<LEdge,LPort> entry : edges.entrySet()){
			entry.getKey().setTarget(entry.getValue());
		}
	}
	
	private void adjustPorts (LNode node, Map<LPort, Double> ports, double offsetStep, PortSide portSide){
		double offset = offsetStep;
    	for (LPort port : Maps.getKeysSortedByValue(ports)){
    		port.getPosition().y = offset;   		
    		offset = offset + offsetStep;
    	}
	}
	
	private void adjustSingleInEdges (List<LEdge> edges){
		for (LEdge edge : edges){
			LPort port = edge.getTarget();
			LPort counterPort = edge.getSource();
			adjustSingleEdge(port,counterPort);
		}
	}
	
	private void adjustSingleOutEdges (List<LEdge> edges){
		for (LEdge edge : edges){
			LPort port = edge.getSource();
			LPort counterPort = edge.getTarget();
			adjustSingleEdge(port,counterPort);
		}
	}
	
	private void adjustSingleEdge (LPort port, LPort counterPort){
		LNode node = port.getNode();
		LNode counterNode = counterPort.getNode();
		double centerY = node.getPosition().y + node.getSize().y / 2;
		double counterCenterY = counterNode.getPosition().y + counterNode.getSize().y / 2;
		if (centerY == counterCenterY){
			port.getPosition().y = counterPort.getPosition().y + (counterNode.getPosition().y - node.getPosition().y);
		}
	}	
		

}
