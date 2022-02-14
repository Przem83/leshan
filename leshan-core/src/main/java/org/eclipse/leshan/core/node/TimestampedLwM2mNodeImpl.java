/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.leshan.core.util.Validate;

public class TimestampedLwM2mNodeImpl<T extends LwM2mNode> implements TimestampedLwM2mNode<T> {

    private final Map<Long, T> timestampedNodes;

    public TimestampedLwM2mNodeImpl(Long timestamp, T node) {
        Validate.notNull(node);
        timestampedNodes = new LinkedHashMap<>();
        timestampedNodes.put(timestamp, node);
    }

    @Override
    public Long getFirstTimestamp() {
        return getFirstEntry().getKey();
    }

    private Map.Entry<Long, T> getFirstEntry() {
        return timestampedNodes.entrySet().iterator().next();
    }

    @Override
    public T getFirstNode() {
        return getFirstEntry().getValue();
    }

    @Override
    public boolean isTimestamped() {
        return getFirstTimestamp() != null && getFirstTimestamp() >= 0;
    }

    @Override
    public Map<Long, T> getTimestampedNodes() {
        return timestampedNodes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((timestampedNodes == null) ? 0 : timestampedNodes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TimestampedLwM2mNodeImpl other = (TimestampedLwM2mNodeImpl<T>) obj;
        if (timestampedNodes == null) {
            if (other.timestampedNodes != null)
                return false;
        } else if (!timestampedNodes.equals(other.timestampedNodes))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return String.format("TimestampedLwM2mNode [timestampedNodes=%s]", timestampedNodes);
    }
}
