package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientsTest {

    @Test
    public void runleshan() throws InterruptedException, TimeoutException {
        //        String serveruri="coaps://127.0.0.1:5684"; // coap://127.0.0.1:5683 or "coaps://127.0.0.1:5684"
        String serveruri="coap://127.0.0.1:5683";
        int shortServerId=12345;
        int lifetime =5 * 60;
        String bindings ="U";
        BindingMode bidingmode = BindingMode.U;
        boolean notifyWhenDisable=false;
        String endpoint="Client_";
        String modelNumber = "V1.0";
        String serialNumber ="12345";
        String security_identity="740784_client";
        String security_psk="3fb750ec902c6fa3fe7efc26d637474dd20c22d07254eb45248afce9545d069c";

        final Logger LOG = LoggerFactory.getLogger(ClientsTest.class);

        //LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);

        Server lwM2Mserver =new Server (shortServerId, lifetime, BindingMode.parse(bindings), notifyWhenDisable, bidingmode);


        ScheduledExecutorService sharedExecutor = Executors.newScheduledThreadPool(2,
                new NamedThreadFactory("shared executor for load tests"));


        // create objects
        ObjectsInitializer initializer = new ObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SERVER, lwM2Mserver);
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device(endpoint, modelNumber, serialNumber,BindingMode.parse(bindings)));
        initializer.setInstancesForObject(7, new ReadRandomData());



        LeshanClient[] clients = new LeshanClient[20];

        if (serveruri.contains("coaps"))
        {

            initializer.setInstancesForObject(SECURITY, psk(serveruri, shortServerId,
                    security_identity.getBytes(), Hex.decodeHex(security_psk.toCharArray())));
            Configuration coapConfig = LeshanClientBuilder.createDefaultCoapConfiguration();
            DtlsConnectorConfig.Builder dtlsConfig = DtlsConnectorConfig.builder(coapConfig);



            for (int i = 0; i < clients.length; i++) {
                LeshanClientBuilder builder = new LeshanClientBuilder(endpoint + i);
                builder.setDtlsConfig(dtlsConfig);
                builder.setSharedExecutor(sharedExecutor);
                builder.setObjects(initializer.createAll());

                clients[i] = builder.build();
            }



        }
        else{

            initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSec(serveruri, shortServerId));
            for (int i = 0; i < clients.length; i++) {
                LeshanClientBuilder builder = new LeshanClientBuilder(endpoint + i);
                builder.setSharedExecutor(sharedExecutor);
                builder.setObjects(initializer.createAll());
                clients[i] = builder.build();
            }

        }



        //LeshanClient client = builder.build();

        for (int i = 0; i < clients.length; i++) {

            SynchronousClientObserver clientObserver = new SynchronousClientObserver();
            clients[i].addObserver(clientObserver);

            LOG.info("### Starting client no: "+ i);
            clients[i].start();
            LOG.info("### Started client no: "+ i);

            clientObserver.waitForRegistration(1000, TimeUnit.MILLISECONDS);

            LOG.info("### Send data: "+ i);

            Map<String, ServerIdentity> registeredServers = clients[i].getRegisteredServers();
            if (registeredServers.isEmpty()) {
                LOG.info("There is no registered server to send to.");
            } else {
                LOG.info("Registered server: " + registeredServers.values().toArray()[0].toString());

                try {
                    for (final ServerIdentity server : registeredServers.values()) {
                        clients[i].sendData(server, ContentFormat.SENML_JSON, Arrays.asList("/7/0/0"), 1000);
                        LOG.info("SendData for object: /7/0/0");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            LOG.info("### Stopping client no: "+ i);

            clients[i].stop(true);
        }
    }
}
