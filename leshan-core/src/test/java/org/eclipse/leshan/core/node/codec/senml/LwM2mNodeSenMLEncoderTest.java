package org.eclipse.leshan.core.node.codec.senml;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodeList;
import org.eclipse.leshan.core.node.codec.LwM2mValueChecker;
import org.eclipse.leshan.senml.SenMLEncoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.junit.Test;

public class LwM2mNodeSenMLEncoderTest {

    @Test
    public void should_encode_timestamped_node() {
        // given

        FakeSenMLEncoder fakeEncoder = new FakeSenMLEncoder();
        LwM2mNodeSenMLEncoder nodeEncoder = new LwM2mNodeSenMLEncoder(fakeEncoder);

        // when
        nodeEncoder.encodeNodes(getExampleTimestampedNodes(false), getFakeModel(), new LwM2mValueChecker());

        // then
        assertEquals(1, fakeEncoder.receivedPack.getRecords().size());
        SenMLRecord record = fakeEncoder.receivedPack.getRecords().get(0);
        assertEquals("12345", record.getStringValue());
        assertEquals(Long.valueOf(2222L), record.getBaseTime());
    }

    private Map<LwM2mPath, LwM2mNode> getExampleTimestampedNodes(boolean multiple) {
        Map<Long, LwM2mNode> timestampedNodes = new LinkedHashMap<>();

        timestampedNodes.put(2222L, LwM2mSingleResource.newStringResource(3, "12345"));
        if (multiple) {
            timestampedNodes.put(4444L, LwM2mSingleResource.newStringResource(3, "67890"));
        }

        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("/2000/2/3"), new TimestampedLwM2mNodeList(timestampedNodes));
        return nodes;
    }

    private LwM2mModel getFakeModel() {
        return new LwM2mModel() {

            @Override
            public ResourceModel getResourceModel(int objectId, int resourceId) {
                return new ResourceModel(null, null, null, null, null, ResourceModel.Type.STRING, null, null, "desc");
            }

            @Override
            public ObjectModel getObjectModel(int objectId) {
                return null;
            }

            @Override
            public Collection<ObjectModel> getObjectModels() {
                return null;
            }
        };
    }

    static class FakeSenMLEncoder implements SenMLEncoder {

        SenMLPack receivedPack;

        @Override
        public byte[] toSenML(SenMLPack pack) throws SenMLException {
            this.receivedPack = pack;
            return new byte[0];
        }
    }

}