package eu.ml82.bpmn_layouter.camunda_model;

import java.util.List;
import java.util.TreeMap;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.kiml.options.SizeConstraint;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.KlayLayeredForBpmn;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;
import eu.ml82.bpmn_layouter.core.properties.PosType;

/**
 * Keep track of generated nodes for a Process
 */
public class BpmnGraphGeneration {

    // Root
    LGraph bpmnGraph;

    // Graph for process
    TreeMap<String, ProcessGraphGeneration> graphProcesses;

    public BpmnGraphGeneration() {

        this.bpmnGraph = new LGraph();
        this.graphProcesses = new TreeMap<String, ProcessGraphGeneration>();
    }

    public void clear() {
        graphProcesses.clear();
    }

    /**
     * Add a ProcessGraphGeneration
     * 
     * @param processId
     * @param poolTitle
     */
    public ProcessGraphGeneration addProcessGraphGeneration(String processId, String poolTitle) {

        LNode poolNode = BpmnGraphGeneration.createNode(bpmnGraph, poolTitle, 0, 0);
        BpmnGraphGeneration.setNodeProperties(poolNode, AutoLayoutNodeType.POOL);
        LGraph poolGraph = BpmnGraphGeneration.addNestedGraph(poolNode);

        ProcessGraphGeneration processGraph = new ProcessGraphGeneration(poolGraph);
        graphProcesses.put(processId, processGraph);

        return processGraph;
    }

    public ProcessGraphGeneration getProcess(String processId) {
        return this.graphProcesses.get(processId);
    }

    public void layout() {

        KlayLayeredForBpmn layouter = new KlayLayeredForBpmn();
        layouter.doLayout(bpmnGraph, PosType.ABSOLUTE);

        // Set nodes offset property
        this.setOffsetProperties();
    }

    /**
     * Set nodes offset property
     */
    private void setOffsetProperties() {

        this.setOffsetProperties(this.bpmnGraph, new KVector(0, 0));
    }

    private void setOffsetProperties(LGraph graph, KVector offset) {

        for (LNode node : graph.getLayerlessNodes()) {
            node.setProperty(BpmnAutoLayoutProperties.OFFSET, offset);

            for (LEdge edge : node.getOutgoingEdges()) {
                edge.setProperty(BpmnAutoLayoutProperties.OFFSET, offset);
            }

            // Treat nested graph
            LGraph nestedGraph = node.getProperty(InternalProperties.NESTED_LGRAPH);
            if (nestedGraph != null) {
                KVector newOffset = new KVector(KVector.sum(offset, node.getPosition()));
                this.setOffsetProperties(nestedGraph, newOffset);
            }
        }
    }

    //
    // Services
    //

    public static boolean isValidLabel(String label) {
        return label != null && !label.isEmpty();
    }

    public static LGraph addNestedGraph(LNode node) {
        LGraph nodeGraph = new LGraph();
        node.setProperty(InternalProperties.NESTED_LGRAPH, nodeGraph);

        return nodeGraph;
    }

    public static LGraph getNestedGraph(LNode node) {
        return node.getProperty(InternalProperties.NESTED_LGRAPH);
    }

    public static LNode createNode(LGraph parentGraph, final String labelText, double posX, double posY) {
        LNode node = new LNode(parentGraph);
        node.getPosition().x = posX;
        node.getPosition().y = posY;

        if (isValidLabel(labelText)) {
            LLabel label = new LLabel(labelText);
            node.getLabels().add(label);
        }

        parentGraph.getLayerlessNodes().add(node);

        return node;
    }

    public static void setNodeProperties(LNode node, final AutoLayoutNodeType autoLayoutNodeType) {
        BpmnGraphGeneration.setNodeProperties(node, autoLayoutNodeType.getBpmnElementType(),
                autoLayoutNodeType.getDefaultSize(), autoLayoutNodeType.isFixedSize());
    }

    public static void setNodeProperties(LNode node, final BpmnElementType bpmnElementType, final KVector defaultSize,
            final boolean fixedSize) {

        if (bpmnElementType != BpmnElementType.UNDEFINED) {
            node.setProperty(BpmnProperties.ELEMENT_TYPE, bpmnElementType);
        }

        if (fixedSize)
            node.setProperty(LayoutOptions.SIZE_CONSTRAINT, SizeConstraint.minimumSizeWithPorts());
        else
            node.setProperty(LayoutOptions.SIZE_CONSTRAINT, SizeConstraint.free());

        node.setProperty(LayoutOptions.MIN_WIDTH, (float) defaultSize.x);
        node.setProperty(LayoutOptions.MIN_HEIGHT, (float) defaultSize.y);
    }

    private static LPort getOrAddLPort(LNode parentNode) {

        LPort lPort;

        List<LPort> parentPorts = parentNode.getPorts();
        if (!parentPorts.isEmpty())
            lPort = parentPorts.get(0);
        else {
            lPort = new LPort();
            // lPort.getMargin().set(5, 5, 5, 5);
            lPort.setNode(parentNode);
        }

        return lPort;
    }

    public static LEdge createEdge(LNode sourceNode, LNode targetNode, final String labelText) {

        // Source port
        LPort sourcePort = getOrAddLPort(sourceNode);

        // Target port
        LPort targetPort = getOrAddLPort(targetNode);

        // Create edge
        LEdge edge = new LEdge();
        edge.setSource(sourcePort);
        edge.setTarget(targetPort);

        // Create edge label
        if (isValidLabel(labelText)) {
            LLabel label = new LLabel(labelText);
            edge.getLabels().add(label);
        }

        return edge;
    }

    public static void setForBoundaryEvent(LEdge edge) {
        edge.setProperty(BpmnProperties.BOUNDARY_EVENT_DUMMY_EDGE, true);
    }

}
