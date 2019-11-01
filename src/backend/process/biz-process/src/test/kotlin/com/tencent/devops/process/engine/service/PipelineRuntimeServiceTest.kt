/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.process.engine.service

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.tencent.devops.common.api.constant.PIPELINE_MATERIAL_ALIASNAME
import com.tencent.devops.common.api.constant.PIPELINE_MATERIAL_BRANCHNAME
import com.tencent.devops.common.api.constant.PIPELINE_MATERIAL_NEW_COMMIT_COMMENT
import com.tencent.devops.common.api.constant.PIPELINE_MATERIAL_NEW_COMMIT_ID
import com.tencent.devops.common.api.constant.PIPELINE_MATERIAL_NEW_COMMIT_TIMES
import com.tencent.devops.common.api.constant.PIPELINE_MATERIAL_URL
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.dao.PipelineBuildContainerDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.service.BuildStartupParamService
import org.jooq.DSLContext
import org.junit.Assert
import org.junit.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.redis.core.RedisTemplate

class PipelineRuntimeServiceTest {

    private val rabbitTemplate: RabbitTemplate = mock()
    private val redisTemplate: RedisTemplate<String, String> = mock()
    private val pipelineEventDispatcher: PipelineEventDispatcher = mock()
    private val websocketDispatcher: WebSocketDispatcher = WebSocketDispatcher(rabbitTemplate)
    private val buildIdGenerator: BuildIdGenerator = BuildIdGenerator()
    private val dslContext: DSLContext = mock()
    private val pipelineBuildDao: PipelineBuildDao = mock()
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao = mock()
    private val pipelineBuildTaskDao: PipelineBuildTaskDao = mock()
    private val pipelineBuildStageDao: PipelineBuildStageDao = mock()
    private val pipelineBuildContainerDao: PipelineBuildContainerDao = mock()
    private val pipelineBuildVarDao: PipelineBuildVarDao = mock()
    private val buildDetailDao: BuildDetailDao = mock()
    private val client: Client = mock()
    private val buildStartupParamService: BuildStartupParamService = mock()
    private val redisOperation: RedisOperation = RedisOperation(redisTemplate)

    private val pipelineRuntimeService = PipelineRuntimeService(
        pipelineEventDispatcher = pipelineEventDispatcher,
        webSocketDispatcher = websocketDispatcher,
        websocketService =  mock(),
        buildIdGenerator = buildIdGenerator,
        dslContext = dslContext,
        pipelineBuildDao = pipelineBuildDao,
        pipelineBuildSummaryDao = pipelineBuildSummaryDao,
        pipelineBuildTaskDao = pipelineBuildTaskDao,
        pipelineBuildStageDao = pipelineBuildStageDao,
        pipelineBuildContainerDao = pipelineBuildContainerDao,
        pipelineBuildVarDao = pipelineBuildVarDao,
        buildDetailDao = buildDetailDao,
        client = client,
        buildStartupParamService = buildStartupParamService,
        templatePipelineDao = mock()
    )

    @Test
    fun getPipelineBuildMaterial() {
        val repoIds = setOf("11", "12", "13")
        val relation = mutableMapOf<String, String>()
        val mockVars = mutableMapOf<String, String>()
        repoIds.forEach { repoId ->
            mockVars["$PIPELINE_MATERIAL_URL$repoId"] = "http://xxxx/$repoId"
            relation["http://xxxx/$repoId"] = repoId
            mockVars["$PIPELINE_MATERIAL_NEW_COMMIT_TIMES$repoId"] = repoId
            mockVars["$PIPELINE_MATERIAL_ALIASNAME$repoId"] = "aliasName$repoId"
            mockVars["$PIPELINE_MATERIAL_BRANCHNAME$repoId"] = "branch_$repoId"
            mockVars["$PIPELINE_MATERIAL_NEW_COMMIT_ID$repoId"] = "new_commit_id_$repoId"
            mockVars["$PIPELINE_MATERIAL_NEW_COMMIT_COMMENT$repoId"] = "comment_$repoId"
        }

        val buildId = "b-12345678901234567890123456789012"
        whenever(pipelineBuildVarDao.getVars(dslContext, buildId)).thenReturn(mockVars)
        val pipelineBuildMaterial = pipelineRuntimeService.getPipelineBuildMaterial(buildId)
        Assert.assertEquals(pipelineBuildMaterial.size, repoIds.size)
        pipelineBuildMaterial.forEach {
            val repoId = relation[it.url]
            Assert.assertEquals("http://xxxx/$repoId", it.url)
            Assert.assertEquals("aliasName$repoId", it.aliasName)
            Assert.assertEquals("branch_$repoId", it.branchName)
            Assert.assertEquals("new_commit_id_$repoId", it.newCommitId)
            Assert.assertEquals("comment_$repoId", it.newCommitComment)
            Assert.assertEquals(repoId, "${it.commitTimes}")
        }

        println(JsonUtil.toJson(pipelineBuildMaterial))
    }
}