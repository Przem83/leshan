/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.demo.servlet;

import com.eclipsesource.json.JsonObject;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.server.demo.LeshanMultiConnectionServer;
import org.eclipse.leshan.server.demo.servlet.json.PublicKeySerDes;
import org.eclipse.leshan.server.demo.servlet.json.SecuritySerializer;
import org.eclipse.leshan.server.demo.servlet.json.X509CertificateSerDes;
import org.eclipse.leshan.server.security.SecurityInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class ServerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final X509CertificateSerDes certificateSerDes;
    private final PublicKeySerDes publicKeySerDes;

    private final LeshanMultiConnectionServer server;
    private final PublicKey publicKey;
    private final X509Certificate serverCertificate;

    public ServerServlet(LeshanMultiConnectionServer server, X509Certificate serverCertificate) {
        this.server = server;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SecurityInfo.class, new SecuritySerializer());
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();

        this.publicKey = null;
        this.serverCertificate = serverCertificate;
    }

    public ServerServlet(LeshanMultiConnectionServer server, PublicKey serverPublicKey) {
        this.server = server;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SecurityInfo.class, new SecuritySerializer());
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();
        this.publicKey = serverPublicKey;
        this.serverCertificate = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        if (path.length != 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if ("security".equals(path[0])) {
            JsonObject security = new JsonObject();
            if (publicKey != null) {
                security.add("pubkey", publicKeySerDes.jSerialize(publicKey));
            } else if (serverCertificate != null) {
                security.add("certificate", certificateSerDes.jSerialize(serverCertificate));
            }
            resp.setContentType("application/json");
            resp.getOutputStream().write(security.toString().getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if ("endpoint".equals(path[0])) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getOutputStream().write(String.format("{ \"getFirstAddressPort\":\"%s\"}",
                    server.getFirstAddress().getPort()).getBytes(StandardCharsets.UTF_8));
            return;
        }

        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
}
