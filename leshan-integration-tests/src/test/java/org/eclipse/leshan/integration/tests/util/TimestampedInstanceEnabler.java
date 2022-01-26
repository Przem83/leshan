package org.eclipse.leshan.integration.tests.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.response.ReadResponse;

public class TimestampedInstanceEnabler extends BaseInstanceEnabler {

    public TimestampedInstanceEnabler(int id) {
        super(id);
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
//        List<TimestampedLwM2mNode> timestampedLwM2mNodes = Arrays.asList(
//                new TimestampedLwM2mNode(123L, LwM2mSingleResource.newFloatResource(resourceid, 111.0)),
//                new TimestampedLwM2mNode(456L, LwM2mSingleResource.newFloatResource(resourceid, 222.0)));
//
//        return ReadResponse.success(timestampedLwM2mNodes);

        LwM2mSingleResource lwM2mSingleResource = LwM2mSingleResource.newFloatResource(resourceid, 111.1);
        lwM2mSingleResource.setTimestamp(123L);

        return ReadResponse.success(lwM2mSingleResource);
    }
}
