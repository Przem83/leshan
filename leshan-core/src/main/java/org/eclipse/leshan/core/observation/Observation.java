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
 *     Michał Wadowski (Orange Polska SA) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.core.observation;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Hex;

import java.util.Map;
import java.util.Objects;

/**
 * An observation of a resource provided by a LWM2M Client.
 */
public class Observation extends AbstractObservation {

    protected final ContentFormat contentFormat;
    private final LwM2mPath path;

    /**
     * Instantiates an {@link CompositeObservation} for the given node paths.
     *
     * @param id token identifier of the observation
     * @param registrationId client's unique registration identifier.
     * @param path resource path for which the observation is set.
     * @param contentFormat contentFormat used to read the resource (could be null).
     * @param context additional information relative to this observation.
     */
    public Observation(byte[] id, String registrationId, LwM2mPath path, ContentFormat contentFormat,
            Map<String, String> context) {
        super(id, registrationId, context);
        this.contentFormat = contentFormat;
        this.path = path;
    }

    /**
     * Gets the requested contentFormat (could be null).
     *
     * @return the resource path
     */
    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    /**
     * Gets the observed resource path.
     *
     * @return the resource path
     */
    public LwM2mPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Observation{" +
                "path=" + path +
                ", id=" + Hex.encodeHexString(id) +
                ", contentFormat=" + contentFormat +
                ", registrationId='" + registrationId + '\'' +
                ", context=" + context +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Observation)) return false;
        if (!super.equals(o)) return false;
        Observation that = (Observation) o;
        return Objects.equals(contentFormat, that.contentFormat) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contentFormat, path);
    }
}
