/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.camunda_modeler;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;

public class Utils {

	private static <T> T getBusinessObjectByClass(PictogramElement pelem, Class<T> class1) {
		PictogramLink link = pelem.getLink();
		if (link == null) return null;
		for (EObject businessObject : link.getBusinessObjects()){
			if (class1.isAssignableFrom(businessObject.getClass())) return (T) businessObject;
		}
		return null;
	}
	
	public static BPMNShape getBpmnDiShape(PictogramElement pelem){
		return getBusinessObjectByClass(pelem, BPMNShape.class);
	}

	public static BaseElement getBaseElement(PictogramElement pelem){
		return getBusinessObjectByClass(pelem, BaseElement.class);
	}
	
}
