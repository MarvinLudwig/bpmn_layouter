/*
 * AutoLayout
 */
package eu.ml82.bpmn_layouter.camunda_model;

import java.util.Collection;
import java.util.TreeMap;
import java.util.UUID;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.DataAssociation;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.LaneSet;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import com.google.common.collect.Multimap;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.math.KVectorChain;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LShape;

/**
 * AutoLayout using Marvin LUDWIG layouter (see <a href="https://app.camunda.com/jira/browse/CAM-1717">this link</a>)
 * based on <a href="http://www.rtsys.informatik.uni-kiel.de/en/research/kieler">the KIELER Project</a>
 */
public class BpmnAutoLayout {

    private static String BPMNDI_DIAGRAM_ID = "BPMNDiagram_1";
    private static String BPMNDI_DIAGRAM_NAME = "BPMNDiagram_Collaboration";

    private static String PREFIX_BPMNDI_PLANE = "BPMNPlane_";
    private static String PREFIX_BPMNDI_SHAPE = "BPMNShape_";
    private static String PREFIX_BPMNDI_SEQUENCE_FLOW = "BPMNEdge_";
    private static String PREFIX_BPMNDI_DATA_ASSOCIATION_FLOW = "BPMNEdge_";

    // Source
    protected BpmnModelInstance modelInstance;

    //
    class InnerGraphGeneration
            extends BpmnGraphGeneration {

        // Keep track of all logical graph by process
        TreeMap<String, BpmnLogicalGraph> logicalProcesses;

        public InnerGraphGeneration() {
            this.logicalProcesses = new TreeMap<String, BpmnLogicalGraph>();
        }

        public BpmnLogicalGraph addLogicalGraph(String processId) {
            BpmnLogicalGraph bpmnLogicalGraph = new BpmnLogicalGraph();
            this.logicalProcesses.put(processId, bpmnLogicalGraph);

            return bpmnLogicalGraph;
        }

        public BpmnLogicalGraph getLogicalGraph(String processId) {
            return this.logicalProcesses.get(processId);
        }
    }

    /**
     * 
     */
    public BpmnAutoLayout(BpmnModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }

    /**
     * @throws Exception
     */
    public void autoLayout() throws Exception {

        this.clearDiagramInformation();
        this.createDiagramInformation();
    }

    /**
     * Clear BPMN DI
     */
    public void clearDiagramInformation() {
        // Reset any previous DI information
        Collection<BpmnDiagram> diagrams = modelInstance.getModelElementsByType(BpmnDiagram.class);
        for (BpmnDiagram diagram : diagrams) {
            modelInstance.getDefinitions().removeChildElement(diagram);
        }
    }

    /**
     * Create logical graph, compute lanes associations, set lanes for unassigned elements , create KIELER graph, launch
     * layout algorithm and create BPMN DI with computed coordinates
     *
     * @throws Exception
     */
    protected void createDiagramInformation() throws Exception {

        InnerGraphGeneration graphGeneration = new InnerGraphGeneration();

        for (org.camunda.bpm.model.bpmn.instance.Process process : modelInstance
                .getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class)) {

            String processId = process.getId();
            // Create Pool
            // TODO use participant to title it
            String poolTitle = null;

            // Create logical info
            BpmnLogicalGraph bpmnLogicalGraph = graphGeneration.addLogicalGraph(processId);
            ProcessGraphGeneration processGraphGeneration = graphGeneration.addProcessGraphGeneration(processId,
                    poolTitle);

            this.createGraph(graphGeneration, bpmnLogicalGraph, processGraphGeneration, process);
        }

        // Layout
        graphGeneration.layout();

        // TODO tests
        // BufferedImage image = bpmnGraph.exportImage();
        // try {
        // ImageIO.write(image, "PNG", new File("Layout result.png"));
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

        // Add BMPMNDI
        createBPMNDI(graphGeneration);
    }

    //
    // Graph
    //
    protected void createGraph(InnerGraphGeneration graphGeneration, BpmnLogicalGraph bpmnLogicalGraph,
            ProcessGraphGeneration processGraphGeneration, final org.camunda.bpm.model.bpmn.instance.Process process)
                    throws Exception {

        Collection<LaneSet> laneSets = process.getLaneSets();
        // Create logical info (and compute lanes for gateways, events)
        this.fillLogicalGraph(laneSets, process.getFlowElements(), bpmnLogicalGraph);

        // Add lanes in graph
        for (LaneSet laneSet : laneSets) {
            for (Lane lane : laneSet.getLanes()) {
                String laneId = lane.getId();
                processGraphGeneration.addLane(laneId, lane.getName());
            }
        }

        // Modify CamundaModel (BPMN.io doesn't show elements which are not associated to lane)
        // Could be done in previous loop but prefer separate logical
        Multimap<String, String> lanesElements = bpmnLogicalGraph.getLanesElements();
        for (LaneSet laneSet : laneSets) {
            for (Lane lane : laneSet.getLanes()) {
                String laneId = lane.getId();

                Collection<String> elementIds = lanesElements.get(laneId);
                if (elementIds != null) {
                    for (String elementId : elementIds) {
                        FlowNode flowNode = bpmnLogicalGraph.getFlowNode(elementId);
                        if (flowNode != null) {
                            lane.getFlowNodeRefs().add(flowNode);
                        }
                    }
                }
            }
        }

        this.fillProcessGraph(graphGeneration, bpmnLogicalGraph, processGraphGeneration);
    }

    protected void createGraph(InnerGraphGeneration graphGeneration, BpmnLogicalGraph bpmnLogicalGraph,
            ProcessGraphGeneration processGraphGeneration,
            final org.camunda.bpm.model.bpmn.instance.SubProcess subProcess) throws Exception {

        Collection<LaneSet> laneSets = subProcess.getLaneSets();
        // Create logical info (and assign lanes to gateways, events)
        this.fillLogicalGraph(laneSets, subProcess.getFlowElements(), bpmnLogicalGraph);

        this.fillProcessGraph(graphGeneration, bpmnLogicalGraph, processGraphGeneration);
    }

    /**
     * Create logical info (and assign lanes to gateways, events)
     *
     * @param laneSets
     * @param flowElements
     * @throws Exception
     */
    protected void fillLogicalGraph(final Collection<LaneSet> laneSets, final Collection<FlowElement> flowElements,
            BpmnLogicalGraph bpmnLogicalGraph) throws Exception {

        // Add element nodes, sequence and DataIO link
        for (FlowElement flowElement : flowElements) {

            if (flowElement instanceof SequenceFlow) {
                SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
                bpmnLogicalGraph.addSequenceFlow(sequenceFlow);
            } else {
                String elementId = flowElement.getId();
                BpmnLogicalNode node = new BpmnLogicalNode(flowElement);

                bpmnLogicalGraph.addNode(elementId, node);

                if (AutoLayoutNodeType.isActivity(node.getNodeType())) {
                    // Data Input/Output specific
                    Activity activity = (Activity) flowElement;

                    String sourceId, targetId;
                    for (DataInputAssociation dataInputAssociation : activity.getDataInputAssociations()) {
                        targetId = elementId; // dataInputAssociation.getTarget().getId();
                        for (ModelElementInstance source : dataInputAssociation.getSources()) {
                        	sourceId = source.getAttributeValue("id");
                            bpmnLogicalGraph
                                    .addDataLink(new DataLink(dataInputAssociation.getId(), sourceId, targetId));
                        }
                    }

                    for (DataOutputAssociation dataOutputAssociation : activity.getDataOutputAssociations()) {
                        targetId = dataOutputAssociation.getTarget().getId();
                        for (ModelElementInstance source : dataOutputAssociation.getSources()) {
                            sourceId = elementId; // source.getId();
                            bpmnLogicalGraph
                                    .addDataLink(new DataLink(dataOutputAssociation.getId(), sourceId, targetId));
                        }
                    }
                } // Data Input/Output specific
            }
        }

        // Create lane nodes
        for (LaneSet laneSet : laneSets) {
            for (Lane lane : laneSet.getLanes()) {
                String laneId = lane.getId();

                for (FlowNode flowNode : lane.getFlowNodeRefs()) {
                    bpmnLogicalGraph.associateLane(flowNode.getId(), laneId);
                }
            }
        }

        // Compute lanes for unassigned elements
        bpmnLogicalGraph.computeLanes();
    }

    /**
     * Fill ProcessGraph with bpmnLogical info
     *
     * @param laneSets
     * @param flowElements
     * @throws Exception
     */
    protected void fillProcessGraph(InnerGraphGeneration graphGeneration, BpmnLogicalGraph bpmnLogicalGraph,
            ProcessGraphGeneration processGraphGeneration) throws Exception {

        // Process all elements and create graph nodes
        for (String elementId : bpmnLogicalGraph.getKeys()) {

            BpmnLogicalNode logicalNode = bpmnLogicalGraph.getNode(elementId);
            AutoLayoutNodeType bpmnType = logicalNode.getNodeType();

            FlowElement flowElement = logicalNode.getFlowElement();
            String elementName = flowElement.getName();
            // Get associated lane
            String laneId = bpmnLogicalGraph.getLaneId(elementId);

            LNode graphNode = null;
            if (bpmnType == AutoLayoutNodeType.SUB_PROCESS) {
                graphNode = processGraphGeneration.addNode(elementId, elementName, laneId);
                if (graphNode != null) {
                    SubProcess subProcess = (SubProcess) flowElement;
                    BpmnLogicalGraph subBpmnLogicalGraph = graphGeneration.addLogicalGraph(elementId);

                    LGraph subProcessGraph = BpmnGraphGeneration.addNestedGraph(graphNode);
                    ProcessGraphGeneration subGraphGeneration = new ProcessGraphGeneration(subProcessGraph);

                    this.createGraph(graphGeneration, subBpmnLogicalGraph, subGraphGeneration, subProcess);
                }
            } else if (AutoLayoutNodeType.isInGraph(bpmnType)) {

                graphNode = processGraphGeneration.addNode(elementId, elementName, laneId);

                if (graphNode == null) {
                    throw new Exception("Couldn't create node !");
                }

                BpmnGraphGeneration.setNodeProperties(graphNode, bpmnType);
            }
        }

        // Create edges
        for (SequenceFlow sequenceFlow : bpmnLogicalGraph.getSequenceFlows()) {
            FlowNode sourceNode = sequenceFlow.getSource();
            FlowNode targetNode = sequenceFlow.getTarget();

            String sourceId = sourceNode.getId();
            String targetId = targetNode.getId();

            // Ensure each sequence has an id (needed to get back graphical info)
            String sequenceId = ensureSequenceFlowIdSet(sequenceFlow);
            LEdge edge = processGraphGeneration.createEdge(sourceId, targetId, sequenceId, sequenceFlow.getName());

            if (edge == null) {
                throw new Exception("Couldn't create edge !");
            }

            if ((sourceNode instanceof BoundaryEvent) || (targetNode instanceof BoundaryEvent))
                BpmnGraphGeneration.setForBoundaryEvent(edge);
        }

        for (DataLink dataLink : bpmnLogicalGraph.getDataLinks()) {
            String sourceId = dataLink.getSourceId();
            String targetId = dataLink.getTargetId();

            LEdge edge = processGraphGeneration.createEdge(sourceId, targetId, dataLink.getId(), "");

            if (edge == null) {
                throw new Exception("Couldn't create edge !");
            }
        }

    }

    //
    // Diagram interchange generation
    //
    protected void createBPMNDI(final InnerGraphGeneration graphGeneration) throws Exception {

        // Create bpmn diagram
        BpmnDiagram bpmnDiagram = modelInstance.newInstance(BpmnDiagram.class);
        bpmnDiagram.setId(BPMNDI_DIAGRAM_ID);
        bpmnDiagram.setName(BPMNDI_DIAGRAM_NAME);

        modelInstance.getDefinitions().addChildElement(bpmnDiagram);

        for (org.camunda.bpm.model.bpmn.instance.Process process : modelInstance
                .getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class)) {

            createBPMNDI(graphGeneration, bpmnDiagram, process);
        }
    }

    protected void createBPMNDI(final InnerGraphGeneration graphGeneration, BpmnDiagram bpmnDiagram,
            org.camunda.bpm.model.bpmn.instance.Process process) throws Exception {

        String processId = process.getId();
        // Create plane for process
        BpmnPlane processPlane = modelInstance.newInstance(BpmnPlane.class);
        processPlane.setId(PREFIX_BPMNDI_PLANE + processId);
        processPlane.setBpmnElement(process);
        bpmnDiagram.setBpmnPlane(processPlane);

        // Fill plane
        ProcessGraphGeneration processGraph = graphGeneration.getProcess(processId);
        if (processGraph == null) {
            throw new Exception("No graphical information for process '" + processId + "' !");
        }

        BpmnLogicalGraph logicalGraph = graphGeneration.getLogicalGraph(processId);
        if (logicalGraph == null) {
            throw new Exception("No logical information for process '" + processId + "' !");
        }

        // TODO attach to collaboration if any
        // LShape shape = processGraph.getProcessShape();
        // BpmnShape bpmnShape = createDI(process, shape);
        // processPlane.getDiagramElements().add(bpmnShape);

        createLanesBPMNDI(process.getLaneSets(), processGraph, processPlane);
        createBPMNDI(graphGeneration, logicalGraph, processGraph, processPlane);
    }

    protected void createBPMNDI(final InnerGraphGeneration graphGeneration, BpmnPlane processPlane,
            org.camunda.bpm.model.bpmn.instance.SubProcess subProcess) throws Exception {

        String subProcessId = subProcess.getId();
        ProcessGraphGeneration processGraph = graphGeneration.getProcess(subProcessId);
        if (processGraph == null) {
            throw new Exception("No graphical information for subprocess '" + subProcessId + "' !");
        }

        BpmnLogicalGraph logicalGraph = graphGeneration.getLogicalGraph(subProcessId);
        if (logicalGraph == null) {
            throw new Exception("No logical information for process '" + subProcessId + "' !");
        }

        createLanesBPMNDI(subProcess.getLaneSets(), processGraph, processPlane);
        createBPMNDI(graphGeneration, logicalGraph, processGraph, processPlane);
    }

    protected void createLanesBPMNDI(final Collection<LaneSet> laneSets, final ProcessGraphGeneration processGraph,
            BpmnPlane processPlane) throws Exception {

        for (LaneSet laneSet : laneSets) {
            for (Lane lane : laneSet.getLanes()) {
                String laneId = lane.getId();
                LShape shape = processGraph.getLaneShape(laneId);

                if (shape == null) {
                    throw new Exception("No graphical information for lane '" + laneId + "' !");
                }

                BpmnShape bpmnShape = createDI(lane, shape);
                bpmnShape.setExpanded(true);

                processPlane.getDiagramElements().add(bpmnShape);
            }
        }
    }

    protected void createBPMNDI(final InnerGraphGeneration graphGeneration, final BpmnLogicalGraph logicalGraph,
            final ProcessGraphGeneration processGraph, BpmnPlane processPlane) throws Exception {

        // Elements
        for (String elementId : logicalGraph.getKeys()) {
            BpmnLogicalNode logicalNode = logicalGraph.getNode(elementId);
            AutoLayoutNodeType nodeType = logicalNode.getNodeType();

            if (AutoLayoutNodeType.isInGraph(nodeType)) {

                LShape shape = processGraph.getElementShape(elementId);
                if (shape == null) {
                    throw new Exception("No graphical information for element '" + elementId + "' !");
                }

                FlowElement flowElement = logicalNode.getFlowElement();
                BpmnShape bpmnShape = createDI(flowElement, shape);

                boolean isSubProcess = (nodeType == AutoLayoutNodeType.SUB_PROCESS);
                if (isSubProcess) {
                    bpmnShape.setExpanded(true);

                    SubProcess subProcess = (SubProcess) flowElement;
                    createBPMNDI(graphGeneration, processPlane, subProcess);
                }

                processPlane.getDiagramElements().add(bpmnShape);
            }
        }

        // Sequences
        for (SequenceFlow sequenceFlow : logicalGraph.getSequenceFlows()) {
            KVectorChain vectorChain = processGraph.getSequenceEdge(sequenceFlow.getId());
            if (!vectorChain.isEmpty()) {
                BpmnEdge bpmnEdge = createDI(sequenceFlow, vectorChain);

                processPlane.getDiagramElements().add(bpmnEdge);
            }
        }

        // DataIO Association
        for (DataLink dataLink : logicalGraph.getDataLinks()) {
            String linkId = dataLink.getId();
            KVectorChain vectorChain = processGraph.getSequenceEdge(linkId);
            if (!vectorChain.isEmpty()) {

                ModelElementInstance modelElement = modelInstance.getModelElementById(linkId);
                if (modelElement instanceof DataAssociation) {
                    DataAssociation dataAssociation = (DataAssociation) modelElement;
                    BpmnEdge bpmnEdge = createDI(dataAssociation, vectorChain);

                    processPlane.getDiagramElements().add(bpmnEdge);
                }
            }
        }
    }

    protected BpmnShape createDI(org.camunda.bpm.model.bpmn.instance.Process process, LShape shape) {
        BpmnShape bpmnShape = createBpmnShape(shape);
        bpmnShape.setId(PREFIX_BPMNDI_SHAPE + process.getId());
        bpmnShape.setBpmnElement(process);

        return bpmnShape;
    }

    protected BpmnShape createDI(Lane lane, LShape shape) {
        BpmnShape bpmnShape = createBpmnShape(shape);
        bpmnShape.setId(PREFIX_BPMNDI_SHAPE + lane.getId());
        bpmnShape.setBpmnElement(lane);

        return bpmnShape;
    }

    protected BpmnShape createDI(FlowElement flowElement, LShape shape) {
        BpmnShape bpmnShape = createBpmnShape(shape);
        bpmnShape.setId(PREFIX_BPMNDI_SHAPE + flowElement.getId());
        bpmnShape.setBpmnElement(flowElement);

        return bpmnShape;
    }

    protected BpmnEdge createDI(SequenceFlow sequenceFlow, KVectorChain vectorChain) {
        BpmnEdge bpmnEdge = this.createBpmnEdge(vectorChain);
        bpmnEdge.setId(PREFIX_BPMNDI_SEQUENCE_FLOW + sequenceFlow.getId());
        bpmnEdge.setBpmnElement(sequenceFlow);

        return bpmnEdge;
    }

    protected BpmnEdge createDI(DataAssociation dataAssociation, KVectorChain vectorChain) {
        BpmnEdge bpmnEdge = this.createBpmnEdge(vectorChain);
        bpmnEdge.setId(PREFIX_BPMNDI_DATA_ASSOCIATION_FLOW + dataAssociation.getId());
        bpmnEdge.setBpmnElement(dataAssociation);

        return bpmnEdge;
    }

    /**
     * Create a BpmnShape
     *
     * @param shape
     * @return
     */
    protected BpmnShape createBpmnShape(LShape shape) {
        double x = shape.getPosition().x;
        double y = shape.getPosition().y;
        double width = shape.getSize().x;
        double height = shape.getSize().y;

        Bounds bounds = modelInstance.newInstance(Bounds.class);
        bounds.setX(x);
        bounds.setY(y);
        bounds.setWidth(width);
        bounds.setHeight(height);

        BpmnShape bpmnShape = modelInstance.newInstance(BpmnShape.class);
        bpmnShape.setBounds(bounds);

        return bpmnShape;
    }

    /**
     * Create a BpmnEdge
     * 
     * @param vectorChain
     * @return
     */
    protected BpmnEdge createBpmnEdge(KVectorChain vectorChain) {

        BpmnEdge bpmnEdge = modelInstance.newInstance(BpmnEdge.class);

        for (KVector point : vectorChain) {

            Waypoint wayPoint = modelInstance.newInstance(Waypoint.class);
            wayPoint.setX(point.x);
            wayPoint.setY(point.y);

            bpmnEdge.getWaypoints().add(wayPoint);
        }

        return bpmnEdge;
    }

    //
    // Service
    //

    /**
     * Set id to sequence if needed, to be able to find their associated edge after
     * 
     * @param sequenceFlow
     * @return Sequence id to use
     */
    private static String ensureSequenceFlowIdSet(SequenceFlow sequenceFlow) {

        if (sequenceFlow.getId() == null) {
            sequenceFlow.setId("SeqFlow-" + UUID.randomUUID().toString());
        }

        return sequenceFlow.getId();
    }

}
