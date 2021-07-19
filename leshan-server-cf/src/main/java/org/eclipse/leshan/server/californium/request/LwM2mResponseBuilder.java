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
package org.eclipse.leshan.server.californium.request;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toLwM2mResponseCode;

import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.observation.ObserveUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is able to create a {@link LwM2mResponse} from a CoAP {@link Response}.
 * <p>
 * Call <code>LwM2mResponseBuilder#visit(coapResponse)</code>, then get the result using {@link #getResponse()}
 * 
 * @param <T> the type of the response to build.
 */
public class LwM2mResponseBuilder<T extends LwM2mResponse> implements DownlinkRequestVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mResponseBuilder.class);

    private LwM2mResponse lwM2mresponse;
    private final Request coapRequest;
    private final Response coapResponse;
    private final String clientEndpoint;
    private final LwM2mModel model;
    private final LwM2mDecoder decoder;

    public LwM2mResponseBuilder(Request coapRequest, Response coapResponse, String clientEndpoint, LwM2mModel model,
            LwM2mDecoder decoder) {
        this.coapRequest = coapRequest;
        this.coapResponse = coapResponse;
        this.clientEndpoint = clientEndpoint;
        this.model = model;
        this.decoder = decoder;
    }

    @Override
    public void visit(ReadRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ReadResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
            lwM2mresponse = new ReadResponse(ResponseCode.CONTENT, content, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(DiscoverRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new DiscoverResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT) {
            // handle success response:
            Link[] links;
            if (MediaTypeRegistry.APPLICATION_LINK_FORMAT != coapResponse.getOptions().getContentFormat()) {
                throw new InvalidResponseException("Client [%s] returned unexpected content format [%s] for [%s]",
                        clientEndpoint, coapResponse.getOptions().getContentFormat(), request);
            } else {
                links = Link.parse(coapResponse.getPayload());
            }
            lwM2mresponse = new DiscoverResponse(ResponseCode.CONTENT, links, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(WriteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new WriteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new WriteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new WriteAttributesResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new WriteAttributesResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ExecuteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ExecuteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new ExecuteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(CreateRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new CreateResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CREATED) {
            // handle success response:
            lwM2mresponse = new CreateResponse(ResponseCode.CREATED,
                    coapResponse.getOptions().getLocationPathCount() == 0 ? null
                            : coapResponse.getOptions().getLocationPathString(),
                    null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(DeleteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new DeleteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.DELETED) {
            // handle success response:
            lwM2mresponse = new DeleteResponse(ResponseCode.DELETED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(SingleObserveRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new SingleObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null, null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT
                // This is for backward compatibility, when the spec say notification used CHANGED code
                || coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
            SingleObservation observation = null;
            if (coapResponse.getOptions().hasObserve()) {
                // observe request successful
                observation = ObserveUtil.createLwM2mSingleObservation(coapRequest);
            }
            // add the observation to an ObserveResponse instance
            lwM2mresponse = new SingleObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null, observation, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(CancelObservationRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new CancelObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), null, null, null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT
                // This is for backward compatibility, when the spec say notification used CHANGED code
                || coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
            lwM2mresponse = new CancelObservationResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null,
                    null, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(ReadCompositeRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new ReadCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT) {
            // handle success response:
            Map<LwM2mPath, LwM2mNode> content = decodeCompositeCoapResponse(request.getPaths(), coapResponse, request,
                    clientEndpoint);
            lwM2mresponse = new ReadCompositeResponse(ResponseCode.CONTENT, content, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(CompositeObserveRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new CompositeObserveResponse(
                    toLwM2mResponseCode(coapResponse.getCode()),
                    null,
                    coapResponse.getPayloadString(),
                    coapResponse,
                    null
            );

        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT
                // This is for backward compatibility, when the spec say notification used CHANGED code
                || coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:

            Map<LwM2mPath, LwM2mNode> content = decodeCompositeCoapResponse(request.getPaths(), coapResponse, request,
                    clientEndpoint);

            CompositeObservation observation = null;
            if (coapResponse.getOptions().hasObserve()) {
                // observe request successful
                observation = ObserveUtil.createLwM2mCompositeObservation(coapRequest);
            }
            // add the observation to an ObserveResponse instance
            lwM2mresponse = new CompositeObserveResponse(toLwM2mResponseCode(coapResponse.getCode()), content, null,
                    coapResponse, observation);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(WriteCompositeRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new WriteCompositeResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new WriteCompositeResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapDiscoverRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapDiscoverResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT) {
            // handle success response:
            Link[] links;
            if (MediaTypeRegistry.APPLICATION_LINK_FORMAT != coapResponse.getOptions().getContentFormat()) {
                throw new InvalidResponseException("Client [%s] returned unexpected content format [%s] for [%s]",
                        clientEndpoint, coapResponse.getOptions().getContentFormat(), request);
            } else {
                links = Link.parse(coapResponse.getPayload());
            }
            lwM2mresponse = new BootstrapDiscoverResponse(ResponseCode.CONTENT, links, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapWriteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new BootstrapWriteResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapReadRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapReadResponse(toLwM2mResponseCode(coapResponse.getCode()), null,
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT) {
            // handle success response:
            LwM2mNode content = decodeCoapResponse(request.getPath(), coapResponse, request, clientEndpoint);
            lwM2mresponse = new BootstrapReadResponse(ResponseCode.CONTENT, content, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapDeleteResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.DELETED) {
            // handle success response:
            lwM2mresponse = new BootstrapDeleteResponse(ResponseCode.DELETED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
        if (coapResponse.isError()) {
            // handle error response:
            lwM2mresponse = new BootstrapFinishResponse(toLwM2mResponseCode(coapResponse.getCode()),
                    coapResponse.getPayloadString(), coapResponse);
        } else if (coapResponse.getCode() == org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED) {
            // handle success response:
            lwM2mresponse = new BootstrapFinishResponse(ResponseCode.CHANGED, null, coapResponse);
        } else {
            // handle unexpected response:
            handleUnexpectedResponseCode(clientEndpoint, request, coapResponse);
        }
    }

    private LwM2mNode decodeCoapResponse(LwM2mPath path, Response coapResponse, LwM2mRequest<?> request,
            String endpoint) {

        // Get content format
        ContentFormat contentFormat = null;
        if (coapResponse.getOptions().hasContentFormat()) {
            contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
        }

        // Decode payload
        try {
            return decoder.decode(coapResponse.getPayload(), contentFormat, path, model);
        } catch (CodecException e) {
            if (LOG.isDebugEnabled()) {
                byte[] payload = coapResponse.getPayload() == null ? new byte[0] : coapResponse.getPayload();
                LOG.debug(
                        String.format("Unable to decode response payload of request [%s] from client [%s] [payload:%s]",
                                request, endpoint, Hex.encodeHexString(payload)));
            }
            throw new InvalidResponseException(e, "Unable to decode response payload of request [%s] from client [%s]",
                    request, endpoint);
        }
    }

    private Map<LwM2mPath, LwM2mNode> decodeCompositeCoapResponse(List<LwM2mPath> paths, Response coapResponse,
            LwM2mRequest<?> request, String endpoint) {

        // Get content format
        ContentFormat contentFormat = null;
        if (coapResponse.getOptions().hasContentFormat()) {
            contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
        }

        // Decode payload
        try {
            return decoder.decodeNodes(coapResponse.getPayload(), contentFormat, paths, model);
        } catch (CodecException e) {
            if (LOG.isDebugEnabled()) {
                byte[] payload = coapResponse.getPayload() == null ? new byte[0] : coapResponse.getPayload();
                LOG.debug(
                        String.format("Unable to decode response payload of request [%s] from client [%s] [payload:%s]",
                                request, endpoint, Hex.encodeHexString(payload)));
            }
            throw new InvalidResponseException(e, "Unable to decode response payload of request [%s] from client [%s]",
                    request, endpoint);
        }
    }

    @SuppressWarnings("unchecked")
    public T getResponse() {
        return (T) lwM2mresponse;
    }

    private void handleUnexpectedResponseCode(String clientEndpoint, LwM2mRequest<?> request, Response coapResponse) {
        throw new InvalidResponseException("Client [%s] returned unexpected response code [%s] for [%s]",
                clientEndpoint, coapResponse.getCode(), request);
    }
}
