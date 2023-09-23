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
 *
 */

package com.tencent.devops.process.trigger

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.trigger.actions.EventActionFactory
import com.tencent.devops.process.trigger.actions.data.PacRepoSetting
import com.tencent.devops.process.trigger.actions.data.PacTriggerPipeline
import com.tencent.devops.process.trigger.actions.pacActions.data.PacEnableEvent
import com.tencent.devops.process.trigger.mq.pacTrigger.PacTriggerEvent
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.RepoPacSyncFileInfo
import com.tencent.devops.repository.pojo.enums.RepoPacSyncStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PacYamlTriggerService @Autowired constructor(
    private val client: Client,
    private val eventActionFactory: EventActionFactory,
    private val dslContext: DSLContext,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val traceEventDispatcher: TraceEventDispatcher,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PacYamlTriggerService::class.java)
    }

    fun enablePac(userId: String, projectId: String, repoHashId: String, scmType: ScmType) {
        logger.info("enable pac|$userId|$projectId|$repoHashId|$scmType")
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repoHashId,
            repositoryType = RepositoryType.ID
        ).data ?: return
        val setting = PacRepoSetting(repository = repository)
        val event = PacEnableEvent(
            userId = userId,
            projectId = projectId,
            repoHashId = repoHashId,
            scmType = scmType
        )
        val action = eventActionFactory.loadEnableEvent(setting = setting, event = event)

        val ciDirId = action.getCiDirId()
        val yamlPathList = action.getYamlPathList()
        client.get(ServiceRepositoryPacResource::class).initPacSyncDetail(
            projectId = projectId,
            repositoryHashId = repoHashId,
            ciDirId = ciDirId,
            syncFileInfoList = yamlPathList.map {
                RepoPacSyncFileInfo(
                    filePath = it.yamlPath,
                    syncStatus = RepoPacSyncStatusEnum.SYNC
                )
            }
        )
        // 如果没有Yaml文件则不初始化
        if (yamlPathList.isEmpty()) {
            logger.warn("enable pac,not found ci yaml from git|$projectId|$repoHashId")
            return
        }
        val path2PipelineExists = pipelineYamlInfoDao.getAllByRepo(
            dslContext = dslContext, projectId = projectId, repoHashId = repoHashId
        ).associate {
            it.filePath to PacTriggerPipeline(
                projectId = it.projectId,
                repoHashId = it.repoHashId,
                filePath = it.filePath,
                pipelineId = it.pipelineId,
                userId = userId
            )
        }
        yamlPathList.forEach {
            val triggerPipeline = path2PipelineExists[it.yamlPath] ?: PacTriggerPipeline(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = it.yamlPath,
                pipelineId = "",
                userId = userId
            )
            action.data.context.pipeline = triggerPipeline
            action.data.context.yamlFile = it
            traceEventDispatcher.dispatch(
                PacTriggerEvent(
                    projectId = projectId,
                    eventStr = objectMapper.writeValueAsString(event),
                    metaData = action.metaData,
                    actionCommonData = action.data.eventCommon,
                    actionContext = action.data.context,
                    actionSetting = action.data.setting
                )
            )
        }
    }
}
