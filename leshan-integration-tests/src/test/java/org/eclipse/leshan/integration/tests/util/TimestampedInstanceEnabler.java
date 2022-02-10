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
        LwM2mSingleResource lwM2mSingleResource = LwM2mSingleResource.newFloatResource(resourceid, 111.1);
        return ReadResponse.success(lwM2mSingleResource);
    }
}
