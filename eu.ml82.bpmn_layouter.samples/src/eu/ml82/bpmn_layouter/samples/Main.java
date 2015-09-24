/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.samples;

import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.KlayLayeredForBpmn;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.properties.PosType;
import eu.ml82.bpmn_layouter.core.utils.Graph;

/*
 * 
 * This example creates a simple graph, layouts it and saves the layouted graph as PNG image.
 * 
 */


public class Main {
	
	public static void main(String[] args) {
		// Top-Level Graph
		LGraph bpmnGraph = new LGraph();
		
		// First Pool
		// Graph
		LGraph poolGraph1 = new LGraph();
		// Node
		LNode pool1 = Utils.createNode(bpmnGraph,0,0,0,0,"Pool 1");
		pool1.setProperty(InternalProperties.NESTED_LGRAPH, poolGraph1); // attach graph to node
		
		// First lane
		// Graph
		LGraph lane1Graph = new LGraph();
		// Graph elements
		LNode startEvent1 = Utils.createNode(lane1Graph,0,0,20,20,"StartEvent 1");
		LNode task1 = Utils.createNode(lane1Graph,0,0,100,80,"Task 1");
		LNode artifact1 = Utils.createNode(lane1Graph,0,0,40,50,"Artifact 1",BpmnElementType.ARTIFACT);
		LNode task7 = Utils.createNode(lane1Graph,0,0,100,80,"Task 7");
		LNode gateway2 = Utils.createNode(lane1Graph,0,0,0,0,"Gateway 2", BpmnElementType.GATEWAY);
		Utils.createEdge(startEvent1, task1, "");
		Utils.createEdge(task1, artifact1, "");
		Utils.createEdge(task7, gateway2, "");
		// Node
		LNode lane1Node = Utils.createNode(poolGraph1,0,0,0,0,"Lane 1");
		lane1Node.setProperty(InternalProperties.NESTED_LGRAPH, lane1Graph);
		
		// Second lane
		// Graph
		LGraph lane2Graph = new LGraph();
		// Graph elements
		LNode task2 = Utils.createNode(lane2Graph,0,0,100,80,"Task 2");
		LNode task3 = Utils.createNode(lane2Graph,0,0,100,80,"Task 3");
		LNode gateway1 = Utils.createNode(lane2Graph,0,0,0,0,"Gateway 1", BpmnElementType.GATEWAY);
		LNode task4 = Utils.createNode(lane2Graph,0,0,100,80,"Task 4");
		LNode task5 = Utils.createNode(lane2Graph,0,0,100,80,"Task 5");
		Utils.createEdge(artifact1, task2, "");
		Utils.createEdge(task1, task2, "");
		Utils.createEdge(task2, task3, "");
		Utils.createEdge(task3, gateway1, "");
		Utils.createEdge(gateway1, task4, "yes");
		Utils.createEdge(gateway1, task5, "no");
		Utils.createEdge(task5, gateway2, "");
		Utils.createEdge(task4, task7, "");
		// Node
		LNode lane2Node = Utils.createNode(poolGraph1,0,100,0,0,"Lane 2");
		lane2Node.setProperty(InternalProperties.NESTED_LGRAPH, lane2Graph);
		
		// Second Pool
		// Graph
		LGraph poolGraph2 = new LGraph();
		// Node
		LNode pool2 = Utils.createNode(bpmnGraph,0,250,0,0,"Pool 2");
		pool2.setProperty(InternalProperties.NESTED_LGRAPH, poolGraph2); // attach graph to node

		LNode task6 = Utils.createNode(poolGraph2, 0,0,100,80,"Task 6");
		Utils.createEdge(task3, task6, "", BpmnElementType.MESSAGE_FLOW);
		
		LNode boundaryEvent1 = Utils.createNode(poolGraph2, 0,0,25,25,"Boundary Event 1");
		LNode task8 = Utils.createNode(poolGraph2, 0,0,100,80,"Task 8");
		Utils.createEdge(boundaryEvent1, task8, "");
		Utils.createEdge(task6, boundaryEvent1, "").setProperty(BpmnProperties.BOUNDARY_EVENT_DUMMY_EDGE, true);
		
		// Layout the graph		
		KlayLayeredForBpmn layouter = new KlayLayeredForBpmn();
		PosType posType = PosType.RELATIVE;
		layouter.doLayout(bpmnGraph, posType);
		
		// Output graph
		Graph.draw(bpmnGraph, "./BPMNLayoutTest.png", posType);
	}

}
