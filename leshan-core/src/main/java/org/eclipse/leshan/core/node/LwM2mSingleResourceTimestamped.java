package org.eclipse.leshan.core.node;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.leshan.core.model.ResourceModel;

public class LwM2mSingleResourceTimestamped implements LwM2mSingleResource, TimestampedLwM2mNode<LwM2mSingleResource> {

    private final SortedMap<Long, LwM2mSingleResource> timestampedNodes = new TreeMap<>();

    public LwM2mSingleResourceTimestamped(Long timestamp, LwM2mSingleResource res) {
        timestampedNodes.put(timestamp, res);
    }

    public void add(LwM2mSingleResourceTimestamped resource) {
        timestampedNodes.putAll(resource.timestampedNodes);
    }

    @Override
    public SortedMap<Long, LwM2mSingleResource> getTimestampedNodes() {
        return timestampedNodes;
    }

    private SortedMap.Entry<Long, LwM2mSingleResource> getFirstEntry() {
        return timestampedNodes.entrySet().iterator().next();
    }

    @Override
    public Long getFirstTimestamp() {
        return getFirstEntry().getKey();
    }

    @Override
    public LwM2mSingleResource getFirstNode() {
        return getFirstEntry().getValue();
    }

    @Override
    public boolean isTimestamped() {
        return getFirstTimestamp() != null && getFirstTimestamp() >= 0;
    }

    @Override
    public int getId() {
        return getFirstNode().getId();
    }

    @Override
    public ResourceModel.Type getType() {
        return getFirstNode().getType();
    }

    @Override
    public Object getValue() {
        return getFirstNode().getValue();
    }

    @Override
    public Object getValue(int id) {
        return getFirstNode().getValue(id);
    }

    @Override
    public LwM2mResourceInstance getInstance(int id) {
        return getFirstNode().getInstance(id);
    }

    @Override
    public Map<Integer, LwM2mResourceInstance> getInstances() {
        return getFirstNode().getInstances();
    }

    @Override
    public boolean isMultiInstances() {
        return getFirstNode().isMultiInstances();
    }

    @Override
    public void accept(LwM2mNodeVisitor visitor) {
        getFirstNode().accept(visitor);
    }

    @Override
    public String toPrettyString(LwM2mPath path) {
        return getFirstNode().toPrettyString(path);
    }

    @Override
    public StringBuilder appendPrettyNode(StringBuilder b, LwM2mPath path) {
        return getFirstNode().appendPrettyNode(b, path);
    }
}
