/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors.artifacts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.math.KVectorChain;
import de.cau.cs.kieler.kiml.options.EdgeType;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.graph.Layer;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import de.cau.cs.kieler.klay.layered.properties.PortType;
import eu.ml82.bpmn_layouter.core.processors.artifacts.BpmnArtifact.OriginalEdge;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.Maps;
import eu.ml82.bpmn_layouter.core.utils.MinMax;
import eu.ml82.bpmn_layouter.core.utils.PointMinMax;
import eu.ml82.bpmn_layouter.core.utils.Utils;

/** 
 * Place artifacts and route edges
 * <br><br>
 * 1. Place artifacts in different positions and route edges (incl. crossing removal)<br>
 * 2. Assign a score to each position (mainly based on edge length)<br>
 * 3. Choose artifact position with least score<br>
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd>none</dd>
 *   <dt>Postcondition:</dt><dd>none</dd>
 *   <dt>Slots:</dt><dd>After phase 5.</dd>
 *   <dt>Same-slot dependencies:</dt><dd>After BpmnContainerPostProcessor</dd>
 * </dl>
 */

public final class BpmnArtifactPostProcessor implements ILayoutProcessor {

	float spacing;
    Map<KVector, Integer> positions;
    Map<Integer, Double> layersX = new TreeMap<Integer, Double>();
    List<PortChange> portChanges = new LinkedList<PortChange>();
    int penalty; // non optimal edge routing lowers routing score
    Set<LNode> containers;
    LGraph lGraph;
    
    KVector debugPos = null; //new KVector(328,135); // for debugging purposes: set fixed artifact position
	
	public void process(final LGraph lGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("Artifact pre-processing", 1);
        
        List<BpmnArtifact> artifacts = lGraph.getProperty(BpmnProperties.ARTIFACTS);
        spacing = lGraph.getProperty(LayoutOptions.SPACING);
        this.lGraph = lGraph;
        
        // get layers' x values
        List<Layer> layers = lGraph.getLayers();
        for (Layer layer : layers){
        	if (layer.getNodes().size() > 0){
	        	Double x = null;
	        	for (LNode node : layer.getNodes()){
	        		double nodeRight = node.getPosition().x + node.getSize().x;
	        		if (x == null || nodeRight > x) x = nodeRight;
	        	}
	        	layersX.put(layer.getIndex(), x);
        	}
        }
               
        // Process artifacts
        if (artifacts != null){
        	// Prepare
        	// Order artifacts by connected nodes position
        	// (only needed if we have a graph with containers)
        	containers = lGraph.getProperty(BpmnProperties.CONTAINERS);
        	Map<BpmnArtifact,Double> artifactMap = null;
        	Double yMax = null;
        	if (containers != null) {
        		artifactMap = new HashMap<BpmnArtifact,Double>();
        	}
	        for (BpmnArtifact artifact : artifacts){
	        	LNode artifactNode = artifact.node;
	    		artifact.containers = new HashMap<LNode,Double>();
	    		
	            // Restore original edges
	            for (OriginalEdge originalEdge : artifact.originalEdges){
	            	LEdge edge = originalEdge.edge;
	    			if (originalEdge.portType == PortType.INPUT){
	    				edge.setSource(originalEdge.oppositeNode.getPorts().get(0));
	    				edge.setTarget(artifactNode.getPorts().get(0));
	    				if (artifactMap != null){
	    					Double sourceNodeY = edge.getSource().getNode().getPosition().y;
			        		if (yMax == null || sourceNodeY > yMax) yMax = sourceNodeY;	 
	    				}
	    			}
	    			else {
	    				edge.setTarget(originalEdge.oppositeNode.getPorts().get(0));
	    				edge.setSource(artifactNode.getPorts().get(0));
	    				if (artifactMap != null){
	    					Double targetNodeY = edge.getTarget().getNode().getPosition().y;
			        		if (yMax == null || targetNodeY > yMax) yMax = targetNodeY; 
	    				}
	    			}
					// add possible artifact containers (successors' and predecessors' containers)
					LNode nodeParent = originalEdge.oppositeNode.getProperty(InternalProperties.PARENT_LNODE);
					if (nodeParent != null)
						artifact.containers.put(nodeParent, nodeParent.getPosition().y);
	            }
	            if (artifactMap != null) artifactMap.put(artifact, yMax);
	        }
	        
			if (artifactMap != null){
	        	artifacts = new LinkedList<BpmnArtifact>(Maps.getKeysSortedByValue(artifactMap));
	        }
    	
	        // Process artifacts
			for (BpmnArtifact artifact : artifacts){
				LNode artifactNode = artifact.node;
	        	positions = new HashMap<KVector, Integer>();
				
	    		if (artifact.originalEdges.size() == 1){ // Artifact has only one incoming or one outgoing edge
	    			SingleEdgeArtifactProcessor.place(artifact,lGraph,positions);
	    		}
	    		else { // Artifacts with more than one edge
	    			
		        	// Get min and max layer
		        	PointMinMax<Double> minMaxY = new PointMinMax<Double>();
		        	MinMax<Integer> layerMinMax = calcMinMax(artifact,minMaxY);
		    		
		    		// Final layer min / max adjustments
	        		layerMinMax.addValue(Math.max(layerMinMax.getMin()-1, 0));
	        		layerMinMax.addValue(Math.min(layerMinMax.getMax()+1, lGraph.getLayers().size()-1));
		    		
	        		// Place artifacts
		        	placeArtifact(artifact,layerMinMax,minMaxY);
		        	
	    		}
	    		
				// Get final position based on score
	    		KVector newPos;
	    		if (debugPos != null) newPos = debugPos;
	    		else newPos = Maps.getKeysSortedByValue(positions).iterator().next();
	        	artifactNode.getPosition().x = newPos.x;
	        	artifactNode.getPosition().y = newPos.y;
		        routeEdges(artifact);
		        
		        // Resize container if necessary
		    	handleContainers(artifact);   

		    	// Attach artifact to its container
		    	LNode artifactContainer = artifactNode.getProperty(InternalProperties.PARENT_LNODE);
				if (artifactContainer != null) artifactContainer.getProperty(InternalProperties.NESTED_LGRAPH).getLayerlessNodes().add(artifact.node);
		    
				// Attach artifact to layer, so it does not collide with following artifacts
				Layer artifactLayer = null;
				for (Entry<Integer, Double> entry : layersX.entrySet()){
					if (newPos.x < entry.getValue()){
						artifactLayer = lGraph.getLayers().get(entry.getKey());
						break;
					}
				}
				int i = 0;
				for (LNode node : artifactLayer.getNodes()){
					if (newPos.y < node.getPosition().y){
						artifactNode.setLayer(i,artifactLayer);
						break;
					}
					i++;
				}
			}
        }
    }
		
	/**
	 * Place artifact on different positions from layer 
	 * with index layerMin to layer with index layerMax.
	 */
	private void placeArtifact(BpmnArtifact artifact, MinMax<Integer> layerMinMax, PointMinMax<Double> minMax){
		LNode artifactNode = artifact.node;
		double artifactHeight = artifactNode.getSize().y;
				
		for (int i = layerMinMax.getMin(); i <= layerMinMax.getMax(); i++){
						
			Layer layer = lGraph.getLayers().get(i);
			if (layer.getNodes().size() == 0) 
				continue;

			// Here we could add per-layer penalty logic
			int penaltyStart = 0;
			
			LNode node1 = null, node2 = null;
			double centerX = 0;
			double newY;
				for (LNode node : layer.getNodes()){
				LNode nodeContainer = node.getProperty(InternalProperties.PARENT_LNODE);
				if (nodeContainer == null || artifact.containers.keySet().contains(nodeContainer)){
					node1 = node2;
					node2 = node;					
					KVector node2Pos = node2.getPosition();
					
					centerX = node2Pos.x 
							+ node2.getSize().x / 2
							- artifact.node.getSize().x / 2;
		    		// above top node
					if ( node1 == null && node2 != null){
			        	penalty = penaltyStart;
						if (minMax.getMaxY() == null || node2Pos.y < minMax.getMinY()){ 
		        			newY = node2Pos.y - artifactNode.getSize().y - spacing / 2;
		        			penalty++;
						}
		        		else{ 
		        			newY = minMax.getMinY() - artifactNode.getSize().y / 2;
		        		}
						
						artifactNode.getPosition().x = centerX;
						artifactNode.getPosition().y = newY;
		        		routeEdges(artifact);
					}
					// Is there space between node1 and node2?
					if ( node1 != null && node2 != null){
						KVector node1Pos = node1.getPosition();
			        	penalty = penaltyStart;
						double bottomNode1 = node1Pos.y + node1.getSize().y;
						double space = node2Pos.y - (bottomNode1 + artifactHeight);
						if (space > 0){ // Yes, there is. Route edges.
							artifactNode.getPosition().x = centerX;
							artifactNode.getPosition().y = bottomNode1 + spacing / 2;
							if (space < spacing*2) penalty++; 
		            		routeEdges(artifact);
						}
					}
				}
			}
			if (node2 != null){
				// below bottom node
				KVector node2Pos = node2.getPosition();
		    	penalty = penaltyStart;
				double bottomNode2 = node2Pos.y + node2.getSize().y;
				if (minMax.getMaxY() == null || bottomNode2 > minMax.getMaxY()){ 
					newY = bottomNode2 + spacing / 2;
					penalty++;
				}
				else {
					newY = minMax.getMaxY() - artifact.node.getSize().y / 2; 
				}
				artifactNode.getPosition().x = centerX;
				artifactNode.getPosition().y = newY;
				routeEdges(artifact);
			}
		}
	}
    
	/**
	 * 1. Route edges
	 * 2. Adjust ports
	 * 3. Calculate edge length. 
	 */
    private int routeEdges(BpmnArtifact artifact){    	
    	double edgeLength = 0;
    	boolean lengthPenalty = false;
    	portChanges.clear();
    	LNode artifactNode = artifact.node;
    	KVector artifactPos = artifactNode.getPosition();
    	for (LEdge edge : artifactNode.getConnectedEdges()){
    		// Get opposite node
    		LNode oppNode;
			if (edge.getSource().getNode() == artifactNode)
    			oppNode = edge.getTarget().getNode();
			else {
				oppNode = edge.getSource().getNode();
			}
						
			// Clear bendpoints
			KVectorChain bendpoints = edge.getBendPoints(); 
			bendpoints.clear();

			KVector oppNodePos = oppNode.getPosition();
			double artifactCenterX = artifactPos.x + artifactNode.getSize().x / 2;
			double artifactCenterY = artifactPos.y + artifactNode.getSize().y / 2;
			double nodeTop = oppNodePos.y;
			double nodeBottom = oppNodePos.y + oppNode.getSize().y;
			double nodeLeft = oppNodePos.x;
			double nodeRight = oppNodePos.x + oppNode.getSize().x;
			double nodeCenterX = oppNodePos.x + oppNode.getSize().x / 2;
			double nodeCenterY = oppNodePos.y + oppNode.getSize().y / 2;
			
			// Create new bendpoints
			if (artifactCenterY != nodeCenterY && artifactCenterX != nodeCenterX){
				if  ((artifactCenterX > nodeRight || artifactCenterX < nodeLeft) 
						&& (artifactCenterY < nodeTop || artifactCenterY > nodeBottom)){
						bendpoints.add(nodeCenterX,artifactCenterY);
				}
				else penalty++;
			}
			
			// Set new port positions
			if (edge.getSource().getNode() == artifactNode){
				setPort(edge,edge.getSource(), true);
				setPort(edge,edge.getTarget(), false);
			}
			else {
				setPort(edge,edge.getSource(), false);
				setPort(edge,edge.getTarget(), true);
			}
    	}
    	
    	// Apply port changes. Needs to be done outside the edge loop.
    	// Otherwise we'd get a concurrent modification error.
    	for (PortChange portChange : portChanges){
	    	if (portChange.portType == PortType.OUTPUT)
	    		portChange.edge.setSource(portChange.port);
	    	else
	    		portChange.edge.setTarget(portChange.port);
    	}
    	
    	for (LEdge edge : artifactNode.getConnectedEdges()){
    		removeCrossings(edge,artifact);
    	}
    	
		// Calculate edge length
		for (LEdge edge : artifactNode.getConnectedEdges()){
			double lastX = edge.getSource().getPosition().x + edge.getSource().getNode().getPosition().x;
			double lastY = edge.getSource().getPosition().y + edge.getSource().getNode().getPosition().y;
			KVector lastPoint = new KVector(lastX, lastY);
			KVectorChain bendpoints = edge.getBendPoints();
			for (KVector bendpoint : bendpoints){
				double segmentLength = + Math.sqrt(Math.pow(lastPoint.x-bendpoint.x,2)
								+ Math.pow(lastPoint.y-bendpoint.y,2));
				edgeLength = edgeLength + segmentLength;
				
				// Penalty for very short edge segment
				if (segmentLength < spacing / 2) lengthPenalty = true;
				
				lastPoint = bendpoint;
			}

			double targetX = edge.getTarget().getPosition().x + edge.getTarget().getNode().getPosition().x;
			double targetY = edge.getTarget().getPosition().y + edge.getTarget().getNode().getPosition().y;
			double segmentLength = + Math.sqrt(Math.pow(lastPoint.x-targetX,2)
							+ Math.pow(lastPoint.y-targetY,2));
			if (segmentLength < spacing / 2) lengthPenalty = true;
			edgeLength = edgeLength + segmentLength; 			
    	}
		
		// Update artifact container
		findArtifactContainer(artifact);
		// Increase penalty if artifact is outside its container
		if (inContainer(artifact) == false) penalty++;
		
		// Increase penalty if artifact edge overlays opposite node edge
		for (LPort port : artifactNode.getPorts(PortSide.EAST)){
			for (LEdge artifactEdge : port.getOutgoingEdges()){
				for (LEdge edge : artifactEdge.getSource().getNode().getIncomingEdges()){
					if (edge != artifactEdge && edge.getBendPoints().size() == 0
						&& artifactEdge.getBendPoints().size() == 0)
						penalty += 2;
				}
			}
		}
		for (LPort port : artifactNode.getPorts(PortSide.WEST)){
			for (LEdge artifactEdge : port.getIncomingEdges()){
				for (LEdge edge : artifactEdge.getSource().getNode().getOutgoingEdges()){
					if (edge != artifactEdge && edge.getBendPoints().size() == 0
						&& artifactEdge.getBendPoints().size() == 0)
						penalty += 2;
				}
			}
		}
    	
		if (lengthPenalty) penalty++; // Penalty for very short edge segment
		int score = (int) (edgeLength * (penalty + 1));
		
		positions.put(new KVector(artifactPos.x,artifactPos.y), score);
		
        // debug output       	
        //System.out.println("x:"+artifactPos.x+" "+" y:"+artifactPos.y
        //					+" score:"+score+" length:"+edgeLength+" penalty:"+penalty);
        
        return penalty;
    }   

    /**
     * Move edges around nodes that are crossed by artifact edges 
     * 
     */
    private void removeCrossings(LEdge edge,BpmnArtifact artifact){
		KVector lastPoint = KVector.sum(edge.getSource().getPosition(),edge.getSource().getNode().getPosition());
    	LNode oppNode;
    	LNode artifactNode = artifact.node;
		if (artifactNode == edge.getSource().getNode()) oppNode = edge.getTarget().getNode();
		else oppNode = edge.getSource().getNode();
		
    	KVectorChain points = new KVectorChain(edge.getBendPoints());
    	points.add(KVector.sum(edge.getTarget().getPosition(),edge.getTarget().getNode().getPosition()));
    	Iterator<KVector> pointsIter= points.iterator();
		LNode targetNode = edge.getTarget().getNode();
    	while (pointsIter.hasNext()){
    		KVector point = pointsIter.next();
    		KVector newPoint = null;
			if (lastPoint.x != point.x) { // horizontal segment
    			KVector changedPoint = checkCrossingHorizontal(edge, lastPoint, point, artifactNode);
				if (changedPoint != null){ // Crossing detected
					lastPoint.y = changedPoint.y;
					// calculate new bendpoint's x position
					double newX = 0;
					if (oppNode.getPosition().x < artifactNode.getPosition().x)
						newX = artifactNode.getPosition().x - spacing / 2;
					else
						newX = artifactNode.getPosition().x + artifactNode.getSize().x + spacing / 2;
					// create new bendpoint
					newPoint = new KVector(newX, changedPoint.y);
					if (targetNode == artifactNode) edge.getBendPoints().add(newPoint);
					else edge.getBendPoints().add(0, newPoint);
				}
			} 
    		else {
    			// TODO
    			// checkCrossingVertical(artifactNode, lastPoint, point);
    		}
			lastPoint = point;
    	}
    	
    }
    
    // Check crossing of artifact's vertical edge segments
    private KVector checkCrossingVertical(LNode node, KVector point1, KVector point2){
    	if (point2.y > point1.y){ // downwards
			for (LNode node2 : node.getLayer().getNodes()){
        		double nodeTop = node2.getPosition().y;
				if (point1.y < nodeTop && point2.y > nodeTop){ // crossing detected
					point2.y = nodeTop - (nodeTop - point1.y) / 2;;
					return new KVector(point2);
				}
			}
		}
		else { // upwards
			int i = node.getLayer().getNodes().size()-1;
			while (i >= 0){
				LNode node2 = node.getLayer().getNodes().get(i);
	    		double nodeBottom = node2.getPosition().y + node.getSize().y;
				if (point1.y > nodeBottom && point2.y < nodeBottom){ // crossing detected
					point2.y = nodeBottom + (point1.y - nodeBottom) / 2;
					return new KVector(point2);
				}
				i--;
			}
		}
		return null;
    }
    
    /**
     * Check crossing of artifact's horizontal edge segments
     */
    private KVector checkCrossingHorizontal(LEdge edge, KVector point1, KVector point2, LNode artifactNode){
    	LNode oppNode;
		if (artifactNode == edge.getSource().getNode()) oppNode = edge.getTarget().getNode();
		else oppNode = edge.getSource().getNode();
    	// get start and end layer of edge segment
    	Integer startLayerIndex = null;
    	Integer endLayerIndex = null;
    	Iterator<Entry<Integer, Double>> layersXIter = layersX.entrySet().iterator();
    	while (layersXIter.hasNext()){
    		Entry<Integer, Double> layerX = layersXIter.next();
    		if (startLayerIndex == null && layerX.getValue() > point1.x) 
    			startLayerIndex = layerX.getKey();
    		if (endLayerIndex == null && layerX.getValue() > point2.x) 
    			endLayerIndex = layerX.getKey();
    		if (startLayerIndex != null && endLayerIndex != null) 
    			break;
    	}
    	int layerCount = layersX.size();
    	if (startLayerIndex == null || endLayerIndex == null){
    		Object layerMaxIndex = layersX.keySet().toArray()[layerCount-1];
    		if (startLayerIndex == null) startLayerIndex = (Integer) layerMaxIndex;
    		if (endLayerIndex == null) endLayerIndex = (Integer) layerMaxIndex;
    	}
    	if (startLayerIndex > endLayerIndex){
    		Integer tmpIndex = endLayerIndex;
    		endLayerIndex = startLayerIndex;
    		startLayerIndex = tmpIndex;
    	}
    	
    	// crossing detection
    	LNode sourceNode = edge.getSource().getNode();
    	LNode targetNode = edge.getTarget().getNode();
		List<Layer> layers = lGraph.getLayers();
		for (int index = startLayerIndex; index <= endLayerIndex; index++){
			Layer layer = layers.get(index);
			for (LNode node2 : layer.getNodes()){
				if (node2 != sourceNode && node2 != targetNode){
	        		double nodeTop = node2.getPosition().y;
	        		double nodeBottom = node2.getPosition().y + node2.getSize().y;
					if (point2.y > nodeTop && point2.y < nodeBottom){ // crossing detected
						if (oppNode.getPosition().y > nodeTop)  point2.y = nodeBottom + spacing / 2; 
						else point2.y = nodeTop - spacing / 2; 
						return new KVector(point2);
					}
				}
			}
		}
	    		
		return null;
    }
    
    class PortChange{
    	LPort port;
    	PortType portType;
    	LEdge edge;
    }
    
    /**
	 * Determine new port side
	 * 
	 */
    private void setPort(LEdge edge, LPort port, Boolean posAbs){
		LNode node = port.getNode();
		int bendpointCount = edge.getBendPoints().size();
		LPort oppPort;
		PortType portType;
    	KVector point2 = null;
		if (port == edge.getSource()){
			oppPort = edge.getTarget();
			portType = PortType.OUTPUT;
			if (bendpointCount > 0) point2 = edge.getBendPoints().get(0);
		}
		else{
			oppPort = edge.getSource();
			portType = PortType.INPUT;
			if (bendpointCount > 0) point2 = edge.getBendPoints().get(bendpointCount-1);
		}

		KVector nodePos;
		LNode oppNode = oppPort.getNode();
		KVector oppNodePos;
		if (posAbs) {
			nodePos = node.getPosition();
			oppNodePos = oppNode.getPosition();
		}
		else  {
			nodePos = node.getPosition();
			oppNodePos = oppNode.getPosition();
		}
		if (point2 == null) point2 = oppNodePos;
		// south
		if (nodePos.y + node.getSize().y < point2.y){
			setPortPos(edge,PortSide.SOUTH,portType);
		}
		// north
		else if ((bendpointCount > 0 && nodePos.y > point2.y)
				|| (bendpointCount == 0 && nodePos.y > oppNodePos.y + oppNode.getSize().y)){
			setPortPos(edge,PortSide.NORTH,portType);
		}
		else {
			// west
			if (nodePos.x > point2.x){
				setPortPos(edge,PortSide.WEST,portType);
			}
			// east
			else if (nodePos.x < point2.x){
				setPortPos(edge,PortSide.EAST,portType);
			}
		}
    }
    
    /**
     * Set a new port with a specific side for an edge source or target.
     * If the port does not exist, it is created.
     * The port changes are collected in a list. Then they are
     * applied outside the edge loop, to avoid concurrent modification errors.
     */
    private void setPortPos(LEdge edge, PortSide portSide, PortType portType) {
    	LNode node;
		LPort newPort = null;
    	if (portType == PortType.OUTPUT){ 
        	LPort source = edge.getSource();
    		node = source.getNode();
    		if (source.getSide() == portSide) newPort = source;
    	}
		else { 
	    	LPort target = edge.getTarget();
			node = edge.getTarget().getNode(); 
    		if (target.getSide() == portSide) newPort = target;
		}
		Iterable<LPort> ports = node.getPorts(portSide);
		if (newPort == null){
			for (LPort port : ports){
				if (!port.getConnectedEdges().iterator().hasNext())
				newPort = port;
			}
		}
		if (newPort == null) { 
			newPort = new LPort();
			newPort.setSide(portSide);
			newPort.setNode(node);
		}
		Utils.centerPort(newPort);		

		PortChange portChange = new PortChange();
		portChange.edge = edge;
		portChange.port = newPort;
		portChange.portType = portType;
		portChanges.add(portChange);
	}
	
	/** 
	 * After one container has been resized, we need to move all other containers 
	 */
	private void moveContainers(double startPointY, double moveY){
        for (LNode container : containers){
        	if (container.getPosition().y > startPointY)
        		container.getPosition().y += moveY;
        }
	}
	
	/**
	 * Find container for artifact and set artifact.node
	 * PARENT_LNODE property.
	 */
	private LNode findArtifactContainer(BpmnArtifact artifact){
		Object[] containerArray = Maps.getKeysSortedByValue(artifact.containers).toArray();
        if (containerArray.length == 0) return null;
		
		LNode artifactContainer = null;
		// Find container for artifact
    	for (int i = 1; i < containerArray.length; i++){
    		LNode tmpContainer = (LNode) containerArray[i];
    		if (artifact.node.getPosition().y < tmpContainer.getPosition().y){
    			artifactContainer = (LNode) containerArray[i-1];
    			break;
    		}
    	}
    	if (artifactContainer == null) artifactContainer = (LNode) containerArray[containerArray.length-1];
		artifact.node.setProperty(InternalProperties.PARENT_LNODE, artifactContainer);
		
		return artifactContainer;
	}
	
	/**
	 * Checks whether the artifact is placed within its containers current boundaries
	 * Returns null if no artifact container is set.
	 */
	static boolean inContainer(BpmnArtifact artifact){
		LNode artifactContainer = artifact.node.getProperty(InternalProperties.PARENT_LNODE);
		if (artifactContainer != null) {
			KVector artifactPos = artifact.node.getPosition();
			if (artifactContainer != null){
				if (artifactPos.y < artifactContainer.getPosition().y
					|| artifactPos.y + artifact.node.getSize().y > artifactContainer.getPosition().y + artifactContainer.getSize().y)
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Adjustment that are needed when containers (lanes, pools) are involved.
	 * 
	 * 1. If artifact is placed outside the container borders, resize container 
	 * 2. Adjust port positions
	 * 
	 */
	private void handleContainers(BpmnArtifact artifact){
		Set<LNode> bpmnContainers = this.lGraph.getProperty(BpmnProperties.CONTAINERS);
		if (bpmnContainers == null) return;
		
        final int containerPaddingLeftRight = lGraph.getProperty(BpmnProperties.CONTAINER_PADDING_LEFT_RIGHT);
        final int containerPaddingTopBottom = lGraph.getProperty(BpmnProperties.CONTAINER_PADDING_LEFT_RIGHT);
        
		LNode artifactNode = artifact.node;
		LNode artifactContainer = artifactNode.getProperty(InternalProperties.PARENT_LNODE);
		
		// Resize container
		KVector artifactPos = artifactNode.getPosition();
		KVector artifactSize = artifactNode.getSize();
		KVector artifactContainerSize = artifactContainer.getSize();
		KVector artifactContainerPos = artifactContainer.getPosition();
		KVector newSize = new KVector(artifactContainerSize);
		// If artifact is placed below the current container, resize the container
		if (artifactPos.y + artifactSize.y > artifactContainerSize.y + artifactContainerPos.y){
			double startPointY = artifactContainer.getPosition().y;
			double newHeight = artifactPos.y - artifactContainer.getPosition().y + artifactSize.y + containerPaddingTopBottom;
			double moveY = newHeight - artifactContainerSize.y; 
			newSize.y = newHeight;
			moveContainers(startPointY, moveY);
			
			// Move nodes, bendpoints and ports
			LGraph nestedGraph;
			for (LNode container : bpmnContainers){
				if (container.getPosition().y > startPointY){
					nestedGraph = container.getProperty(InternalProperties.NESTED_LGRAPH);
		    		for (LNode node : nestedGraph.getLayerlessNodes()){
						node.getPosition().y += moveY;
		    			for (LEdge edge : node.getOutgoingEdges()){
		    				for (KVector bendpoint : edge.getBendPoints()){
								if (bendpoint.y > newSize.y) bendpoint.y += moveY;
							}
						}
		    		}
				}
			}
			nestedGraph = artifactContainer.getProperty(InternalProperties.NESTED_LGRAPH);
    		for (LNode node : nestedGraph.getLayerlessNodes()){
    			for (LEdge edge : node.getOutgoingEdges()){
    				for (KVector bendpoint : edge.getBendPoints()){
						if (bendpoint.y > artifactContainerSize.y + artifactContainerPos.y) bendpoint.y += moveY;
					}
				}    			
    			for (LEdge edge : node.getIncomingEdges()){
    				for (KVector bendpoint : edge.getBendPoints()){
						if (bendpoint.y > artifactContainerPos.y + artifactContainerSize.y
						&& bendpoint.y < artifactContainerPos.y + newSize.y) bendpoint.y -= moveY;
					}
				}
    		}
		}
		else if (artifactPos.y < artifactContainerPos.y){ // More space on the top needed -> move down all elements
			double moveY = artifactSize.y + containerPaddingTopBottom;
			double startPointY = artifactContainer.getPosition().y;
			newSize.y = artifactContainerSize.y + moveY;
			moveContainers(startPointY, moveY);
			
			// Move nodes, bendpoints and ports
			LGraph nestedGraph;
			for (LNode container : bpmnContainers){
				if (container.getPosition().y >= startPointY){
					nestedGraph = container.getProperty(InternalProperties.NESTED_LGRAPH);
		    		for (LNode node : nestedGraph.getLayerlessNodes()){
						node.getPosition().y += moveY;
		    			for (LEdge edge : node.getOutgoingEdges()){
		    				for (KVector bendpoint : edge.getBendPoints()){
								if (bendpoint.y > newSize.y) bendpoint.y += moveY;
							}
						}
		    		}
				}
			}    
			nestedGraph = artifactContainer.getProperty(InternalProperties.NESTED_LGRAPH);
    		for (LNode node : nestedGraph.getLayerlessNodes()){
    			for (LEdge edge : node.getIncomingEdges()){
    				for (KVector bendpoint : edge.getBendPoints()){
						if (bendpoint.y > startPointY && bendpoint.y < startPointY + moveY) 
							bendpoint.y += moveY;
					}
				}
    		}
		}
		else if (artifactPos.x + artifactSize.x > artifactContainerSize.x)
			newSize.x = artifactPos.x + artifactSize.x + containerPaddingLeftRight;
		
		// Resize parent containers
		resizeParentContainer(artifactNode,new KVector(newSize.x - artifactContainerSize.x, newSize.y - artifactContainerSize.y)); 
		
		// Set new container size
		artifactContainerSize = newSize;
	}
	
	private void resizeParentContainer(LNode node,KVector vector){
		LNode parentNode = node.getProperty(InternalProperties.PARENT_LNODE);
		if (parentNode!= null){
			parentNode.getSize().add(vector);
			resizeParentContainer(parentNode, vector);
		}
	}
	
	/**
	 * 1. Calculate min and max layer for artifact placement
	 * 2. Calculate min and max y-position (based on artifact's opposite nodes position)
	 */
	private MinMax<Integer> calcMinMax(BpmnArtifact artifact, PointMinMax<Double> posMinMax){
		MinMax<Integer> layerMinMax = new MinMax<Integer>();
		MinMax<Integer> inMinMax = new MinMax<Integer>();
		MinMax<Integer> outMinMax = new MinMax<Integer>();
		LNode artifactNode = artifact.node;
		for (LEdge edge : artifactNode.getConnectedEdges()){
			
			boolean undirected = false;
			if (edge.getProperty(LayoutOptions.EDGE_TYPE) == EdgeType.UNDIRECTED)
				undirected = true;
				
			LNode oppNode;
			if (edge.getSource().getNode() == artifactNode){ // out edge
				oppNode = edge.getTarget().getNode();
				int outLayer = oppNode.getLayer().getIndex();
				outMinMax.addValue(outLayer);
				if (undirected) inMinMax.addValue(outLayer);
			}
			else { // in edge
				oppNode = edge.getSource().getNode();
				int inLayer = oppNode.getLayer().getIndex();
				inMinMax.addValue(oppNode.getLayer().getIndex());
				if (undirected) outMinMax.addValue(inLayer);
			}
			
			// min / max y-coordinate
			posMinMax.addY(oppNode.getPosition().y + oppNode.getSize().y / 2);
			
		}

		// min / max of layer index
		if (outMinMax.getMin() == null) {
			layerMinMax.addValue(inMinMax.getMax());
		}
		else if (inMinMax.getMin() == null) {
			layerMinMax.addValue(outMinMax.getMin());
		}
		else {
			layerMinMax.addValue(outMinMax.getMin());
			layerMinMax.addValue(inMinMax.getMax());
		}
		
		return layerMinMax;
	}

}