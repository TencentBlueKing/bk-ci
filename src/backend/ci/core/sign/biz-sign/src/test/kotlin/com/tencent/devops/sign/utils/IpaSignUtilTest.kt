package com.tencent.devops.sign.utils

import com.tencent.devops.common.api.util.FileUtil
import junit.framework.Assert.assertEquals
import org.junit.Test
import java.io.File

class IpaSignUtilTest {

    private val inputStreamFile = "./test.ipa"
    private val outputStreamStreamFile = "./test_md5.ipa"

    /*
    * 复制流到目标文件，并计算md5
    * */
    @Test
    fun copyInputStreamToFile() {
        val testFile = File(inputStreamFile)
        val exists = testFile.exists()
        if (!exists) {
            testFile.parentFile.mkdirs()
            testFile.writeText("Just for test!!!")
        }
        val inputStream = File(inputStreamFile).inputStream()
        val outputStreamFile = File(outputStreamStreamFile)
        val md5 = IpaFileUtil.copyInputStreamToFile(inputStream, outputStreamFile)
        assertEquals(FileUtil.getMD5(testFile), md5)
        if (!exists) {
            testFile.delete()
        }
        outputStreamFile.delete()
    }
}