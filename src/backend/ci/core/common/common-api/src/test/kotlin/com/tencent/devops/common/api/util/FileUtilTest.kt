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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.exception.ErrorCodeException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileUtilTest {
    @Test
    fun getMD5() {
        val file = File.createTempFile("md5_", ".tmp")
        file.deleteOnExit()
        file.writeText("123 aaa")
        println("create file: ${file.canonicalPath}")
        assert("bd94e431dfd6319590fe5908dd36d54a" == FileUtil.getMD5(file))
        assert("bd94e431dfd6319590fe5908dd36d54a" == FileUtil.getMD5(file.readText()))
        assert("bd94e431dfd6319590fe5908dd36d54a" == FileUtil.getMD5(file.readBytes()))
    }

    @Test
    fun outFile() {
        FileUtil.outFile("build/", "test", "test")
        val file = File("build/test")
        Assertions.assertEquals(file.exists(), true)
        Assertions.assertEquals(file.name, "test")
        Assertions.assertEquals(file.readText(), "test")
    }

    @Test
    fun getSafeFileName_acceptsLegitimateNames() {
        Assertions.assertEquals("mystore-1.0.0.zip", FileUtil.getSafeFileName("mystore-1.0.0.zip"))
        Assertions.assertEquals("linux-x64-1.0.tar.gz", FileUtil.getSafeFileName("linux-x64-1.0.tar.gz"))
        Assertions.assertEquals("中文插件-1.0.zip", FileUtil.getSafeFileName("中文插件-1.0.zip"))
        Assertions.assertEquals("foo.bar.baz.tar.gz", FileUtil.getSafeFileName("foo.bar.baz.tar.gz"))
    }

    @Test
    fun getSafeFileName_stripsClientPathPrefix() {
        // 旧版 IE/curl 客户端可能提交带路径的 filename，统一 basename 化
        Assertions.assertEquals("foo.zip", FileUtil.getSafeFileName("C:\\Users\\me\\foo.zip"))
        Assertions.assertEquals("foo.zip", FileUtil.getSafeFileName("/home/me/foo.zip"))
        Assertions.assertEquals("passwd", FileUtil.getSafeFileName("../etc/passwd"))
        Assertions.assertEquals("foo.zip", FileUtil.getSafeFileName("a/b/c/foo.zip"))
    }

    @Test
    fun getSafeFileName_rejectsDangerousInputs() {
        assertThrows<ErrorCodeException> { FileUtil.getSafeFileName(null) }
        assertThrows<ErrorCodeException> { FileUtil.getSafeFileName("") }
        assertThrows<ErrorCodeException> { FileUtil.getSafeFileName("   ") }
        assertThrows<ErrorCodeException> { FileUtil.getSafeFileName(".") }
        assertThrows<ErrorCodeException> { FileUtil.getSafeFileName("..") }
        assertThrows<ErrorCodeException> { FileUtil.getSafeFileName("....zip") }
        assertThrows<ErrorCodeException> { FileUtil.getSafeFileName("foo\u0000.zip") }
        // Windows 盘符
        assertThrows<ErrorCodeException> { FileUtil.getSafeFileName("C:foo.zip") }
    }

    @Test
    fun resolveSafeChildFile_acceptsLegitimateRelativePaths(@TempDir baseDir: File) {
        // pkgLocalPath 这类合法的多级相对路径
        val result = FileUtil.resolveSafeChildFile(baseDir, "linux/x64/foo.tar.gz")
        Assertions.assertEquals(File(baseDir, "linux/x64/foo.tar.gz"), result)

        val basenameResult = FileUtil.resolveSafeChildFile(baseDir, "foo.zip")
        Assertions.assertEquals(File(baseDir, "foo.zip"), basenameResult)
    }

    @Test
    fun resolveSafeChildFile_rejectsTraversal(@TempDir baseDir: File) {
        assertThrows<ErrorCodeException> {
            FileUtil.resolveSafeChildFile(baseDir, "../etc/passwd")
        }
        assertThrows<ErrorCodeException> {
            FileUtil.resolveSafeChildFile(baseDir, "../../../etc/passwd")
        }
        assertThrows<ErrorCodeException> {
            FileUtil.resolveSafeChildFile(baseDir, "foo/../../bar")
        }
    }

    @Test
    fun resolveSafeChildFile_rejectsBlankAndNull(@TempDir baseDir: File) {
        assertThrows<ErrorCodeException> { FileUtil.resolveSafeChildFile(baseDir, null) }
        assertThrows<ErrorCodeException> { FileUtil.resolveSafeChildFile(baseDir, "") }
        assertThrows<ErrorCodeException> { FileUtil.resolveSafeChildFile(baseDir, "   ") }
        assertThrows<ErrorCodeException> { FileUtil.resolveSafeChildFile(baseDir, "foo\u0000.zip") }
    }

    @Test
    fun resolveSafeChildFile_returnsRawPathToKeepStringConcatCompatible(@TempDir baseDir: File) {
        // 返回值保留传入 baseDir 的路径形式（未 canonical 化），方便调用方做 removePrefix 等字符串处理
        val result = FileUtil.resolveSafeChildFile(baseDir, "a/b/c.txt")
        Assertions.assertTrue(
            result.path.startsWith(baseDir.path),
            "expected result.path '${result.path}' to start with baseDir.path '${baseDir.path}'"
        )
    }

    @Test
    fun resolveSafeChildFile_blocksSimilarPrefixAttack(@TempDir parent: File) {
        // 防止 /srv/data 与 /srv/data_evil 这类前缀相似但元素不同的越界
        val baseDir = File(parent, "data").apply { mkdirs() }
        val siblingEvil = File(parent, "data_evil").apply { mkdirs() }
        File(siblingEvil, "foo").writeText("x")
        assertThrows<ErrorCodeException> {
            FileUtil.resolveSafeChildFile(baseDir, "../data_evil/foo")
        }
    }
}
