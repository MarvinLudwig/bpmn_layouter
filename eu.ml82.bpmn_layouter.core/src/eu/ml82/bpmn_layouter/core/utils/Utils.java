package eu.ml82.bpmn_layouter.core.utils;

import java.util.Iterator;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;

public class Utils {
    
    /**
     * Remove all outgoing edges of a node
     */
    public static void removeOutgoingEdges(LNode node){
    	removeOutgoingEdges(node,0);
    }
    
    /** 
     * Remove all outgoing edges of a need, but keep a given number
     */
    public static void removeOutgoingEdges(LNode node, int keep){
    	Iterator<LEdge> edges = node.getOutgoingEdges().iterator();
    	int i = 0;
    	while (edges.hasNext()){
			LEdge edge = edges.next();
			if (i >= keep){
				edges.remove();
				Iterator<LEdge> targetEdges = edge.getTarget().getIncomingEdges().iterator();
				while (targetEdges.hasNext()){
					LEdge targetEdge = targetEdges.next();
					if (edge == targetEdge) {
						targetEdges.remove();
						break;
					}
				} 
    		}
			i++;
		}
    }
	
    /** 
     * Remove all incoming edges of a need, but keep a given number
     */
    public static void removeIncomingEdges(LNode node){
    	removeIncomingEdges(node,0);
    }
    
    /**
     *  Remove all incoming edges of a need, but keep a given number
     */
    public static void removeIncomingEdges(LNode node, int keep){
    	Iterator<LEdge> edges = node.getIncomingEdges().iterator();
    	int i = 0;
    	while (edges.hasNext()){
			LEdge edge = edges.next();
			if (i >= keep){
				edges.remove();
				Iterator<LEdge> sourceEdges = edge.getSource().getOutgoingEdges().iterator();
				while (sourceEdges.hasNext()){
					LEdge sourceEdge = sourceEdges.next();
					if (edge == sourceEdge) {
						sourceEdges.remove();
						break;
					}
				} 
    		}
			i++;
		}
    }
    
    /**
     *  Count number of outgoing edges
     */
    public static int countOutgoingEdges(LNode node){
    	Iterator<LEdge> edges = node.getOutgoingEdges().iterator();
    	return countEdges(edges);
    }
    
    /**
     *  Count number of incoming edges
     */
    public static int countIncomingEdges(LNode node){
    	Iterator<LEdge> edges = node.getIncomingEdges().iterator();
    	return countEdges(edges);
    }
    
    /**
     *  Total edge count of an edge iterator
     */
    public static int countEdges(Iterator<LEdge> edges){
    	int edgeCount = 0;
    	while (edges.hasNext()){
			edgeCount++;
			edges.next();
		}
    	return edgeCount;
    }
    
    /** 
     * Get node's port or create it if it does not exist
     */
    public static LPort getPort(LNode node, PortSide portSide){
		Iterator<LPort> ports = node.getPorts(portSide).iterator();
		if (ports.hasNext()) 
			return ports.next();
		else{
			LPort newPort = new LPort();
			newPort.setSide(portSide);
			newPort.setNode(node);
			KVector newPos = newPort.getPosition();
			double nodeWidth = node.getSize().x;
			double nodeHeight = node.getSize().y;
			if (portSide == PortSide.WEST || portSide == PortSide.EAST)
				newPos.y = nodeHeight / 2;
			else
				newPos.x = nodeWidth / 2;
			if (portSide == PortSide.WEST)
				newPos.x = 0;
			else if (portSide == PortSide.EAST)
				newPos.x = nodeWidth;
			else if (portSide == PortSide.NORTH)
				newPos.y = 0;
			else if (portSide == PortSide.SOUTH)
				newPos.y = nodeHeight;
			return newPort;
		}
    }
    
    /** 
     * Transform to absolute coordinates
     */
    public static KVector calcAbsolutePos(LNode node){
    	if (node == null) return null;
	    KVector pos = new KVector(node.getPosition());
	    do {
	        node = node.getProperty(InternalProperties.PARENT_LNODE);
	        if (node != null) {
	            pos.add(node.getPosition());
	        }
	    } while (node != null);
	    return pos;
    }

	public static void centerPort(LPort port) {
		KVector newPos = port.getPosition();
		PortSide portSide = port.getSide();
		LNode node = port.getNode();
		double nodeWidth = node.getSize().x;
		double nodeHeight = node.getSize().y;
		if (portSide == PortSide.WEST || portSide == PortSide.EAST)
			newPos.y = nodeHeight / 2;
		else
			newPos.x = nodeWidth / 2;
		if (portSide == PortSide.WEST)
			newPos.x = 0;
		else if (portSide == PortSide.EAST)
			newPos.x = nodeWidth;
		else if (portSide == PortSide.NORTH)
			newPos.y = 0;
		else if (portSide == PortSide.SOUTH)
			newPos.y = nodeHeight;
	}

}