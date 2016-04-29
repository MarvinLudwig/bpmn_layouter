/*
 * Camunda BPMN model wrapper for the BPMN auto-layouter
 *
 * Copyright 2015 by Christophe Bertoncini
 *
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.camunda_model;

import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.DataInput;
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutput;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.Task;

public class BpmnLogicalNode {

    private FlowElement flowElement;

    private AutoLayoutNodeType nodeType;

    /**
     * @param node
     */
    public BpmnLogicalNode(FlowElement flowElement) {
        this.flowElement = flowElement;
        this.nodeType = BpmnLogicalNode.getBpmnType(flowElement);
    }

    public FlowElement getFlowElement() {
        return flowElement;
    }

    public AutoLayoutNodeType getNodeType() {
        return nodeType;
    }

    //
    // Service
    //

    public static AutoLayoutNodeType getBpmnType(FlowElement flowElement) {

        AutoLayoutNodeType bpmnType;

        if (flowElement instanceof Event) {
            bpmnType = AutoLayoutNodeType.EVENT;
        } else if (flowElement instanceof Gateway) {
            bpmnType = AutoLayoutNodeType.GATEWAY;
        } else if (flowElement instanceof SubProcess) {
            bpmnType = AutoLayoutNodeType.SUB_PROCESS;
        } else if (flowElement instanceof CallActivity) {
            bpmnType = AutoLayoutNodeType.CALL_ACTIVITY;
        } else if (flowElement instanceof Task) {
            bpmnType = AutoLayoutNodeType.TASK;
        } else if (flowElement instanceof DataObject) {
            bpmnType = AutoLayoutNodeType.DATA_OBJECT;
        } else if (flowElement instanceof DataObjectReference) {
            bpmnType = AutoLayoutNodeType.DATA_OBJECT_REFERENCE;
        } else if (flowElement instanceof DataInput) {
            bpmnType = AutoLayoutNodeType.DATA_INPUT;
        } else if (flowElement instanceof DataOutput) {
            bpmnType = AutoLayoutNodeType.DATA_OUTPUT;
        } else {
            bpmnType = AutoLayoutNodeType.UNDEFINED;
        }

        return bpmnType;
    }

}
