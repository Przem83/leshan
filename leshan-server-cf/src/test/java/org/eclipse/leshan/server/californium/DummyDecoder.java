package org.eclipse.leshan.server.californium;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.request.ContentFormat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DummyDecoder implements LwM2mDecoder {
    @Override
    public LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model)
            throws CodecException {
        return LwM2mSingleResource.newResource(15, "Example");
    }

    @Override
    public <T extends LwM2mNode> T decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model,
                                          Class<T> nodeClass) throws CodecException {
        return null;
    }

    @Override
    public Map<LwM2mPath, LwM2mNode> decodeNodes(byte[] content, ContentFormat format, List<LwM2mPath> paths,
                                                 LwM2mModel model) throws CodecException {
        return null;
    }

    @Override
    public List<TimestampedLwM2mNode> decodeTimestampedData(byte[] content, ContentFormat format, LwM2mPath path,
                                                            LwM2mModel model) throws CodecException {
        return Collections.singletonList(new TimestampedLwM2mNode(
                null, decode(null, null, null, null))
        );
    }

    @Override
    public List<LwM2mPath> decodePaths(byte[] content, ContentFormat format) throws CodecException {
        return null;
    }

    @Override
    public boolean isSupported(ContentFormat format) {
        return false;
    }

    @Override
    public Set<ContentFormat> getSupportedContentFormat() {
        return null;
    }
}
