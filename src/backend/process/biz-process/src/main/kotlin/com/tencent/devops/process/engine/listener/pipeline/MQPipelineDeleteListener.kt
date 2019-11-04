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

package com.tencent.devops.process.engine.listener.pipeline

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.pojo.event.PipelineDeleteEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.service.PipelineUserService
import com.tencent.devops.process.service.label.PipelineGroupService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  MQ实现的流水线删除事件
 *
 * @version 1.0
 */
@Component
class MQPipelineDeleteListener @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val pipelineResDao: PipelineResDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineUserService: PipelineUserService,
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val modelCheckPlugin: ModelCheckPlugin
) : BaseListener<PipelineDeleteEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineDeleteEvent) {
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val userId = event.userId
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val allVersionModel = pipelineResDao.getAllVersionModel(transactionContext, pipelineId)
            allVersionModel.forEach {
                try {
                    val model = objectMapper.readValue(it, Model::class.java)
                    modelCheckPlugin.beforeDeleteElementInExistsModel(userId, model, null, pipelineId)
                } catch (ignored: Exception) {
                    logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", ignored)
                }
            }
            if (event.clearUpModel) {
                pipelineResDao.deleteAllVersion(transactionContext, pipelineId)
                pipelineSettingDao.delete(transactionContext, pipelineId)
            }
        }

        if (event.clearUpModel) {
            pipelineGroupService.deleteAllUserFavorByPipeline(userId, pipelineId) // 删除收藏该流水线上所有记录
            pipelineGroupService.deletePipelineLabel(userId, pipelineId)
            pipelineUserService.delete(pipelineId)
            pipelineRuntimeService.deletePipelineBuilds(projectId, pipelineId, userId)
        }
    }
}
