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

package com.tencent.devops.process.plugin.trigger.element

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TIMER_TRIGGER_SVN_BRANCH_NOT_EMPTY
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerTriggerTaskService
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import org.slf4j.LoggerFactory

@ElementBiz
class TimerTriggerElementBizPlugin constructor(
    private val pipelineTimerService: PipelineTimerService,
    private val timerTriggerTaskService: PipelineTimerTriggerTaskService,
    private val pipelineRepositoryService: PipelineRepositoryService
) : ElementBizPlugin<TimerTriggerElement> {

    override fun elementClass(): Class<TimerTriggerElement> {
        return TimerTriggerElement::class.java
    }

    override fun check(element: TimerTriggerElement, appearedCnt: Int) = Unit

    override fun afterCreate(
        element: TimerTriggerElement,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean,
        container: Container,
        yamlInfo: PipelineYamlVo?
    ) {
        val params = pipelineRepositoryService.getTriggerParams((container as TriggerContainer))
        logger.info("[$pipelineId]|$userId| Timer trigger [${element.name}] enable=${element.elementEnabled()}")
        val crontabExpressions = timerTriggerTaskService.getCrontabExpressions(
            params = params,
            element = element
        )
        val repo = timerTriggerTaskService.getRepo(
            projectId = projectId,
            element = element,
            params = params,
            yamlInfo = yamlInfo
        )
        // svn仓库分支必填
        if (repo != null && repo.getScmType() == ScmType.CODE_SVN && element.branches.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_TIMER_TRIGGER_SVN_BRANCH_NOT_EMPTY
            )
        }
        // 删除旧的定时器任务, taskId未补充完整时, 保存流水线可能生成重复的定时任务
        pipelineTimerService.deleteTimer(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            taskId = ""
        )
        // 参数使用变量时，解析变量
        val finalBranches = element.branches
                ?.filter { it.isNotBlank() }
                ?.map { EnvUtils.parseEnv(it, params) }
                ?.toSet()

        if (crontabExpressions.isNotEmpty()) {
            val result = pipelineTimerService.saveTimer(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                crontabExpressions = crontabExpressions,
                channelCode = channelCode,
                repoHashId = repo?.repoHashId,
                branchs = finalBranches,
                noScm = element.noScm,
                taskId = element.id ?: "",
                startParam = element.convertStartParams()
            )
            logger.info("[$pipelineId]|$userId| Update pipeline timer|crontab=$crontabExpressions")
            if (result.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ILLEGAL_TIMER_CRONTAB
                )
            }
        } else {
            pipelineTimerService.deleteTimer(projectId, pipelineId, userId, element.id ?: "")
            logger.info("[$pipelineId]|$userId| Delete pipeline timer")
        }
    }

    override fun beforeDelete(element: TimerTriggerElement, param: BeforeDeleteParam) {
        if (param.pipelineId.isNotBlank()) {
            with(param) {
                val taskId = element.id ?: ""
                // 直接删除定时触发器，需兼容旧数据（taskId为空）
                pipelineTimerService.deleteTimer(projectId, pipelineId, userId, "")
                pipelineTimerService.deleteTimer(projectId, pipelineId, userId, taskId).let {
                    logger.info("beforeDelete|$pipelineId|delete [${element.id}] timer|result=$it")
                }
                pipelineTimerService.deleteTimerBranch(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repoHashId = null,
                    branch = null,
                    taskId = taskId
                ).let {
                    logger.info("$pipelineId|delete [${element.id}] timer branch|deleteCount=$it")
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimerTriggerElementBizPlugin::class.java)
    }
}
