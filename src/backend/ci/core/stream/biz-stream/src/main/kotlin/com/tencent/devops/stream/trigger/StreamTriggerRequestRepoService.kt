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

package com.tencent.devops.stream.trigger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestRepoEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.GitRequestRepoEvent
import com.tencent.devops.stream.pojo.StreamRepoHookEvent
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.data.context.RepoTrigger
import com.tencent.devops.stream.trigger.actions.streamActions.StreamRepoTriggerAction
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import com.tencent.devops.stream.trigger.parsers.CheckStreamSetting
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ComplexCondition")
@Service
class StreamTriggerRequestRepoService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val streamSettingDao: StreamBasicSettingDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestRepoEventDao: GitRequestRepoEventDao,
    private val streamTriggerCache: StreamTriggerCache,
    private val exHandler: StreamTriggerExceptionHandler,
    private val eventActionFactory: EventActionFactory,
    @org.springframework.context.annotation.Lazy
    private val streamTriggerRequestService: StreamTriggerRequestService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val redisOperation: RedisOperation,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamTriggerRequestRepoService::class.java)
    }

    // 通用不区分projectId，多流水线触发
    fun repoTriggerBuild(
        triggerPipelineList: List<StreamRepoHookEvent>,
        eventStr: String,
        actionCommonData: String,
        actionContext: String
    ): Boolean? {
        // 深拷贝转换，不影响主流程
        val action = eventActionFactory.loadByData(
            eventStr = eventStr,
            actionCommonData = objectMapper.readValue(actionCommonData),
            actionContext = objectMapper.readValue(actionContext),
            actionSetting = null
        )!!

        action.data.context.repoTrigger = RepoTrigger("", triggerPipelineList)

        logger.info(
            "StreamTriggerRequestRepoService|repoTriggerBuild" +
                "|requestEventId|${action.data.context.requestEventId}"
        )

        if (triggerPipelineList.isEmpty()) {
            logger.info("StreamTriggerRequestRepoService|repoTriggerBuild|pipeline list is empty ,skip it")
            return true
        }

        pipelineResourceDao.getPipelinesInIds(
            dslContext = dslContext,
            gitProjectId = null,
            pipelineIds = triggerPipelineList.map { it.pipelineId }
        ).map {
            StreamTriggerPipeline(
                gitProjectId = it.gitProjectId.toString(),
                pipelineId = it.pipelineId,
                filePath = it.filePath,
                displayName = it.displayName,
                enabled = it.enabled,
                creator = it.creator
            )
        }.forEach { gitProjectPipeline ->
            // 添加跨库触发相关数据
            action.data.context.pipeline = gitProjectPipeline
            exHandler.handle(action) {
                // 使用跨项目触发的action
                triggerPerPipeline(
                    StreamRepoTriggerAction(
                        baseAction = action,
                        client = client,
                        streamGitConfig = streamGitConfig,
                        streamBasicSettingService = streamBasicSettingService,
                        redisOperation = redisOperation,
                        streamTriggerCache = streamTriggerCache
                    )
                )
            }
        }

        return true
    }

    private fun triggerPerPipeline(
        action: BaseAction
    ): Boolean {
        logger.info(
            "StreamTriggerRequestRepoService|triggerPerPipeline" +
                "|requestEventId|${action.data.context.requestEventId}"
        )
        val pipeline = action.data.context.pipeline!!

        // 剔除不触发的情形
        streamSettingDao.getSetting(dslContext, pipeline.gitProjectId.toLong())?.let { setting ->
            action.data.setting = StreamTriggerSetting(setting)
            logger.info(
                "StreamTriggerRequestRepoService|triggerPerPipeline" +
                    "|requestEventId|${action.data.context.requestEventId}|action|${action.format()}"
            )
            try {
                CheckStreamSetting.checkGitProjectConf(action)
            } catch (triggerException: StreamTriggerException) {
                return false
            }
            val targetProjectInfo = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                gitProjectKey = pipeline.gitProjectId,
                action = action,
                getProjectInfo = action.api::getGitProjectInfo
            )

            // 这里把第一个访问工蜂项目的接口异常抓住,主要是为了兼容项目被删除之后触发异常.待删除项目闭环处理之后.可去除该限制
            if (targetProjectInfo == null) {
                logger.warn(
                    "StreamTriggerRequestRepoService|triggerPerPipeline" +
                        "|may be deleted, repo trigger error|project[${pipeline.gitProjectId}]"
                )
                return false
            }

            action.data.context.repoTrigger = action.data.context.repoTrigger!!.copy(
                branch = targetProjectInfo.defaultBranch!!
            )
            action.data.context.defaultBranch = action.data.context.repoTrigger!!.branch

            gitRequestRepoEventDao.saveGitRequest(
                dslContext,
                GitRequestRepoEvent(
                    eventId = action.data.context.requestEventId!!,
                    pipelineId = pipeline.pipelineId,
                    buildId = null,
                    targetGitProjectId = pipeline.gitProjectId.toLong(),
                    sourceGitProjectId = action.data.eventCommon.gitProjectId.toLong(),
                    createTime = null
                )
            )

            // 校验mr请求是否产生冲突
            if (!action.checkMrConflict(path2PipelineExists = mapOf(pipeline.filePath to pipeline))) {
                return false
            }
            return streamTriggerRequestService.matchAndTriggerPipeline(
                action = action,
                path2PipelineExists = mapOf(pipeline.filePath to pipeline)
            )
        } ?: return false
    }
}
