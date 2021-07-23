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
package org.eclipse.leshan.core.response;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.CompositeObservation;

import java.util.List;
import java.util.Map;

public class CancelCompositeObservationResponse extends ObserveCompositeResponse {

    public CancelCompositeObservationResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content, String errorMessage,
            Object coapResponse, CompositeObservation observation) {
        super(code, content, errorMessage, coapResponse, observation);
    }

    public CancelCompositeObservationResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content,
            Map<LwM2mPath, List<TimestampedLwM2mNode>> timestampedValues, String errorMessage, Object coapResponse,
            CompositeObservation observation) {
        super(code, content, timestampedValues, errorMessage, coapResponse, observation);
    }

    @Override
    public String toString() {
        if (errorMessage != null) {
            return String.format("CancelCompositeObservationResponse [code=%s, errormessage=%s]", code, errorMessage);
        } else if (timestampedValues != null) {
            return String.format("CancelCompositeObservationResponse [code=%s, content=%s, observation=%s, " +
                    "timestampedValues=" + "%d entries]", code, content, observation,
                    timestampedValues.keySet().size());
        } else {
            return String.format("CancelCompositeObservationResponse [code=%s, content=%s, compositeObservation=%s]",
                    code, content, observation);
        }
    }
}
