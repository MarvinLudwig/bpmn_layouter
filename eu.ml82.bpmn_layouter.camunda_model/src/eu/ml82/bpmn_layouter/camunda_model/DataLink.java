/*
 * Camunda BPMN model wrapper for the BPMN auto-layouter
 *
 * Copyright 2015 by Christophe Bertoncini
 *
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.camunda_model;

/**
 * "Sequence" between DataObjectReference and Activity
 */
public class DataLink {

    String id;

    String sourceId;

    String targetId;

    public DataLink(String id, String sourceId, String targetId) {
        this.id = id;

        this.sourceId = sourceId;
        this.targetId = targetId;
    }

    public String getId() {
        return this.id;
    }

    public String getSourceId() {
        return this.sourceId;
    }

    public String getTargetId() {
        return this.targetId;
    }

}
