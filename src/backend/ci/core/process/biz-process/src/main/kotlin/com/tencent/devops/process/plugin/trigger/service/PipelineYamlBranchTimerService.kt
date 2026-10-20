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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_BRANCH_TIMER_DEFAULT_BRANCH
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_BRANCH_TIMER_EMPTY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_BRANCH_TIMER_MISMATCH
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_BRANCH_TIMER_WILDCARD
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileInfo
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 非默认分支 YAML 的 yaml-branch 定时。与发布版本定时分开登记，不覆盖主干 schedules。
 */
@Service
class PipelineYamlBranchTimerService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineTimerService: PipelineTimerService,
    private val timerTriggerTaskService: PipelineTimerTriggerTaskService
) {

    fun reconcile(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        branch: String,
        defaultBranch: String,
        repoHashId: String,
        filePath: String,
        channelCode: ChannelCode
    ): List<String> {
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        ) ?: return emptyList()
        val onDefaultBranch = branch == defaultBranch || resource.status != VersionStatus.BRANCH
        val reasons = mutableListOf<String>()
        val desired = linkedMapOf<String, TimerTriggerElement>()
        resource.model.getTriggerContainer().elements.filterIsInstance<TimerTriggerElement>().forEach { element ->
            val yamlBranch = element.yamlBranch?.trim().orEmpty()
            if (element.yamlBranch == null) {
                return@forEach
            }
            val reason = skipReason(
                yamlBranch = yamlBranch,
                fileBranch = branch,
                onDefaultBranch = onDefaultBranch
            )
            if (reason != null) {
                reasons += reason
                return@forEach
            }
            val elementId = element.id
            if (elementId.isNullOrBlank()) {
                logger.warn("skip yaml-branch timer without element id|$projectId|$pipelineId|$branch")
                return@forEach
            }
            val params = timerTriggerTaskService.getParams(resource.model)
            val crontabExpressions = timerTriggerTaskService.getCrontabExpressions(
                params = params,
                element = element
            )
            if (crontabExpressions.isEmpty()) {
                return@forEach
            }
            val repo = timerTriggerTaskService.getRepo(
                projectId = projectId,
                element = element,
                params = params,
                yamlFileInfo = PipelineYamlFileInfo(repoHashId = repoHashId, filePath = filePath)
            )
            val taskId = timerTaskId(yamlBranch, elementId)
            val startParam = (element.convertStartParams() ?: emptyMap()).toMutableMap()
            startParam[ELEMENT_ID_PARAM] = elementId
            val saved = pipelineTimerService.saveTimer(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                crontabExpressions = crontabExpressions,
                channelCode = channelCode,
                repoHashId = repo?.repoHashId,
                branchs = setOf(yamlBranch),
                noScm = element.noScm,
                taskId = taskId,
                startParam = startParam,
                yamlBranch = yamlBranch
            )
            if (saved.isNotOk()) {
                logger.warn(
                    "save yaml-branch timer fail|$projectId|$pipelineId|$branch|$taskId|${saved.message}"
                )
            } else {
                desired[elementId] = element
            }
        }
        pipelineTimerService.listYamlBranchTimers(
            projectId = projectId,
            pipelineId = pipelineId,
            yamlBranch = branch
        ).forEach { row ->
            val elementId = elementIdOf(row.startParam)
            if (elementId == null || !desired.containsKey(elementId)) {
                deleteOne(userId, projectId, pipelineId, row.taskId)
            }
        }
        if (reasons.isNotEmpty()) {
            logger.info("yaml-branch schedule skipped|$projectId|$pipelineId|$branch|$reasons")
        }
        return reasons
    }

    fun deleteBranch(userId: String, projectId: String, pipelineId: String, branch: String) {
        pipelineTimerService.listYamlBranchTimers(
            projectId = projectId,
            pipelineId = pipelineId,
            yamlBranch = branch
        ).forEach { deleteOne(userId, projectId, pipelineId, it.taskId) }
    }

    fun deletePipeline(userId: String, projectId: String, pipelineId: String) {
        pipelineTimerService.listYamlBranchTimers(
            projectId = projectId,
            pipelineId = pipelineId,
            yamlBranch = null
        ).forEach { deleteOne(userId, projectId, pipelineId, it.taskId) }
    }

    private fun deleteOne(userId: String, projectId: String, pipelineId: String, taskId: String) {
        pipelineTimerService.deleteTimer(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            taskId = taskId
        )
        pipelineTimerService.deleteTimerBranch(
            projectId = projectId,
            pipelineId = pipelineId,
            repoHashId = null,
            branch = null,
            taskId = taskId
        )
    }

    private fun skipReason(yamlBranch: String, fileBranch: String, onDefaultBranch: Boolean): String? {
        return when {
            yamlBranch.isBlank() -> message(BK_YAML_BRANCH_TIMER_EMPTY, "yaml-branch 为空，未登记定时")
            yamlBranch.contains('*') || yamlBranch.contains('?') -> message(
                BK_YAML_BRANCH_TIMER_WILDCARD,
                "yaml-branch 不支持通配，未登记定时：$yamlBranch",
                arrayOf(yamlBranch)
            )
            onDefaultBranch -> message(
                BK_YAML_BRANCH_TIMER_DEFAULT_BRANCH,
                "yaml-branch 写在默认分支上，未登记定时"
            )
            yamlBranch != fileBranch -> message(
                BK_YAML_BRANCH_TIMER_MISMATCH,
                "yaml-branch（$yamlBranch）与当前分支（$fileBranch）不一致，未登记定时",
                arrayOf(yamlBranch, fileBranch)
            )
            else -> null
        }
    }

    private fun message(code: String, defaultMessage: String, params: Array<String>? = null): String {
        return I18nUtil.getCodeLanMessage(
            messageCode = code,
            params = params,
            defaultMessage = defaultMessage
        )
    }

    companion object {
        const val ELEMENT_ID_PARAM = "__ciYamlBranchElementId"
        private val logger = LoggerFactory.getLogger(PipelineYamlBranchTimerService::class.java)

        fun timerTaskId(yamlBranch: String, elementId: String): String {
            return "y-" + DigestUtils.md5Hex("$yamlBranch:$elementId")
        }

        fun elementIdOf(startParamJson: String?): String? {
            if (startParamJson.isNullOrBlank()) {
                return null
            }
            return try {
                JsonUtil.to(startParamJson, object : TypeReference<Map<String, String>>() {})[ELEMENT_ID_PARAM]
            } catch (ignored: Exception) {
                null
            }
        }
    }
}
