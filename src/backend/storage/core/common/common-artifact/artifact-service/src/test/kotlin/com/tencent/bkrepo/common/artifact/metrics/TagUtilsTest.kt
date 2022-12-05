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

package com.tencent.bkrepo.common.artifact.metrics

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TagUtilsTest {

    @Test
    fun testTagOfProjectAndRepo() {
        val includeAllRepositories = listOf("*/*")
        val includeSingleProjectRepositories = listOf("$UT_PROJECT/*")
        val includeOneTypeRepository = listOf("*/$LOG_REPO")
        val includeCertainRepositories = listOf("$UT_PROJECT/$UT_REPO_1", "$UT_PROJECT/$UT_REPO_2")
        Assertions.assertEquals(
            Tags.of(Tag.of(PROJECT_ID, UT_PROJECT), Tag.of(REPO_NAME, UT_REPO_1)),
            TagUtils.tagOfProjectAndRepo(UT_PROJECT, UT_REPO_1, includeAllRepositories)
        )
        Assertions.assertEquals(
            Tags.of(Tag.of(PROJECT_ID, UT_PROJECT), Tag.of(REPO_NAME, StringPool.UNKNOWN)),
            TagUtils.tagOfProjectAndRepo(UT_PROJECT, UT_REPO_1, includeSingleProjectRepositories)
        )
        Assertions.assertEquals(
            Tags.of(Tag.of(PROJECT_ID, StringPool.UNKNOWN), Tag.of(REPO_NAME, LOG_REPO)),
            TagUtils.tagOfProjectAndRepo(UT_PROJECT, LOG_REPO, includeOneTypeRepository)
        )
        Assertions.assertEquals(
            Tags.of(Tag.of(PROJECT_ID, UT_PROJECT), Tag.of(REPO_NAME, UT_REPO_1)),
            TagUtils.tagOfProjectAndRepo(UT_PROJECT, UT_REPO_1, includeAllRepositories)
        )
        Assertions.assertEquals(
            Tags.of(Tag.of(PROJECT_ID, UT_PROJECT), Tag.of(REPO_NAME, UT_REPO_2)),
            TagUtils.tagOfProjectAndRepo(UT_PROJECT, UT_REPO_2, includeCertainRepositories)
        )
    }

    companion object {
        private const val UT_PROJECT = "ut-project"
        private const val UT_REPO_1 = "repo1"
        private const val UT_REPO_2 = "repo2"
        private const val LOG_REPO = "log"
    }
}
