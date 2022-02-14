/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.node;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.junit.Test;

public class LwM2MResourceTest {

    @Test
    public void two_identical_strings_are_equal() {
        assertEquals(LwM2mSingleResourceImpl.newStringResource(10, "hello"),
                LwM2mSingleResourceImpl.newStringResource(10, "hello"));
    }

    @Test
    public void two_non_identical_strings_are_not_equal() {
        assertNotEquals(LwM2mSingleResourceImpl.newStringResource(10, "hello"),
                LwM2mSingleResourceImpl.newStringResource(10, "world"));
        assertNotEquals(LwM2mSingleResourceImpl.newStringResource(11, "hello"),
                LwM2mSingleResourceImpl.newStringResource(10, "hello"));
    }

    @Test
    public void two_identical_opaques_are_equal() {
        assertEquals(LwM2mSingleResourceImpl.newBinaryResource(10, "hello".getBytes()),
                LwM2mSingleResourceImpl.newBinaryResource(10, "hello".getBytes()));
    }

    @Test
    public void two_non_identical_opaques_are_not_equal() {
        assertNotEquals(LwM2mSingleResourceImpl.newBinaryResource(10, "hello".getBytes()),
                LwM2mSingleResourceImpl.newBinaryResource(10, "world".getBytes()));
        assertNotEquals(LwM2mSingleResourceImpl.newBinaryResource(11, "hello".getBytes()),
                LwM2mSingleResourceImpl.newBinaryResource(10, "hello".getBytes()));
    }

    @Test
    public void two_string_and_binary_are_not_equal() {
        assertNotEquals(LwM2mSingleResourceImpl.newStringResource(10, "hello"),
                LwM2mSingleResourceImpl.newBinaryResource(10, "hello".getBytes()));
    }

    @Test
    public void two_identical_multiple_strings_are_equal() {
        Map<Integer, String> values1 = new HashMap<>();
        values1.put(0, "hello");
        Map<Integer, String> values2 = new HashMap<>();
        values2.put(0, "hello");

        assertEquals(LwM2mMultipleResourceImpl.newStringResource(10, values1),
                LwM2mMultipleResourceImpl.newStringResource(10, values2));
    }

    @Test
    public void two_non_identical_multiple_strings_are_not_equal() {
        Map<Integer, String> values1 = new HashMap<>();
        values1.put(0, "hello");
        Map<Integer, String> values2 = new HashMap<>();
        values2.put(0, "world");

        assertNotEquals(LwM2mMultipleResourceImpl.newStringResource(10, values1),
                LwM2mMultipleResourceImpl.newStringResource(10, values2));
        assertNotEquals(LwM2mMultipleResourceImpl.newStringResource(11, values1),
                LwM2mMultipleResourceImpl.newStringResource(10, values1));
    }

    @Test
    public void two_identical_multiple_opaques_are_equal() {
        Map<Integer, byte[]> values1 = new HashMap<>();
        values1.put(0, "hello".getBytes());
        Map<Integer, byte[]> values2 = new HashMap<>();
        values2.put(0, "hello".getBytes());

        assertEquals(LwM2mMultipleResourceImpl.newBinaryResource(10, values1),
                LwM2mMultipleResourceImpl.newBinaryResource(10, values2));
    }

    @Test
    public void two_non_identical_multiple_opaques_are_not_equal() {
        Map<Integer, byte[]> values1 = new HashMap<>();
        values1.put(0, "hello".getBytes());
        Map<Integer, byte[]> values2 = new HashMap<>();
        values2.put(0, "world".getBytes());

        assertNotEquals(LwM2mMultipleResourceImpl.newBinaryResource(10, values1),
                LwM2mMultipleResourceImpl.newBinaryResource(10, values2));
        assertNotEquals(LwM2mMultipleResourceImpl.newBinaryResource(11, values1),
                LwM2mMultipleResourceImpl.newBinaryResource(10, values1));
    }

    @Test
    public void two_multiple_string_and_multiple_binary_are_not_equal() {
        Map<Integer, String> values1 = new HashMap<>();
        values1.put(0, "hello");
        Map<Integer, byte[]> values2 = new HashMap<>();
        values2.put(0, "hello".getBytes());

        assertNotEquals(LwM2mMultipleResourceImpl.newStringResource(10, values1),
                LwM2mMultipleResourceImpl.newBinaryResource(10, values2));
    }

    @Test(expected = LwM2mNodeException.class)
    public void string_resource_with_null_value() {
        LwM2mSingleResourceImpl.newStringResource(1, null);
    }

    @Test(expected = LwM2mNodeException.class)
    public void generic_resource_with_null_value() {
        LwM2mSingleResourceImpl.newResource(1, null, Type.INTEGER);
    }

    @Test(expected = LwM2mNodeException.class)
    public void generic_instance_with_incompatible_value_and_type() {
        LwM2mSingleResourceImpl.newResource(0, "a string", Type.BOOLEAN);
    }

    @Test(expected = LwM2mNodeException.class)
    public void integer_multi_instances_resource_with_null_value() {
        Map<Integer, Long> values = new HashMap<>();
        values.put(2, 2L);
        values.put(3, null);
        LwM2mMultipleResourceImpl.newIntegerResource(0, values);
    }

    @Test(expected = LwM2mNodeException.class)
    public void generic_multi_instances_resource_with_null_value() {
        Map<Integer, String> values = new HashMap<>();
        values.put(2, "value");
        values.put(3, null);
        LwM2mMultipleResourceImpl.newResource(0, values, Type.STRING);
    }

    @Test(expected = LwM2mNodeException.class)
    public void generic_multi_instance_with_incompatible_value_and_type() {
        Map<Integer, String> values = new HashMap<>();
        values.put(2, "value");
        values.put(3, null);
        LwM2mMultipleResourceImpl.newResource(0, values, Type.BOOLEAN);
    }
}
