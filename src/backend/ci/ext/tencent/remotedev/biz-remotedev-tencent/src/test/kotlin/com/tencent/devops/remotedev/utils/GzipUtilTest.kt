package com.tencent.devops.remotedev.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class GzipUtilTest {

    @Test
    fun gzipBytes() {
        val data = """
            i'm test,
            so, do it.
        """
        val gzip = GzipUtil.gzipBytes(data.toByteArray())
        val unzip = String(GzipUtil.unzipBytes(gzip))
        Assertions.assertEquals(unzip, data)
    }
}
