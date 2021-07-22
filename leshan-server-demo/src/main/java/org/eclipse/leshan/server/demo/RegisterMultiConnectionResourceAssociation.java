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
 *     Michał Wadowski (Orange Polska SA) - add multi-protocol capability
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.californium.registration.RegisterResource;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

public class RegisterMultiConnectionResourceAssociation extends RegisterResource implements ConnectionUriAssociation {

    private static final String QUERY_PARAM_ENDPOINT = "ep=";

    private static final String QUERY_PARAM_BINDING_MODE = "b=";

    private static final String QUERY_PARAM_LWM2M_VERSION = "lwm2m=";

    private static final String QUERY_PARAM_SMS = "sms=";

    private static final String QUERY_PARAM_LIFETIME = "lt=";

    private static final String QUERY_PARAM_QUEUEMMODE = "Q"; // since LWM2M 1.1

    private static final Logger LOG = LoggerFactory.getLogger(RegisterResource.class);

    private final RegistrationHandler registrationHandler;

    protected Map<Identity, URI> identityToConnectionUriMap = new HashMap<>();

    public RegisterMultiConnectionResourceAssociation(RegistrationHandler registrationHandler) {
        super(registrationHandler);
        this.registrationHandler = registrationHandler;
    }

    @Override
    protected void handleRegister(CoapExchange exchange, Request request) {
        // Get identity
        // --------------------------------
        Identity sender = extractIdentity(request.getSourceContext());

        // Create LwM2m request from CoAP request
        // --------------------------------
        // We don't check content media type is APPLICATION LINK FORMAT for now as this is the only format we can expect
        String endpoint = null;
        Long lifetime = null;
        String smsNumber = null;
        String lwVersion = null;
        EnumSet<BindingMode> binding = null;
        Boolean queueMode = null;

        // Get object Links
        Link[] objectLinks = Link.parse(request.getPayload());

        Map<String, String> additionalParams = new HashMap<>();

        // Get parameters
        for (String param : request.getOptions().getUriQuery()) {
            if (param.startsWith(QUERY_PARAM_ENDPOINT)) {
                endpoint = param.substring(3);
            } else if (param.startsWith(QUERY_PARAM_LIFETIME)) {
                lifetime = Long.valueOf(param.substring(3));
            } else if (param.startsWith(QUERY_PARAM_SMS)) {
                smsNumber = param.substring(4);
            } else if (param.startsWith(QUERY_PARAM_LWM2M_VERSION)) {
                lwVersion = param.substring(6);
            } else if (param.startsWith(QUERY_PARAM_BINDING_MODE)) {
                binding = BindingMode.parse(param.substring(2));
            } else if (param.equals(QUERY_PARAM_QUEUEMMODE)) {
                queueMode = true;
            } else {
                String[] tokens = param.split("\\=");
                if (tokens != null && tokens.length == 2) {
                    additionalParams.put(tokens[0], tokens[1]);
                }
            }
        }

        // Create request
        Request coapRequest = exchange.advanced().getRequest();
        RegisterRequest registerRequest = new RegisterRequest(endpoint, lifetime, lwVersion, binding, queueMode,
                smsNumber, objectLinks, additionalParams, coapRequest);

        // Handle request
        // -------------------------------
        final SendableResponse<RegisterResponse> sendableResponse = registrationHandler.register(sender,
                registerRequest);
        RegisterResponse response = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        // -------------------------------
        if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CREATED) {
            exchange.setLocationPath(RESOURCE_NAME + "/" + response.getRegistrationID());
            exchange.respond(CoAP.ResponseCode.CREATED);

            URI uri = exchange.advanced().getEndpoint().getUri();

            identityToConnectionUriMap.put(sender, uri);
        } else {
            exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
        }
        sendableResponse.sent();
    }

    @Override
    public URI getConnectionUri(Identity identity) {
        if (identityToConnectionUriMap.containsKey(identity)) {
            return identityToConnectionUriMap.get(identity);
        } else {
            return null;
        }
    }
}
