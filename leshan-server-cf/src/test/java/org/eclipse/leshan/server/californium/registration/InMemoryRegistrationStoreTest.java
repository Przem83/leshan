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
package org.eclipse.leshan.server.californium.registration;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.server.californium.observation.ObserveUtil;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InMemoryRegistrationStoreTest {

    CaliforniumRegistrationStore store;
    String ep = "urn:endpoint";
    InetAddress address;
    int port = 23452;
    Long lifetime = 10000L;
    String sms = "0171-32423545";
    EnumSet<BindingMode> binding = EnumSet.of(BindingMode.U, BindingMode.Q, BindingMode.S);
    Link[] objectLinks = Link.parse("</3>".getBytes(StandardCharsets.UTF_8));
    String registrationId = "4711";
    Registration registration;

    @Before
    public void setUp() throws Exception {
        address = InetAddress.getLocalHost();
        store = new InMemoryRegistrationStore();
    }

    @Test
    public void update_registration_keeps_properties_unchanged() {
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        RegistrationUpdate update = new RegistrationUpdate(registrationId, Identity.unsecure(address, port), null, null,
                null, null, null);
        UpdatedRegistration updatedRegistration = store.updateRegistration(update);
        assertEquals(lifetime, updatedRegistration.getUpdatedRegistration().getLifeTimeInSec());
        Assert.assertSame(binding, updatedRegistration.getUpdatedRegistration().getBindingMode());
        assertEquals(sms, updatedRegistration.getUpdatedRegistration().getSmsNumber());

        assertEquals(registration, updatedRegistration.getPreviousRegistration());

        Registration reg = store.getRegistrationByEndpoint(ep);
        assertEquals(lifetime, reg.getLifeTimeInSec());
        Assert.assertSame(binding, reg.getBindingMode());
        assertEquals(sms, reg.getSmsNumber());
    }

    @Test
    public void client_registration_sets_time_to_live() {
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);
        Assert.assertTrue(registration.isAlive());
    }

    @Test
    public void update_registration_to_extend_time_to_live() {
        givenASimpleRegistration(0L);
        store.addRegistration(registration);
        Assert.assertFalse(registration.isAlive());

        RegistrationUpdate update = new RegistrationUpdate(registrationId, Identity.unsecure(address, port), lifetime,
                null, null, null, null);
        UpdatedRegistration updatedRegistration = store.updateRegistration(update);
        Assert.assertTrue(updatedRegistration.getUpdatedRegistration().isAlive());

        Registration reg = store.getRegistrationByEndpoint(ep);
        Assert.assertTrue(reg.isAlive());
    }

    @Test
    public void put_coap_observation_with_valid_request() {
        // given
        String examplePath = "/1/2/3";
        Token exampleToken = Token.EMPTY;

        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        Observation observationToStore = prepareCoapObservation(examplePath, exampleToken);

        // when
        store.put(exampleToken, observationToStore);

        // then
        Observation observationFetched = store.get(exampleToken);

        assertNotNull(observationFetched);
        assertEquals(observationToStore.toString(), observationFetched.toString());
    }

    @Test
    public void get_single_observation_from_request() {
        // given
        String examplePath = "/1/2/3";
        Token exampleToken = Token.EMPTY;

        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        Observation observationToStore = prepareCoapObservation(examplePath, exampleToken);

        // when
        store.put(exampleToken, observationToStore);

        // then
        org.eclipse.leshan.core.observation.Observation leshanObservation = store.getObservation(registrationId, exampleToken.getBytes());
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof SingleObservation);
        SingleObservation singleObservation = (SingleObservation) leshanObservation;
        assertEquals(examplePath, singleObservation.getPath().toString());
    }

    @Test
    public void get_composite_observation_from_request() {
        // given
        List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));
        Token exampleToken = Token.EMPTY;

        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        Observation observationToStore = prepareCoapObservation(examplePaths, exampleToken);

        // when
        store.put(exampleToken, observationToStore);

        // then
        org.eclipse.leshan.core.observation.Observation leshanObservation = store.getObservation(registrationId, exampleToken.getBytes());
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof CompositeObservation);
        CompositeObservation compositeObservation = (CompositeObservation) leshanObservation;
        assertEquals(examplePaths, compositeObservation.getPaths());
    }

    private Observation prepareCoapObservation(String path, Token token) {
        SingleObserveRequest observeRequest = new SingleObserveRequest(null, path);

        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(
                ep, registrationId, observeRequest
        );

        Request coapRequest = new Request(CoAP.Code.GET);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(token);
        coapRequest.setObserve();
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());

        return new Observation(coapRequest, null);
    }

    private Observation prepareCoapObservation(List<LwM2mPath> paths, Token token) {
        CompositeObserveRequest observeRequest = new CompositeObserveRequest(null, null, paths);

        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(
                ep, registrationId, observeRequest
        );

        Request coapRequest = new Request(CoAP.Code.FETCH);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(token);
        coapRequest.setObserve();
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());

        return new Observation(coapRequest, null);
    }

    private void givenASimpleRegistration(Long lifetime) {

        Registration.Builder builder = new Registration.Builder(registrationId, ep, Identity.unsecure(address, port));

        registration = builder.lifeTimeInSec(lifetime).smsNumber(sms).bindingMode(binding).objectLinks(objectLinks)
                .build();
    }
}
