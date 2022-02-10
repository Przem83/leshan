package org.eclipse.leshan.core.node;

import java.util.Map;

import org.eclipse.leshan.core.request.argument.InvalidArgumentException;

public class TimestampedLwM2mNodeList implements LwM2mNode {

    private final Map<Long, LwM2mNode> timestampedNodes;
    private final int id;

    public TimestampedLwM2mNodeList(Map<Long, LwM2mNode> timestampedNodes) {
        validate(timestampedNodes);
        this.id = timestampedNodes.entrySet().iterator().next().getValue().getId();
        this.timestampedNodes = timestampedNodes;
    }

    private void validate(Map<Long, LwM2mNode> timestampedNodes) {
        if (timestampedNodes.isEmpty()) {
            throw new IllegalArgumentException("Node list should not be empty");
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void accept(LwM2mNodeVisitor visitor) {

    }

    @Override
    public String toPrettyString(LwM2mPath path) {
        return null;
    }

    @Override
    public StringBuilder appendPrettyNode(StringBuilder b, LwM2mPath path) {
        return null;
    }

    public Map<Long, LwM2mNode> getTimestampedNodes() {
        return timestampedNodes;
    }
}
