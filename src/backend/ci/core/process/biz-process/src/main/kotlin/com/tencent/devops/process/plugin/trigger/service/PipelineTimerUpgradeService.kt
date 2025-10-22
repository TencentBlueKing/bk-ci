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

package com.tencent.devops.process.plugin.trigger.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

/**
 * 流水线定时服务
 * @version 1.0
 */
@Service
open class PipelineTimerUpgradeService @Autowired constructor(
    val pipelineRepositoryService: PipelineRepositoryService,
    val pipelineTimerService: PipelineTimerService,
    val timerTriggerTaskService: PipelineTimerTriggerTaskService
) {
    @SuppressWarnings("NestedBlockDepth", "CyclomaticComplexMethod")
    fun upgrade(userId: String, targetProjectId: String?, targetPipelineId: String?) {
        logger.info("upgrade pipeline timer|$targetProjectId|$targetPipelineId|$userId")
        var offset = 0
        val limit = 1000
        do {
            val records = pipelineTimerService.listPipeline(
                projectId = targetProjectId,
                pipelineId = targetPipelineId,
                offset = offset,
                limit = limit
            )
            if (records.isEmpty()) {
                logger.info("timer records is empty|$targetProjectId|$targetPipelineId")
                return
            }
            records.forEach parseModel@{ (projectId, pipelineId) ->
                val timerList = pipelineTimerService.listTimer(projectId, pipelineId)
                val model = pipelineRepositoryService.getModel(
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                if (model == null) {
                    val deleteCount = pipelineTimerService.cleanTimer(projectId, pipelineId)
                    logger.warn("model is null|deleted $deleteCount timer tasks|$projectId|$pipelineId")
                    return@parseModel
                }
                val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
                if (pipelineInfo == null) {
                    logger.warn("pipeline info is null|projectId=$projectId|pipelineId=$pipelineId")
                    return@parseModel
                }
                // 默认参数
                val params = timerTriggerTaskService.getParams(model)
                // 没有禁用的触发器
                val timerTriggerElements = getTimerTriggerConfig(model)
                val lastModifyUser = pipelineInfo.lastModifyUser
                when {
                    // 功能发布后，添加了多个定时触发器
                    timerTriggerElements.size > 1 -> {
                        val containsEmptyTaskId = timerList.any { it.taskId.isNullOrBlank() }
                        // 存在空的taskId，则删掉重新添加定时任务
                        if (containsEmptyTaskId) {
                            logger.info("contains empty taskId, save again|$projectId|$pipelineId")
                            timerList.forEach {
                                val result = pipelineTimerService.deleteTimer(
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    taskId = it.taskId,
                                    userId = lastModifyUser
                                )
                                logger.info("delete timer task|$projectId|$pipelineId")
                                if (result.data != true) {
                                    logger.error(
                                        "delete timer fail|projectId=$projectId|" +
                                                "pipelineId=$pipelineId|taskId=${it.taskId}"
                                    )
                                }
                            }
                        }
                        // 重新添加定时任务
                        saveTimer(
                            timerTriggerElements = timerTriggerElements,
                            params = params,
                            latestVersion = model.latestVersion,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            pipelineInfo = pipelineInfo
                        )
                    }

                    // 正常情况
                    timerList.size == 1 && timerTriggerElements.size == 1 -> {
                        val timerRecord = timerList[0]
                        val timerTriggerElement = timerTriggerElements[0]
                        val crontab = JsonUtil.toJson(
                            timerTriggerTaskService.getCrontabExpressions(
                                params = params,
                                element = timerTriggerElement
                            ),
                            false
                        )
                        // 填充taskId 和 startParam
                        if (timerRecord.taskId.isNullOrBlank()) {
                            // 更新定时任务
                            val result = pipelineTimerService.updateTimer(
                                projectId = projectId,
                                pipelineId = pipelineId,
                                taskId = timerTriggerElement.id ?: "",
                                userId = lastModifyUser,
                                startParam = timerTriggerElement.convertStartParams(),
                                crontabExpressionJson = crontab
                            ).data
                            logger.info("update timer task|$projectId|$pipelineId|${timerTriggerElement.id}|$result")
                        }
                    }

                    timerTriggerElements.isEmpty() -> {
                        // 没有定时触发器，清空定时任务
                        val deleteCount = pipelineTimerService.cleanTimer(projectId, pipelineId)
                        logger.info(
                            "timer trigger element is empty, deleted $deleteCount timer tasks" +
                                    "|$projectId|$pipelineId"
                        )
                    }

                    else -> {
                        logger.warn(
                            "skip upgrade|projectId=$projectId|pipelineId=$pipelineId|" +
                                    "timerCount[${timerList.size}]|timerTriggerCount[${timerTriggerElements.size}]"
                        )
                        return@parseModel
                    }
                }
            }
            val count = records.size
            offset += limit
        } while (count == 1000)
    }

    @SuppressWarnings("NestedBlockDepth", "CyclomaticComplexMethod")
    fun upgradeBranch(
        userId: String,
        targetProjectId: String?,
        targetPipelineId: String?
    ) {
        logger.info(
            "upgrade pipeline timer branch|$targetProjectId|$targetPipelineId|$userId"
        )
        var offset = 0
        val limit = 1000
        do {
            val records = pipelineTimerService.getTimerBranch(
                projectId = targetProjectId,
                pipelineId = targetPipelineId,
                offset = offset,
                limit = limit
            )
            if (records.isEmpty()) {
                logger.info("timer branch is empty|projectId=$targetProjectId|pipelineId=$targetPipelineId")
                return
            }
            records.forEach parseModel@{
                val (projectId, pipelineId) = it
                val model = pipelineRepositoryService.getModel(projectId, pipelineId)
                // 流水线不存在，清除数据
                if (model == null) {
                    val deleteCount = pipelineTimerService.deleteTimerBranch(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        repoHashId = null,
                        branch = null,
                        taskId = null
                    )
                    logger.warn("model is null|deleted $deleteCount|$projectId|$pipelineId")
                    return@parseModel
                }
                val timerTriggerConfig = getTimerTriggerConfig(model)
                // 没有触发器开启noScm，清除数据
                val noScmTimerList = timerTriggerConfig.filter { config -> config.noScm == true }
                if (noScmTimerList.isEmpty()) {
                    val deleteCount = pipelineTimerService.deleteTimerBranch(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        repoHashId = null,
                        branch = null,
                        taskId = null
                    )
                    logger.warn("noScm trigger is not exist|deleted $deleteCount|$projectId|$pipelineId")
                    return@parseModel
                }
                // 已保存的[定时触发版本信息]
                val timerBranchRecords = pipelineTimerService.getTimerBranch(
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                // 全部都是老数据，没有taskId
                val emptyTaskIdRecords = timerBranchRecords.filter { record -> record.taskId.isNullOrBlank() }
                val noRecordsHaveTaskId = emptyTaskIdRecords.size == timerBranchRecords.size
                when {
                    noScmTimerList.size == 1 && noRecordsHaveTaskId -> {
                        // 批量修改
                        val updateCount = pipelineTimerService.updateTimerBranch(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            sourceBranch = null,
                            sourceRepoHashId = null,
                            sourceTaskId = null,
                            targetTaskId = noScmTimerList.first().id ?: ""
                        )
                        logger.info("change timer branch|updated $updateCount|$projectId|$pipelineId")
                    }
                    // 一个触发器，配置了多条分支，其中有部分record存在taskId，仅需更新空taskId的数据即可
                    noScmTimerList.size == 1 -> {
                        emptyTaskIdRecords.forEach changeEmptyTaskId@{ record ->
                            val taskId = noScmTimerList.first().id ?: ""
                            val updateCount = try {
                                pipelineTimerService.updateTimerBranch(
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    sourceBranch = record.branch,
                                    sourceRepoHashId = record.repoHashId,
                                    sourceTaskId = "",
                                    targetTaskId = taskId
                                )
                            } catch (ignored: DuplicateKeyException) {
                                // 主键冲突，移除脏数据
                                val deleteCount = pipelineTimerService.deleteTimerBranch(
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    repoHashId = record.repoHashId,
                                    branch = record.branch,
                                    taskId = ""
                                )
                                logger.warn("duplicate key|deleted $deleteCount|$projectId|$pipelineId|" +
                                        "${record.branch}|${record.repoHashId}")
                                return@changeEmptyTaskId
                            } catch (ignored: Exception) {
                                logger.warn(
                                    "fail to update timer branch|$projectId|$pipelineId|$taskId" +
                                            "${record.branch}|${record.repoHashId}", ignored
                                )
                                return@changeEmptyTaskId
                            }
                            logger.info(
                                "change timer branch|updated $updateCount timer branch|$projectId|$pipelineId|" +
                                        "branch=${record.branch}|repoHashId=${record.repoHashId}"
                            )
                        }
                    }
                    // 流水线存在多个触发器，存量数据中存在taskId为空的脏数据
                    noScmTimerList.size > 1 && !noRecordsHaveTaskId -> {
                        val deleteCount = pipelineTimerService.deleteTimerBranch(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            repoHashId = null,
                            branch = null,
                            taskId = ""
                        )
                        logger.warn("clean empty taskId|deleted $deleteCount timer branch|$projectId|$pipelineId")
                    }

                    else -> {
                        logger.warn(
                            "skip upgrade timer branch|$projectId|$pipelineId|timerCount[${timerTriggerConfig.size}]|" +
                                    "timerBranchRecords[${timerBranchRecords.size}]|" +
                                    "emptyTaskIdRecords[${emptyTaskIdRecords.size}]"
                        )
                    }
                }
            }
            val count = records.size
            offset += limit
        } while (count == 1000)
    }

    private fun saveTimer(
        timerTriggerElements: List<TimerTriggerElement>,
        params: Map<String, String>,
        latestVersion: Int,
        projectId: String,
        pipelineId: String,
        pipelineInfo: PipelineInfo
    ) {
        timerTriggerElements.forEach { element ->
            val crontabExpressions = timerTriggerTaskService.getCrontabExpressions(
                params = params,
                element = element
            )
            val repo = timerTriggerTaskService.getRepo(
                projectId = projectId,
                pipelineId = pipelineId,
                element = element,
                params = params,
                latestVersion = latestVersion
            )
            pipelineTimerService.saveTimer(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = pipelineInfo.lastModifyUser,
                crontabExpressions = crontabExpressions,
                channelCode = pipelineInfo.channelCode,
                repoHashId = repo?.repoHashId,
                branchs = element.branches?.toSet(),
                noScm = element.noScm,
                taskId = element.id ?: "",
                startParam = element.convertStartParams()
            )
        }
    }

    /**
     * 获取取定时触发器
     */
    fun getTimerTriggerConfig(model: Model) = model.getTriggerContainer()
        .elements.filter { it is TimerTriggerElement && it.elementEnabled() }.map { it as TimerTriggerElement }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTimerUpgradeService::class.java)
    }
}
