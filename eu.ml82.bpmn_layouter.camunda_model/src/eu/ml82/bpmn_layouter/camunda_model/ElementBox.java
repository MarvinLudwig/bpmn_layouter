/*
 * Camunda BPMN model wrapper for the BPMN auto-layouter
 *
 * Copyright 2015 by Christophe Bertoncini
 *
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */

package eu.ml82.bpmn_layouter.camunda_model;

import de.cau.cs.kieler.core.math.KVector;
import de.cau.cs.kieler.klay.layered.graph.LShape;

/**
 * Object to handle LShape abstract class properties
 */
public class ElementBox
        extends LShape {

    /** the serial version UID. */
    private static final long serialVersionUID = 5222245004175354687L;

    public ElementBox() {

    }

    public ElementBox(KVector position, KVector size) {

        this.getPosition().x = position.x;
        this.getPosition().y = position.y;

        this.getSize().x = size.x;
        this.getSize().y = size.y;
    }

    public ElementBox(LShape shape, KVector offset) {
        KVector position = shape.getPosition();

        this.getPosition().x = position.x + offset.x;
        this.getPosition().y = position.y + offset.y;

        this.getSize().x = shape.getSize().x;
        this.getSize().y = shape.getSize().y;
    }

}
