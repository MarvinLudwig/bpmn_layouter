package eu.ml82.bpmn_layouter.camunda_model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * BpmnLogicalGraph : keep track of lanes, dataobjectreference, assign Lanes to nodes
 */
public class BpmnLogicalGraph
        extends LogicalGraph<String, BpmnLogicalNode> {

    // Lane for node
    private TreeMap<String, String> nodeToLane;

    // Sequences
    private List<SequenceFlow> sequences;

    // "Sequence" between DataObjectReference and Activity
    private List<DataLink> dataLinks;

    public BpmnLogicalGraph() {
        nodeToLane = new TreeMap<String, String>();
        sequences = new ArrayList<SequenceFlow>();
        dataLinks = new ArrayList<DataLink>();
    }

    /**
     * Clear all info
     */
    @Override
    public void clear() {
        this.dataLinks.clear();
        this.sequences.clear();
        this.nodeToLane.clear();

        super.clear();
    }

    /**
     * For convenience
     *
     * @param nodeId
     * @return
     */
    public AutoLayoutNodeType getNodeType(String nodeId) {
        BpmnLogicalNode logicalNode = this.getNode(nodeId);
        return (logicalNode != null) ? logicalNode.getNodeType() : null;
    }

    /**
     * For convenience
     *
     * @param nodeId
     * @return
     */
    public FlowElement getFlowElement(String nodeId) {
        BpmnLogicalNode logicalNode = this.getNode(nodeId);
        return (logicalNode != null) ? logicalNode.getFlowElement() : null;
    }

    /**
     * For convenience
     *
     * @param nodeId
     * @return
     */
    public FlowNode getFlowNode(String nodeId) {

        FlowElement flowElement = this.getFlowElement(nodeId);
        if (flowElement instanceof FlowNode)
            return (FlowNode) flowElement;

        return null;
    }

    /**
     * Add sequence and set link with it
     * 
     * @param sequenceFlow
     */
    public void addSequenceFlow(SequenceFlow sequenceFlow) {
        this.sequences.add(sequenceFlow);

        String sourceId = sequenceFlow.getSource().getId();
        String targetId = sequenceFlow.getTarget().getId();
        super.addLink(sourceId, targetId);
    }

    public void associateLane(String nodeId, String laneId) {
        nodeToLane.put(nodeId, laneId);
    }

    public String getLaneId(String nodeId) {
        return this.nodeToLane.get(nodeId);
    }

    public List<SequenceFlow> getSequenceFlows() {
        return this.sequences;
    }

    /**
     * Add a DataLink and set link with it
     * 
     * @param dataLink
     */
    public void addDataLink(DataLink dataLink) {
        this.dataLinks.add(dataLink);

        String sourceId = dataLink.getSourceId();
        String targetId = dataLink.getTargetId();
        super.addLink(sourceId, targetId);
    }

    public List<DataLink> getDataLinks() {
        return this.dataLinks;
    }

    /**
     * Evaluate rank in path for a task
     */
    private class TaskRank {

        private String taskId;

        private int rank;

        public String getTaskId() {
            return taskId;
        }

        public int getRank() {
            return rank;
        }

        public TaskRank(String taskId) {
            this(taskId, 0);
        }

        public TaskRank(String taskId, int rank) {
            this.taskId = taskId;
            this.rank = rank;
        }

        public void incRank() {
            this.rank++;
        }
    }

    /**
     * Find the "nearest" task/sub process node
     *
     * @param directedNodes
     * @param nodeId
     * @param ancestors
     * @return
     * @throws Exception
     */
    private TaskRank findTaskMinRank(Multimap<String, String> directedNodes, String nodeId, Set<String> ancestors)
            throws Exception {

        AutoLayoutNodeType nodeType = this.getNodeType(nodeId);
        if (nodeType == null) {
            throw new Exception("Bad argument");
        }

        if (AutoLayoutNodeType.isActivity(nodeType)) {
            return new TaskRank(nodeId);
        }

        TaskRank result = null;

        ancestors.add(nodeId);
        for (String testNodeId : directedNodes.get(nodeId)) {
            if (!ancestors.contains(testNodeId)) {
                TaskRank testTask = findTaskMinRank(directedNodes, testNodeId, ancestors);
                if (testTask != null) {
                    if ((result == null) || (result.getRank() > testTask.getRank())) {
                        result = testTask;
                    }
                }
            }
        }
        ancestors.remove(nodeId);

        if (result != null) {
            result.incRank();
        }

        return result;
    }

    private TaskRank findPreviousTaskMinRank(String nodeId) throws Exception {
        return this.findTaskMinRank(this.predecessors, nodeId, new TreeSet<String>());
    }

    private TaskRank findNextTaskMinRank(String nodeId) throws Exception {
        return this.findTaskMinRank(this.successors, nodeId, new TreeSet<String>());
    }

    /**
     * Fix lanes for unsigned elements: set gateway to same lane as "nearest" task, then set other elements to same as
     * their previous one if exists, following if not
     */
    public void computeLanes() throws Exception {

        // First pass to set gateway lanes
        Set<String> allNodes = new TreeSet<String>();

        for (Map.Entry<String, BpmnLogicalNode> entry : nodes.entrySet()) {
            String nodeId = entry.getKey();
            allNodes.add(nodeId);

            AutoLayoutNodeType nodeType = entry.getValue().getNodeType();

            if (nodeType == AutoLayoutNodeType.GATEWAY) {
                // Use lane of nearest task
                String laneId = null;

                TaskRank previousTask = this.findPreviousTaskMinRank(nodeId);
                TaskRank nextTask = this.findNextTaskMinRank(nodeId);
                TaskRank selected = null;

                if (previousTask != null) {

                    if ((nextTask != null) && (nextTask.getRank() < previousTask.getRank())) {
                        selected = nextTask;
                    } else {
                        selected = previousTask;
                    }
                } else {
                    // if (nextTask == null) {
                    // throw new Exception("Gateway without task");
                    // }

                    selected = nextTask;
                }

                if (selected != null) {
                    laneId = this.getLaneId(selected.getTaskId());
                    if (laneId != null) {
                        this.nodeToLane.put(nodeId, laneId);
                    }
                }
            }
        }

        // Second pass to set node without lanes
        allNodes.removeAll(this.nodeToLane.keySet());

        for (String nodeId : allNodes) {
            // Set to previous if exist, next instead
            Collection<String> linkedNodes = this.predecessors.get(nodeId);
            if ((linkedNodes == null) || (linkedNodes.size() == 0)) {
                linkedNodes = this.successors.get(nodeId);
            }

            String laneId = null;
            if ((linkedNodes != null) && (linkedNodes.size() != 0)) {
                String linkedNodeId = linkedNodes.iterator().next();
                laneId = this.getLaneId(linkedNodeId);
            }

            if (laneId != null) {
                this.nodeToLane.put(nodeId, laneId);
            }
            // else {
            // throw new Exception("Node without task");
            // }
        }
    }

    /**
     * Get lanes and all their associated elements
     *
     * @return
     */
    public Multimap<String, String> getLanesElements() {

        Multimap<String, String> lanesElements = TreeMultimap.create();

        for (Map.Entry<String, String> entry : this.nodeToLane.entrySet()) {
            String idElement = entry.getKey();
            String idLane = entry.getValue();
            lanesElements.put(idLane, idElement);
        }

        return lanesElements;
    }

}
