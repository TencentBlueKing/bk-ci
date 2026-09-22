/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
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

package com.tencent.devops.common.archive.util

import com.tencent.devops.common.api.exception.ErrorCodeException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PathUtilTest {

    @Test
    fun normalizeAndValidateRepoPath_acceptsLegitimateRelativePaths() {
        Assertions.assertEquals("file/20240101/uuid.zip", PathUtil.normalizeAndValidateRepoPath("file/20240101/uuid.zip"))
        Assertions.assertEquals("store/1.0.0/pkg.tgz", PathUtil.normalizeAndValidateRepoPath("store/1.0.0/pkg.tgz"))
        Assertions.assertEquals("foo/bar", PathUtil.normalizeAndValidateRepoPath("foo/./bar"))
        Assertions.assertEquals("/bk-custom/p/file.zip", PathUtil.normalizeAndValidateRepoPath("/bk-custom/p/file.zip"))
        Assertions.assertEquals("foo/bar", PathUtil.normalizeAndValidateRepoPath("foo\\bar"))
        Assertions.assertEquals("foo..bar.txt", PathUtil.normalizeAndValidateRepoPath("foo..bar.txt"))
        Assertions.assertEquals("foo+bar.zip", PathUtil.normalizeAndValidateRepoPath("foo+bar.zip"))
        Assertions.assertEquals("dir/sub/", PathUtil.normalizeAndValidateRepoPath("dir/sub/"))
        Assertions.assertEquals("v:1.0/pkg.zip", PathUtil.normalizeAndValidateRepoPath("v:1.0/pkg.zip"))
    }

    @Test
    fun normalizeAndValidateRepoPath_rejectsTraversalAndEncodedTraversal() {
        assertThrows<ErrorCodeException> { PathUtil.normalizeAndValidateRepoPath("../evil") }
        assertThrows<ErrorCodeException> { PathUtil.normalizeAndValidateRepoPath("foo/../../evil") }
        assertThrows<ErrorCodeException> { PathUtil.normalizeAndValidateRepoPath("%2e%2e%2fevil") }
        assertThrows<ErrorCodeException> { PathUtil.normalizeAndValidateRepoPath("%252e%252e%252fevil") }
        assertThrows<ErrorCodeException> { PathUtil.normalizeAndValidateRepoPath("C:/Windows/evil") }
        assertThrows<ErrorCodeException> { PathUtil.normalizeAndValidateRepoPath("C:\\Windows\\evil") }
        assertThrows<ErrorCodeException> { PathUtil.normalizeAndValidateRepoPath("") }
        assertThrows<ErrorCodeException> { PathUtil.normalizeAndValidateRepoPath("foo\u0000.zip") }
    }

    @Test
    fun normalizeAndValidateRepoPath_enforcesRequiredPrefix() {
        Assertions.assertEquals(
            "atom/1.0.0/pkg.zip",
            PathUtil.normalizeAndValidateRepoPath("atom/1.0.0/pkg.zip", requiredPrefix = "atom/1.0.0")
        )
        assertThrows<ErrorCodeException> {
            PathUtil.normalizeAndValidateRepoPath("other/1.0.0/pkg.zip", requiredPrefix = "atom/1.0.0")
        }
        assertThrows<ErrorCodeException> {
            PathUtil.normalizeAndValidateRepoPath("p1/b1/../../other/evil", requiredPrefix = "p1/b1")
        }
    }

    @Test
    fun getNormalizedPath_decodesThenRejectsTraversal() {
        Assertions.assertEquals("a/b", PathUtil.getNormalizedPath("a%2Fb"))
        assertThrows<ErrorCodeException> { PathUtil.getNormalizedPath("%2e%2e/etc/passwd") }
    }
}
