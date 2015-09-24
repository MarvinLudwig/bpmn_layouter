/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors.artifacts;

import java.util.List;
import java.util.Map;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.kiml.options.EdgeType;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.Layer;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;

/**
 * Place artifact that have only one incoming or one outgoing edge.
 */

public class SingleEdgeArtifactProcessor {
	
	LGraph graph; 
	
	/**
	 * Place artifact that have only one incoming or one outgoing edge.
	 * @param positions 
	 */
	public static void place(BpmnArtifact artifact, LGraph graph, Map<KVector, Integer> positions){
		int layerIndex;
		Layer layer = null;
		LNode artifactNode = artifact.node;
		KVector artifactSize = artifactNode.getSize();
		LNode oppNode;
		Float spacing = graph.getProperty(LayoutOptions.SPACING);
		List<Layer> layers = graph.getLayers();
		LEdge edge = artifactNode.getConnectedEdges().iterator().next();
		EdgeType edgeType = edge.getProperty(LayoutOptions.EDGE_TYPE); 
		PortSide portSide;
		
		// Get layer
		if (edge.getSource().getNode() == artifactNode){ // West
			oppNode = edge.getTarget().getNode();
			portSide = PortSide.WEST;
		}
		else{ // East
			oppNode = edge.getSource().getNode();
			layerIndex = oppNode.getLayer().getIndex();
			portSide = PortSide.EAST;
		}
		layerIndex = oppNode.getLayer().getIndex();
		KVector oppNodePos = oppNode.getPosition();
		KVector oppNodeSize = oppNode.getSize();
		
		// Set artifact container
		LNode artifactContainer = oppNode.getProperty(InternalProperties.PARENT_LNODE);
		if (artifactContainer != null) artifactNode.setProperty(InternalProperties.PARENT_LNODE, artifactContainer);
		
		KVector newPos = new KVector();
		KVector artifactPos = artifactNode.getPosition();
		boolean placed = false;
		newPos.x = oppNodePos.x + oppNodeSize.x / 2 - artifactSize.x / 2;
		layer = graph.getLayers().get(layerIndex);
		int penalty = 0;
		// North
		newPos.y = oppNodePos.y - artifactSize.y - spacing / 2;
		if (collision(newPos, artifact, layer) == false) {
			penalty = 0;
			artifactPos.x = newPos.x;
			artifactPos.y = newPos.y;
			if (BpmnArtifactPostProcessor.inContainer(artifact) == false) penalty++;
			positions.put(newPos,penalty);
			placed = true;
		}
		// South
		newPos.y = oppNodePos.y + oppNodeSize.y + spacing / 2;
		if (collision(newPos, artifact, layer) == false) {
			penalty = 0;
			artifactPos.x = newPos.x;
			artifactPos.y = newPos.y;
			if (BpmnArtifactPostProcessor.inContainer(artifact) == false) penalty++;
			positions.put(newPos,penalty);
			placed = true;
		}
		
		if (placed == false || penalty > 0){
			newPos.y = oppNodePos.y + oppNodeSize.y / 2 - artifactSize.y / 2;
			// East
			if (edgeType == EdgeType.UNDIRECTED || portSide == PortSide.EAST){
				newPos.x = oppNodePos.x + oppNodeSize.x + spacing / 2;
				if (layerIndex+1 < layers.size()) {
					layer = layers.get(layerIndex+1);
					placed = !collision(newPos, artifact, layer);
				}
				else placed = true;  // Eastern end of the graph
				if (placed == true) positions.put(newPos,0);
			}
			
			// West
			if (edgeType == EdgeType.UNDIRECTED || portSide == PortSide.WEST){
				newPos.x = oppNodePos.x - artifactSize.x - spacing / 2;
				if (layerIndex > 0) {
					layer = layers.get(layerIndex-1);
					placed = !collision(newPos, artifact, layer);
				}
				else placed = true;  // Eastern end of the graph
				if (placed == true) positions.put(newPos,0);
			}
		}		
	}
	
	/**
	 *  Check collisions with nodes in certain layer
	 */
	private static boolean collision(KVector newPos, BpmnArtifact artifact, Layer layer){
		boolean collision = false;
		double artifactTop = newPos.y;
		double artifactBottom = newPos.y + artifact.node.getSize().y;
		for (LNode node : layer.getNodes()){
			double nodeTop = node.getPosition().y;
			double nodeBottom = node.getPosition().y + node.getSize().y;
			if ((artifactTop > nodeTop && artifactTop < nodeBottom)
				|| (artifactBottom > nodeTop && artifactBottom < nodeBottom)){
				collision = true;
				break;
			}
		}
		return collision;
	}
}
