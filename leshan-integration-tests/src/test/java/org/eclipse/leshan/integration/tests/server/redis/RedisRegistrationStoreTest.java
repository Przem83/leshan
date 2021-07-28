/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.server.redis;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.AbstractObservation;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.integration.tests.util.RedisIntegrationTestHelper;
import org.eclipse.leshan.server.californium.observation.ObserveUtil;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RedisRegistrationStoreTest {

    private final String ep = "urn:endpoint";
    private final int port = 23452;
    private final Long lifetime = 10000L;
    private final String sms = "0171-32423545";
    private final EnumSet<BindingMode> binding = EnumSet.of(BindingMode.U, BindingMode.Q, BindingMode.S);
    private final Link[] objectLinks = Link.parse("</3>".getBytes(StandardCharsets.UTF_8));
    private final String registrationId = "4711";
    private final Token exampleToken = Token.EMPTY;

    CaliforniumRegistrationStore store;
    InetAddress address;
    Registration registration;

    RedisIntegrationTestHelper helper;

    @Before
    public void setUp() throws UnknownHostException {
        helper = new RedisIntegrationTestHelper();
        address = InetAddress.getLocalHost();
        store = new RedisRegistrationStore(helper.createJedisPool());
    }

    @Test
    public void get_observation_from_request() {
        // given
        String examplePath = "/1/2/3";

        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        Observation observationToStore = prepareCoapObservationOnSingle(examplePath);

        // when
        store.put(exampleToken, observationToStore);

        // then
        AbstractObservation leshanObservation = store.getObservation(registrationId,
                exampleToken.getBytes()
        );
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof org.eclipse.leshan.core.observation.Observation);
        org.eclipse.leshan.core.observation.Observation observation =
                (org.eclipse.leshan.core.observation.Observation) leshanObservation;
        assertEquals(examplePath, observation.getPath().toString());
    }

    @Test
    public void get_composite_observation_from_request() {
        // given
        List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));
        Token exampleToken = Token.EMPTY;

        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        Observation observationToStore = prepareCoapObservationOnComposite(examplePaths);

        // when
        store.put(exampleToken, observationToStore);

        // then
        AbstractObservation leshanObservation = store.getObservation(registrationId,
                exampleToken.getBytes()
        );
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof CompositeObservation);
        CompositeObservation compositeObservation = (CompositeObservation) leshanObservation;
        assertEquals(examplePaths, compositeObservation.getPaths());
    }

    private void givenASimpleRegistration(Long lifetime) {
        Registration.Builder builder = new Registration.Builder(registrationId, ep, Identity.unsecure(address, port));

        registration = builder.lifeTimeInSec(lifetime)
                .smsNumber(sms)
                .bindingMode(binding)
                .objectLinks(objectLinks)
                .build();
    }

    private Observation prepareCoapObservationOnSingle(String path) {
        ObserveRequest observeRequest = new ObserveRequest(null, path);

        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(
                ep, registrationId, observeRequest
        );

        return prepareCoapObservation(new Request(CoAP.Code.GET), userContext);
    }

    private Observation prepareCoapObservationOnComposite(List<LwM2mPath> paths) {
        ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(null, null, paths);

        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(
                ep, registrationId, observeRequest
        );

        return prepareCoapObservation(new Request(CoAP.Code.FETCH), userContext);
    }

    private Observation prepareCoapObservation(Request coapRequest, Map<String, String> userContext) {
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.setObserve();
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());
        coapRequest.setMID(1);

        coapRequest.setDestinationContext(new AddressEndpointContext(new InetSocketAddress(address, port)));

        return new Observation(coapRequest, null);
    }
}
