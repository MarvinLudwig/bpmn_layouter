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
import java.util.Map.Entry;

import org.camunda.bpm.modeler.core.utils.GraphicsUtil;
import org.camunda.bpm.modeler.core.utils.LabelUtil;
import org.camunda.bpm.modeler.ui.diagram.editor.Bpmn2MultiPageEditor;
import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.Group;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.impl.*;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPart;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.mm.algorithms.AbstractText;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.*;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.internal.parts.IPictogramElementEditPart;
import org.eclipse.graphiti.ui.services.GraphitiUi;
import org.eclipse.swt.SWTException;
import org.eclipse.ui.IWorkbenchPart;

import de.cau.cs.kieler.core.kgraph.*;
import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.properties.IProperty;
import de.cau.cs.kieler.kiml.config.VolatileLayoutConfig;
import de.cau.cs.kieler.kiml.graphiti.GraphitiDiagramLayoutManager;
import de.cau.cs.kieler.kiml.graphiti.GraphitiLayoutConfig;
import de.cau.cs.kieler.kiml.klayoutdata.*;
import de.cau.cs.kieler.kiml.klayoutdata.impl.KShapeLayoutImpl;
import de.cau.cs.kieler.kiml.options.EdgeLabelPlacement;
import de.cau.cs.kieler.kiml.options.EdgeType;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.kiml.options.SizeConstraint;
import de.cau.cs.kieler.kiml.service.LayoutMapping;
import de.cau.cs.kieler.kiml.util.KimlUtil;
import de.cau.cs.kieler.kiml.util.nodespacing.Spacing.Margins;
import eu.ml82.bpmn_layouter.core.properties.BpmnElementType;
import eu.ml82.bpmn_layouter.core.properties.BpmnProperties;

import org.eclipse.graphiti.mm.algorithms.styles.Font;

public class DiagramLayoutManager extends GraphitiDiagramLayoutManager {
	
	private KNode root = null;
    
    private boolean supports = false;
    
    private HashMap<BoundaryEvent, KNode> boundaryEvents;
    private HashMap<BoundaryEvent, KNode> boundaryEventsAttachedTo;
    private Diagram diagram;
    
	public KNode getRoot() {
		return root;
	}

	@Override
	public LayoutMapping<PictogramElement> buildLayoutGraph(final IWorkbenchPart workbenchPart,
            Object diagramPart) {
		
		boundaryEvents = new HashMap<BoundaryEvent, KNode>();
		boundaryEventsAttachedTo = new HashMap<BoundaryEvent, KNode>();
		
		diagramPart = workbenchPart.getSite().getPage().getActivePart();
		IWorkbenchPart bpmnEditor = (IWorkbenchPart) ((Bpmn2MultiPageEditor)workbenchPart).getBpmnEditor();
		
		// get diagram
		EditPart editorContent = ((DiagramEditor) bpmnEditor).getGraphicalViewer().getContents();
        PictogramElement pe = ((IPictogramElementEditPart) editorContent).getPictogramElement();
		IPeService peService = Graphiti.getPeService();
		diagram = peService.getDiagramForPictogramElement(pe);
		
		LayoutMapping<PictogramElement> mapping = super.buildLayoutGraph( bpmnEditor, diagramPart);
	
		
		mapping.getLayoutConfigs().add(new VolatileLayoutConfig()	
		.setValue(LayoutOptions.LAYOUT_HIERARCHY,true)
		.setValue(LayoutOptions.ALGORITHM,"eu.ml82.bpmn_layouter")
		.setValue(LayoutOptions.SPACING,40.0f)
		//.setValue(LayoutOptions.ANIMATE,true)
		// This is for GEF only, Graphiti does not support Animation,
		// maybe in the future: https://bugs.eclipse.org/bugs/show_bug.cgi?id=469392
		);
        
        processBoundaryEvents(mapping);
        
		return mapping;	
	}
	
	// Needs to be done after all nodes have been created
	private void processBoundaryEvents(LayoutMapping<PictogramElement> mapping){
		
        VolatileLayoutConfig config = new VolatileLayoutConfig(GraphitiLayoutConfig.PRIORITY - 1);
		
		for (Entry<BoundaryEvent, KNode> entry : boundaryEvents.entrySet()){
        	
			BoundaryEvent boundaryEvent = entry.getKey();
			KNode boundaryEventNode = entry.getValue();
			KNode attachedTo = boundaryEventsAttachedTo.get(boundaryEvent);
			
			// This is important to identify the relationship
			// between the boundary event and its "host"
			// there would be some cleaner ways (without dummy edge) to achieve this
			// but they would be more complicated afterwards.
			KEdge dummyEdge = KimlUtil.createInitializedEdge();
			KEdgeLayout dummyEdgeLayout = dummyEdge.getData(KEdgeLayout.class);
			dummyEdgeLayout.setProperty(BpmnProperties.BOUNDARY_EVENT_DUMMY_EDGE, true);
	        dummyEdge.setTarget(boundaryEventNode);
	        dummyEdge.setSource(attachedTo);	        
	        addLayoutToConfig(config,dummyEdge);
        }
        
        // Add configurations to mapping	        
		mapping.getLayoutConfigs().add(config);
	}
	
	@SuppressWarnings("unchecked")
	private void addLayoutToConfig(VolatileLayoutConfig portConfig,KGraphElement element){
		element.getData();
		KLayoutData layout = element.getData(KLayoutData.class);
		for (Entry<IProperty<?>, Object> property : layout.getProperties()) {
            if (property.getKey() != null && property.getValue() != null) {
                portConfig.setValue((IProperty<Object>) property.getKey(), element,
                		de.cau.cs.kieler.kiml.config.LayoutContext.GRAPH_ELEM, property.getValue());
            }
        }
	}
	
	@Override
    public boolean supports(final Object object) {
        if (supports == true || object.getClass().getName().equals("org.camunda.bpm.modeler.ui.diagram.editor.Bpmn2MultiPageEditor")){
        	supports = true;
        	return true;
        }
        return false;
    }
	
	@Override
	protected boolean isNodeShape(final Shape shape) {
		boolean isLabel = Graphiti.getPeService().getPropertyValue(shape, GraphicsUtil.LABEL_PROPERTY) != null;
		EObject businessObject = Utils.getBpmnDiShape(shape);
		if (businessObject!= null && !isLabel){
	        // Do not create nodes for elements inside of sub processes.
			// By doing so, the content of sub processes is not layouted.
			// That function might be implemented in a later release.
	        if (businessObject instanceof SubProcessImpl) return false;
	        else return true;
		}
		return false;
    }
		
	@Override
	protected KNode createNode(final LayoutMapping<PictogramElement> mapping,
            final KNode parentNode, final Shape shape) {
		
        BaseElement businessObject = Utils.getBaseElement(shape);
              
		KNode childNode = KimlUtil.createInitializedNode();

		childNode.setParent(parentNode);
		
        // set the node's layout, considering margins and insets
        KShapeLayout nodeLayout = (KShapeLayout) childNode.getData(KShapeLayout.class);
        computeInsets(nodeLayout.getInsets(), shape);
        Margins nodeMargins = computeMargins(shape);
        nodeLayout.setProperty(LayoutOptions.MARGINS, nodeMargins);
        GraphicsAlgorithm nodeGa = shape.getGraphicsAlgorithm();
        if (parentNode == null) {
            nodeLayout.setPos(nodeGa.getX() + (float) nodeMargins.left,
                    nodeGa.getY() + (float) nodeMargins.top);
        } else {
            KShapeLayout parentLayout = (KShapeLayout) parentNode.getData(KShapeLayout.class);
            Margins parentMargins = parentLayout.getProperty(LayoutOptions.MARGINS);
            KInsets parentInsets = parentLayout.getInsets();
            nodeLayout.setPos(nodeGa.getX() + (float) (nodeMargins.left - parentMargins.left)
                    - parentInsets.getLeft(),
                    nodeGa.getY() + (float) (nodeMargins.top - parentMargins.top)
                    - parentInsets.getTop());
        }
        nodeLayout.setSize(nodeGa.getWidth(), nodeGa.getHeight());
        
        // The modification flag must initially be false
        nodeLayout.resetModificationFlag();
        
        mapping.getGraphMap().put(childNode, shape);       				
      
        // Add sequence and message flows as connections to the layout
        for (Anchor anchor : shape.getAnchors()) {           
        	for (Connection connection : anchor.getOutgoingConnections()) {
	    		mapping.getProperty(CONNECTIONS).add(connection);
        	}
    	}  
                         
        // Set BPMN element type
        if (businessObject instanceof SubProcess)
        	nodeLayout.setProperty(BpmnProperties.ELEMENT_TYPE, BpmnElementType.SUBPROCESS);
        else if (businessObject instanceof Gateway)
        	nodeLayout.setProperty(BpmnProperties.ELEMENT_TYPE, BpmnElementType.GATEWAY);
    	else if (businessObject instanceof Event)
    		nodeLayout.setProperty(BpmnProperties.ELEMENT_TYPE, BpmnElementType.EVENT);
    	else if (businessObject instanceof DataObjectReferenceImpl || businessObject instanceof DataStoreReferenceImpl) 
    		nodeLayout.setProperty(BpmnProperties.ELEMENT_TYPE, BpmnElementType.ARTIFACT);
    	else if (businessObject instanceof TextAnnotationImpl) 
    		nodeLayout.setProperty(BpmnProperties.ELEMENT_TYPE, BpmnElementType.ARTIFACT);    	
    	else if (businessObject instanceof Group){ 
        	nodeLayout.setProperty(BpmnProperties.ELEMENT_TYPE, BpmnElementType.GROUP);
        	// Allow LayoutCommand to change the group size
        	nodeLayout.setProperty(LayoutOptions.SIZE_CONSTRAINT,SizeConstraint.free());
    	}
        
        // Boundary events need some special handling later
        if (businessObject instanceof BoundaryEventImpl){
        	boundaryEvents.put((BoundaryEvent) businessObject, childNode);
        }	
        // Also, if the current node is a one a boundary event is attached to
        // we save it for later
        if (businessObject instanceof Activity){
        	Activity activity = ((Activity) businessObject);
        	for (BoundaryEvent boundaryEvent : activity.getBoundaryEventRefs()){
        		boundaryEventsAttachedTo.put(boundaryEvent, childNode);
        	}
        } 
        
        // Set ID
        nodeLayout.setProperty(BpmnProperties.ID, businessObject.getId());
        
        // Set label (for debugging purposes only)
        KLabel label = KimlUtil.createInitializedLabel(childNode);
        KShapeLayout labelLayout = (KShapeLayout) label.getData(KShapeLayout.class);
        labelLayout.setPos(10, 10);
        labelLayout.setSize(10, 10);
        String labelText = null;
        if (businessObject instanceof FlowElement){
        	labelText = ((FlowElement) businessObject).getName();
        }
        if (labelText == null || labelText == "") labelText = businessObject.getId();
        label.setText(labelText);
        
		return childNode;
		
	}
	
	@Override
	protected KLabel createLabel(final KLabeledGraphElement parentElement, final Shape shape,
            final float offsetx, final float offsety) {
        KLabel label = KimlUtil.createInitializedLabel(parentElement);
        KShapeLayout labelLayout = (KShapeLayout) label.getData(KShapeLayout.class);
        
        GraphicsAlgorithm ga = ((ContainerShape) shape).getChildren().get(0).getGraphicsAlgorithm();
        int xpos = ga.getX(), ypos = ga.getY();
        int width = ga.getWidth(), height = ga.getHeight();

        if (ga instanceof AbstractText) {
            AbstractText abstractText = (AbstractText) ga;
            String labelText = abstractText.getValue();
            label.setText(labelText);
            
            IGaService gaService = Graphiti.getGaService();
            Font font = gaService.getFont(abstractText, true);
    
            IDimension textSize = null;
            try {
                textSize = GraphitiUi.getUiLayoutService().calculateTextSize(labelText, font);
            } catch (SWTException exception) {
                // ignore exception
            }
            if (textSize != null) {
                if (textSize.getWidth() < width) {
                    int diff = width - textSize.getWidth();
                    switch (gaService.getHorizontalAlignment(abstractText, true)) {
                    case ALIGNMENT_CENTER:
                        xpos += diff / 2;
                        break;
                    case ALIGNMENT_RIGHT:
                        xpos += diff;
                        break;
                    default:
                        break;
                    }
                    width -= diff;
                }
                if (textSize.getHeight() < height) {
                    int diff = height - textSize.getHeight();
                    switch (gaService.getVerticalAlignment(abstractText, true)) {
                    case ALIGNMENT_MIDDLE:
                        ypos += diff / 2;
                        break;
                    case ALIGNMENT_BOTTOM:
                        ypos += diff;
                        break;
                    default:
                        break;
                    }
                    height -= diff;
                }
            }
        }
        
        labelLayout.setPos(xpos + offsetx, ypos + offsety);
        labelLayout.setSize(width, height);
        
        // the modification flag must initially be false
        labelLayout.resetModificationFlag();
        return label;
    }
	
	protected KLabel createEdgeLabel(final LayoutMapping<PictogramElement> mapping,
            final KEdge parentEdge, final Shape labelShape) {
		KLabel label = KimlUtil.createInitializedLabel(parentEdge);
        mapping.getGraphMap().put(label, labelShape);

        // set label placement
        KShapeLayout labelLayout = (KShapeLayout) label.getData(KShapeLayout.class);
        labelLayout.setProperty(LayoutOptions.EDGE_LABEL_PLACEMENT, EdgeLabelPlacement.CENTER);

        GraphicsAlgorithm ga = ((ContainerShape) labelShape).getChildren().get(0).getGraphicsAlgorithm();
        KVector labelPos = new KVector();
        labelPos.x += ga.getX();
        labelPos.y += ga.getY();
        labelLayout.applyVector(labelPos);

        if (ga instanceof AbstractText) {
            AbstractText text = (AbstractText) ga;
            String labelText = text.getValue();
            label.setText(labelText);

            IGaService gaService = Graphiti.getGaService();
            Font font = gaService.getFont(text, true);
    
            if (labelText != null) {
                IDimension textSize = null;
                try {
                    textSize = GraphitiUi.getUiLayoutService().calculateTextSize(labelText, font, true);
                } catch (SWTException exception) {
                    // ignore exception
                }
                if (textSize != null) {
                    labelLayout.setSize(textSize.getWidth(), textSize.getHeight());
                }
            }
        }

        // the modification flag must initially be false
        labelLayout.resetModificationFlag();
        return label;
	}
	
	@Override
	protected KEdge createEdge(final LayoutMapping<PictogramElement> mapping,
            final Connection connection) {
		KEdge edge = super.createEdge(mapping, connection);
		KEdgeLayout edgeLayout = (KEdgeLayout) edge.getData(KEdgeLayout.class);
		BaseElement businessObject = Utils.getBaseElement(connection);
        if (businessObject instanceof MessageFlowImpl) 
        	edgeLayout.setProperty(BpmnProperties.ELEMENT_TYPE,BpmnElementType.MESSAGE_FLOW);
        else if (businessObject instanceof AssociationImpl) 
        	edgeLayout.setProperty(LayoutOptions.EDGE_TYPE,EdgeType.UNDIRECTED);
        	
        ContainerShape labelShape = LabelUtil.getLabelShape(connection, diagram);
        if (labelShape != null){
        	GraphicsAlgorithm ga = ((ContainerShape) labelShape).getChildren().get(0).getGraphicsAlgorithm();
        	if (ga instanceof AbstractText) {
                AbstractText text = (AbstractText) ga;
                String labelText = text.getValue();
            	if (labelText.length() > 0)createEdgeLabel(mapping, edge, labelShape);
        	}
        }
        
		return edge;
	}
	
    @Override // Override in order to use BPMNLayoutCommand
    protected void transferLayout(final LayoutMapping<PictogramElement> mapping) {
        DiagramEditor diagramEditor = mapping.getProperty(DIAGRAM_EDITOR);
        int layoutWidth = (int) ((KShapeLayoutImpl)mapping.getLayoutGraph().getData().get(0)).getWidth();
        LayoutCommand command = new LayoutCommand(diagramEditor.getEditingDomain(),
                diagramEditor.getDiagramTypeProvider().getFeatureProvider(),layoutWidth);
        for (Entry<KGraphElement, PictogramElement> entry : mapping.getGraphMap().entrySet()) {
            command.add(entry.getKey(), entry.getValue());
        }
        mapping.setProperty(LAYOUT_COMMAND, command);
        
        // correct the layout by adding the offset determined from the selection
        KVector offset = mapping.getProperty(COORDINATE_OFFSET);
        if (offset != null) {
            addOffset(mapping.getLayoutGraph(), offset);
        }
    }
    
    // Exact copy of the super method as it is private
    /**
     * Add the given offset to all direct children of the given graph.
     * 
     * @param parentNode the parent node
     * @param offset the offset to add
     */
    private static void addOffset(final KNode parentNode, final KVector offset) {
        // correct the offset with the minimal computed coordinates
        double minx = Integer.MAX_VALUE;
        double miny = Integer.MAX_VALUE;
        for (KNode child : parentNode.getChildren()) {
            KShapeLayout nodeLayout = child.getData(KShapeLayout.class);
            minx = Math.min(minx, nodeLayout.getXpos());
            miny = Math.min(miny, nodeLayout.getYpos());
        }
        
        // add the corrected offset
        offset.add(-minx, -miny);
        KimlUtil.translate(parentNode, (float) offset.x, (float) offset.y);
    }
	
}
