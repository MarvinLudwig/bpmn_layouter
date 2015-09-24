/*
 * BPMN Auto-Layouter
 * 
 * Copyright 2015 by Marvin Ludwig - http://www.marvin-ludwig.de
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.camunda_modeler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.camunda.bpm.modeler.core.di.DIUtils;
import org.camunda.bpm.modeler.core.features.PropertyNames;
import org.camunda.bpm.modeler.core.utils.LabelUtil;
import org.camunda.bpm.modeler.ui.features.activity.subprocess.ResizeExpandableActivityFeature;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.impl.ResizeShapeContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;

import de.cau.cs.kieler.core.kgraph.KEdge;
import de.cau.cs.kieler.core.kgraph.KGraphElement;
import de.cau.cs.kieler.core.kgraph.KLabel;
import de.cau.cs.kieler.core.kgraph.KNode;
import de.cau.cs.kieler.core.kgraph.KPort;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.math.KVectorChain;
import de.cau.cs.kieler.kiml.klayoutdata.KEdgeLayout;
import de.cau.cs.kieler.kiml.klayoutdata.KShapeLayout;
import de.cau.cs.kieler.kiml.util.KimlUtil;
import de.cau.cs.kieler.kiml.graphiti.GraphitiLayoutCommand;

public class LayoutCommand extends GraphitiLayoutCommand {
		
	/** list of graph elements and pictogram elements to layout. */
    private HashMap<PictogramElement, KGraphElement> elements = new HashMap<PictogramElement, KGraphElement>();
    
    private IFeatureProvider thefeatureProvider;
    public LayoutCommand(
            final TransactionalEditingDomain domain,
            final IFeatureProvider thefeatureProvider, int layoutWidth) {
    	super(domain, thefeatureProvider);
    	this.thefeatureProvider = thefeatureProvider;
    	thefeatureProvider.getDiagramTypeProvider().getDiagram();
	}
    
    private Map<KEdgeLayout, KVectorChain> bendpointsMap =
            new HashMap<KEdgeLayout, KVectorChain>();

	@Override
    protected void doExecute() {  
    	    	    	    	
        for (Entry<PictogramElement,KGraphElement> entry : elements.entrySet()) {
            KGraphElement element = entry.getValue();
            if (element instanceof KPort) {
                applyPortLayout((KPort) element, entry.getKey());
            } else if (element instanceof KNode) {
                applyNodeLayout((KNode) element, entry.getKey());
            } else if (element instanceof KLabel
                    && ((KLabel) element).eContainer() instanceof KEdge) {
                applyEdgeLabelLayout((KLabel) element, entry.getKey());
            }
        }
        
        for (Entry<PictogramElement,KGraphElement> entry : elements.entrySet()) {
            KGraphElement element = entry.getValue();
            if (element instanceof KEdge) {
                applyEdgeLayout((KEdge) element, entry.getKey());
            } 
    		// Update label position (label is PictogramElement: flow elements)
            if (element instanceof KNode)
    		LabelUtil.repositionLabel(entry.getKey(),thefeatureProvider);
        }

    }
	
	@Override
	protected void applyEdgeLayout(final KEdge kedge, final PictogramElement pelem) {
				
		super.applyEdgeLayout(kedge, pelem);
		
		kedge.getSource().getOutgoingEdges().size();
		kedge.getTarget().getIncomingEdges().size();
		// adjust bendpoints
		if (pelem instanceof FreeFormConnection) {
            FreeFormConnection connection = (FreeFormConnection) pelem;
            List<Point> bendpoints = connection.getBendpoints();
            if (bendpoints.size() > 0){
            	// startpoint
	            	Shape source = (Shape) connection.getStart().getParent();
	                Point startPoint = bendpoints.get(0);
	        		Point newStartPoint = adjustBendpoint(startPoint,source);
	        		if (newStartPoint != null){
		        		bendpoints.remove(startPoint);
		        		bendpoints.add(0, newStartPoint);
	        		}
            	// endpoint
	            	Point lastPoint = bendpoints.get(bendpoints.size()-1);
	                PictogramElement target = (PictogramElement) connection.getEnd().getParent();
	        		Point newLastPoint = adjustBendpoint(lastPoint,target);
	        		if (newLastPoint != null){
		        		bendpoints.remove(lastPoint);
		        		bendpoints.add(newLastPoint);
	        		}
            }
		}
		
		DIUtils.updateDI(pelem);
	}
	
	// The layout algorithm uses coordinates of type double whereas
	// Graphiti uses int. Due to rounding, it can happen that lines are not
	// straight, so we fix it here.
	Point adjustBendpoint (Point bendpoint, PictogramElement pelem){

		GraphicsAlgorithm ga;
		int offsetX = 0, offsetY = 0;
		if (pelem instanceof Shape){
			Shape parent = ((Shape) pelem).getContainer();
			while (parent != null){
				ga = parent.getGraphicsAlgorithm();
				offsetX = offsetX + ga.getX();
				offsetY = offsetY + ga.getY();
				parent = parent.getContainer();
			}
		}
		ga = pelem.getGraphicsAlgorithm();
		
		boolean createNewPoint = false;
		int centerX = offsetX + ga.getX() + ga.getWidth() / 2;
		int bendpointX = bendpoint.getX();
		if (bendpointX <= centerX + 1 && bendpointX >= centerX - 1){
			bendpointX = centerX;
			createNewPoint = true;
		}
		
		int centerY = offsetY + ga.getY() + ga.getHeight() / 2;
		int bendpointY = bendpoint.getY();
		if (bendpointY <= centerY + 1 && bendpointY >= centerY - 1){
			bendpointY = centerY;
			createNewPoint = true;
		}
		
		if (createNewPoint){
			Point newPoint = Graphiti.getGaService().createPoint(bendpointX,bendpointY);
			return newPoint;
		}
		return null;
	}
	
	@Override
	protected void applyNodeLayout(final KNode knode, final PictogramElement pelem) {
		
		KShapeLayout nodeLayout = knode.getData(KShapeLayout.class);
		int newX = (int) (nodeLayout.getXpos());
		int newY = (int) (nodeLayout.getYpos());
		GraphicsAlgorithm ga = pelem.getGraphicsAlgorithm();
		ga.setX(newX);
		ga.setY(newY);
		ga.setWidth((int) nodeLayout.getWidth());
		ga.setHeight((int) nodeLayout.getHeight());
		
		if (pelem instanceof Shape){
			// If a node's parent has changed during layout
			// we have to adjust it here, too.
			Shape shape = (Shape) pelem;
			ContainerShape currentParent = shape.getContainer();
			if (currentParent != null){
				KNode kNodeParent = knode.getParent();
				PictogramElement newParent = null;
				for (Entry<PictogramElement, KGraphElement> entry : elements.entrySet()){
					if (entry.getValue() == kNodeParent){
						newParent = entry.getKey();
					}
				}
				if (newParent != currentParent && newParent instanceof ContainerShape){
					shape.setContainer((ContainerShape) newParent);
				}
			}	
			
			// Sub-processes need some special resizing
			BaseElement businessObject = Utils.getBaseElement(shape);
			if (businessObject instanceof SubProcess){
				ResizeShapeContext resizeContext = new ResizeShapeContext(shape);
				
				// TODO: Avoid repair of connections,
				// especially connections within sub-processes, as it takes a while.
				// Unfortunately the line below has no effect
				resizeContext.putProperty(PropertyNames.LAYOUT_REPAIR_CONNECTIONS, false);
				
				resizeContext.setHeight((int) nodeLayout.getHeight());
				resizeContext.setWidth((int) nodeLayout.getWidth());
				resizeContext.setX(newX);
				resizeContext.setY(newY);
				ResizeExpandableActivityFeature resizeFeature = new ResizeExpandableActivityFeature(thefeatureProvider);
				resizeFeature.resizeShape(resizeContext);
			}
		}
		
		// Update label position (label is GraphicsAlgorithm: pools and lanes)
		if (pelem instanceof ContainerShape){
			ContainerShape containerShape = (ContainerShape) pelem;
			for (Shape child : containerShape.getChildren()) {
				GraphicsAlgorithm childGa = child.getGraphicsAlgorithm();
				if (childGa instanceof Text) {
					Graphiti.getGaService().setLocationAndSize(childGa, 5, 0, 15, ga.getHeight());
				}
			}
		}
		
		DIUtils.updateDI(pelem);
	}  
	
	/**
	 *  The labels as they are used in the BPMN modeler are not decorators
	 *  but shapes, so we need to do a slightly different handling here
	 *  than in the original GraphitiLayoutCommand.
	 */
	@Override
    protected void applyEdgeLabelLayout(final KLabel klabel,
            final PictogramElement pelem) {
		GraphicsAlgorithm ga = pelem.getGraphicsAlgorithm();
        KEdge kedge = (KEdge) klabel.eContainer();
        
        KShapeLayout shapeLayout = klabel.getData(KShapeLayout.class);
        KVector position = shapeLayout.createVector();
        KNode parent = kedge.getSource();
        if (!KimlUtil.isDescendant(kedge.getTarget(), parent)) {
            parent = parent.getParent();
        }
        KimlUtil.toAbsolute(position, parent);
        ga.setX((int) Math.round(position.x));
        ga.setY((int) Math.round(position.y) - 7);
    }
	
	@Override
    public void add(final KGraphElement graphElement,
            final PictogramElement pictogramElement) {
			
			elements.put(pictogramElement,graphElement);
        
    }

	

}
