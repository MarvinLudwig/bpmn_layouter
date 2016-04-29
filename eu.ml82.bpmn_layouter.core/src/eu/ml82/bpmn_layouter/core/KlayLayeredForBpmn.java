/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import de.cau.cs.kieler.core.alg.BasicProgressMonitor;
import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.math.KVectorChain;
import de.cau.cs.kieler.kiml.options.Direction;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.kiml.options.PortSide;
import de.cau.cs.kieler.klay.layered.ILayoutProcessor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LGraphUtil;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.graph.Layer;
import de.cau.cs.kieler.klay.layered.intermediate.BigNodesIntermediateProcessor;
import de.cau.cs.kieler.klay.layered.intermediate.BigNodesPostProcessor;
import de.cau.cs.kieler.klay.layered.intermediate.BigNodesPreProcessor;
import de.cau.cs.kieler.klay.layered.intermediate.EndLabelProcessor;
import de.cau.cs.kieler.klay.layered.intermediate.LabelAndNodeSizeProcessor;
import de.cau.cs.kieler.klay.layered.intermediate.LabelDummyInserter;
import de.cau.cs.kieler.klay.layered.intermediate.LabelDummyRemover;
import de.cau.cs.kieler.klay.layered.intermediate.LabelDummySwitcher;
import de.cau.cs.kieler.klay.layered.intermediate.LabelSideSelector;
import de.cau.cs.kieler.klay.layered.intermediate.LayerSizeAndGraphHeightCalculator;
import de.cau.cs.kieler.klay.layered.intermediate.LongEdgeJoiner;
import de.cau.cs.kieler.klay.layered.intermediate.LongEdgeSplitter;
import de.cau.cs.kieler.klay.layered.intermediate.NodeMarginCalculator;
import de.cau.cs.kieler.klay.layered.intermediate.PortSideProcessor;
import de.cau.cs.kieler.klay.layered.intermediate.ReversedEdgeRestorer;
import de.cau.cs.kieler.klay.layered.p1cycles.GreedyCycleBreaker;
import de.cau.cs.kieler.klay.layered.p2layers.NetworkSimplexLayerer;
import de.cau.cs.kieler.klay.layered.p3order.LayerSweepCrossingMinimizer;
import de.cau.cs.kieler.klay.layered.p4nodes.bk.BKNodePlacer;
import de.cau.cs.kieler.klay.layered.p5edges.OrthogonalEdgeRouter;
import de.cau.cs.kieler.klay.layered.properties.FixedAlignment;
import de.cau.cs.kieler.klay.layered.properties.InteractiveReferencePoint;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import de.cau.cs.kieler.klay.layered.properties.Properties;
import de.cau.cs.kieler.klay.layered.properties.Spacings;
import eu.ml82.bpmn_layouter.core.processors.BpmnContainerPostProcessor;
import eu.ml82.bpmn_layouter.core.processors.BpmnContainerPreProcessor;
import eu.ml82.bpmn_layouter.core.processors.BpmnGatewayProcessor;
import eu.ml82.bpmn_layouter.core.processors.BpmnMessageFlowIntermediateProcessor;
import eu.ml82.bpmn_layouter.core.processors.BpmnMessageFlowPostProcessor;
import eu.ml82.bpmn_layouter.core.processors.BpmnMessageFlowPreProcessor;
import eu.ml82.bpmn_layouter.core.processors.BpmnPortPostProcessor;
import eu.ml82.bpmn_layouter.core.processors.BpmnPortPreProcessor;
import eu.ml82.bpmn_layouter.core.processors.artifacts.BpmnArtifact;
import eu.ml82.bpmn_layouter.core.processors.artifacts.BpmnArtifactIntermediateProcessor;
import eu.ml82.bpmn_layouter.core.processors.artifacts.BpmnArtifactPostProcessor;
import eu.ml82.bpmn_layouter.core.processors.artifacts.BpmnArtifactPreProcessor;
import eu.ml82.bpmn_layouter.core.processors.boundary_events.BpmnBoundaryEventIntermediateProcessor1;
import eu.ml82.bpmn_layouter.core.processors.boundary_events.BpmnBoundaryEventIntermediateProcessor2;
import eu.ml82.bpmn_layouter.core.processors.boundary_events.BpmnBoundaryEventPostProcessor;
import eu.ml82.bpmn_layouter.core.processors.boundary_events.BpmnBoundaryEventPreProcessor;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.properties.PosType;
import eu.ml82.bpmn_layouter.core.utils.Maps;
import eu.ml82.bpmn_layouter.core.utils.Utils;


/**
*
* This is the place where the magic happens :)
* Layout processors are called from here.
*
*/

public final class KlayLayeredForBpmn {
	

    private Set<LNode> flowNodes;    
    // BPMN containers are pools, lanes or subprocesses
    private Map<LNode,Double> bpmnContainers_pre;
    private Set<LNode> subProcesses;
    private List<LEdge> messageFlows;
    IKielerProgressMonitor monitor;
	
    /**
     * 
     * Layout LGraph. 
     * 
     * The new node positions are either relative (PosType.RELATIVE)
     * or absolute (PosType.ABSOLUTE).
     * 
     */
	public void doLayout(final LGraph lgraph, PosType posType) {
		doLayout(lgraph,posType,null);
	}

    public void doLayout(final LGraph lGraph, PosType posType, IKielerProgressMonitor monitor) {

        this.monitor = monitor;
    	
    	if (monitor == null) {
        	monitor = new BasicProgressMonitor(0);
        }
        monitor.begin("Layered layout", 1);

        lGraph
		.setProperty(Properties.INTERACTIVE_REFERENCE_POINT, InteractiveReferencePoint.CENTER)
		.setProperty(Properties.FIXED_ALIGNMENT,FixedAlignment.BALANCED)
		.setProperty(BpmnProperties.CONTAINER_PADDING_TOP_BOTTOM, 15)
        .setProperty(BpmnProperties.CONTAINER_PADDING_LEFT_RIGHT, 30)
        .setProperty(BpmnProperties.CONTAINER_SPACING, 25);
        
        if (lGraph.getProperty(LayoutOptions.SPACING) == null) 
        	lGraph.setProperty(LayoutOptions.SPACING,40.0f);

        configureGraphProperties(lGraph);

        flowNodes = new LinkedHashSet<LNode>();    
        bpmnContainers_pre = new HashMap<LNode,Double>();
        subProcesses = new HashSet<LNode>();
        messageFlows = new LinkedList<LEdge>(); // filled during dive
        
        List<LNode> originalTopLevelNodes =  new LinkedList<LNode>(lGraph.getLayerlessNodes());
        boolean simpleLayout = dive(lGraph,null,0);
        
        // In cases where a pool accidentally contains a flow element
        // and lanes, we must remove that pool from the container list.
        List<LNode> containerParents = new ArrayList<LNode>();
        for (LNode container : bpmnContainers_pre.keySet()){
        	LNode parent = container.getProperty(InternalProperties.PARENT_LNODE);
        	containerParents.add(parent);
        }
        for (LNode containerParent : containerParents){
        	bpmnContainers_pre.remove(containerParent);
        }
        
        layoutSubProcesses(lGraph);

        lGraph.setProperty(BpmnProperties.MESSAGE_FLOWS,messageFlows); // Important: After sub-process layouting
                               
        if (simpleLayout){ // No pools and lanes
        	doLayout(1,lGraph,monitor);
        	doLayout(2,lGraph,monitor);
        	lGraph.getOffset().y = 0;
        	lGraph.getOffset().x = 0;
        	doLayout(3,lGraph,monitor);
        }
        else{ // Layout with pools and lanes
        	doBpmnLayout(lGraph, monitor);
        }
        
        // Move all nodes away from the layers
        List<LNode> layerlessNodes = lGraph.getLayerlessNodes();
        layerlessNodes.clear();
        layerlessNodes.addAll(originalTopLevelNodes);
        
        for (Layer layer : lGraph) {
            layer.getNodes().clear();
        }
        lGraph.getLayers().clear();
        
        // Remove remaining dummy nodes and edges
        cleanGraph(lGraph);        
        
        // Calculate absolute position of sub-process children
        subProcessPostProcessing();
        
        // Debugging: Output graph as PNG image   
        // Graph.draw(lGraph, "./graph.png", PosType.ABSOLUTE);
                
        // Make node positions relative
        if (posType == PosType.RELATIVE) RelativePositions.make(lGraph, subProcesses);

        monitor.done();
        System.out.println("Layout done");
    }

	/** 
     * Remove remaining dummy nodes and labels
     */
	private void cleanGraph(LGraph graph) {
		Iterator<LNode> nodes = graph.getLayerlessNodes().iterator();
		while (nodes.hasNext()){
			LNode node = nodes.next();
			LGraph nestedGraph = node.getProperty(InternalProperties.NESTED_LGRAPH);
			if (nestedGraph != null) cleanGraph(nestedGraph);
			else {
				if (node.getProperty(InternalProperties.ORIGIN) == null) 
					nodes.remove();
				Iterator<LLabel> labels = node.getLabels().iterator();
				while (labels.hasNext()){
					LLabel label = labels.next();
					if (label.getProperty(InternalProperties.ORIGIN) == null) 
						labels.remove();
				}
			}
		}
    }

    
    /**
    * Dive into the graph tree to perform these actions:<br>
    * 1.) Get bottom level nodes / nodes that have no children<br>
    * 2.) Get pools and lanes<br>
    * 3.) Check whether we can do a "simple" layout (no pools or lanes)<br>
    */
    private boolean dive(LGraph graph,LNode parent,double parentYPos) {
    	boolean simpleLayout = true;
    	Object originDummy = new Object();
        for (LNode node : graph.getLayerlessNodes()) {
            LGraph nestedGraph = node.getProperty(InternalProperties.NESTED_LGRAPH);
        	node.setProperty(InternalProperties.PARENT_LNODE, parent);
        	
        	// Origin is set when importing a KGraph.
        	// If we create the LGraph directly, we need to set it here for
        	// - Proper working of Big Nodes processing
        	// - Deletion of dummy nodes after layouting
        	Object origin = node.getProperty(InternalProperties.ORIGIN);
        	if (origin == null) node.setProperty(InternalProperties.ORIGIN, originDummy);
        	for (LLabel label : node.getLabels()){
            	origin = label.getProperty(InternalProperties.ORIGIN);
            	if (origin == null) label.setProperty(InternalProperties.ORIGIN, originDummy);
        	}

        	Double containerYPos = null;		
            if (nestedGraph != null && nestedGraph.getLayerlessNodes().size() > 0) { // the node has children, must be a lane or pool
            	if (parent == null) 
            		containerYPos = node.getPosition().y;
            	else  containerYPos = parentYPos + node.getPosition().y;

            	BpmnElementType elementType = node.getProperty(BpmnProperties.ELEMENT_TYPE);
            	if (elementType != null && elementType == BpmnElementType.SUBPROCESS){
                	flowNodes.add(node);
                	addMessageFlow(node);
                	subProcesses.add(node);
                	node.setProperty(LayoutOptions.SIZE_CONSTRAINT, null);
                	if (parent != null) {
                    	// add parent to container list
                    	bpmnContainers_pre.put(parent,parentYPos);
                	}
                	for (LNode subProcessChildNode : nestedGraph.getLayerlessNodes()){
                		subProcessChildNode.setProperty(InternalProperties.PARENT_LNODE, node);
                    	origin = subProcessChildNode.getProperty(InternalProperties.ORIGIN);
                    	if (origin == null) subProcessChildNode.setProperty(InternalProperties.ORIGIN, originDummy);
                	}
            	}	
            	else {
                	simpleLayout = false;
            		dive(nestedGraph, node, containerYPos);           	
            	}
            }
            else { // we are at the bottom of the tree (reached a flow node)
            	flowNodes.add(node);
            	addMessageFlow(node);
            	if (parent != null) {
					// add parent to container list
                	bpmnContainers_pre.put(parent,parentYPos);
            	}
            } 
        }
        
        return simpleLayout;
    }    
    
    private void addMessageFlow(LNode node){
    	for (LEdge edge : node.getOutgoingEdges()){
    		BpmnElementType flowType = edge.getProperty(BpmnProperties.ELEMENT_TYPE);
    		if (flowType == BpmnElementType.MESSAGE_FLOW) messageFlows.add(edge);
    	}
    }
        
    /**
     * Layout with pools and lanes split into 3 parts
     * Part 1: Complete graph
     * Part 2: "Local" graphs within pool or lane
     * Part 3: Complete graph
     */ 
    private void doBpmnLayout(final LGraph graph, final IKielerProgressMonitor monitor) {
        boolean monitorStarted = monitor.isRunning();
        if (!monitorStarted) {
            monitor.begin("Component Layout", 1);
        }
    	
        final double CONTAINER_PADDING_TOP_BOTTOM = graph.getProperty(BpmnProperties.CONTAINER_PADDING_TOP_BOTTOM);
        final double CONTAINER_PADDING_LEFT_RIGHT = graph.getProperty(BpmnProperties.CONTAINER_PADDING_LEFT_RIGHT);
        final double CONTAINER_SPACING = graph.getProperty(BpmnProperties.CONTAINER_SPACING);
    	
        /////////// Layout part 1
                
    	// Prepare graph for layout part 1 (layering) by creating a flat graph
        // with only flow nodes / no containers
        graph.getLayerlessNodes().clear();
        for (LNode node : flowNodes){
        	graph.getLayerlessNodes().add(node);
        }         
        
        // Layering processors
                
        doLayout(1,graph,monitor);
        
    	// Add new nodes (e.g. big node dummy nodes, edge label dummy nodes)
    	for(Layer layer : graph.getLayers()){
    		for(LNode node : layer.getNodes()){
				if (!flowNodes.contains(node)){ 
					// We need to attached the dummy node to
					// the corresponding BPMN container
					LNode parentNode = node.getProperty(InternalProperties.PARENT_LNODE);
					LNode predecessor = node;
					while (parentNode == null){
						// Get node's predecessor and check whether it has
						// the parent node property set
						if (predecessor.getIncomingEdges() != null && predecessor.getIncomingEdges().iterator().hasNext()){
							predecessor = predecessor.getIncomingEdges().iterator().next().getSource().getNode();
							parentNode = predecessor.getProperty(InternalProperties.PARENT_LNODE);
						}
						else break;
					}
					if (parentNode != null){
						node.setProperty(InternalProperties.PARENT_LNODE,parentNode);
						LGraph localGraph = parentNode.getProperty(InternalProperties.NESTED_LGRAPH);
						localGraph.getLayerlessNodes().add(node);
					}
				}
    		}
    	}
                
        /////////// Layout part 2: layout local graph
    	// Make copy of layers and layer nodes
        Map<Layer, LinkedHashSet<LNode>> originalLayerNodes = new LinkedHashMap<Layer,LinkedHashSet<LNode>>();
        Map<Layer, LinkedHashSet<LNode>> layoutedLayerNodes = new LinkedHashMap<Layer,LinkedHashSet<LNode>>();
    	for(Layer layer : graph.getLayers()){
    		originalLayerNodes.put(layer, new LinkedHashSet<LNode>(layer.getNodes()));
    		layoutedLayerNodes.put(layer, new LinkedHashSet<LNode>());
    	}
   	
    	double lanePosY = 0;
    	double poolPosY = 0;
    	double laneSizeY = 0;
    	double poolSizeY = 0;
    	LNode currentPool = null; 

    	List<LEdge> crossContainerEdges = new LinkedList<LEdge>();
    	List<BpmnArtifact> artifacts = graph.getProperty(BpmnProperties.ARTIFACTS);
    	Map<BpmnArtifact,List<LEdge>> tmpArtifacts = new HashMap<BpmnArtifact,List<LEdge>>();
    	for (BpmnArtifact artifact : artifacts){
    		if (artifact.dummyEdges != null)
    			tmpArtifacts.put(artifact, new LinkedList<LEdge>(artifact.dummyEdges));
    	}
    	
    	Set<LNode> bpmnContainers = Maps.getKeysSortedByValue(bpmnContainers_pre);
        graph.setProperty(BpmnProperties.CONTAINERS,bpmnContainers);
        
    	// Layout each container (pool or lane)
        for (LNode bpmnContainer : bpmnContainers){   
        	
        	LGraph localGraph = bpmnContainer.getProperty(InternalProperties.NESTED_LGRAPH);
        	List<LNode> localNodes = localGraph.getLayerlessNodes();        	
        	LNode parentNode = bpmnContainer.getProperty(InternalProperties.PARENT_LNODE);
        	                	
        	//// Prepare local graph
        	
        	// Remove all non-local nodes and edges from the graph 
        	crossContainerEdges.addAll(new LocalGraphHandler().prepareGraph(graph, localNodes));
        	// Remove empty layers, as they would cause errors during crossing minimization        
        	Iterator<Layer> layerIterator = graph.getLayers().iterator();
            while (layerIterator.hasNext()){
            	Layer layer = layerIterator.next();
            	if (layer.getNodes().size() == 0) layerIterator.remove();
            }
            
            // Prepare artifacts - remove and store non local dummy edges
			for (BpmnArtifact artifact : artifacts){
				if (artifact.dummyEdges != null){
					Iterator<LEdge> dummyEdges = artifact.dummyEdges.iterator();
	            	while (dummyEdges.hasNext()){
	            		LEdge dummyEdge = dummyEdges.next();
	            		if (dummyEdge.getSource().getNode().getProperty(InternalProperties.PARENT_LNODE) != bpmnContainer
	            			|| dummyEdge.getTarget().getNode().getProperty(InternalProperties.PARENT_LNODE) != bpmnContainer){
	            			dummyEdges.remove();
	            		}
	            	}
				}
            }
                
            // DO LAYOUT PART 2
            doLayout(2,graph,monitor);
            
            // Restore artifact dummy edges
			for (Entry<BpmnArtifact, List<LEdge>> entry : tmpArtifacts.entrySet()){
				entry.getKey().dummyEdges = entry.getValue();
            }
        	
            // Add y offset for container height
            int minY = Integer.MAX_VALUE; 
        	for(Layer layer : graph.getLayers()){
        		for(LNode node : layer.getNodes()){
        			minY = (int) Math.min(minY, node.getPosition().y);
        		}
        	}
        	for(Layer layer : graph.getLayers()){
        		for(LNode node : layer.getNodes()){
        			node.getPosition().y = node.getPosition().y - minY + CONTAINER_PADDING_TOP_BOTTOM;
        		}
        	}
        	        	
        	// Set container position 
        	LNode nextPool;
        	if (parentNode != null) nextPool = parentNode;
        	else nextPool = bpmnContainer;
        	
        	if (nextPool != currentPool){
            	lanePosY = 0;
            	poolSizeY = 0;
            	if (parentNode != null){
                	bpmnContainer.getPosition().x = CONTAINER_PADDING_LEFT_RIGHT;
                	bpmnContainer.getPosition().y = lanePosY;
            		parentNode.getPosition().x = 0;
            		parentNode.getPosition().y = poolPosY;
            	}
            	else {
                	bpmnContainer.getPosition().x = 0;
                	bpmnContainer.getPosition().y = poolPosY;
            	}
            	poolPosY += CONTAINER_SPACING;
        	}
        	else if (parentNode != null) {
            	bpmnContainer.getPosition().x = CONTAINER_PADDING_LEFT_RIGHT;
            	bpmnContainer.getPosition().y = lanePosY;
        	}
        	
        	currentPool = nextPool;
        	lanePosY = lanePosY + graph.getSize().y + 2 * CONTAINER_PADDING_TOP_BOTTOM - 1;
        	poolPosY = poolPosY + graph.getSize().y + 2 * CONTAINER_PADDING_TOP_BOTTOM;
        	
        	// Set container size
        	laneSizeY = graph.getSize().y + 2 * CONTAINER_PADDING_TOP_BOTTOM;
        	bpmnContainer.getSize().y = laneSizeY;
        	if (parentNode != null){
        		poolSizeY = poolSizeY + laneSizeY - 1;
        		parentNode.getSize().y = poolSizeY + 1;
        	}
        	
        	// Add new nodes from part 2 (e.g. long edge dummy nodes)
        	for(Layer layer : graph.getLayers()){
        		for(LNode node : layer.getNodes()){
					if (!localNodes.contains(node)){ 
						HashSet<LNode> originalLayer = originalLayerNodes.get(layer);	
        				originalLayer.add(node);
					}
				// Add nodes to set of layouted nodes
				// Needed for keeping node order of layouted nodes when restoring original graph
				LinkedHashSet<LNode> nodeSet = layoutedLayerNodes.get(layer);
				nodeSet.addAll(layer.getNodes());
				}
        	}
        	
        	// Restore original graph
        	// It's important to keep the in-layer node order
        	graph.getLayers().clear();
        	for(Entry<Layer, LinkedHashSet<LNode>> entry : originalLayerNodes.entrySet()){
        		Layer originalLayer = entry.getKey();
        		HashSet<LNode> originalNodes = entry.getValue();
        		graph.getLayers().add(originalLayer);
        		
        		// add nodes that are already layouted (to keep order)
        		LinkedHashSet<LNode> tmpNodeSet = new LinkedHashSet<LNode>(layoutedLayerNodes.get(originalLayer));
        		//add remaining nodes
        		tmpNodeSet.addAll(originalNodes);
        		
        		originalLayer.getNodes().clear();
        		originalLayer.getNodes().addAll(tmpNodeSet);
    		}
        }
        
        /////////// Layout part 3
        graph.getLayerlessNodes().clear();
        // Restore container-crossing edges
        for (LEdge edge : crossContainerEdges){
        	BpmnElementType flowType = edge.getProperty(BpmnProperties.ELEMENT_TYPE);
        	LNode sourceNode = edge.getSource().getNode();
        	LNode targetNode = edge.getTarget().getNode();
    		if (flowType == BpmnElementType.MESSAGE_FLOW){
	        	LPort southPort = new LPort(); 
	        	southPort.setSide(PortSide.SOUTH);
	        	LPort northPort = new LPort(); 
	        	northPort.setSide(PortSide.NORTH);
	        	southPort.setNode(sourceNode);
	        	northPort.setNode(targetNode);
	        	edge.setSource(southPort);
	        	edge.setTarget(northPort);
    		}
    		else {
    			edge.setSource(Utils.getPort(sourceNode, edge.getSource().getSide()));
    			edge.setTarget(Utils.getPort(targetNode, edge.getTarget().getSide()));
    		}
        }
        
        doLayout(3,graph,monitor);
        
        // Resize graph
        graph.getSize().x = graph.getSize().x + 100;
        graph.getSize().y = poolPosY;
        graph.getOffset().y = 0;
    }

    // The original KlayLayered has a very flexible layout processor management.
    // For a BPMN diagram we know what we need, so we create a fixed layout processor setup below.
    // BE CAREFUL: The order is important. Changing processor positions will most likely break 
    //             parts of the layout.
    private void doLayout (int part, LGraph graph, IKielerProgressMonitor monitor){
		List<ILayoutProcessor> algorithms = new LinkedList<ILayoutProcessor>();
		if (part == 1){
			algorithms.add(new BpmnArtifactPreProcessor());
	        algorithms.add(new GreedyCycleBreaker()); // phase 1
	        algorithms.add(new PortSideProcessor());
			algorithms.add(new BpmnMessageFlowPreProcessor());
			algorithms.add(new BpmnBoundaryEventPreProcessor());
			algorithms.add(new BigNodesPreProcessor());
	        algorithms.add(new LabelDummyInserter());
	        algorithms.add(new NetworkSimplexLayerer()); // phase
			algorithms.add(new BpmnMessageFlowIntermediateProcessor());
			algorithms.add(new BpmnBoundaryEventIntermediateProcessor1());
			algorithms.add(new BigNodesIntermediateProcessor());
			algorithms.add(new LongEdgeSplitter());
		}
		else if (part == 2){
			// Part 2 must not contain processors that
			// change node to layer assignments!
	        algorithms.add(new LabelDummySwitcher());
			algorithms.add(new LayerSweepCrossingMinimizer()); // phase 3
			algorithms.add(new LabelAndNodeSizeProcessor());
			algorithms.add(new NodeMarginCalculator());
			algorithms.add(new LabelSideSelector());
			algorithms.add(new BpmnArtifactIntermediateProcessor());
			algorithms.add(new BpmnPortPreProcessor());
			//algorithms.add(new GreedySwitchProcessor()); TODO: Activate?
			algorithms.add(new BKNodePlacer()); // phase 4
			algorithms.add(new LayerSizeAndGraphHeightCalculator());		
			algorithms.add(new BpmnBoundaryEventIntermediateProcessor2());
		}
		else if (part == 3){
			// LayerSizeAndGraphHeightCalculator is called twice. In part 2
			// in order to calculate the height of the containers. And in part 3
			// again to calculate the layer size over all containers.	
			algorithms.add(new BpmnPortPostProcessor()); 
			algorithms.add(new BpmnContainerPreProcessor());
			// From here node positions are absolute
			algorithms.add(new LayerSizeAndGraphHeightCalculator());	        
			algorithms.add(new OrthogonalEdgeRouter()); // phase 5
			algorithms.add(new BigNodesPostProcessor());
			algorithms.add(new LongEdgeJoiner());
			algorithms.add(new ReversedEdgeRestorer());
			algorithms.add(new LabelDummyRemover());
			algorithms.add(new EndLabelProcessor());
			algorithms.add(new BpmnBoundaryEventPostProcessor());
			algorithms.add(new BpmnArtifactPostProcessor());
			algorithms.add(new BpmnContainerPostProcessor());
			// From here all nodes are on their final position
			algorithms.add(new BpmnGatewayProcessor());
			algorithms.add(new BpmnMessageFlowPostProcessor());
		}
        
        // Invoke each layout processor
        for (ILayoutProcessor processor : algorithms) {
        	if (monitor.isCanceled()) {
               return;
        	}
			processor.process(graph, monitor.subTask(0));
        }
    }   
    
    /**
     * - Calculate sub-processes children's absolute positions
     * - Make all layered nodes layerless
     */
	private void subProcessPostProcessing() {
	   for (LNode subProcess : subProcesses){
        	LGraph subGraph = subProcess.getProperty(InternalProperties.NESTED_LGRAPH);
        	List<LNode> layerlessNodes = subGraph.getLayerlessNodes();
        	
        	// Calculate offset
        	KVector offset = new KVector();
        	offset.x = subProcess.getPosition().x + subGraph.getProperty(BpmnProperties.CONTAINER_PADDING_LEFT_RIGHT);
        	offset.y = subProcess.getPosition().y + subGraph.getProperty(BpmnProperties.CONTAINER_PADDING_TOP_BOTTOM);
        	
        	for (Layer layer : subGraph.getLayers()){
        		for (LNode node : layer.getNodes()){
        			KVector nodePos = node.getPosition();
        			nodePos.add(offset);
        			layerlessNodes.add(node);
        			for (LEdge edge : node.getOutgoingEdges()){
        				// Adjust bendpoints
        				KVectorChain bendpoints = edge.getBendPoints();
        				for (KVector bendpoint : bendpoints){
        					bendpoint.add(offset);
        				}
        			}
        		}
        		
        	}
        }
	}

	/**
	 * 
	 * Perform complete layout on all sub-processes.
	 * Thereafter we have the sub-process size and we can
	 * handle it as any other node. 
	 */
	private void layoutSubProcesses(LGraph lGraph) {
		// Layout sub-processes
	    for (LNode subProcess : subProcesses){
	    	LGraph subGraph = subProcess.getProperty(InternalProperties.NESTED_LGRAPH);
	    	subGraph.getAllProperties().putAll(lGraph.getAllProperties());
	    	doLayout(1,subGraph,monitor);
	    	doLayout(2,subGraph,monitor);
	    	doLayout(3,subGraph,monitor);
	    	subProcess.getSize().x = subGraph.getSize().x + 2*lGraph.getProperty(BpmnProperties.CONTAINER_PADDING_LEFT_RIGHT);
	    	subProcess.getSize().y = subGraph.getSize().y + 2*lGraph.getProperty(BpmnProperties.CONTAINER_PADDING_TOP_BOTTOM);
	    }
	}
    
	////////////////////////////////////////////////////////////////////////////////
	// Graph Preprocessing (Property Configuration)
	
	/** the minimal spacing between edges, so edges won't overlap. */
	private static final float MIN_EDGE_SPACING = 2.0f;
	
	/**
     * Set special layout options for the layered graph.
     * 
     * @param lgraph a new layered graph
     */
	private void configureGraphProperties(final LGraph lgraph) {
        // check the bounds of some layout options
        lgraph.checkProperties(InternalProperties.SPACING, InternalProperties.BORDER_SPACING,
                Properties.THOROUGHNESS, InternalProperties.ASPECT_RATIO);
        
        float spacing = lgraph.getProperty(InternalProperties.SPACING);
        if (lgraph.getProperty(Properties.EDGE_SPACING_FACTOR) * spacing < MIN_EDGE_SPACING) {
            // Edge spacing is determined by the product of object spacing and edge spacing factor.
            // Make sure the resulting edge spacing is at least 2 in order to avoid overlapping edges.
            lgraph.setProperty(Properties.EDGE_SPACING_FACTOR, MIN_EDGE_SPACING / spacing);
        }
        
        Direction direction = lgraph.getProperty(LayoutOptions.DIRECTION);
        if (direction == Direction.UNDEFINED) {
            lgraph.setProperty(LayoutOptions.DIRECTION, LGraphUtil.getDirection(lgraph));
        }
        
        // set the random number generator based on the random seed option
        Integer randomSeed = lgraph.getProperty(Properties.RANDOM_SEED);
        if (randomSeed == 0) {
            lgraph.setProperty(InternalProperties.RANDOM, new Random());
        } else {
            lgraph.setProperty(InternalProperties.RANDOM, new Random(randomSeed));
        }
        
        // pre-calculate spacing information
        Spacings spacings = new Spacings(lgraph);
        lgraph.setProperty(InternalProperties.SPACINGS, spacings);
    }

}
