package org.eclipse.leshan.core.response;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.core.node.LwM2mSingleResource.newResource;
import static org.junit.Assert.*;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SingleObserveResponseTest {

    @Parameters(name = "{0}")
    public static Collection<?> responseCodes() {
        return Arrays.asList(CONTENT, CHANGED);
    }

    private final ResponseCode responseCode;

    public SingleObserveResponseTest(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }

    @Test
    public void should_throw_invalid_response_exception_if_no_content() {
        assertThrows(InvalidResponseException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                new SingleObserveResponse(responseCode, null, null, null, null);
            }
        });
    }

    @Test
    public void should_throw_invalid_response_exception_if_no_content_and_empty_timestamped_values() {
        assertThrows(InvalidResponseException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                new SingleObserveResponse(responseCode, null, Collections.<TimestampedLwM2mNode>emptyList(), null, null);
            }
        });
    }

    @Test
    public void should_not_throw_exception_if_has_content() {
        // given
        LwM2mSingleResource exampleResource = newResource(15, "example");

        // when
        SingleObserveResponse response = new SingleObserveResponse(
                responseCode, exampleResource, null, null, null
        );

        // then
        assertEquals(exampleResource, response.getContent());
        assertNull(response.getTimestampedValues());
    }

    @Test
    public void should_get_content_from_first_of_timestamped_values() {
        // given
        List<TimestampedLwM2mNode> timestampedValues = Arrays.asList(
                new TimestampedLwM2mNode(123L, newResource(15, "example 1")),
                new TimestampedLwM2mNode(456L, newResource(15, "example 2"))
        );

        // when
        SingleObserveResponse response = new SingleObserveResponse(
                responseCode, null, timestampedValues, null, null
        );

        // then
        assertEquals(timestampedValues.get(0).getNode(), response.getContent());
        assertEquals(timestampedValues, response.getTimestampedValues());
    }

    @Test
    public void should_pass_empty_timestamped_values_if_only_one_node_without_timestamp() {
        // given
        List<TimestampedLwM2mNode> timestampedValues = Collections.singletonList(
                new TimestampedLwM2mNode(null, newResource(15, "example 1"))
        );

        // when
        SingleObserveResponse response = new SingleObserveResponse(
                responseCode, null, timestampedValues, null, null
        );

        // then
        assertEquals(timestampedValues.get(0).getNode(), response.getContent());
        assertNull(response.getTimestampedValues());
    }
}