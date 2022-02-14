package org.eclipse.leshan.core.node;

import java.util.Map;

public interface LwM2mObject extends LwM2mNode {
    @Override
    void accept(LwM2mNodeVisitor visitor);

    @Override
    int getId();

    Map<Integer, LwM2mObjectInstance> getInstances();

    LwM2mObjectInstance getInstance(int id);

    @Override
    String toPrettyString(LwM2mPath path);

    @Override
    StringBuilder appendPrettyNode(StringBuilder b, LwM2mPath path);
}
