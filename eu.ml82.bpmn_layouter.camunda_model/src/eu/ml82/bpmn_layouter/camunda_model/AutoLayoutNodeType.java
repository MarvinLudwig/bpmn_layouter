package eu.ml82.bpmn_layouter.camunda_model;

import java.util.EnumSet;

import de.cau.cs.kieler.core.math.KVector;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;

/**
 * Type used by AutoLayout to handle nodes
 */
public enum AutoLayoutNodeType {

    UNDEFINED(
            BpmnElementType.UNDEFINED,
            false,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT),

    POOL(
            BpmnElementType.POOL,
            false,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT),

    LANE(
            BpmnElementType.LANE,
            false,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT),

    CALL_ACTIVITY(
            BpmnElementType.UNDEFINED,
            false,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT),

    TASK(
            BpmnElementType.UNDEFINED,
            false,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT),

    SUB_PROCESS(
            BpmnElementType.SUBPROCESS,
            false,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT),

    EVENT(
            BpmnElementType.EVENT,
            true,
            BpmnAutoLayoutProperties.DEFAULT_EVENT_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_EVENT_HEIGHT),

    GATEWAY(
            BpmnElementType.GATEWAY,
            true,
            BpmnAutoLayoutProperties.DEFAULT_GATEWAY_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_GATEWAY_HEIGHT),

    DATA_OBJECT(
            BpmnElementType.UNDEFINED,
            true,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT),

    DATA_OBJECT_REFERENCE(
            BpmnElementType.UNDEFINED,
            true,
            BpmnAutoLayoutProperties.DEFAULT_DATA_OBJECT_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_DATA_OBJECT_HEIGHT),

    DATA_INPUT(
            BpmnElementType.UNDEFINED,
            true,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT),

    DATA_OUTPUT(
            BpmnElementType.UNDEFINED,
            true,
            BpmnAutoLayoutProperties.DEFAULT_NODE_WIDTH,
            BpmnAutoLayoutProperties.DEFAULT_NODE_HEIGHT);

    /**
     * All types related to Activity
     */
    private static EnumSet<AutoLayoutNodeType> activityNodeTypes = EnumSet.of(AutoLayoutNodeType.TASK,
            AutoLayoutNodeType.CALL_ACTIVITY, AutoLayoutNodeType.SUB_PROCESS);

    /**
     * All types which are not handled in graph
     */
    private static EnumSet<AutoLayoutNodeType> unhandledNodeTypes = EnumSet.of(AutoLayoutNodeType.DATA_OBJECT,
            AutoLayoutNodeType.DATA_INPUT, AutoLayoutNodeType.DATA_OUTPUT);

    /**
     * BpmnElementType for node
     */
    private BpmnElementType bpmnElementType;

    /**
     * If node has fixed sized
     */
    private boolean fixedSize;

    /**
     * Default size of node
     */
    private KVector defaultSize;

    private AutoLayoutNodeType(BpmnElementType bpmnElementType, boolean fixedSize, double defaultWidth,
            double defaultHeight) {
        this.bpmnElementType = bpmnElementType;
        this.fixedSize = fixedSize;
        this.defaultSize = new KVector(defaultWidth, defaultHeight);
    }

    /**
     * @return
     */
    public BpmnElementType getBpmnElementType() {
        return this.bpmnElementType;
    }

    /**
     * @return
     */
    public KVector getDefaultSize() {
        return this.defaultSize.clone();
    }

    /**
     * @return
     */
    public boolean isFixedSize() {
        return this.fixedSize;
    }

    /**
     * @param nodeType
     * @return
     */
    public static boolean isActivity(AutoLayoutNodeType nodeType) {
        return activityNodeTypes.contains(nodeType);
    }

    /**
     * @param nodeType
     * @return
     */
    public static boolean isInGraph(AutoLayoutNodeType nodeType) {
        return !unhandledNodeTypes.contains(nodeType);
    }

}
