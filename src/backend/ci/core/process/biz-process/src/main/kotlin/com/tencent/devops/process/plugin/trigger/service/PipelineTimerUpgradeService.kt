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

package com.tencent.devops.process.plugin.trigger.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
    fun upgrade(userId: String, targetProjectId: String?, targetPipelineId: String?) {
        logger.info("upgrade pipeline timer")
        var offset = 0
        val limit = 100
        do {
            val records = pipelineTimerService.listPipeline(
                projectId = targetProjectId,
                pipelineId = targetPipelineId,
                offset = offset,
                limit = limit
            )
            records.forEach parseModel@{ (projectId, pipelineId) ->
                val timerList = pipelineTimerService.listTimer(projectId, pipelineId)
                val model = pipelineRepositoryService.getModel(
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                if (model == null) {
                    logger.warn("model is null|projectId=$projectId|pipelineId=$pipelineId")
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
                            timerTriggerElements= timerTriggerElements,
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
                        if (timerRecord.taskId.isNullOrBlank() && timerRecord.crontab == crontab) {
                            // 更新定时任务
                            pipelineTimerService.updateTimer(
                                projectId = projectId,
                                pipelineId = pipelineId,
                                taskId = timerTriggerElement.id ?: "",
                                userId = lastModifyUser,
                                startParam = timerTriggerElement.startParams,
                                crontabExpressionJson = crontab
                            )
                        }
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
                startParam = element.startParams
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
