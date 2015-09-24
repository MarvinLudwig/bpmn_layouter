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
import java.util.Map;
import java.util.Map.Entry;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.graph.Layer;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.PointMinMax;

/**
 * 
 * Route gateway edges<br>
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd>Edges are (pre)routed by OrthogonalEdgeRouter.</dd>
 *   <dt>Postcondition:</dt><dd>Gateway edges are routed.</dd>
 *   <dt>Slots:</dt><dd>After phase 5.</dd>
 *   <dt>Same-slot dependencies:</dt><dd></dd>
 * </dl>
 * 
 */

public class BpmnGatewayProcessor implements ILayoutProcessor {
	
	Map<LEdge,LPort> newEdgeSources = new HashMap<LEdge,LPort>();
	LPort southPort;
	LPort northPort;
	
	public void process(LGraph layeredGraph,
			IKielerProgressMonitor progressMonitor) {
		
		for (Layer layer : layeredGraph.getLayers()){
        	for (LNode node : layer.getNodes()){
        		southPort = null;
        		northPort = null;
        		BpmnElementType elementType = node.getProperty(BpmnProperties.ELEMENT_TYPE);
        		
        		// Gateways
        		if (elementType == BpmnElementType.GATEWAY){
	    			// get the north and south port
	        		for (LPort port : node.getPorts()){
						if (port.getSide() == PortSide.SOUTH) southPort = port;
						else if (port.getSide() == PortSide.NORTH) northPort = port;
	        		}
	        		
	        		// if ports do not exist, create them
	        		double nodeHeight = node.getSize().y;
	        		double nodeWidth = node.getSize().x;
	        		if (southPort == null) 
	        			southPort = createPort(node,nodeWidth / 2,nodeHeight,PortSide.SOUTH);
	        		if (northPort == null) 
	        			northPort = createPort(node,nodeWidth / 2,0,PortSide.NORTH);
	        		
	        		// process edges
	        		processGatewayEdges(node.getOutgoingEdges(),node);
        		}
			}
		}
		
	    for(Entry<LEdge,LPort> entry : newEdgeSources.entrySet()) {
	    	LEdge edge = entry.getKey();
	    	LPort port = entry.getValue();
	    	edge.setSource(port);
	    }
	}

	private void processGatewayEdges(Iterable<LEdge> edges, LNode gatewayNode){
		Integer offsetX = null;
		PointMinMax<Double> pointMinMax = new PointMinMax<Double>();	

		double gatewayCenterX = gatewayNode.getPosition().x + gatewayNode.getSize().x / 2;
		double gatewayCenterY = gatewayNode.getPosition().y + gatewayNode.getSize().y / 2; 
		
		double gatewayTop = gatewayNode.getPosition().y;
		double gatewayBottom = gatewayTop + gatewayNode.getSize().y; 
		
		int edgeCount = 0;
		for (LEdge edge : edges){			
			// get bendpoints min and max X position
			for (KVector bendpoint : edge.getBendPoints()){
				pointMinMax.addX(bendpoint.x);
				pointMinMax.addY(bendpoint.y);
			}	
			edgeCount++; 
		}
		
		if (edgeCount < 2) return;
		
		// calculate x offset
		offsetX = (int) (gatewayCenterX - pointMinMax.getMinX());

		for (LEdge edge : edges){
			LNode connectedNode = edge.getTarget().getNode();
			double connectedNodeCenterY = connectedNode.getPosition().y + connectedNode.getSize().y / 2;
			
			for (KVector bendpoint : edge.getBendPoints()){
				bendpoint.x = bendpoint.x + offsetX;
				if (connectedNodeCenterY < gatewayCenterY){
					newEdgeSources.put(edge,northPort);
					if (bendpoint.y > gatewayTop) bendpoint.y = gatewayTop - 5;
				}
				else if (connectedNodeCenterY > gatewayCenterY){
					newEdgeSources.put(edge,southPort);
					if (bendpoint.y < gatewayBottom) bendpoint.y = gatewayBottom + 5;
				}				
			}
		}

	}
	
	LPort createPort(LNode node, double xPos, double yPos, PortSide side){	        
        LPort newPort = new LPort();
        newPort.getPosition().x = xPos;
        newPort.getPosition().y = yPos;
        newPort.setSide(side);
        newPort.setNode(node);
        return newPort;
	}

}
