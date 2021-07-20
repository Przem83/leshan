package org.eclipse.leshan.core.response;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.CompositeObservation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositeObserveResponse extends ReadCompositeResponse {

    protected final CompositeObservation observation;

    protected final Map<LwM2mPath, List<TimestampedLwM2mNode>> timestampedValues;

    public CompositeObserveResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content, String errorMessage,
                                    Object coapResponse, CompositeObservation observation) {
        this(code, content, null, errorMessage, coapResponse, observation);
    }

    public CompositeObserveResponse(ResponseCode code, Map<LwM2mPath, LwM2mNode> content, Map<LwM2mPath,
            List<TimestampedLwM2mNode>> timestampedValues, String errorMessage, Object coapResponse,
                                    CompositeObservation observation) {
        super(code, getContent(content, timestampedValues), errorMessage, coapResponse);
        this.timestampedValues = timestampedValues;
        this.observation = observation;
    }

    private static Map<LwM2mPath, LwM2mNode> getContent(Map<LwM2mPath, LwM2mNode> content, Map<LwM2mPath,
            List<TimestampedLwM2mNode>> timestampedValues) {
        if (content != null || timestampedValues == null || timestampedValues.isEmpty()) {
            return content;
        }

        Map<LwM2mPath, LwM2mNode> result = new HashMap<>();
        for (Map.Entry<LwM2mPath, List<TimestampedLwM2mNode>> entry : timestampedValues.entrySet()) {
            if (entry.getValue().size() > 0) {
                LwM2mPath path = entry.getKey();
                LwM2mNode node = entry.getValue().get(0).getNode();
                result.put(path, node);
            }
        }
        return result;
    }

    public CompositeObservation getObservation() {
        return observation;
    }

    public Map<LwM2mPath, List<TimestampedLwM2mNode>> getTimestampedValues() {
        return timestampedValues;
    }

    @Override
    public String toString() {
        if (errorMessage != null) {
            return String.format("CompositeObserveResponse [code=%s, errormessage=%s]", code, errorMessage);
        } else if (timestampedValues != null) {
            return String.format("CompositeObserveResponse [code=%s, content=%s, observation=%s, timestampedValues= " +
                    "%d entries]", code, content, observation, timestampedValues.keySet().size());
        } else {
            return String.format("CompositeObserveResponse [code=%s, content=%s, compositeObservation=%s]", code,
                    content, observation);
        }
    }

    public static CompositeObserveResponse success(Map<LwM2mPath, LwM2mNode> content) {
        return new CompositeObserveResponse(ResponseCode.CONTENT, content, null, null, null);
    }

    public static CompositeObserveResponse notFound() {
        return new CompositeObserveResponse(ResponseCode.NOT_FOUND, null, null, null, null);
    }

    public static CompositeObserveResponse internalServerError(String errorMessage) {
        return new CompositeObserveResponse(ResponseCode.INTERNAL_SERVER_ERROR, null, errorMessage, null, null);
    }
}
