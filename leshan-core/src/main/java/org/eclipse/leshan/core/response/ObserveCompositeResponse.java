/**
 * Copyright (c) 2021 Orange
 *
 * This source code is licensed under the EPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.eclipse.leshan.core.response;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;

import java.util.List;
import java.util.Map;

public class ObserveCompositeResponse extends ReadCompositeResponse {

    protected final Observation observation;

    public ObserveCompositeResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content, String errorMessage, Object coapResponse, Observation observation) {
        super(code, content, errorMessage, coapResponse);

        this.observation = observation;
    }

    public Observation getObservation() {
        return observation;
    }

    //    protected final Observation observation;
//    protected final List<TimestampedLwM2mNode> timestampedValues;

//    public ObserveCompositeResponse(ResponseCode code, LwM2mNode content, List<TimestampedLwM2mNode> timestampedValues,
//                                    Observation observation, String errorMessage) {
//        this(code, content, timestampedValues, observation, errorMessage, null);
//    }

//    public ObserveCompositeResponse(ResponseCode code, LwM2mNode content, List<TimestampedLwM2mNode> timestampedValues,
//                                    Observation observation, String errorMessage, Object coapResponse) {
//        super(code, timestampedValues != null && !timestampedValues.isEmpty() ? timestampedValues.get(0).getNode()
//                : content, errorMessage, coapResponse);
//
//        // CHANGED is out of spec but is supported for backward compatibility. (previous draft version)
//        if (ResponseCode.CHANGED.equals(code)) {
//            if (content == null)
//                throw new InvalidResponseException("Content is mandatory for successful response");
//        }
//
//        this.observation = observation;
//        this.timestampedValues = timestampedValues;
//    }

//    public List<TimestampedLwM2mNode> getTimestampedLwM2mNode() {
//        return timestampedValues;
//    }
//
//    @Override
//    public boolean isSuccess() {
//        // CHANGED is out of spec but is supported for backward compatibility. (previous draft version)
//        return getCode().equals(ResponseCode.CONTENT) || getCode().equals(ResponseCode.CHANGED);
//    }
//
//    @Override
//    public String toString() {
//        if (errorMessage != null)
//            return String.format("ObserveResponse [code=%s, errormessage=%s]", code, errorMessage);
//        else if (timestampedValues != null)
//            return String.format("ObserveResponse [code=%s, content=%s, observation=%s, timestampedValues= %d nodes]",
//                    code, content, observation, timestampedValues.size());
//        else
//            return String.format("ObserveResponse [code=%s, content=%s, observation=%s]", code, content, observation);
//    }
//
//    public Observation getObservation() {
//        return observation;
//    }
//
//    // Syntactic sugar static constructors :

    public static ObserveCompositeResponse success(Map<LwM2mPath, LwM2mNode> content) {
        return new ObserveCompositeResponse(ResponseCode.CONTENT, content, null, null, null);
    }

//    public static ObserveCompositeResponse success(LwM2mNode content) {
//        return new ObserveCompositeResponse(ResponseCode.CONTENT, content, null, null, null);
//    }
//
//    public static ObserveCompositeResponse success(List<TimestampedLwM2mNode> timestampedValues) {
//        return new ObserveCompositeResponse(ResponseCode.CONTENT, null, timestampedValues, null, null);
//    }
//
//    public static ObserveCompositeResponse badRequest(String errorMessage) {
//        return new ObserveCompositeResponse(ResponseCode.BAD_REQUEST, null, null, null, errorMessage);
//    }
//
    public static ObserveCompositeResponse notFound() {
        return new ObserveCompositeResponse(ResponseCode.NOT_FOUND, null, null, null, null);
    }
//
//    public static ObserveCompositeResponse unauthorized() {
//        return new ObserveCompositeResponse(ResponseCode.UNAUTHORIZED, null, null, null, null);
//    }
//
//    public static ObserveCompositeResponse methodNotAllowed() {
//        return new ObserveCompositeResponse(ResponseCode.METHOD_NOT_ALLOWED, null, null, null, null);
//    }
//
//    public static ObserveCompositeResponse notAcceptable() {
//        return new ObserveCompositeResponse(ResponseCode.NOT_ACCEPTABLE, null, null, null, null);
//    }
//
    public static ObserveCompositeResponse internalServerError(String errorMessage) {
        return new ObserveCompositeResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, errorMessage, null, null);
    }
}
