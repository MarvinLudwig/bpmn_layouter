package eu.ml82.bpmn_layouter.camunda_model.test;

import java.io.File;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import eu.ml82.bpmn_layouter.camunda_model.BpmnAutoLayout;
import eu.ml82.bpmn_layouter.core.properties.PosType;
import eu.ml82.bpmn_layouter.core.utils.Graph;

/*
 * Test with a model created at runtime
 *  
 * */
public class Test_Model {
	
	static BpmnModelInstance modelInstance;

    public static void main(String[] args) throws Exception {

        // Root path for source and result test files
        String ROOT_TEST_FILES = "./";
        File destTestFile = new File(ROOT_TEST_FILES + "AutoLayout Result.bpmn");

        modelInstance = createModelInstance();

        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(modelInstance);
        bpmnAutoLayout.autoLayout();

        // String xmlBpmn = Bpmn.convertToString(modelInstance);

        Bpmn.writeModelToFile(destTestFile, modelInstance);
        // Output graph
		Graph.draw(bpmnAutoLayout.getLGraph(), ROOT_TEST_FILES + "BPMNLayoutTest.png", PosType.ABSOLUTE); 
    }
    
    private static BpmnModelInstance createModelInstance(){
    	modelInstance = Bpmn.createEmptyModel();
    	Definitions definitions = modelInstance.newInstance(Definitions.class);
    	definitions.setTargetNamespace("http://ml82.eu/test");
    	modelInstance.setDefinitions(definitions);
    	
        // create process
        Process process = createElement(definitions, "process-with-parallel-gateway", Process.class);

        // create elements
        StartEvent startEvent = createElement(process, "start", StartEvent.class);
        ParallelGateway fork = createElement(process, "fork", ParallelGateway.class);
        UserTask task1 = createElement(process, "task1", UserTask.class);
        ServiceTask task2 = createElement(process, "task2", ServiceTask.class);
        ParallelGateway join = createElement(process, "join", ParallelGateway.class);
        EndEvent endEvent = createElement(process, "end", EndEvent.class);

        // create flows
        createSequenceFlow(process, startEvent, fork);
        createSequenceFlow(process, fork, task1);
        createSequenceFlow(process, fork, task2);
        createSequenceFlow(process, task1, join);
        createSequenceFlow(process, task2, join);
        createSequenceFlow(process, join, endEvent);
    	
    	return modelInstance;
    }
    
    private static <T extends BpmnModelElementInstance> T createElement(BpmnModelElementInstance parentElement, String id, Class<T> elementClass) {
        T element = modelInstance.newInstance(elementClass);
        element.setAttributeValue("id", id, true);
        parentElement.addChildElement(element);
        return element;
    }

	private static SequenceFlow createSequenceFlow(Process process, FlowNode from, FlowNode to) {
	   SequenceFlow sequenceFlow = createElement(process, from.getId() + "-" + to.getId(), SequenceFlow.class);
	   process.addChildElement(sequenceFlow);
	   sequenceFlow.setSource(from);
	   from.getOutgoing().add(sequenceFlow);
	   sequenceFlow.setTarget(to);
	   to.getIncoming().add(sequenceFlow);
	   return sequenceFlow;
	}

}
