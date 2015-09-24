/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors.artifacts;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import de.cau.cs.kieler.klay.layered.properties.PortType;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.utils.Utils;

/**
 * 
 * Create dummy edges
 *
 * <dl>
 *   <dt>Precondition:</dt><dd>none</dd>
 *   <dt>Postcondition:</dt><dd>none</dd>
 *   <dt>Slots:</dt><dd>Before phase 1.</dd>
 *   <dt>Same-slot dependencies:</dt><dd></dd>
 * </dl>
 */

public final class BpmnArtifactPreProcessor implements ILayoutProcessor {

    public void process(final LGraph layeredGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("Artifact pre-processing", 1);
        
        List<BpmnArtifact> artifacts = new LinkedList<BpmnArtifact>();
        Iterator<LNode> nodeIterator = layeredGraph.getLayerlessNodes().iterator();
        while (nodeIterator.hasNext()) {
            LNode node = nodeIterator.next();
            BpmnElementType elementType = node.getProperty(BpmnProperties.ELEMENT_TYPE);
            if (elementType == BpmnElementType.ARTIFACT){
        		BpmnArtifact artifact = new BpmnArtifact();
    			artifacts.add(artifact);
    			artifact.node = node;
    			// only artifacts with at least one incoming and one outgoing edge
            	if (node.getIncomingEdges().iterator().hasNext() 
            		&& node.getOutgoingEdges().iterator().hasNext()){	
                	// connect involved tasks directly by dummy edges
            		Iterator<LEdge> incomingEdges = node.getIncomingEdges().iterator();
            		List<LEdge> dummyEdges = new LinkedList<LEdge>();
            		while (incomingEdges.hasNext()) {
            			LEdge incomingEdge = incomingEdges.next();
            			Iterator<LEdge> outgoingEdges = node.getOutgoingEdges().iterator();
            			while (outgoingEdges.hasNext()) {
            				LEdge outgoingEdge = outgoingEdges.next();
                			LEdge dummyEdge = new LEdge();
                			dummyEdge.setSource(incomingEdge.getSource());
                			dummyEdge.setTarget(outgoingEdge.getTarget());
							dummyEdge.setProperty(BpmnProperties.ARTIFACT_DUMMY_EDGE, true);
                			dummyEdges.add(dummyEdge);
            			}
            		}
            		artifact.dummyEdges = dummyEdges;
                }
            	// for all artifacts
        		for (LEdge edge : node.getIncomingEdges()) artifact.addOriginalEdge(edge, PortType.INPUT);
        		for (LEdge edge : node.getOutgoingEdges()) artifact.addOriginalEdge(edge, PortType.OUTPUT);
        		Utils.removeIncomingEdges(node);
        		Utils.removeOutgoingEdges(node);
        		nodeIterator.remove();
        		// remove artifact from its container
        		LNode container = artifact.node.getProperty(InternalProperties.PARENT_LNODE);
        		if (container != null){
	        		container.getProperty(InternalProperties.NESTED_LGRAPH).getLayerlessNodes().remove(artifact.node);
	        		artifact.node.setProperty(InternalProperties.PARENT_LNODE,null);
        		}
            }
        }
        layeredGraph.setProperty(BpmnProperties.ARTIFACTS, artifacts);
    }


}