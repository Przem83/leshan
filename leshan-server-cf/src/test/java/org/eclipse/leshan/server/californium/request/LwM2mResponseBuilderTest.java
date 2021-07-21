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
 *     Michał Wadowski (Orange Polska SA) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.DummyDecoder;
import org.eclipse.leshan.server.californium.observation.ObserveUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LwM2mResponseBuilderTest {

    @Test
    public void visit_observe_request() {
        // given
        String examplePath = "/1/2/3";

        ObserveRequest observeRequest = new ObserveRequest(null, examplePath);

        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(
                null, null, observeRequest
        );

        Request coapRequest = new Request(null);
        coapRequest.setToken(Token.EMPTY);
        coapRequest.setUserContext(userContext);

        Response coapResponse = new Response(CoAP.ResponseCode.CONTENT);
        coapResponse.getOptions().setObserve(1);

        LwM2mResponseBuilder<ObserveResponse> responseBuilder = new LwM2mResponseBuilder<>(
                coapRequest, coapResponse, null, null, new DummyDecoder()
        );
        // when
        responseBuilder.visit(observeRequest);

        // then
        ObserveResponse response = responseBuilder.getResponse();
        assertNotNull(response);
        assertNotNull(response.getObservation());

        Observation observation = response.getObservation();
        assertEquals(examplePath, observation.getPath().toString());
    }

    @Test
    public void visit_observe_composite_request() {
        // given
        List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));

        ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(null, null, examplePaths);

        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(
                null, null, observeRequest
        );

        Request coapRequest = new Request(null);
        coapRequest.setToken(Token.EMPTY);
        coapRequest.setUserContext(userContext);

        Response coapResponse = new Response(CoAP.ResponseCode.CONTENT);
        coapResponse.getOptions().setObserve(1);

        LwM2mResponseBuilder<ObserveCompositeResponse> responseBuilder = new LwM2mResponseBuilder<>(
                coapRequest, coapResponse, null, null, new DummyDecoder()
        );
        // when
        responseBuilder.visit(observeRequest);

        // then
        ObserveCompositeResponse response = responseBuilder.getResponse();
        assertNotNull(response);
        assertNotNull(response.getObservation());

        CompositeObservation observation = response.getObservation();
        assertEquals(examplePaths, observation.getPaths());
    }

}
