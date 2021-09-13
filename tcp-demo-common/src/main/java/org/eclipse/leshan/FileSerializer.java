package org.eclipse.leshan;

import static org.eclipse.californium.core.coap.CoAP.MessageFormat.*;

import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.network.serialization.MessageHeader;
import org.eclipse.californium.elements.util.DatagramWriter;

public class FileSerializer extends DataSerializer {

    @Override
    protected void serializeHeader(DatagramWriter writer, MessageHeader header) {
        writer.write(VERSION, VERSION_BITS);
        writer.write(header.getType().value, TYPE_BITS);
        writer.write(header.getToken().length(), TOKEN_LENGTH_BITS);
        writer.write(header.getCode(), CODE_BITS);
        writer.write(header.getMID(), MESSAGE_ID_BITS);
        writer.writeBytes(header.getToken().getBytes());
    }
}
