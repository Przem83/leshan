package org.eclipse.leshan;

import java.nio.ByteBuffer;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.MessageHeader;
import org.eclipse.californium.elements.util.DatagramReader;

public class FileParser extends DataParser {
    @Override
    protected MessageHeader parseHeader(DatagramReader reader) {

        byte[] headerSizeBytes = reader.readBytes(4);
        int headerSize = ByteBuffer.wrap(headerSizeBytes).getInt();

        byte[] headerBytes = reader.readBytes(headerSize - 4);

        byte[] versionBytes = new byte[4];
        System.arraycopy(headerBytes, 0, versionBytes, 0, versionBytes.length);


        byte[] typeBytes = new byte[4];
        System.arraycopy(headerBytes, 4, typeBytes, 0, typeBytes.length);


        byte[] codeBytes = new byte[4];
        System.arraycopy(headerBytes, 8, codeBytes, 0, codeBytes.length);
        

        byte[] midBytes = new byte[4];
        System.arraycopy(headerBytes, 12, midBytes, 0, midBytes.length);
        

        byte[] lengthBytes = new byte[4];
        System.arraycopy(headerBytes, 16, lengthBytes, 0, lengthBytes.length);


        byte[] tokenBytes = new byte[headerBytes.length - 5*4];
        System.arraycopy(headerBytes, 20, tokenBytes, 0, tokenBytes.length);

        int version = ByteBuffer.wrap(versionBytes).getInt();

        CoAP.Type type = CoAP.Type.valueOf(ByteBuffer.wrap(typeBytes).getInt());
        Token token = new Token(tokenBytes);


        int code = ByteBuffer.wrap(codeBytes).getInt();
        int mid = ByteBuffer.wrap(midBytes).getInt();
        int length = ByteBuffer.wrap(lengthBytes).getInt();


        return new MessageHeader(version,
                type, token, code, mid, length
        );
    }
}
