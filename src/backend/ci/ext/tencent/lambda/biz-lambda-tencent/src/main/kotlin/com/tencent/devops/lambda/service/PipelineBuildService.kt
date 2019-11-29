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
package com.tencent.devops.lambda.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildElementFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.lambda.LambdaMessageCode.ERROR_LAMBDA_PROJECT_NOT_EXIST
import com.tencent.devops.lambda.dao.BuildTaskDao
import com.tencent.devops.lambda.dao.LambdaPipelineBuildDao
import com.tencent.devops.lambda.dao.PipelineResDao
import com.tencent.devops.lambda.dao.PipelineTemplateDao
import com.tencent.devops.lambda.pojo.BuildData
import com.tencent.devops.lambda.pojo.ElementData
import com.tencent.devops.lambda.pojo.ProjectOrganize
import com.tencent.devops.lambda.storage.ESService
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PipelineBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val lambdaPipelineBuildDao: LambdaPipelineBuildDao,
    private val pipelineResDao: PipelineResDao,
    private val pipelineTemplateDao: PipelineTemplateDao,
    private val buildTaskDao: BuildTaskDao,
    private val esService: ESService
) {

    fun onBuildFinish(event: PipelineBuildFinishBroadCastEvent) {
        val info = getBuildInfo(event.buildId)
        if (info == null) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] The build info is not exist")
            return
        }
        val model = getModel(info.pipelineId, info.version)
        if (model == null) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] Fail to get the pipeline model")
            return
        }

        val projectInfo = projectCache.get(info.projectId)

        val data = BuildData(
            projectId = info.projectId,
            pipelineId = info.pipelineId,
            buildId = info.buildId,
            userId = info.startUser,
            status = info.status.name,
            trigger = info.trigger,
            beginTime = info.startTime ?: 0,
            endTime = info.endTime ?: 0,
            buildNum = info.buildNum,
            templateId = templateCache.get(info.pipelineId),
            bgName = projectInfo.bgName,
            deptName = projectInfo.deptName,
            centerName = projectInfo.centerName,
            model = model,
            errorType = event.errorType,
            errorCode = event.errorCode,
            errorMsg = event.errorMsg
        )
        esService.build(data)
    }

    fun onBuildElementFinish(event: PipelineBuildElementFinishBroadCastEvent) {
        val task = buildTaskDao.getTask(dslContext, event.buildId, event.elementId)
        if (task == null) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.elementId}] Fail to get the build task")
            return
        }
        val data = ElementData(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            elementId = event.elementId,
            elementName = task.taskName ?: "",
            status = BuildStatus.values()[task.status ?: 0].name,
            beginTime = task.startTime?.timestampmilli() ?: 0,
            endTime = task.endTime?.timestampmilli() ?: 0,
            type = task.taskType ?: "",
            atomCode = task.taskAtom ?: "",
            errorType = event.errorType,
            errorCode = event.errorCode,
            errorMsg = event.errorMsg
        )
        esService.buildElement(data)
    }

    private val projectCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*Build*/, ProjectOrganize>(
            object : CacheLoader<String, ProjectOrganize>() {
                override fun load(projectId: String): ProjectOrganize {
                    val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
                    if (projectInfo == null) {
                        logger.warn("[$projectId] Fail to get the project info")
                        throw InvalidParamException(
                            message = "Fail to get the project info, projectId=$projectId",
                            errorCode = ERROR_LAMBDA_PROJECT_NOT_EXIST,
                            params = arrayOf(projectId)
                        )
                    }
                    return ProjectOrganize(
                        projectId = projectId,
                        bgName = projectInfo.bgName ?: "",
                        deptName = projectInfo.deptName ?: "",
                        centerName = projectInfo.centerName ?: ""
                    )
                }
            }
        )

    private val templateCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*pipelineId*/, String/*templateId*/>(
            object : CacheLoader<String, String>() {
                override fun load(pipelineId: String): String {
                    return pipelineTemplateDao.getTemplate(dslContext, pipelineId)?.templateId ?: ""
                }
            }
        )

    private fun getBuildInfo(buildId: String): BuildInfo? {
        return convert(lambdaPipelineBuildDao.getBuildInfo(dslContext, buildId))
    }

    private fun getModel(pipelineId: String, version: Int): String? {
        return pipelineResDao.getModel(dslContext, pipelineId, version)
    }

    private fun convert(t: TPipelineBuildHistoryRecord?): BuildInfo? {
        return if (t == null) {
            null
        } else {
            BuildInfo(
                projectId = t.projectId,
                pipelineId = t.pipelineId,
                buildId = t.buildId,
                version = t.version,
                buildNum = t.buildNum,
                trigger = t.trigger,
                status = BuildStatus.values()[t.status],
                startUser = t.startUser,
                startTime = t.startTime?.timestampmilli() ?: 0L,
                endTime = t.endTime?.timestampmilli() ?: 0L,
                taskCount = t.taskCount,
                firstTaskId = t.firstTaskId,
                parentBuildId = t.parentBuildId,
                parentTaskId = t.parentTaskId,
                channelCode = ChannelCode.valueOf(t.channel),
                errorType = if (t.errorType == null) null else ErrorType.values()[t.errorType],
                errorCode = t.errorCode,
                errorMsg = t.errorMsg
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildService::class.java)
    }
}