package org.eclipse.leshan.core.node;

import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.leshan.core.model.ResourceModel;

public class TimestampedLwM2mNodeList implements LwM2mResource {

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

    @Override
    public ResourceModel.Type getType() {
        return null;
    }

    @Override
    public boolean isMultiInstances() {
        return false;
    }

    @Override
    public Object getValue() {
        return null;
    }

    /**
     * @exception NoSuchElementException use {@link #getValue()} instead.
     */
    @Override
    public Object getValue(int id) {
        throw new NoSuchElementException("There is no 'values' on single resources, use getValue() instead.");
    }

    /**
     * @exception NoSuchElementException use {@link #getValue()} instead.
     */
    @Override
    public LwM2mResourceInstance getInstance(int id) {
        throw new NoSuchElementException("There is no 'instance' on single resources, use getValue() instead.");
    }

    /**
     * @exception NoSuchElementException use {@link #getValue()} instead.
     */
    @Override
    public Map<Integer, LwM2mResourceInstance> getInstances() {
        throw new NoSuchElementException("There is no 'instances' on single resources, use getValue() instead.");
    }
}
