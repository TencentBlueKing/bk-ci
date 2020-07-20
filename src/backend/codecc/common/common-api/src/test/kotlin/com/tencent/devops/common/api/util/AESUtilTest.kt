package com.tencent.devops.common.api.util

import com.tencent.devops.common.util.AESUtil
import org.junit.Assert
import org.junit.Test

/**
 * @version 1.0
 */
class AESUtilTest {

    @Test
    fun encrypt() {
        val str = "123456789012345678901234567123456789012345"
        val encrypted = AESUtil.encrypt("1234567890abcdef1234567890abcdef", str)
        val decrpted = AESUtil.decrypt("1234567890abcdef1234567890abcdef", encrypted)
        Assert.assertEquals(str, decrpted)
    }

    @Test
    fun encrypt2() {
        val str = "123456789012345678901234567123456789012345".toByteArray()
        val encrypted = AESUtil.encrypt("1234567890abcdef1234567890abcdef", str)
        val decrpted = AESUtil.decrypt("1234567890abcdef1234567890abcdef", encrypted)
        Assert.assertEquals(str.contentToString(), decrpted.contentToString())
    }
}
