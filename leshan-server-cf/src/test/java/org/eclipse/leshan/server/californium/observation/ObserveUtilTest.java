package org.eclipse.leshan.server.californium.observation;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CompositeObserveRequest;
import org.eclipse.leshan.core.request.SingleObserveRequest;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ObserveUtilTest {

    @Test
    public void should_create_single_observation_from_context() {
        // given
        String examplePath = "/1/2/3";
        String exampleRegistrationId = "registrationId";
        Token exampleToken = Token.EMPTY;

        SingleObserveRequest observeRequest = new SingleObserveRequest(null, examplePath);

        // when
        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(
                null, exampleRegistrationId, observeRequest
        );
        userContext.put("extraKey", "extraValue");

        Request coapRequest = new Request(null);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());

        SingleObservation observation = ObserveUtil.createLwM2mSingleObservation(coapRequest);

        // then
        assertEquals(examplePath, observation.getPath().toString());
        assertEquals(exampleRegistrationId, observation.getRegistrationId());
        assertEquals(exampleToken.getBytes(), observation.getId());
        assertTrue(observation.getContext().containsKey("extraKey"));
        assertEquals("extraValue", observation.getContext().get("extraKey"));
        assertEquals(ContentFormat.DEFAULT, observation.getContentFormat());
    }

    @Test
    public void should_create_composite_observation_from_context() {
        // given
        List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));
        String exampleRegistrationId = "registrationId";
        Token exampleToken = Token.EMPTY;

        CompositeObserveRequest observeRequest = new CompositeObserveRequest(null, null, examplePaths);

        // when
        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(
                null, exampleRegistrationId, observeRequest
        );
        userContext.put("extraKey", "extraValue");

        Request coapRequest = new Request(null);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());

        CompositeObservation observation = ObserveUtil.createLwM2mCompositeObservation(coapRequest);

        // then
        assertEquals(examplePaths, observation.getPaths());
        assertEquals(exampleRegistrationId, observation.getRegistrationId());
        assertEquals(exampleToken.getBytes(), observation.getId());
        assertTrue(observation.getContext().containsKey("extraKey"));
        assertEquals("extraValue", observation.getContext().get("extraKey"));
        assertEquals(ContentFormat.DEFAULT, observation.getContentFormat());
    }

    @Test
    public void should_not_create_observation_without_context() {
        // given
        final Request coapRequest = new Request(null);

        // when / then
        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                ObserveUtil.createLwM2mSingleObservation(coapRequest);
            }
        });
    }

    @Test
    public void should_not_create_observation_without_path_in_context() {
        // given
        Map<String, String> userContext = new HashMap<>();

        final Request coapRequest = new Request(null);
        coapRequest.setUserContext(userContext);

        // when / then
        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                ObserveUtil.createLwM2mSingleObservation(coapRequest);
            }
        });
    }
}