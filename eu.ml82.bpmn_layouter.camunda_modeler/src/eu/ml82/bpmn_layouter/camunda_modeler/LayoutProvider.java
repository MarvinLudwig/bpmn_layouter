/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2010 by
 * + Christian-Albrechts-University of Kiel
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.camunda_modeler;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.kgraph.KNode;
import de.cau.cs.kieler.kiml.AbstractLayoutProvider;
import de.cau.cs.kieler.klay.layered.KlayLayered;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.transform.IGraphTransformer;
import de.cau.cs.kieler.klay.layered.graph.transform.KGraphTransformer;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.KlayLayeredForBpmn;
import eu.ml82.bpmn_layouter.core.properties.PosType;

/**
 * Layout provider to connect the layered layouter to the Eclipse based layout services.
 * 
 * @see KlayLayered
 * 
 * @author msp
 * @author cds
 * @kieler.design 2012-08-10 chsch grh
 * @kieler.rating proposed yellow by msp
 */
public final class LayoutProvider extends AbstractLayoutProvider {
    
    ///////////////////////////////////////////////////////////////////////////////
    // Variables

    /** the layout algorithm used for regular layout runs. */
    private final KlayLayeredForBpmn klayLayeredForBpmn = new KlayLayeredForBpmn();


    ///////////////////////////////////////////////////////////////////////////////
    // Regular Layout
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void doLayout(final KNode kgraph, final IKielerProgressMonitor progressMonitor) {
        // Import the graph (layeredGraph won't be null since the KGraphImporter always returns an
        // LGraph instance, even though the IGraphImporter interface would allow null as a return
        // value)
        IGraphTransformer<KNode> graphImporter = new KGraphTransformer();
        LGraph layeredGraph = graphImporter.importGraph(kgraph);

        klayLayeredForBpmn.doLayout(layeredGraph, PosType.RELATIVE, progressMonitor);
        
        if (!progressMonitor.isCanceled()) {
        	// Apply parent node changes
        	applyParentNodeChanges(layeredGraph);
            // Apply the layout results to the original graph
            graphImporter.applyLayout(layeredGraph);
        }
    }
    
    private void applyParentNodeChanges(LGraph graph){
    	for (LNode node : graph.getLayerlessNodes()){
    		LGraph nestedGraph = node.getProperty(InternalProperties.NESTED_LGRAPH);
    		if (nestedGraph != null) applyParentNodeChanges(nestedGraph);
    		Object origin = node.getProperty(InternalProperties.ORIGIN);
    		if (origin instanceof KNode){
    			KNode kNodeOrigin = ((KNode) origin);
    			LNode lNodeParent = node.getProperty(InternalProperties.PARENT_LNODE);
    			if (lNodeParent != null){
        			KNode newParent = (KNode) lNodeParent.getProperty(InternalProperties.ORIGIN);
        			KNode currentParent = kNodeOrigin.getParent();
        			if (currentParent != newParent){
        				kNodeOrigin.setParent(newParent);
        			}
    			}
    		}
    	}
    }
    
    
    /**
     * Return the layered layout algorithm.
     * 
     * @return the layout algorithm
     */
    public KlayLayeredForBpmn getLayoutAlgorithm() {
        return klayLayeredForBpmn;
    }

}

