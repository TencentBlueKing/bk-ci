package com.tencent.bk.devops.atom.pojo;

import com.tencent.bk.devops.atom.common.DataType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @version 1.0
 */
public class StringDataTest {

    @Test
    public void test() {
        String value = "value";
        StringData data = new StringData(value);
        assertEquals(value, data.getValue());
        assertEquals(DataType.string, data.getType());
    }
}
