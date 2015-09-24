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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.Utils;

/**
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd></dd>
 *   <dt>Postcondition:</dt><dd></dd>
 *   <dt>Slots:</dt><dd>After phase 5.</dd>
 *   <dt>Same-slot dependencies:</dt><dd>After BpmnContainerPostProcessor</dd>
 * </dl>
 * 
 */

public final class BpmnMessageFlowPostProcessor implements ILayoutProcessor {

    /**
     * {@inheritDoc}
     */
    public void process(final LGraph lGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("BPMN Message flow post-processor", 1);

        List<LEdge> messageFlows = lGraph.getProperty(BpmnProperties.MESSAGE_FLOWS);
        if (messageFlows == null) return;
        
        Map<LEdge, LPort> edgeSourceList = new HashMap<LEdge, LPort>();
        Map<LEdge, LPort> edgeTargetList = new HashMap<LEdge, LPort>();
        
		for (LEdge edge : messageFlows){
			LPort sourcePort = edge.getSource();
			LPort targetPort = edge.getTarget();
			LNode sourceNode = sourcePort.getNode();
			LNode targetNode = targetPort.getNode();
			KVector targetNodePos = targetNode.getPosition();
			KVector sourceNodePos = sourceNode.getPosition();
			// adjust port sides
			PortSide sourcePortSide;
			PortSide targetPortSide;
			if (sourceNodePos.y > targetNodePos.y) {
				sourcePortSide = PortSide.NORTH;
				targetPortSide = PortSide.SOUTH;
			}
			else {
				sourcePortSide = PortSide.SOUTH;
				targetPortSide = PortSide.NORTH;
			}
			sourcePort = Utils.getPort(edge.getSource().getNode(),sourcePortSide);
			targetPort = Utils.getPort(edge.getTarget().getNode(),targetPortSide);
			
			// Make sure ports are centered, might not be the case after Big Nodes processing
			Utils.centerPort(sourcePort);
			Utils.centerPort(targetPort);
			
			edgeSourceList.put(edge, sourcePort);
			edgeTargetList.put(edge, targetPort);
			// Edge routing
			LLabel label = edge.getProperty(BpmnProperties.LABEL);
			Double y = null;
			double x1 = 0;
			double x2 = 0;
			if (targetNode.getPosition().x != sourceNode.getPosition().x 
					|| label != null){ // For labels also, as we need the y position calculated here
				LNode topNode;
				KVector topNodePos;
				KVector bottomNodePos;
				if (targetNodePos.y < sourceNodePos.y){
					topNodePos = targetNodePos;
					bottomNodePos = sourceNodePos;
					topNode = targetNode;
				}
				else {
					topNodePos = sourceNodePos;
					bottomNodePos = targetNodePos;
					topNode = sourceNode;
				}
				double bottomNodeTop = bottomNodePos.y;
				double topNodeBottom = topNodePos.y + topNode.getSize().y;
				y = (bottomNodeTop - topNodeBottom) / 2;
				if (sourceNodePos.y > targetNodePos.y)
					y = sourceNode.getPosition().y - y;
				else 
					y = y + sourceNode.getPosition().y + sourceNode.getSize().y;
				// Add two bendpoints
				if (targetNode.getPosition().x != sourceNode.getPosition().x) {
					x1 = sourceNode.getPosition().x + sourceNode.getSize().x / 2;
					edge.getBendPoints().add(x1,y);
					x2 = targetNode.getPosition().x + targetNode.getSize().x / 2;
					edge.getBendPoints().add(x2,y);
				}
				else { // Calculate x1 for label placement only
					x1 = x2 = targetNode.getPosition().x + targetNode.getSize().x / 2;
				}
			}
			
			// Label
			if (label != null){
				edge.getLabels().add(label);
				KVector labelPos = label.getPosition();
				if (Math.abs(x1-x2) > label.getSize().x){ // Enough space to center label between bendpoints
					labelPos.x = sourceNode.getPosition().x + sourcePort.getPosition().x
										+ (Math.abs(targetNodePos.x - sourceNodePos.x
										+ targetPort.getPosition().x - sourcePort.getPosition().x)/2);
					labelPos.y = y;
				}
				else {
					labelPos.x = Math.max(x1, x2) - label.getSize().x - 10;
					if (x1 == x2) labelPos.y = y - label.getSize().y / 2;
					else  labelPos.y = y - label.getSize().y - 5;
				}
			}

		}
        
		// Adjust port x
        for (Entry<LEdge, LPort> entry : edgeSourceList.entrySet()){
        	LEdge edge = entry.getKey();
			edge.setSource(entry.getValue());
        }
        for (Entry<LEdge, LPort> entry : edgeTargetList.entrySet()){
        	LEdge edge = entry.getKey();
			edge.setTarget(entry.getValue());
        }
    }

}
