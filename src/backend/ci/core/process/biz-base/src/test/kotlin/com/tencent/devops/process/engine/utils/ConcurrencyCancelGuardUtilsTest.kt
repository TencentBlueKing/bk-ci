/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.engine.utils

import com.tencent.devops.process.engine.pojo.ConcurrencyGroupBuild
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ConcurrencyCancelGuardUtilsTest {

    @Test
    fun contextOnlyResolveBuildNumOnRetry() {
        var loaded = false
        val retryContext = ConcurrencyCancelContext.of(
            pipelineId = "p-1",
            buildId = "b-1",
            isRetry = true
        ) {
            loaded = true
            5
        }
        Assertions.assertEquals(5, retryContext.retryBuildNum)
        Assertions.assertTrue(loaded)

        loaded = false
        val newStartContext = ConcurrencyCancelContext.of(
            pipelineId = "p-1",
            buildId = "b-new",
            isRetry = false
        ) {
            loaded = true
            1
        }
        Assertions.assertNull(newStartContext.retryBuildNum)
        Assertions.assertFalse(loaded)
    }

    @Test
    fun skipCancelSelfAlways() {
        val current = ConcurrencyCancelContext("p-1", "b-self")
        Assertions.assertTrue(
            filter(current, listOf(build("p-1", "b-self", 1))).isEmpty()
        )
        val retryCurrent = ConcurrencyCancelContext("p-1", "b-self", retryBuildNum = 5)
        Assertions.assertTrue(
            filter(retryCurrent, listOf(build("p-1", "b-self", 8))).isEmpty()
        )
    }

    @Test
    fun newTriggerStillCancelOtherBuilds() {
        val result = ConcurrencyCancelGuardUtils.filterTargets(
            candidateBuilds = listOf(build("p-1", "b-old-running", 3)),
            currentContext = ConcurrencyCancelContext("p-1", "b-new")
        )
        Assertions.assertEquals(listOf(build("p-1", "b-old-running", 3)), result)
    }

    @Test
    fun retrySkipNewerSamePipelineBuild() {
        val current = ConcurrencyCancelContext("p-1", "b-old", retryBuildNum = 5)
        Assertions.assertTrue(
            filter(current, listOf(build("p-1", "b-latest", 6))).isEmpty()
        )
    }

    @Test
    fun retryStillCancelOlderSamePipelineBuild() {
        val current = ConcurrencyCancelContext("p-1", "b-latest", retryBuildNum = 6)
        Assertions.assertEquals(
            listOf(build("p-1", "b-old-running", 5)),
            filter(current, listOf(build("p-1", "b-old-running", 5)))
        )
    }

    @Test
    fun retryKeepCrossPipelineCancel() {
        val result = ConcurrencyCancelGuardUtils.filterTargets(
            candidateBuilds = listOf(build("p-2", "b-p2", 99)),
            currentContext = ConcurrencyCancelContext("p-1", "b-p1", retryBuildNum = 1)
        )
        Assertions.assertEquals(listOf(build("p-2", "b-p2", 99)), result)
    }

    private fun filter(
        current: ConcurrencyCancelContext,
        candidates: List<ConcurrencyGroupBuild>
    ) = ConcurrencyCancelGuardUtils.filterTargets(candidates, current)

    private fun build(pipelineId: String, buildId: String, buildNum: Int) =
        ConcurrencyGroupBuild(pipelineId, buildId, buildNum)
}
