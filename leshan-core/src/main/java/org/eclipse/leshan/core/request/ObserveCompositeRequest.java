/**
 * Copyright (c) 2021 Orange
 *
 * This source code is licensed under the EPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;

import java.util.*;

public class ObserveCompositeRequest extends AbstractLwM2mRequest<ObserveCompositeResponse>
        implements CompositeDownlinkRequest<ObserveCompositeResponse> {

    private final ContentFormat requestContentFormat;
    private final ContentFormat responseContentFormat;

    private final List<LwM2mPath> paths;

    private final Map<String, String> context;

    public ObserveCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat, String... paths) {
        this(requestContentFormat, responseContentFormat, newPaths(paths));
    }

    public ObserveCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat, List<LwM2mPath> paths) {
        this(requestContentFormat, responseContentFormat, paths, null);
    }

    public ObserveCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat, List<LwM2mPath> paths, Object coapRequest) {
        super(coapRequest);

        this.requestContentFormat = requestContentFormat;
        this.responseContentFormat = responseContentFormat;
        this.paths = paths;

//        if (context == null || context.isEmpty())
            this.context = Collections.emptyMap();
//        else
//            this.context = Collections.unmodifiableMap(new HashMap<>(context));
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<LwM2mPath> getPaths() {
        return paths;
    }

    protected static List<LwM2mPath> newPaths(String[] paths) {
        try {
            List<LwM2mPath> res = new ArrayList<>(paths.length);
            for (String path : paths) {
                res.add(new LwM2mPath(path));
            }
            return res;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException();
        }
    }

    public ContentFormat getRequestContentFormat() {
        return requestContentFormat;
    }

    public ContentFormat getResponseContentFormat() {
        return responseContentFormat;
    }

    public Map<String, String> getContext() {
        return context;
    }
}
