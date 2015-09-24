/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;

/**
 *   Resize container width.
 * 
 *   Move nodes, bendpoints and labels to the right in order to have
 *   enough space on the left for the container labels.
 *
 * <dl>
 *   <dt>Precondition:</dt><dd>none</dd>
 *   <dt>Postcondition:</dt><dd>none</dd>
 *   <dt>Slots:</dt><dd>After phase 5.</dd>
 *   <dt>Same-slot dependencies:</dt>Before BpmnGroupPostProcessor<dd></dd>
 * </dl>
 */

public class BpmnContainerPostProcessor implements ILayoutProcessor {

	@Override
	public void process(LGraph lGraph,
			IKielerProgressMonitor progressMonitor) {
		Set<LNode> bpmnContainers = lGraph.getProperty(BpmnProperties.CONTAINERS);
		
		if (bpmnContainers != null){
			
			final int CONTAINER_PADDING_LEFT_RIGHT = lGraph.getProperty(BpmnProperties.CONTAINER_PADDING_LEFT_RIGHT);
			
			// Resize container width
	        for (LNode container : bpmnContainers){ 
	           	LNode parentNode = container.getProperty(InternalProperties.PARENT_LNODE);
	           	double graphSizeX = lGraph.getSize().x;
	        	if (parentNode != null){
	        		parentNode.getSize().x = graphSizeX + CONTAINER_PADDING_LEFT_RIGHT * 3;
	            	container.getSize().x = graphSizeX + CONTAINER_PADDING_LEFT_RIGHT * 2;
	        	} 
	        	else container.getSize().x = graphSizeX + CONTAINER_PADDING_LEFT_RIGHT * 3;
	        }
			
			// Move nodes, bendpoints and labels to the right in order to have
	        // enough space on the left for the container labels.
	        for (LNode bpmnContainer : bpmnContainers){ 
	            List<KVector> bendpoints = new ArrayList<KVector>();     
	        	double offsetX = 0;
	        	LGraph localGraph = bpmnContainer.getProperty(InternalProperties.NESTED_LGRAPH);
	        	List<LNode> localNodes = localGraph.getLayerlessNodes();        	
	        	offsetX = CONTAINER_PADDING_LEFT_RIGHT * 2;
	        	for (LNode node : localNodes){
	        		node.getPosition().x = node.getPosition().x + offsetX;
	        		for (LEdge edge : node.getOutgoingEdges()){
	        			// Move labels
	        			for (LLabel label : edge.getLabels()){
	        				label.getPosition().x = label.getPosition().x + offsetX;
	        			}        			
	        			// Move bendpoints
	        			for (KVector bendpoint : edge.getBendPoints()){
	        				bendpoints.add(bendpoint);
	        			}
	        		}
	        	}
				for (KVector bendpoint : bendpoints){
		        	bendpoint.add(new KVector(offsetX, 0));
		        }  
	        }
		}

	}

}
