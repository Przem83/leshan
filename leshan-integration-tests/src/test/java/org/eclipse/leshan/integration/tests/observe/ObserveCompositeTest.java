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

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//@RunWith(Parameterized.class)
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
    public void can_read_resources() throws InterruptedException {
        Registration currentRegistration = helper.getCurrentRegistration();


        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        ObserveCompositeResponse observeCompositeResponse = helper.server.send(
                currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON,"/3/0/15")
        );

        assertEquals(ResponseCode.CONTENT, observeCompositeResponse.getCode());

        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, observeCompositeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        listener.getResponse().getContent();
//        assertEquals(LwM2mSingleResource.newStringResource(15, "Europe/Paris"), listener.getResponse().getContent());

//        ReadCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
//                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1/0/1"));
//
//        // verify result
//        assertEquals(CONTENT, response.getCode());
//        assertContentFormat(responseContentFormat, response);
//
//        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
//        assertEquals(0, resource.getId());
//        assertEquals(Type.STRING, resource.getType());
//
//        resource = (LwM2mSingleResource) response.getContent("/1/0/1");
//        assertEquals(1, resource.getId());
//        assertEquals(Type.INTEGER, resource.getType());

    }

}
