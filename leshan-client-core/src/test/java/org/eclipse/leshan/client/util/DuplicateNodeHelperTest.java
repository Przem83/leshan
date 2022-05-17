package org.eclipse.leshan.client.util;

import org.eclipse.leshan.core.node.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class DuplicateNodeHelperTest {
    private final LwM2mSingleResource resource1 = LwM2mSingleResource.newIntegerResource(5, 10);
    private final LwM2mSingleResource resource2 = LwM2mSingleResource.newIntegerResource(4, 20);
    private final LwM2mObjectInstance instanceWithBothResources = new LwM2mObjectInstance(3, resource1, resource2);
    private final LwM2mObjectInstance emptyInstance = new LwM2mObjectInstance(2, Collections.emptyList());
    private final LwM2mObject objectWithBothInstances = new LwM2mObject(1, instanceWithBothResources, emptyInstance);

    @Test public void overlapping_instance_and_resource() {
        Map<LwM2mPath, LwM2mNode> content = new HashMap<>();
        content.put(new LwM2mPath(1, 3, 5), resource1);
        content.put(new LwM2mPath(1, 3), instanceWithBothResources);

        Map<LwM2mPath, LwM2mNode> expectedResult = Collections.singletonMap(new LwM2mPath(1, 3), instanceWithBothResources);
        Map<LwM2mPath, LwM2mNode> contentWithoutDuplicates = DuplicateNodeHelper.removeDuplicateNodes(content);

        Assert.assertEquals(expectedResult, contentWithoutDuplicates);
    }

    @Test public void overlapping_object_and_instance() {
        Map<LwM2mPath, LwM2mNode> content = new HashMap<>();
        content.put(new LwM2mPath(1, 2), emptyInstance);
        content.put(new LwM2mPath(1), objectWithBothInstances);

        Map<LwM2mPath, LwM2mNode> expectedResult = Collections.singletonMap(new LwM2mPath(1), objectWithBothInstances);
        Map<LwM2mPath, LwM2mNode> contentWithoutDuplicates = DuplicateNodeHelper.removeDuplicateNodes(content);

        Assert.assertEquals(expectedResult, contentWithoutDuplicates);
    }

    @Test public void overlapping_object_and_instance_and_resource() {
        Map<LwM2mPath, LwM2mNode> content = new HashMap<>();
        content.put(new LwM2mPath(1, 3, 5), resource1);
        content.put(new LwM2mPath(1, 3), instanceWithBothResources);
        content.put(new LwM2mPath(1), objectWithBothInstances);

        Map<LwM2mPath, LwM2mNode> expectedResult = Collections.singletonMap(new LwM2mPath(1), objectWithBothInstances);
        Map<LwM2mPath, LwM2mNode> contentWithoutDuplicates = DuplicateNodeHelper.removeDuplicateNodes(content);

        Assert.assertEquals(expectedResult, contentWithoutDuplicates);
    }
}