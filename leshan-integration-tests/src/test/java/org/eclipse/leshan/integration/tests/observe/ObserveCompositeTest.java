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
package org.eclipse.leshan.integration.tests.observe;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.AbstractObservation;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

public class ObserveCompositeTest {

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void can_composite_observe_on_single_resource() throws InterruptedException {
        String examplePath = "/3/0/15";
        String exampleValue = "Europe/Paris";

        Registration currentRegistration = helper.getCurrentRegistration();

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        ObserveCompositeResponse observeCompositeResponse = helper.server.send(
                currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, examplePath)
        );

        assertEquals(ResponseCode.CONTENT, observeCompositeResponse.getCode());
        assertNotNull(observeCompositeResponse.getCoapResponse());
        assertThat(observeCompositeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeCompositeResponse.getObservation();
        assertNotNull(observation);
        assertEquals(1, observation.getPaths().size());
        assertEquals(examplePath, observation.getPaths().get(0).toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<AbstractObservation> observations =
                helper.server.getObservationService().getObservations(helper.getCurrentRegistration());
        assertEquals("We should have only one observation", 1, observations.size());
        assertTrue("New observation is not there", observations.contains(observation));

        Map<String, Object> nodes = new HashMap<>();
        nodes.put(examplePath, exampleValue);

        LwM2mResponse writeResponse = helper.server.send(
                helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris")
        );
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = ((ObserveCompositeResponse) listener.getResponse()).getContent();
        assertTrue(content.containsKey(new LwM2mPath(examplePath)));
        assertEquals(LwM2mSingleResource.newStringResource(15, exampleValue), content.get(new LwM2mPath(examplePath)));

        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_composite_observe_on_multiple_resources() throws InterruptedException {
        String examplePath1 = "/3/0/15";
        String examplePath2 = "/3/0/14";
        String exampleValue = "Europe/Paris";

        Registration currentRegistration = helper.getCurrentRegistration();

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        ObserveCompositeResponse observeCompositeResponse = helper.server.send(
                currentRegistration,
                new ObserveCompositeRequest(
                        ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, examplePath1, examplePath2
                )
        );

        assertEquals(ResponseCode.CONTENT, observeCompositeResponse.getCode());
        assertNotNull(observeCompositeResponse.getCoapResponse());
        assertThat(observeCompositeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeCompositeResponse.getObservation();
        assertNotNull(observation);
        assertEquals(2, observation.getPaths().size());
        assertEquals(examplePath1, observation.getPaths().get(0).toString());
        assertEquals(examplePath2, observation.getPaths().get(1).toString());

        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<AbstractObservation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals("We should have only one observation", 1, observations.size());
        assertTrue("New observation is not there", observations.contains(observation));

        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());


        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = ((ObserveCompositeResponse) listener.getResponse()).getContent();
        assertTrue(content.containsKey(new LwM2mPath(examplePath1)));
        assertTrue(content.containsKey(new LwM2mPath(examplePath2)));
        assertEquals(
                LwM2mSingleResource.newStringResource(15, exampleValue),
                content.get(new LwM2mPath(examplePath1))
        );
        assertEquals(
                LwM2mSingleResource.newStringResource(14, "+02"),
                content.get(new LwM2mPath(examplePath2))
        );

        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_composite_observe_on_multiple_resources_with_write_composite() throws InterruptedException {
        String examplePath1 = "/3/0/15";
        String examplePath2 = "/3/0/14";
        String exampleValue1 = "Europe/Paris";
        String exampleValue2 = "+11";

        Registration currentRegistration = helper.getCurrentRegistration();

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        ObserveCompositeResponse observeCompositeResponse = helper.server.send(
                currentRegistration,
                new ObserveCompositeRequest(
                        ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, examplePath1, examplePath2
                )
        );

        assertEquals(ResponseCode.CONTENT, observeCompositeResponse.getCode());
        assertNotNull(observeCompositeResponse.getCoapResponse());
        assertThat(observeCompositeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeCompositeResponse.getObservation();
        assertNotNull(observation);
        assertEquals(2, observation.getPaths().size());
        assertEquals(examplePath1, observation.getPaths().get(0).toString());
        assertEquals(examplePath2, observation.getPaths().get(1).toString());

        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<AbstractObservation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals("We should have only one observation", 1, observations.size());
        assertTrue("New observation is not there", observations.contains(observation));

        Map<String, Object> nodes = new HashMap<>();
        nodes.put(examplePath1, exampleValue1);
        nodes.put(examplePath2, exampleValue2);

        WriteCompositeResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(ContentFormat.SENML_JSON, nodes)
        );
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());


        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = ((ObserveCompositeResponse) listener.getResponse()).getContent();
        assertTrue(content.containsKey(new LwM2mPath(examplePath1)));
        assertTrue(content.containsKey(new LwM2mPath(examplePath2)));
        assertEquals(
                LwM2mSingleResource.newStringResource(15, exampleValue1),
                content.get(new LwM2mPath(examplePath1))
        );
        assertEquals(
                LwM2mSingleResource.newStringResource(14, exampleValue2),
                content.get(new LwM2mPath(examplePath2))
        );

        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }


    @Test
    public void can_observe_instance() throws InterruptedException {
        String examplePath = "/3/0";
        String exampleValue = "Europe/Paris";

        Registration currentRegistration = helper.getCurrentRegistration();

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        ObserveCompositeResponse observeCompositeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, examplePath)
        );

        assertEquals(ResponseCode.CONTENT, observeCompositeResponse.getCode());
        assertNotNull(observeCompositeResponse.getCoapResponse());
        assertThat(observeCompositeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeCompositeResponse.getObservation();
        assertNotNull(observation);
        assertEquals(1, observation.getPaths().size());
        assertEquals(examplePath, observation.getPaths().get(0).toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<AbstractObservation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals("We should have only one observation", 1, observations.size());
        assertTrue("New observation is not there", observations.contains(observation));

        Map<String, Object> nodes = new HashMap<>();
        nodes.put(examplePath, exampleValue);

        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris")
        );
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = ((ObserveCompositeResponse) listener.getResponse()).getContent();
        assertTrue(content.containsKey(new LwM2mPath(examplePath)));

        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(ContentFormat.SENML_JSON, 3, 0));
        assertEquals(readResp.getContent(), content.get(new LwM2mPath(examplePath)));
    }

    @Test
    public void can_observe_object() throws InterruptedException {
        String examplePath = "/3";
        String exampleValue = "Europe/Paris";

        Registration currentRegistration = helper.getCurrentRegistration();

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        ObserveCompositeResponse observeCompositeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, examplePath)
        );

        assertEquals(ResponseCode.CONTENT, observeCompositeResponse.getCode());
        assertNotNull(observeCompositeResponse.getCoapResponse());
        assertThat(observeCompositeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeCompositeResponse.getObservation();
        assertNotNull(observation);
        assertEquals(1, observation.getPaths().size());
        assertEquals(examplePath, observation.getPaths().get(0).toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<AbstractObservation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals("We should have only one observation", 1, observations.size());
        assertTrue("New observation is not there", observations.contains(observation));

        Map<String, Object> nodes = new HashMap<>();
        nodes.put(examplePath, exampleValue);

        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = ((ObserveCompositeResponse) listener.getResponse()).getContent();
        assertTrue(content.containsKey(new LwM2mPath(examplePath)));

        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(ContentFormat.SENML_JSON, 3));
        assertEquals(readResp.getContent(), content.get(new LwM2mPath(examplePath)));
    }
}
