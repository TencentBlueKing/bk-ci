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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_VERSION
import io.mockk.every
import io.mockk.mockk
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BuildVariableServiceTest {

    private val dslContext: DSLContext = mockk()
    private val pipelineBuildVarDao: PipelineBuildVarDao = mockk()
    private val redisOperation: RedisOperation = RedisOperation(mockk())
    private val pipelineAsCodeService: PipelineAsCodeService = mockk(relaxed = true)

    private val buildVariableService = BuildVariableService(
        commonDslContext = dslContext,
        pipelineBuildVarDao = pipelineBuildVarDao,
        redisOperation = redisOperation,
        pipelineAsCodeService = pipelineAsCodeService
    )

    @Test
    fun getAllVariable() {
        val projectId = "devops"
        val buildId = "b-1234567890"
        val pipelineId = "p-1234567890"

        val mockVars = mutableMapOf(
            "pipeline.start.channel" to "BS",
            "pipeline.start.isMobile" to "false",
            "pipeline.name" to "流水线名称",
            "pipeline.build.num" to "210",
            "pipeline.start.type" to "SERVICE",
            "pipeline.start.user.id" to "user123",
            "pipeline.start.user.name" to "人名",
            "pipeline.version" to "34"
        )

        every { pipelineBuildVarDao.getVars(dslContext, projectId, buildId) } returns (mockVars)
        val allVariable = buildVariableService.getAllVariable(projectId, pipelineId, buildId)

        Assertions.assertEquals(mockVars["pipeline.start.channel"], allVariable[PIPELINE_START_CHANNEL])
        Assertions.assertEquals(allVariable["pipeline.start.channel"], allVariable[PIPELINE_START_CHANNEL])

        Assertions.assertEquals(mockVars["pipeline.start.isMobile"], allVariable[PIPELINE_START_MOBILE])
        Assertions.assertEquals(allVariable["pipeline.start.isMobile"], allVariable[PIPELINE_START_MOBILE])

        Assertions.assertEquals(mockVars["pipeline.name"], allVariable[PIPELINE_NAME])
        Assertions.assertEquals(allVariable["pipeline.name"], allVariable[PIPELINE_NAME])

        Assertions.assertEquals(mockVars["pipeline.build.num"], allVariable[PIPELINE_BUILD_NUM])
        Assertions.assertEquals(allVariable["pipeline.build.num"], allVariable[PIPELINE_BUILD_NUM])

        Assertions.assertEquals(mockVars["pipeline.name"], allVariable[PIPELINE_NAME])
        Assertions.assertEquals(allVariable["pipeline.name"], allVariable[PIPELINE_NAME])

        Assertions.assertEquals(mockVars["pipeline.start.type"], allVariable[PIPELINE_START_TYPE])
        Assertions.assertEquals(allVariable["pipeline.start.type"], allVariable[PIPELINE_START_TYPE])

        Assertions.assertEquals(mockVars["pipeline.start.user.id"], allVariable[PIPELINE_START_USER_ID])
        Assertions.assertEquals(allVariable["pipeline.start.user.id"], allVariable[PIPELINE_START_USER_ID])

        Assertions.assertEquals(mockVars["pipeline.start.user.name"], allVariable[PIPELINE_START_USER_NAME])
        Assertions.assertEquals(allVariable["pipeline.start.user.name"], allVariable[PIPELINE_START_USER_NAME])

        Assertions.assertEquals(mockVars["pipeline.version"], allVariable[PIPELINE_VERSION])
        Assertions.assertEquals(allVariable["pipeline.version"], allVariable[PIPELINE_VERSION])
    }
}
