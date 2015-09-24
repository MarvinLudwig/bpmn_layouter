package eu.ml82.bpmn_layouter.core.processors;

import java.util.HashSet;
import java.util.Set;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;

/**
 * 
 * Processor that is only needed if we have elements in containers.
 * 
 * Make the graph a graph with absolute node positions,
 * as following processors assume absolute node positions.
 *
 */

public class BpmnContainerPreProcessor implements ILayoutProcessor {
	Set<LNode> topLevelContainers;
	
	@Override
	public void process(LGraph lGraph,
			IKielerProgressMonitor progressMonitor) {
		Set<LNode> containers = lGraph.getProperty(BpmnProperties.CONTAINERS);
		if (containers != null){
			// Get top level container(s)
			topLevelContainers = new HashSet<LNode>();
			for (LNode container : containers){
				getTopLevel(container);
			}
			//
			for (LNode container : topLevelContainers){
				LGraph nestedGraph = container.getProperty(InternalProperties.NESTED_LGRAPH);
				makeAbsolute(nestedGraph, container.getPosition());
			}
		}
	}
	
	/**
	 * Make node positions absolute.
	 * ("Virtually" remove nodes from their containers)
	 */
	private void makeAbsolute (LGraph nestedGraph, KVector offset){
		for (LNode node : nestedGraph.getLayerlessNodes()){
			nestedGraph = node.getProperty(InternalProperties.NESTED_LGRAPH);
			if (nestedGraph != null) {
				KVector newOffset = new KVector(KVector.sum(offset, node.getPosition()));
				makeAbsolute(nestedGraph, newOffset);
			}
			node.getPosition().add(offset);
		}
	}

	private void getTopLevel(LNode container){
		LNode parent = container.getProperty(InternalProperties.PARENT_LNODE);
		if (parent == null) topLevelContainers.add(container);
		else getTopLevel (parent);
	}

}
