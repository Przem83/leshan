package org.eclipse.leshan.core.node;

import java.util.Map;

public interface LwM2mObjectInstance extends LwM2mNode {
    /**
     * Undefined instance Id
     */
    int UNDEFINED = -1;

    @Override
    void accept(LwM2mNodeVisitor visitor);

    @Override
    int getId();

    Map<Integer, LwM2mResource> getResources();

    LwM2mResource getResource(int id);

    @Override
    String toPrettyString(LwM2mPath path);

    @Override
    StringBuilder appendPrettyNode(StringBuilder b, LwM2mPath path);
}
