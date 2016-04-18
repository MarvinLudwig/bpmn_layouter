package eu.ml82.bpmn_layouter.camunda_model;

import java.io.File;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class Test {

    public static void main(String[] args) {

        // Root path for source and result test files
        String ROOT_TEST_FILES = "";
        String sourceFilename = "/home/marvin/Desktop/B.2.0-export.bpmn";

        File srcTestFile = new File(ROOT_TEST_FILES + sourceFilename);
        File destTestFile = new File(ROOT_TEST_FILES + "AutoLayout Result - " + sourceFilename);

        testAutoLayout(srcTestFile, destTestFile);
    }

    private static void testAutoLayout(File sourceFile, File destinationFile) {

        BpmnModelInstance modelInstance;

        try {
            modelInstance = Bpmn.readModelFromFile(sourceFile);

            BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(modelInstance);
            bpmnAutoLayout.autoLayout();

            // String xmlBpmn = Bpmn.convertToString(modelInstance);

            Bpmn.writeModelToFile(destinationFile, modelInstance);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
