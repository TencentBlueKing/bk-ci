package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.common.artifact.hash.md5
import com.tencent.bkrepo.common.artifact.hash.sha1
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertEquals

internal class MavenUtilTest {

    @Test
    fun extractDigest() {
        val data = "123456"
        val md5 = data.md5()
        val sha1 = data.sha1()
        assertEquals(md5, MavenUtil.extractDigest(md5.byteInputStream()))
        assertEquals(sha1, MavenUtil.extractDigest(sha1.byteInputStream()))
    }
}
