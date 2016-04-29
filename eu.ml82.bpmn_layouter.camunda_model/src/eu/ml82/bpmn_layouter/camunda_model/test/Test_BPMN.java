package eu.ml82.bpmn_layouter.camunda_model.test;

import java.io.File;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import eu.ml82.bpmn_layouter.camunda_model.BpmnAutoLayout;
import eu.ml82.bpmn_layouter.core.properties.PosType;
import eu.ml82.bpmn_layouter.core.utils.Graph;

/*
 * Test with an imported BPMN file
 *  
 * */
public class Test_BPMN {

    public static void main(String[] args) {

        // Root path for source and result test files
        String ROOT_TEST_FILES = "./";
        String sourceFilename = "test.bpmn";

        File srcTestFile = new File(ROOT_TEST_FILES + sourceFilename);
        File destTestFile = new File(ROOT_TEST_FILES + "AutoLayout Result - " + sourceFilename);

        testAutoLayout(srcTestFile, destTestFile, ROOT_TEST_FILES);
    }

    private static void testAutoLayout(File sourceFile, File destinationFile, String ROOT_TEST_FILES) {

        BpmnModelInstance modelInstance;

        try {
            modelInstance = Bpmn.readModelFromFile(sourceFile);

            BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(modelInstance);
            bpmnAutoLayout.autoLayout();

            // String xmlBpmn = Bpmn.convertToString(modelInstance);

            Bpmn.writeModelToFile(destinationFile, modelInstance);
            // Output graph
    		Graph.draw(bpmnAutoLayout.getLGraph(), ROOT_TEST_FILES + "BPMNLayoutTest.png", PosType.ABSOLUTE);
            

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
