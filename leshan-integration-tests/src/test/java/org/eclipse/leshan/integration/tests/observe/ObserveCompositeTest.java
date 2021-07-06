/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.observe;

import org.eclipse.leshan.client.californium.request.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//@RunWith(Parameterized.class)
public class ObserveCompositeTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    Logger log = LoggerFactory.getLogger(ObserveCompositeTest.class);

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
    public void can_read_resources() throws InterruptedException {

        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

//        ObserveCompositeResponse observeCompositeResponse = helper.server.send(
//                currentRegistration,
//                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON,"/3/0/15")
//        );

        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(3, 0, 15));

        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

//        Map<String, Object> nodes = new HashMap<>();
//        nodes.put("/3/0/15", "Europe/Paris");
//
//        WriteCompositeResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
//                new WriteCompositeRequest(ContentFormat.SENML_JSON, nodes));

        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        assertTrue(listener.receivedNotify().get());
        assertEquals(LwM2mSingleResource.newStringResource(15, "Europe/Paris"), listener.getResponse().getContent());

    }

}
