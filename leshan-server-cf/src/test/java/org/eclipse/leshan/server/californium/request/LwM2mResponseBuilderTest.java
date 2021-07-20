package org.eclipse.leshan.server.californium.request;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.CompositeObserveRequest;
import org.eclipse.leshan.core.request.SingleObserveRequest;
import org.eclipse.leshan.core.response.CompositeObserveResponse;
import org.eclipse.leshan.core.response.SingleObserveResponse;
import org.eclipse.leshan.server.californium.DummyDecoder;
import org.eclipse.leshan.server.californium.observation.ObserveUtil;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LwM2mResponseBuilderTest {

    @Test
    public void visit_observe_request() {
        // given
        String examplePath = "/1/2/3";

        SingleObserveRequest observeRequest = new SingleObserveRequest(null, examplePath);

        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(
                null, null, observeRequest
        );

        Request coapRequest = new Request(null);
        coapRequest.setToken(Token.EMPTY);
        coapRequest.setUserContext(userContext);

        Response coapResponse = new Response(CoAP.ResponseCode.CONTENT);
        coapResponse.getOptions().setObserve(1);

        LwM2mResponseBuilder<SingleObserveResponse> responseBuilder = new LwM2mResponseBuilder<>(
                coapRequest, coapResponse, null, null, new DummyDecoder()
        );
        // when
        responseBuilder.visit(observeRequest);

        // then
        SingleObserveResponse response = responseBuilder.getResponse();
        assertNotNull(response);
        assertNotNull(response.getObservation());

        SingleObservation observation = response.getObservation();
        assertEquals(examplePath, observation.getPath().toString());
    }

    @Test
    public void visit_observe_composite_request() {
        // given
        List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));

        CompositeObserveRequest observeRequest = new CompositeObserveRequest(null, null, examplePaths);

        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(
                null, null, observeRequest
        );

        Request coapRequest = new Request(null);
        coapRequest.setToken(Token.EMPTY);
        coapRequest.setUserContext(userContext);

        Response coapResponse = new Response(CoAP.ResponseCode.CONTENT);
        coapResponse.getOptions().setObserve(1);

        LwM2mResponseBuilder<CompositeObserveResponse> responseBuilder = new LwM2mResponseBuilder<>(
                coapRequest, coapResponse, null, null, new DummyDecoder()
        );
        // when
        responseBuilder.visit(observeRequest);

        // then
        CompositeObserveResponse response = responseBuilder.getResponse();
        assertNotNull(response);
        assertNotNull(response.getObservation());

        CompositeObservation observation = response.getObservation();
        assertEquals(examplePaths, observation.getPaths());
    }

}