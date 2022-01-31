package org.eclipse.leshan.integration.tests;

import java.util.Random;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.response.ReadResponse;

public class ReadRandomData extends BaseInstanceEnabler {

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        switch (resourceid) {
            case 0:
                return ReadResponse.success(resourceid, new Random().nextInt());
        }
        return ReadResponse.notFound();
    }

}