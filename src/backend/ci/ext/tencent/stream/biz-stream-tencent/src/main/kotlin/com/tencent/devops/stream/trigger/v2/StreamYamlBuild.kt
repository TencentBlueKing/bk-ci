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

package com.tencent.devops.stream.trigger.v2

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.QualityRulesException
import com.tencent.devops.stream.common.exception.TriggerBaseException
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.pojo.GitCITriggerLock
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.utils.GitCIPipelineUtils
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.stream.config.StreamStorageBean
import com.tencent.devops.stream.service.GitCIPipelineService
import com.tencent.devops.stream.trigger.parsers.modelCreate.ModelCreate
import com.tencent.devops.stream.trigger.timer.pojo.StreamTimer
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamYamlBuild @Autowired constructor(
    private val streamYamlBaseBuild: StreamYamlBaseBuild,
    private val dslContext: DSLContext,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val pipelineService: GitCIPipelineService,
    private val redisOperation: RedisOperation,
    private val modelCreate: ModelCreate,
    private val streamStorageBean: StreamStorageBean,
    private val streamTimerService: StreamTimerService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlBuild::class.java)
        private const val ymlVersion = "v2.0"
        const val VARIABLE_PREFIX = "variables."
        private val channelCode = ChannelCode.GIT
    }

    @Throws(TriggerBaseException::class, ErrorCodeException::class)
    @SuppressWarnings("LongParameterList")
    fun gitStartBuild(
        pipeline: GitProjectPipeline,
        event: GitRequestEvent,
        yaml: ScriptBuildYaml,
        originYaml: String,
        parsedYaml: String,
        normalizedYaml: String,
        gitBuildId: Long?,
        onlySavePipeline: Boolean,
        isTimeTrigger: Boolean,
        gitProjectInfo: GitCIProjectInfo? = null,
        changeSet: Set<String>? = null,
        params: Map<String, String> = mapOf()
    ): BuildId? {
        val start = LocalDateTime.now().timestampmilli()
        // pipelineId可能为blank所以使用filePath为key
        val triggerLock = GitCITriggerLock(
            redisOperation = redisOperation,
            gitProjectId = event.gitProjectId,
            filePath = pipeline.filePath
        )
        try {
            triggerLock.lock()
            val gitBasicSetting = streamBasicSettingDao.getSetting(dslContext, event.gitProjectId)!!
            // 避免出现多个触发拿到空的pipelineId后依次进来创建，所以需要在锁后重新获取pipeline
            val realPipeline = pipelineService.getPipelineByFile(
                event.gitProjectId,
                pipeline.filePath
            ) ?: pipeline
            // 优先创建流水线为了绑定红线
            if (realPipeline.pipelineId.isBlank()) {
                streamYamlBaseBuild.savePipeline(
                    pipeline = realPipeline,
                    event = event,
                    gitCIBasicSetting = gitBasicSetting,
                    model = createTriggerModel(gitBasicSetting)
                )
            }

            // 改名时保存需要修改名称
            realPipeline.displayName = pipeline.displayName

            // 如果是定时触发需要注册事件
            if (isTimeTrigger) {
                streamTimerService.saveTimer(
                    StreamTimer(
                        projectId = GitCIPipelineUtils.genGitProjectCode(event.gitProjectId),
                        pipelineId = realPipeline.pipelineId,
                        userId = event.userId,
                        crontabExpressions = listOf(yaml.triggerOn?.schedules?.cron.toString()),
                        gitProjectId = event.gitProjectId,
                        // 未填写则在每次触发拉默认分支
                        branchs = yaml.triggerOn?.schedules?.branches?.ifEmpty {
                            listOf(gitProjectInfo?.defaultBranch!!)
                        } ?: listOf(gitProjectInfo?.defaultBranch!!),
                        always = yaml.triggerOn?.schedules?.always ?: false,
                        channelCode = channelCode,
                        eventId = event.id!!,
                        originYaml = originYaml
                    )
                )
            }

            return if (gitBuildId != null) {
                startBuildPipeline(
                    pipeline = realPipeline,
                    event = event,
                    yaml = yaml,
                    gitBuildId = gitBuildId,
                    changeSet = changeSet,
                    gitBasicSetting = gitBasicSetting,
                    params = params
                )
            } else if (onlySavePipeline) {
                savePipeline(
                    pipeline = realPipeline,
                    event = event,
                    yaml = yaml,
                    changeSet = changeSet,
                    gitBasicSetting = gitBasicSetting
                )
                null
            } else {
                null
            }
        } catch (e: Throwable) {
            logger.warn("Fail to start the git ci build($event)", e)
            val (block, message, reason) = when (e) {
                is JsonProcessingException, is ParamBlankException, is CustomException -> {
                    Triple(
                        (event.objectKind == TGitObjectKind.MERGE_REQUEST.value),
                        e.message,
                        TriggerReason.PIPELINE_PREPARE_ERROR
                    )
                }
                is QualityRulesException -> {
                    Triple(
                        false,
                        e.message,
                        TriggerReason.CREATE_QUALITY_RULRS_ERROR
                    )
                }
                // 指定异常直接扔出在外面统一处理
                is TriggerBaseException, is ErrorCodeException -> {
                    throw e
                }
                else -> {
                    logger.error("event: ${event.id} unknow error: ${e.message}")
                    Triple(false, e.message, TriggerReason.UNKNOWN_ERROR)
                }
            }
            TriggerException.triggerError(
                request = event,
                pipeline = pipeline,
                reason = reason,
                reasonParams = listOf(message ?: ""),
                yamls = Yamls(originYaml, parsedYaml, normalizedYaml),
                version = ymlVersion,
                commitCheck = CommitCheck(
                    block = block,
                    state = GitCICommitCheckState.FAILURE
                )
            )
        } finally {
            streamStorageBean.buildTime(LocalDateTime.now().timestampmilli() - start)
            triggerLock.unlock()
        }
    }

    private fun createTriggerModel(gitBasicSetting: GitCIBasicSetting) = Model(
        name = GitCIPipelineUtils.genBKPipelineName(gitBasicSetting.gitProjectId),
        desc = "",
        stages = listOf(
            Stage(
                id = VMUtils.genStageId(1),
                name = VMUtils.genStageId(1),
                containers = listOf(
                    TriggerContainer(
                        id = "0",
                        name = "构建触发",
                        elements = listOf(
                            ManualTriggerElement(
                                name = "手动触发",
                                id = "T-1-1-1"
                            )
                        )
                    )
                )
            )
        )
    )

    @SuppressWarnings("LongParameterList")
    private fun startBuildPipeline(
        pipeline: GitProjectPipeline,
        event: GitRequestEvent,
        yaml: ScriptBuildYaml,
        gitBuildId: Long,
        gitBasicSetting: GitCIBasicSetting,
        changeSet: Set<String>? = null,
        params: Map<String, String> = mapOf()
    ): BuildId? {
        logger.info("Git request gitBuildId:$gitBuildId, pipeline:$pipeline, event: $event, yaml: $yaml")

        // create or refresh pipeline
        val model = modelCreate.createPipelineModel(
            event = event,
            gitBasicSetting = gitBasicSetting,
            yaml = yaml,
            pipeline = pipeline,
            changeSet = changeSet,
            webhookParams = params
        )
        logger.info("startBuildPipeline gitBuildId:$gitBuildId, pipeline:$pipeline, model: $model")

        streamYamlBaseBuild.savePipeline(pipeline, event, gitBasicSetting, model)
        return streamYamlBaseBuild.startBuild(
            pipeline = pipeline,
            event = event,
            gitCIBasicSetting = gitBasicSetting,
            model = model,
            gitBuildId = gitBuildId
        )
    }

    private fun savePipeline(
        pipeline: GitProjectPipeline,
        event: GitRequestEvent,
        yaml: ScriptBuildYaml,
        changeSet: Set<String>? = null,
        gitBasicSetting: GitCIBasicSetting
    ) {
        val model = modelCreate.createPipelineModel(
            event = event,
            gitBasicSetting = gitBasicSetting,
            yaml = yaml,
            changeSet = changeSet,
            pipeline = pipeline
        )
        logger.info("savePipeline pipeline:$pipeline, model: $model")
        streamYamlBaseBuild.savePipeline(pipeline, event, gitBasicSetting, model)
    }
}
