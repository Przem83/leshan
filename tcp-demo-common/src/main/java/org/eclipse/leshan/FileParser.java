package org.eclipse.leshan;

import static org.eclipse.californium.core.coap.CoAP.MessageFormat.*;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAPMessageFormatException;
import org.eclipse.californium.core.coap.MessageFormatException;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.MessageHeader;
import org.eclipse.californium.elements.util.DatagramReader;

public class FileParser extends DataParser {
    @Override
    protected MessageHeader parseHeader(DatagramReader reader) {

        if (!reader.bytesAvailable(4)) {
            throw new MessageFormatException(
                    "Message too short! " + (reader.bitsLeft() / Byte.SIZE) + " must be at least 4 bytes!");
        }
        int version = reader.read(VERSION_BITS);
        int type = reader.read(TYPE_BITS);
        int tokenLength = reader.read(TOKEN_LENGTH_BITS);
        if (tokenLength > 8) {
            // must be treated as a message format error according to CoAP spec
            // https://tools.ietf.org/html/rfc7252#section-3
            throw new MessageFormatException("Message has invalid token length (> 8) " + tokenLength);
        }
        int code = reader.read(CODE_BITS);
        int mid = reader.read(MESSAGE_ID_BITS);
        if (!reader.bytesAvailable(tokenLength)) {
            throw new CoAPMessageFormatException("Message too short for token! " + (reader.bitsLeft() / Byte.SIZE)
                    + " must be at least " + tokenLength + " bytes!", null, mid, code, CoAP.Type.CON.value == type);
        }
        Token token = Token.fromProvider(reader.readBytes(tokenLength));

        return new MessageHeader(version, CoAP.Type.valueOf(type), token, code, mid, 0);
    }
}
