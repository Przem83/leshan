/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.response;

import java.util.List;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;

/**
 * Specialized ReadResponse to a Observe request, with the corresponding Observation.
 *
 * This can be useful to listen to updates on the specific Observation.
 */
public class SingleObserveResponse extends ReadResponse {

    protected final SingleObservation observation;
    protected final List<TimestampedLwM2mNode> timestampedValues;

    public SingleObserveResponse(ResponseCode code, LwM2mNode content, List<TimestampedLwM2mNode> timestampedValues,
                                 SingleObservation observation, String errorMessage) {
        this(code, content, timestampedValues, observation, errorMessage, null);
    }

    public SingleObserveResponse(ResponseCode code, LwM2mNode content, List<TimestampedLwM2mNode> timestampedValues,
                                 SingleObservation observation, String errorMessage, Object coapResponse) {
        super(code, getContent(content, timestampedValues), errorMessage, coapResponse);

        // CHANGED is out of spec but is supported for backward compatibility. (previous draft version)
        if (ResponseCode.CHANGED.equals(code)) {
            if (this.content == null)
                throw new InvalidResponseException("Content is mandatory for successful response");
        }

        this.observation = observation;
        this.timestampedValues = hasSingleItemWithoutTimestamp(timestampedValues) ? null : timestampedValues;
    }

    private static LwM2mNode getContent(LwM2mNode content, List<TimestampedLwM2mNode> timestampedValues) {
        if (hasSingleItemWithoutTimestamp(timestampedValues)) {
            content = timestampedValues.get(0).getNode();
            timestampedValues = null;
        }

        return getFirstListItemOrContent(content, timestampedValues);
    }

    private static LwM2mNode getFirstListItemOrContent(LwM2mNode content, List<TimestampedLwM2mNode> timestampedValues) {
        return timestampedValues != null && timestampedValues.size() > 0 ? timestampedValues.get(0).getNode() : content;
    }

    private static boolean hasSingleItemWithoutTimestamp(List<TimestampedLwM2mNode> timestampedValues) {
        return timestampedValues != null && timestampedValues.size() == 1 && !timestampedValues.get(0).isTimestamped();
    }

    public List<TimestampedLwM2mNode> getTimestampedValues() {
        return timestampedValues;
    }

    @Override
    public boolean isSuccess() {
        // CHANGED is out of spec but is supported for backward compatibility. (previous draft version)
        return getCode().equals(ResponseCode.CONTENT) || getCode().equals(ResponseCode.CHANGED);
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("ObserveResponse [code=%s, errormessage=%s]", code, errorMessage);
        else if (timestampedValues != null)
            return String.format("ObserveResponse [code=%s, content=%s, observation=%s, timestampedValues= %d nodes]",
                    code, content, observation, timestampedValues.size());
        else
            return String.format("ObserveResponse [code=%s, content=%s, observation=%s]", code, content, observation);
    }

    public SingleObservation getObservation() {
        return observation;
    }

    // Syntactic sugar static constructors :

    public static SingleObserveResponse success(LwM2mNode content) {
        return new SingleObserveResponse(ResponseCode.CONTENT, content, null, null, null);
    }

    public static SingleObserveResponse success(List<TimestampedLwM2mNode> timestampedValues) {
        return new SingleObserveResponse(ResponseCode.CONTENT, null, timestampedValues, null, null);
    }

    public static SingleObserveResponse badRequest(String errorMessage) {
        return new SingleObserveResponse(ResponseCode.BAD_REQUEST, null, null, null, errorMessage);
    }

    public static SingleObserveResponse notFound() {
        return new SingleObserveResponse(ResponseCode.NOT_FOUND, null, null, null, null);
    }

    public static SingleObserveResponse unauthorized() {
        return new SingleObserveResponse(ResponseCode.UNAUTHORIZED, null, null, null, null);
    }

    public static SingleObserveResponse methodNotAllowed() {
        return new SingleObserveResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null, null);
    }

    public static SingleObserveResponse notAcceptable() {
        return new SingleObserveResponse(ResponseCode.NOT_ACCEPTABLE, null, null, null, null);
    }

    public static SingleObserveResponse internalServerError(String errorMessage) {
        return new SingleObserveResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, null, null, errorMessage);
    }
}
