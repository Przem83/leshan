/*******************************************************************************
 * Copyright (c) 2021 Orange Polska SA.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Michał Wadowski (Orange Polska SA) - Add Observe-Composite feature.
 *******************************************************************************/
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

public class ObserveCompositeResponseTest {

    @Test
    public void should_create_response_with_content() {
        // given
        Map<LwM2mPath, LwM2mNode> exampleContent = new HashMap<>();
        exampleContent.put(
                new LwM2mPath("/1/2/3"), newResource(15, "example 1")
        );
        exampleContent.put(
                new LwM2mPath("/2/3/4"), newResource(16, "example 2")
        );

        // when
        ObserveCompositeResponse response = new ObserveCompositeResponse(
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
        ObserveCompositeResponse response = new ObserveCompositeResponse(
                CONTENT, null, exampleTimestampedValues, null, null, null
        );

        // then
        Map<LwM2mPath, LwM2mNode> expectedContent = new HashMap<>();
        expectedContent.put(
                new LwM2mPath("/1/2/3"), newResource(15, "example 1a")
        );
        expectedContent.put(
                new LwM2mPath("/2/3/4"), newResource(16, "example 2a")
        );

        assertEquals(expectedContent, response.getContent());
        assertEquals(exampleTimestampedValues, response.getTimestampedValues());
    }

}
