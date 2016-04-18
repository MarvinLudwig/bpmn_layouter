package eu.ml82.bpmn_layouter.camunda_model;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.properties.IProperty;
import de.cau.cs.kieler.core.properties.Property;

/**
 * Properties attached to KIELER graph objects by BpmnAutoLayout
 */
public class BpmnAutoLayoutProperties {

    public final static float DEFAULT_NODE_WIDTH = 200.0f;
    public final static float DEFAULT_NODE_HEIGHT = 100.0f;

    public final static float DEFAULT_EVENT_WIDTH = 30.0f;
    public final static float DEFAULT_EVENT_HEIGHT = 30.0f;

    public final static float DEFAULT_GATEWAY_WIDTH = 40.0f;
    public final static float DEFAULT_GATEWAY_HEIGHT = 40.0f;

    public final static float DEFAULT_DATA_OBJECT_WIDTH = 60.0f;
    public final static float DEFAULT_DATA_OBJECT_HEIGHT = 80.0f;

    /**
     * Used to store offset of node
     */
    public static final IProperty<KVector> OFFSET = new Property<KVector>(
            "eu.ml82.bpmn_layouter.camunda_model.properties.offset", new KVector());

}
