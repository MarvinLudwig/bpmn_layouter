package eu.ml82.bpmn_layouter.core.properties;

import java.util.List;
import java.util.Set;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.properties.IProperty;
import de.cau.cs.kieler.core.properties.Property;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import eu.ml82.bpmn_layouter.core.processors.artifacts.BpmnArtifact;

public class BpmnProperties {
	
	////// used in KGraph and LGraph => need to be defined in plugin.xml
	////// to make it from KGraph to LGraph
	////// important: need to be set when creating KGraph

    public static final IProperty<BpmnElementType> ELEMENT_TYPE 
    = new Property<BpmnElementType>(
        "eu.ml82.bpmn_layouter.core.properties.element_type", BpmnElementType.UNDEFINED);
    
    public static final IProperty<Boolean> BOUNDARY_EVENT_DUMMY_EDGE
    = new Property<Boolean>(
        "eu.ml82.bpmn_layouter.core.properties.boundary_event_dummy_edge", false);  
    
    public static final IProperty<Boolean> ARTIFACT_DUMMY_EDGE
    = new Property<Boolean>(
        "eu.ml82.bpmn_layouter.core.properties.artifact_dummy_edge", false); 
    
    public static final IProperty<String> ID
    = new Property<String>(
        "eu.ml82.bpmn_layouter.core.properties.id", "");
           
    ////// only used in LGraph
    
    // stores the original size of a node
    // used for boundary event processing
    public static final IProperty<KVector> ORIGINAL_SIZE
    = new Property<KVector>(
        "eu.ml82.bpmn_layouter.core.properties.original_size");   
    
    public static final IProperty<List<BpmnArtifact>> ARTIFACTS
    = new Property<List<BpmnArtifact>>(
        "eu.ml82.bpmn_layouter.core.properties.artifacts");
    
    public static final IProperty<List<LEdge>> MESSAGE_FLOWS
    = new Property<List<LEdge>>(
        "eu.ml82.bpmn_layouter.core.properties.message_flows");
    
    public static final IProperty<List<LNode>> BOUNDARY_EVENTS
    = new Property<List<LNode>>(
        "eu.ml82.bpmn_layouter.core.properties.boundary_events");
    
    // Stores edges's original source node when switching source nodes 
    // Used for message flow and boundary event handling 
    public static final IProperty<LNode> ORIGINAL_SOURCE_NODE
    = new Property<LNode>(
        "eu.ml82.bpmn_layouter.core.properties.original_source_node");
    
    public static final IProperty<LNode> ATTACHED_TO
    = new Property<LNode>(
        "eu.ml82.bpmn_layouter.core.properties.attached_to");
    
    // Store an element's label, needed for message flows
    public static final IProperty<LLabel> LABEL
    = new Property<LLabel>(
        "eu.ml82.bpmn_layouter.core.properties.label");
    
    // BPMN containers: pools, lanes, TODO: sub-processes
    public static final IProperty<Set<LNode>> CONTAINERS
    = new Property<Set<LNode>>(
        "eu.ml82.bpmn_layouter.core.properties.containers");
    
    public static final IProperty<Integer> CONTAINER_PADDING_TOP_BOTTOM
    = new Property<Integer>(
        "eu.ml82.bpmn_layouter.core.properties.container_padding_top_bottom");
    
    public static final IProperty<Integer> CONTAINER_PADDING_LEFT_RIGHT
    = new Property<Integer>(
        "eu.ml82.bpmn_layouter.core.properties.container_padding_left_right");
    
    public static final IProperty<Integer> CONTAINER_SPACING
    = new Property<Integer>(
        "eu.ml82.bpmn_layouter.core.properties.container_spacing");

}
