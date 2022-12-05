/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.rpm.util

import com.tencent.bkrepo.rpm.pojo.IndexType
import com.tencent.bkrepo.rpm.util.XmlStrUtils.findPackageIndex
import com.tencent.bkrepo.rpm.util.XmlStrUtils.indexOf
import com.tencent.bkrepo.rpm.util.xStream.XStreamUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.regex.Pattern

class XmlStrUtilsTest {
    /**
     * 按照仓库设置的repodata 深度分割请求参数
     */
    @Test
    fun resolveRepodataUriTest() {
        val uri = "/7/os/x86_64/hello-world-1-1.x86_64.rpm"
        val depth = 3
        val repodataUri = XmlStrUtils.resolveRepodataUri(uri, depth)
        Assertions.assertEquals("7/os/x86_64/", repodataUri.repoDataPath)
        Assertions.assertEquals("hello-world-1-1.x86_64.rpm", repodataUri.artifactRelativePath)

        val uri2 = "/7/hello-world-1-1.x86_64.rpm"
        val depth2 = 1
        val repodataUri2 = XmlStrUtils.resolveRepodataUri(uri2, depth2)
        Assertions.assertEquals("/7/", repodataUri2.repoDataPath)
        Assertions.assertEquals("hello-world-1-1.x86_64.rpm", repodataUri2.artifactRelativePath)

        val uri3 = "/hello-world-1-1.x86_64.rpm"
        val depth3 = 0
        val repodataUri3 = XmlStrUtils.resolveRepodataUri(uri3, depth3)
        Assertions.assertEquals("/", repodataUri3.repoDataPath)
        Assertions.assertEquals("hello-world-1-1.x86_64.rpm", repodataUri3.artifactRelativePath)
    }

    @Test
    fun updatePackageCountTest() {
        val start = System.currentTimeMillis()
        val file = File("/Downloads/60M.xml")
        val randomAccessFile = RandomAccessFile(file, "rw")
        XmlStrUtils.updatePackageCount(randomAccessFile, IndexType.PRIMARY, 0, true)
        println(System.currentTimeMillis() - start)
    }

    @Test
    fun packagesModifyTest01() {
        val regex = "^<metadata xmlns=\"http://linux.duke.edu/metadata/common\" xmlns:rpm=\"http://linux.duke" +
            ".edu/metadata/rpm\" packages=\"(\\d+)\">$"
        val str = "<metadata xmlns=\"http://linux.duke.edu/metadata/common\" xmlns:rpm=\"http://linux.duke" +
            ".edu/metadata/rpm\" packages=\"\">"
        val matcher = Pattern.compile(regex).matcher(str)
        if (matcher.find()) {
            println(matcher.group(1).toInt())
        }
    }

    @Test
    fun indexOfTest() {
        val file = File("/Users/weaving/Downloads/filelist/21e8c7280184d7428e4fa259c669fa4b2cfef05f-filelists.xml")
        val randomAccessFile = RandomAccessFile(file, "r")
        val index = indexOf(
            randomAccessFile,
            """<package pkgid="cb764f7906736425286341f6c5939347b01c5c17" 
            |name="httpd" arch="x86_64">""".trimMargin()
        )
        Assertions.assertEquals(287, index)
    }

    @Test
    fun indexPackageTest() {
        val file = File("others.xml")
        val randomAccessFile = RandomAccessFile(file, "r")
        val prefixStr = "  <package pkgid="
        val locationStr =
            """name="trpc-go-helloword">
    <version epoch="0" ver="0.0.1" rel="1"/>"""
        val suffixStr = "</package>"
        val xmlIndex = findPackageIndex(randomAccessFile, prefixStr, locationStr, suffixStr)
        if (xmlIndex != null) {
            println(xmlIndex.prefixIndex)
            println(xmlIndex.locationIndex)
            println(xmlIndex.suffixIndex)
            println(xmlIndex.suffixEndIndex)
        }
    }

    @Test
    fun updateFileTest() {
        val file = File("others.xml")
        XmlStrUtils.updatePackageXml(RandomAccessFile(file, "rw"), 3, 1, "a".toByteArray())
    }

    @Test
    fun resolvePackageCountTest() {
        val file = File("${System.getenv("HOME")}/Downloads/63da8904a2791e4965dcda350b26ffa3d1eda27b-primary")
        val randomAccessFile = RandomAccessFile(file, "r")
        val count = XmlStrUtils.resolvePackageCount(randomAccessFile, IndexType.PRIMARY)
        print("count: $count")
    }

    @Test
    fun test() {
        val file = File(
            "${System.getenv("HOME")}/Downloads/nfaprofile_consumer_request-master" +
                "-20200921636887-1-x86_64.rpm"
        )
        println(XStreamUtil.checkMarkFile(file.inputStream().readBytes(), IndexType.PRIMARY))
    }
}
