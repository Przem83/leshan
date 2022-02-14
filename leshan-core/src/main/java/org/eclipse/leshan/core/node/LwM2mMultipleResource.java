package org.eclipse.leshan.core.node;

import java.util.Map;

import org.eclipse.leshan.core.model.ResourceModel;

public interface LwM2mMultipleResource extends LwM2mResource {
    @Override
    int getId();

    @Override
    ResourceModel.Type getType();

    @Override
    Object getValue();

    @Override
    Object getValue(int id);

    @Override
    LwM2mResourceInstance getInstance(int id);

    @Override
    Map<Integer, LwM2mResourceInstance> getInstances();

    @Override
    boolean isMultiInstances();

    @Override
    void accept(LwM2mNodeVisitor visitor);

    @Override
    String toPrettyString(LwM2mPath path);

    @Override
    StringBuilder appendPrettyNode(StringBuilder b, LwM2mPath path);
}
