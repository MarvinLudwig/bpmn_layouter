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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.kiml.options.EdgeLabelPlacement;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;

/**
 * 
 * Move source node and store original source node.
 * 
 * <dl>
 *   <dt>Precondition:</dt><dd></dd>
 *   <dt>Postcondition:</dt><dd></dd>
 *   <dt>Slots:</dt><dd>Before phase 2.</dd>
 *   <dt>Same-slot dependencies:</dt><dd>Before BigNodesPreProcessor.</dd>
 * </dl>
 * 
 */

public final class BpmnMessageFlowPreProcessor implements ILayoutProcessor {

    /**
     * {@inheritDoc}
     */
    public void process(final LGraph lGraph, final IKielerProgressMonitor monitor) {
        monitor.begin("BPMN Message flow pre-processor", 1);
        
        Map<LEdge, LPort> edges = new HashMap<LEdge, LPort>();
        List<LEdge> messageFlows = lGraph.getProperty(BpmnProperties.MESSAGE_FLOWS);
        if (messageFlows != null){
			for (LEdge edge : messageFlows){			
				LNode node = edge.getSource().getNode();
				Iterator<LEdge> edgeIterator = node.getIncomingEdges().iterator();
				if (edgeIterator.hasNext()){
	    			LPort port = edgeIterator.next().getSource();
	    			edge.setProperty(BpmnProperties.ORIGINAL_SOURCE_NODE, edge.getSource().getNode());
	    			edges.put(edge, port);
	    			// Message flow edge labels are not to be handled by
	    			// the LabelDummyProcessor.
	    			Iterator<LLabel> labels = edge.getLabels().iterator();
	    			while(labels.hasNext()){
	    				LLabel label = labels.next();
	    				EdgeLabelPlacement labelPlacement = label.getProperty(LayoutOptions.EDGE_LABEL_PLACEMENT);
	    				if (labelPlacement == EdgeLabelPlacement.CENTER){
	    					edge.setProperty(BpmnProperties.LABEL, label);
	    					labels.remove();
	    				}
	    			}
				}
			}
        }
			
        for (Entry<LEdge, LPort> entry : edges.entrySet()){
        	LEdge edge = entry.getKey();
			edge.setSource(entry.getValue());
        }
        
    }

}
