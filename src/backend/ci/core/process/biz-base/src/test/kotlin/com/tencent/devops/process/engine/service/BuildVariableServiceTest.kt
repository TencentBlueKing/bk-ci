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

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_VERSION
import org.jooq.DSLContext
import org.junit.Assert
import org.junit.Test

class BuildVariableServiceTest {

    private val dslContext: DSLContext = mock()
    private val pipelineBuildVarDao: PipelineBuildVarDao = mock()
    private val redisOperation: RedisOperation = RedisOperation(mock())

    private val buildVariableService = BuildVariableService(
        commonDslContext = dslContext,
        pipelineBuildVarDao = pipelineBuildVarDao,
        redisOperation = redisOperation
    )

    @Test
    fun getAllVariable() {
        val projectId = "devops"
        val buildId = "b-1234567890"

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

        whenever(pipelineBuildVarDao.getVars(dslContext, projectId, buildId)).thenReturn(mockVars)
        val allVariable = buildVariableService.getAllVariable(projectId, buildId)

        Assert.assertEquals(mockVars["pipeline.start.channel"], allVariable[PIPELINE_START_CHANNEL])
        Assert.assertEquals(allVariable["pipeline.start.channel"], allVariable[PIPELINE_START_CHANNEL])

        Assert.assertEquals(mockVars["pipeline.start.isMobile"], allVariable[PIPELINE_START_MOBILE])
        Assert.assertEquals(allVariable["pipeline.start.isMobile"], allVariable[PIPELINE_START_MOBILE])

        Assert.assertEquals(mockVars["pipeline.name"], allVariable[PIPELINE_NAME])
        Assert.assertEquals(allVariable["pipeline.name"], allVariable[PIPELINE_NAME])

        Assert.assertEquals(mockVars["pipeline.build.num"], allVariable[PIPELINE_BUILD_NUM])
        Assert.assertEquals(allVariable["pipeline.build.num"], allVariable[PIPELINE_BUILD_NUM])

        Assert.assertEquals(mockVars["pipeline.name"], allVariable[PIPELINE_NAME])
        Assert.assertEquals(allVariable["pipeline.name"], allVariable[PIPELINE_NAME])

        Assert.assertEquals(mockVars["pipeline.start.type"], allVariable[PIPELINE_START_TYPE])
        Assert.assertEquals(allVariable["pipeline.start.type"], allVariable[PIPELINE_START_TYPE])

        Assert.assertEquals(mockVars["pipeline.start.user.id"], allVariable[PIPELINE_START_USER_ID])
        Assert.assertEquals(allVariable["pipeline.start.user.id"], allVariable[PIPELINE_START_USER_ID])

        Assert.assertEquals(mockVars["pipeline.start.user.name"], allVariable[PIPELINE_START_USER_NAME])
        Assert.assertEquals(allVariable["pipeline.start.user.name"], allVariable[PIPELINE_START_USER_NAME])

        Assert.assertEquals(mockVars["pipeline.version"], allVariable[PIPELINE_VERSION])
        Assert.assertEquals(allVariable["pipeline.version"], allVariable[PIPELINE_VERSION])
    }
}
