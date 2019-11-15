/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.archive

import org.junit.Test
import java.io.File

class FileDigestUtilsTest {

    @Test
    fun fileSha1() {
        val file = File.createTempFile("tmp_", "txt")
        file.deleteOnExit()
        val data1 = "1234567890\n"
        val data2 = "0987654321\n"
        file.writeText(data1)
        file.writeText(data2)
        file.deleteOnExit()
        // merge File
        val expect = FileDigestUtils.fileSha1(arrayOf(file.absolutePath))
        println("expect_fileSha1=$expect")
    }

    @Test
    fun fileMD5() {
        val file = File.createTempFile("tmp_", "txt")
        file.deleteOnExit()
        val data1 = "1234567890\n"
        val data2 = "0987654321\n"
        file.writeText(data1)
        file.writeText(data2)
        file.deleteOnExit()
        val expect = FileDigestUtils.fileMD5(arrayOf(file.absolutePath))
        println("expect_fileMD5=$expect")
    }

    @Test
    fun fileSha256() {
        val file = File.createTempFile("tmp_", "txt")
        file.deleteOnExit()
        val data1 = "1234567890\n"
        val data2 = "0987654321\n"
        file.writeText(data1)
        file.writeText(data2)
        file.deleteOnExit()
        val expect = FileDigestUtils.fileSha256(arrayOf(file.absolutePath))
        println("expect_fileSha256=$expect")
    }
}