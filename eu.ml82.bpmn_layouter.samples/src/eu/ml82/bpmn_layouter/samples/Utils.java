package eu.ml82.bpmn_layouter.samples;

import de.cau.cs.kieler.kiml.options.EdgeLabelPlacement;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.kiml.options.NodeLabelPlacement;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;

public class Utils {
	public static LNode createNode(LGraph parentGraph, double posX, double posY, double width, double height, String labelText){
		return createNode(parentGraph, posY, posY, width, height, labelText, null);
	}
	
	public static LNode createNode(LGraph parentGraph, double posX, double posY, double width, double height, String labelText, BpmnElementType elementType){
		LNode node = new LNode(parentGraph);
		parentGraph.getLayerlessNodes().add(node);
		node.getPosition().x = posX;
		node.getPosition().y = posY;
		node.getSize().x = width;
		node.getSize().y = height;
		if (elementType != null){ 
			node.setProperty(BpmnProperties.ELEMENT_TYPE, elementType);
		}
		
		// Create label
		if (!labelText.equals("") && labelText != null){
			LLabel label = new LLabel(labelText);
			node.getLabels().add(label);
			node.setProperty(LayoutOptions.NODE_LABEL_PLACEMENT, NodeLabelPlacement.insideCenter());
		}
		
		return node;
	}
	
	public static LEdge createEdge(LNode sourceNode, LNode targetNode, String labelText, BpmnElementType elementType){
		// Source port
		Iterable<LPort> sourcePorts = sourceNode.getPorts(PortSide.EAST);
		LPort sourcePort;
		if (sourcePorts.iterator().hasNext()) sourcePort = sourcePorts.iterator().next();
		else  {
			sourcePort = new LPort();
			sourcePort.setSide(PortSide.EAST);
			sourcePort.setNode(sourceNode);
		}
		
		// Target port
		Iterable<LPort> targetPorts = targetNode.getPorts(PortSide.WEST);
		LPort targetPort;
		if (targetPorts.iterator().hasNext()) targetPort = targetPorts.iterator().next();
		else  {
			targetPort = new LPort(); 
			targetPort.setSide(PortSide.WEST);
			targetPort.setNode(targetNode);
		}
		
		// Create edge
		LEdge edge = new LEdge();
		edge.setSource(sourcePort);
		edge.setTarget(targetPort);
		
		// Create edge label
		if (!labelText.equals("") && labelText != null){
			LLabel label = new LLabel(labelText);
			edge.getLabels().add(label);
			edge.setProperty(LayoutOptions.EDGE_LABEL_PLACEMENT, EdgeLabelPlacement.CENTER);
		}
		
		if (elementType != null){ 
			edge.setProperty(BpmnProperties.ELEMENT_TYPE, elementType);
		}
			
		return edge;
	}	
	
	public static LEdge createEdge(LNode sourceNode, LNode targetNode, String labelText) {
		return createEdge(sourceNode, targetNode, labelText, null);		
	}
}
