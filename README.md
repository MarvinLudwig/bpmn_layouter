Release v0.9: Support for almost every BPMN element, exception: Groups are not supported yet. Soon to come.

BPMN Layouter
=====
This BPMN auto-layouter allows you to make nice looking BPMN diagrams by one click.  
It comes as a plugin for the Eclipse-based [Camunda Modeler](https://github.com/camunda/camunda-modeler) and as a Eclipse-independent class that receives the BPMN diagram as a graph and returns it with new node and edge positions.

The layout algorithm (actually a couple of algorithms) is based on [KIELER](http://www.rtsys.informatik.uni-kiel.de/en/research/kieler/), a graph and layout framework by the University of Kiel - more specifically KLay Layered which is part of the KIELER pragmatics project.

KLay Layered is based on 5 layout phases. You can get a introduction to these layout phases [here](http://rtsys.informatik.uni-kiel.de/~biblio/downloads/theses/kpe-bt.pdf) (theses in German, see chapter 2.3, page 7 ff) or [here](http://rtsys.informatik.uni-kiel.de/~biblio/downloads/theses/jjc-mt.pdf) (theses in English, see chapter 2.4, page 16 ff). 

KIELER is a very modular and well-documented project. It uses intermediate processors between the main layout phases. This makes the layout process rather complex, but also very flexibel.

So much about KIELER, now about this project:

## eu.ml82.bpmn_layouter.camunda_modeler
This package contains the Eclipse plugin. It translates the Graphiti graph to a KIELER `KGraph` object (`DiagramLayoutManager`) and after layouting back again to Graphiti (`LayoutCommand`). 

`KGraph` is KIELERS generic graph format. In this package the transformation from `KLay` to `LGraph` is also invoked. This is a fully KIELER internal process without any BPMN-specific code.
`LGraph` is a special graph class for KIELERS KLay Layered algorithm.

This package is based on `de.cau.cs.kieler.kiml.graphiti` with some BPMN-specific adjustments.

## eu.ml82.bpmn_layouter.core
This is the main layout package. `KlayLayeredForBpmn.doLayout(final LGraph lgraph, PosType posType)` expects an `LGraph` object and returns a layouted `LGraph` object.

The `LGraph` object needs the following BPMN-specific information:
* `BpmnProperties.BOUNDARY_EVENT_DUMMY_EDGE` marks a dummy edge between a boundary event and the activity it is attached to
* `BpmnProperties.ELEMENT_TYPE` marks special BPMN elements (Gateway, Pool, Lane, Subprocess, Message Flow)
* `LayoutOptions.EDGE_TYPE` needs to be set to `EdgeType.UNDIRECTED` for undirected associations.

When working with the `LGraph` layout results, you have to note that you can set `posType` to `PosType.RELATIVE` or `PosType.ABSOLUTE`.
`PosType.RELATIVE` returns the layout results as relative positions which means: 
* Node position are relative to their parent node. Example: A task within a pool has the y-position 35, the pool's y-position is 250, so the task's absolute y-position is 285.
* Port positions (the position where an edge is attached to a node) are relative to the node they belong to.
* Edge bendpoints are relative to source node's container.
* If edge source and target do not belong to the same container, the target port's position is relative to the source port.
In case of `PosType.ABSOLUTE` all positions are absolute, with one exception:
* Port positions (the position where an edge is attached to a node) are relative to the node they belong to.

That's it, everything else is just a plain LGraph with nodes (`LNode`) and edges (`LEdge`).

The BPMN part of the layouter is mainly packed in intermediate layout processors. The detailed description is coming soon:
### Pools / Lanes
### Message Flows
### Boundary Events
### Artifacts
### Groups

## Dependencies
* KIELER Pragmatics 2015-06
* Camunda Modeler 2.7.0

## Build
You can build via Maven or in Eclipse by clicking "Build All" in the site.xml settings dialog.  

## Compiled Plugin
You can install this plugin via its [update site](http://www.marvin-ludwig.de/files/bpmn_layouter/updatesite)

## Eclipse-independent bundle
The core layout function has some indirect dependencies on Eclipse (mainly EMF) on the plugin level. Though on class level there are no Eclipse dependencies.  
Using the Maven assembly plugin you can build a bundle with all class-level dependencies by running  
`mvn -f pom_bundle.xml dependency:unpack-dependencies assembly:single` in the eu.ml82.bpmn_layouter.core folder.
