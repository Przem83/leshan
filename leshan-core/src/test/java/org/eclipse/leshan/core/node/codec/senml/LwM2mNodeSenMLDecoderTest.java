package org.eclipse.leshan.core.node.codec.senml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodeList;
import org.eclipse.leshan.senml.SenMLDecoder;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.junit.Assert;
import org.junit.Test;

public class LwM2mNodeSenMLDecoderTest {

    @Test
    public void should_decode_single_timestamped_value() {
        // given
        LwM2mNodeSenMLDecoder nodeDecoder = new LwM2mNodeSenMLDecoder(fakeDecoder(false), true);

        // when
        Map<LwM2mPath, LwM2mNode> nodeMap = nodeDecoder.decodeNodes(new byte[] {},null, getFakeModel());

        // then
        Assert.assertEquals(1, nodeMap.size());
        Assert.assertTrue(nodeMap.containsKey(new LwM2mPath("/2000/2/3")));
        LwM2mNode node = nodeMap.get(new LwM2mPath("/2000/2/3"));
        Assert.assertTrue(node instanceof TimestampedLwM2mNodeList);
        TimestampedLwM2mNodeList timestampedNodes = (TimestampedLwM2mNodeList) node;
        Assert.assertEquals(3, timestampedNodes.getId());
        Assert.assertEquals(getExampleTimestampedNodes(false), timestampedNodes.getTimestampedNodes());
    }

    @Test
    public void should_decode_multiple_timestamped_values() {
        // given
        LwM2mNodeSenMLDecoder nodeDecoder = new LwM2mNodeSenMLDecoder(fakeDecoder(true), true);

        // when
        Map<LwM2mPath, LwM2mNode> nodeMap = nodeDecoder.decodeNodes(new byte[] {},null, getFakeModel());

        // then
        Assert.assertEquals(1, nodeMap.size());
        Assert.assertTrue(nodeMap.containsKey(new LwM2mPath("/2000/2/3")));
        LwM2mNode node = nodeMap.get(new LwM2mPath("/2000/2/3"));
        Assert.assertTrue(node instanceof TimestampedLwM2mNodeList);
        TimestampedLwM2mNodeList timestampedNodes = (TimestampedLwM2mNodeList) node;
        Assert.assertEquals(3, timestampedNodes.getId());
        Assert.assertEquals(getExampleTimestampedNodes(true), timestampedNodes.getTimestampedNodes());
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

    private SenMLDecoder fakeDecoder(boolean multiple) {
        return encodedSenML -> {
            SenMLPack pack = new SenMLPack();
            SenMLRecord record = new SenMLRecord();
            record.setBaseTime(2222L);
            record.setBaseName("/2000/2");
            record.setName("/3");
            record.setStringValue("12345");
            pack.addRecord(record);

            if (multiple) {
                record = new SenMLRecord();
                record.setBaseTime(4444L);
                record.setName("/3");
                record.setStringValue("67890");
                pack.addRecord(record);
            }

            return pack;
        };
    }

    private Map<Long, LwM2mNode> getExampleTimestampedNodes(boolean multiple) {
        Map<Long, LwM2mNode> timestampedNodes = new HashMap<>();
        timestampedNodes.put(2222L, LwM2mSingleResource.newStringResource(3, "12345"));
        if (multiple) {
            timestampedNodes.put(4444L, LwM2mSingleResource.newStringResource(3, "67890"));
        }
        return timestampedNodes;
    }
}