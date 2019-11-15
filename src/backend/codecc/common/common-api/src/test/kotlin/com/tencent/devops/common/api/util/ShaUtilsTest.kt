package com.tencent.devops.common.api.util

import com.tencent.devops.common.util.ShaUtils
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.ThreadLocalRandom

/**
 * @version 1.0
 */
class ShaUtilsTest {

    private var byteArray: ByteArray = byteArrayOf(1, 2, 3, 99, -12, -23, 69, 21, 112, -99)
    private val expectSha1 = "ec6a2bdb3c5dd567da1899216eab87358059520e"

    @Test
    fun sha1() {
        val sha1 = ShaUtils.sha1(byteArray)
        println("sha1=$sha1")
        Assert.assertEquals(DigestUtils.sha1Hex(byteArray), sha1)
        Assert.assertEquals(expectSha1, sha1)
    }

    @Test
    fun hmacSha1() {
        val key = ByteArray(20)
        ThreadLocalRandom.current().nextBytes(key)
        val hmacSha1 = ShaUtils.hmacSha1(key, byteArray)
        println("hmacSha1=$hmacSha1")
    }

    @Test
    fun isEqual() {
        val key = ByteArray(20)
        ThreadLocalRandom.current().nextBytes(key)
        Assert.assertFalse(ShaUtils.isEqual(key, byteArray))
        val sha1 = ShaUtils.sha1(key)
        Assert.assertFalse(ShaUtils.isEqual(expectSha1, sha1))
    }
}
