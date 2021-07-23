/*******************************************************************************
 * Copyright (c) 2021 Orange Polska SA.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Michał Wadowski (Orange Polska SA) - Add Cancel Composite-Observation feature.
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.AbstractObservation;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.response.CancelCompositeObservationResponse;
import org.eclipse.leshan.core.util.Hex;

import java.util.Collection;
import java.util.List;

/**
 * A Lightweight M2M request for actively cancel an composite-observation.
 * <p>
 * At server side this will not remove the observation from the observation store, to do it you need to use
 * {@code ObservationService#cancelObservation()}
 * </p>
 */
public class CancelCompositeObservationRequest extends AbstractLwM2mRequest<CancelCompositeObservationResponse>
        implements CompositeDownlinkRequest<CancelCompositeObservationResponse> {

    private final CompositeObservation observation;

    /**
     * @param observation the observation to cancel actively
     */
    public CancelCompositeObservationRequest(CompositeObservation observation) {
        super(null);
        this.observation = observation;
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public AbstractObservation getObservation() {
        return observation;
    }

    @Override
    public final String toString() {
        return String.format("CancelCompositeObservation [paths=%s token=%s]", getPaths(),
                Hex.encodeHexString(observation.getId()));
    }

    @Override
    public List<LwM2mPath> getPaths() {
        return observation.getPaths();
    }

    public ContentFormat getRequestContentFormat() {
        return observation.getRequestContentFormat();
    }

    public ContentFormat getResponseContentFormat() {
        return observation.getResponseContentFormat();
    }
}
