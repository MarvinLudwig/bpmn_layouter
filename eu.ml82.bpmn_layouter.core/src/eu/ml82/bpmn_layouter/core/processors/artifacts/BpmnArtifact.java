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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.properties.PortType;

public class BpmnArtifact {

	public LNode node;
	public List<LEdge> dummyEdges;
	Set<OriginalEdge> originalEdges = new HashSet<OriginalEdge>();
	HashMap<LNode, Double> containers; // Possible artifact containers and their y-position, this is only used in BpmnArtifactPostProcessor

	// we store the original edge's node
	// cause the port may be changed by layout processors
	public class OriginalEdge {
		LEdge edge;
		LNode oppositeNode;
		PortType portType; // in or out edge from artifact's point of view
	}
	
	public void addOriginalEdge (LEdge edge, PortType portType) {
		OriginalEdge originalEdge = new OriginalEdge();
		if (portType == PortType.INPUT)
			originalEdge.oppositeNode = edge.getSource().getNode();
		else 
			originalEdge.oppositeNode = edge.getTarget().getNode();	
		originalEdge.portType = portType;	
		originalEdge.edge = edge;
		originalEdges.add(originalEdge);
	}
}
