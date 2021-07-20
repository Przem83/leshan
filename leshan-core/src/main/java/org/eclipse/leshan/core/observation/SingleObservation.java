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
package org.eclipse.leshan.core.observation;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * An observation of a resource provided by a LWM2M Client.
 */
public class SingleObservation extends Observation {

    private final LwM2mPath path;

    public SingleObservation(byte[] id, String registrationId, LwM2mPath path, ContentFormat contentFormat,
                             Map<String, String> context) {
        super(id, registrationId, contentFormat, context);
        this.path = path;
    }

    public LwM2mPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "SingleObservation{" +
                "path=" + path +
                ", id=" + Arrays.toString(id) +
                ", contentFormat=" + contentFormat +
                ", registrationId='" + registrationId + '\'' +
                ", context=" + context +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SingleObservation)) return false;
        if (!super.equals(o)) return false;
        SingleObservation that = (SingleObservation) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }
}
