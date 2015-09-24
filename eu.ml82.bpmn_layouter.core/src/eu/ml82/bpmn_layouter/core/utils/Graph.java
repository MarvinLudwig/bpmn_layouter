package eu.ml82.bpmn_layouter.core.utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.core.math.KVectorChain;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import eu.ml82.bpmn_layouter.core.properties.PosType;

public class Graph {
	
	/** 
	 * Output graph as PNG image
	 */
	public static void draw(LGraph graph, String fileName, PosType posType){
		KVector graphSize = graph.getSize();
	    BufferedImage image = new BufferedImage((int) graphSize.x, (int) graphSize.y, BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = image.createGraphics();
	    g.setColor(Color.WHITE);
	    g.fillRect(0, 0, (int) graphSize.x, (int) graphSize.y);
	    
	    g.setPaint(Color.BLACK);
		drawRecursive(graph,new KVector(0,0),g,posType);
	    
	    try {
			ImageIO.write(image, "PNG", new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void drawRecursive(LGraph graph, KVector offset, Graphics g, PosType posType){
		for (LNode node : graph.getLayerlessNodes()){
			// draw node
			KVector nodePos;
			if (posType == PosType.RELATIVE) nodePos = KVector.sum(node.getPosition(), offset);
			else nodePos = node.getPosition();	
			KVector nodeSize = node.getSize();
			g.drawRect((int) nodePos.x,(int) nodePos.y,(int) nodeSize.x,(int) nodeSize.y);
			// draw node label
			if (node.getLabels().size() > 0){
				LLabel nodeLabel = node.getLabels().get(0);
				KVector nodeLabelPos = KVector.sum(nodeLabel.getPosition(), nodePos);
				g.drawString(nodeLabel.getText(), (int) nodeLabelPos.x, (int) nodeLabelPos.y);
			}
			// draw edges
			for (LEdge edge : node.getOutgoingEdges()){
				// get waypoints
				KVectorChain bendpoints = edge.getBendPoints();
				int pointCount = bendpoints.size()+2;
				KVector[] waypoints = new KVector[pointCount];
				LPort sourcePort = edge.getSource(); 
				waypoints[0] = KVector.sum(sourcePort.getPosition(), nodePos);
				int i = 1;
				for (KVector bendpoint : bendpoints){
					if (posType == PosType.RELATIVE) waypoints[i] = KVector.sum(bendpoint, offset);
					else  waypoints[i] = bendpoint;
					i++;
				}
				LPort targetPort = edge.getTarget(); 
				if (posType == PosType.RELATIVE) waypoints[i] = KVector.sum(targetPort.getPosition(), targetPort.getNode().getPosition(), offset);
				else waypoints[i] = KVector.sum(targetPort.getPosition(), targetPort.getNode().getPosition()); 
				// draw line
				int [] pointsX = new int[pointCount];
				int [] pointsY = new int[pointCount];
				i = 0;
				for (KVector waypoint : waypoints){
					pointsX[i] = (int) waypoint.x;
					pointsY[i] = (int) waypoint.y;
					i++;
				}
				g.drawPolyline(pointsX, pointsY, pointCount);
				// draw edge label
				for (LLabel edgeLabel : edge.getLabels()){
					KVector edgeLabelPos;
					edgeLabelPos = KVector.sum(edgeLabel.getPosition(),offset);
					g.drawString(edgeLabel.getText(), (int) edgeLabelPos.x, (int) edgeLabelPos.y);
				}
			}
			// draw node's nested graph
			LGraph nestedGraph = node.getProperty(InternalProperties.NESTED_LGRAPH);
			if (nestedGraph != null) {
				KVector newOffset = new KVector(KVector.sum(offset, node.getPosition()));
				drawRecursive(nestedGraph, newOffset, g, posType);
			}
		}
	}

}
