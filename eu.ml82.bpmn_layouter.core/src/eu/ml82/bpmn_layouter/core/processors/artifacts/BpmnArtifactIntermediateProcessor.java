/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.ml82.eu
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core.processors.artifacts;

import java.util.Iterator;
import java.util.List;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode.NodeType;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;

/**
 * 
 * Delete dummy edges before node placer. 
 * Dummy edges were needed for crossing minimization.
 *
 * <dl>
 *   <dt>Precondition:</dt><dd>none</dd>
 *   <dt>Postcondition:</dt><dd>none</dd>
 *   <dt>Slots:</dt><dd>After phase 3.</dd>
 *   <dt>Same-slot dependencies:</dt><dd></dd>
 * </dl>
 */

public final class BpmnArtifactIntermediateProcessor implements ILayoutProcessor {

    /**
     * {@inheritDoc}
     */
    public void process(final LGraph layeredGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("Artifact intermediate processing 2", 1);
        
        List<BpmnArtifact> artifacts = layeredGraph.getProperty(BpmnProperties.ARTIFACTS);
        
        if (artifacts != null){
	        for (BpmnArtifact artifact : artifacts){
	        	if (artifact.dummyEdges != null)
			        for (LEdge edge : artifact.dummyEdges){
						removeLongEdge(edge);
			        }
	        		// We don't need them any more.
	        		artifact.dummyEdges = null; 
	    	}
    	}
    }
    
    private void removeLongEdge(LEdge edge){
    	LPort source = edge.getSource();
    	Iterator<LEdge> outgoing = source.getOutgoingEdges().iterator();
		while (outgoing.hasNext()){
			LEdge cur = outgoing.next();
			if (cur == edge) outgoing.remove();
		}
    	
    	LPort target = edge.getTarget();
    	Iterator<LEdge> incoming = target.getIncomingEdges().iterator();
		while (incoming.hasNext()){
			LEdge cur = incoming.next();
			if (cur == edge){
				incoming.remove();
			}
		}
    	NodeType nodeType = target.getNode().getNodeType();
		if (nodeType == NodeType.LONG_EDGE){
			removeLongEdge (target.getNode().getOutgoingEdges().iterator().next());
		}
    }
    

}
