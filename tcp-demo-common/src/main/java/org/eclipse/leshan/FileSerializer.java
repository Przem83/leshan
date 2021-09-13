package org.eclipse.leshan;

import java.nio.ByteBuffer;

import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.network.serialization.MessageHeader;
import org.eclipse.californium.elements.util.DatagramWriter;

public class FileSerializer extends DataSerializer {

    @Override
    protected void serializeHeader(DatagramWriter writer, MessageHeader header) {

        int headerSize = (4 * 5) + header.getToken().getBytes().length + 4;
        byte[] headerBytes = new byte[headerSize];

        byte[] headerSizeBytes = ByteBuffer.allocate(4).putInt(headerSize).array();
        System.arraycopy(headerSizeBytes, 0, headerBytes, 0, headerSizeBytes.length);

        byte[] versionBytes = ByteBuffer.allocate(4).putInt(header.getVersion()).array();
        System.arraycopy(versionBytes, 0, headerBytes, 4, versionBytes.length);

        byte[] typeBytes = ByteBuffer.allocate(4).putInt(header.getType().value).array();
        System.arraycopy(typeBytes, 0, headerBytes, 8, typeBytes.length);

        byte[] codeBytes = ByteBuffer.allocate(4).putInt(header.getCode()).array();
        System.arraycopy(codeBytes, 0, headerBytes, 12, codeBytes.length);

        byte[] midBytes = ByteBuffer.allocate(4).putInt(header.getMID()).array();
        System.arraycopy(midBytes, 0, headerBytes, 16, midBytes.length);

        byte[] lengthBytes = ByteBuffer.allocate(4).putInt(header.getBodyLength()).array();
        System.arraycopy(lengthBytes, 0, headerBytes, 20, lengthBytes.length);

        System.arraycopy(header.getToken().getBytes(), 0, headerBytes, 24, header.getToken().getBytes().length);

        writer.writeBytes(headerBytes);
    }
}
