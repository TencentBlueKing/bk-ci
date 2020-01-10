package com.tencent.devops.common.api.util

import com.tencent.devops.common.util.ReflectUtil
import org.junit.Assert
import org.junit.Test

/**
 * @version 1.0
 */
class ReflectUtilTest {

    @Test
    fun isNativeType() {
        Assert.assertFalse(ReflectUtil.isNativeType("string"))
        Assert.assertTrue(ReflectUtil.isNativeType(Int.MAX_VALUE))
        Assert.assertTrue(ReflectUtil.isNativeType(Long.MAX_VALUE))
        Assert.assertTrue(ReflectUtil.isNativeType(Float.MIN_VALUE))
        Assert.assertTrue(ReflectUtil.isNativeType(Double.MAX_VALUE))
        Assert.assertTrue(ReflectUtil.isNativeType(Byte.MIN_VALUE))
        Assert.assertTrue(ReflectUtil.isNativeType(Short.MIN_VALUE))
    }
}
