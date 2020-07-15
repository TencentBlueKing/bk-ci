package com.tencent.devops.sign.utils

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.io.*


public class IpaSignUtilTest {
    private val bufferSize = 8 * 1024
    private val inputStreamFile = "/data/test/test.ipa"
    private val outputStreamStreamFile = "/data/test/test_md5.ipa"


    /*
    * 复制流到目标文件，并计算md5
    * */
    @Test
    fun copyInputStreamToFile(
    ) {
        val inputStream = File(inputStreamFile).inputStream()
        val outputStreamFile = File(outputStreamStreamFile)
        val md5 = IpaFileUtil.copyInputStreamToFile(inputStream, outputStreamFile)
        assertEquals("20280fbe2856a679abc636f0db263231", md5)
    }
}