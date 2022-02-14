package org.eclipse.leshan.core.node;

import java.util.SortedMap;

public interface TimestampedLwM2mNode<T extends LwM2mNode> {
    Long getFirstTimestamp();

    T getFirstNode();

    boolean isTimestamped();

    SortedMap<Long, T> getTimestampedNodes();
}
