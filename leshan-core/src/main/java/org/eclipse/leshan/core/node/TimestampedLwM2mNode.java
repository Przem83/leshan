package org.eclipse.leshan.core.node;

import java.util.Map;

public interface TimestampedLwM2mNode<T extends LwM2mNode> {
    Long getFirstTimestamp();

    T getFirstNode();

    boolean isTimestamped();

    Map<Long, T> getTimestampedNodes();
}
