package eu.ml82.bpmn_layouter.core.utils;

import de.cau.cs.kieler.klay.layered.graph.LNode;

public class BpmnContainer {
	
public Integer height;
public LNode graph;

public BpmnContainer (LNode graph, Integer height){
	this.graph = graph;
	this.height = height;
}

}
