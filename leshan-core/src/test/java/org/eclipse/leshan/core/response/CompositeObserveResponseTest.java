package org.eclipse.leshan.core.response;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.core.node.LwM2mSingleResource.newResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CompositeObserveResponseTest {

    @Test
    public void should_create_response_with_content() {
        // given
        Map<LwM2mPath, LwM2mNode> exampleContent = new HashMap<>();
        exampleContent.put(new LwM2mPath("/1/2/3"), newResource(15, "example 1"));
        exampleContent.put(new LwM2mPath("/2/3/4"), newResource(16, "example 2"));

        // when
        CompositeObserveResponse response = new CompositeObserveResponse(
                CONTENT, exampleContent, null, null, null
        );

        // then
        assertEquals(exampleContent, response.getContent());
        assertNull(response.getTimestampedValues());
    }

    @Test
    public void should_create_response_with_timestamped_values() {
        // given
        Map<LwM2mPath, List<TimestampedLwM2mNode>> exampleTimestampedValues = new HashMap<>();

        exampleTimestampedValues.put(
                new LwM2mPath("/1/2/3"),
                Arrays.asList(
                        new TimestampedLwM2mNode(123L, newResource(15, "example 1a")),
                        new TimestampedLwM2mNode(456L, newResource(15, "example 1b"))
                )
        );
        exampleTimestampedValues.put(
                new LwM2mPath("/2/3/4"),
                Arrays.asList(
                        new TimestampedLwM2mNode(123L, newResource(16, "example 2a")),
                        new TimestampedLwM2mNode(456L, newResource(16, "example 2b"))
                )
        );

        // when
        CompositeObserveResponse response = new CompositeObserveResponse(
                CONTENT, null, exampleTimestampedValues, null, null, null
        );

        // then
        Map<LwM2mPath, LwM2mNode> expectedContent = new HashMap<>();
        expectedContent.put(new LwM2mPath("/1/2/3"), newResource(15, "example 1a"));
        expectedContent.put(new LwM2mPath("/2/3/4"), newResource(16, "example 2a"));

        assertEquals(expectedContent, response.getContent());
        assertEquals(exampleTimestampedValues, response.getTimestampedValues());
    }

}