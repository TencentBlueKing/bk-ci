package com.tencent.devops.common.api.util

import com.tencent.devops.common.util.HashUtil
import org.junit.Assert
import org.junit.Test

/**
 * @version 1.0
 */
class HashUtilTest {

    @Test(expected = IllegalArgumentException::class)
    fun maxLongId_9007199254740992L() {
        val expected = Long.MAX_VALUE
        HashUtil.encodeLongId(expected)
    }

    @Test
    fun longId() {
        val expected = 9007199254740992L
        val hashId = HashUtil.encodeLongId(expected)
        print("hashId=$hashId")
        val actual = HashUtil.decodeIdToLong(hashId)
        Assert.assertEquals(expected, actual)
        Assert.assertEquals(0, HashUtil.decodeIdToLong(""))
    }

    @Test
    fun intId() {
        val expected = Int.MAX_VALUE
        val hashId = HashUtil.encodeIntId(expected)
        print("hashId=$hashId")
        val actual = HashUtil.decodeIdToInt(hashId)
        Assert.assertEquals(expected, actual)
        Assert.assertEquals(0, HashUtil.decodeIdToInt(""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun maxOtherLongId_9007199254740992L() {
        val expected = Long.MAX_VALUE
        HashUtil.encodeOtherLongId(expected)
    }

    @Test
    fun otherLongId() {
        val expected = 9007199254740992L
        val hashId = HashUtil.encodeOtherLongId(expected)
        print("hashId=$hashId")
        val actual = HashUtil.decodeOtherIdToLong(hashId)
        Assert.assertEquals(expected, actual)
        Assert.assertEquals(0L, HashUtil.decodeOtherIdToLong(""))
    }

    @Test
    fun otherIdToInt() {
        val expected = Int.MAX_VALUE
        val hashId = HashUtil.encodeOtherIntId(expected)
        print("hashId=$hashId")
        val actual = HashUtil.decodeOtherIdToInt(hashId)
        Assert.assertEquals(expected, actual)
        Assert.assertEquals(0, HashUtil.decodeOtherIdToInt(""))
    }
}
