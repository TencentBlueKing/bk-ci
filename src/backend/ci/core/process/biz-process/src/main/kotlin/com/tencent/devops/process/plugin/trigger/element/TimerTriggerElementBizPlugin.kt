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

package com.tencent.devops.process.plugin.trigger.element

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementCheckResult
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TIMER_TRIGGER_SVN_BRANCH_NOT_EMPTY
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.plugin.trigger.util.CronExpressionUtils
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.utils.PIPELINE_TIMER_DISABLE
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import org.quartz.CronExpression
import org.slf4j.LoggerFactory

@ElementBiz
class TimerTriggerElementBizPlugin constructor(
    private val pipelineTimerService: PipelineTimerService,
    private val client: Client
) : ElementBizPlugin<TimerTriggerElement> {

    override fun elementClass(): Class<TimerTriggerElement> {
        return TimerTriggerElement::class.java
    }

    override fun check(
        projectId: String?,
        userId: String,
        stage: Stage,
        container: Container,
        element: TimerTriggerElement,
        contextMap: Map<String, String>,
        appearedCnt: Int,
        isTemplate: Boolean,
        oauthUser: String?,
        pipelineId: String
    ) = ElementCheckResult(true)

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
        val crontabExpressions = mutableSetOf<String>()
        val params = (container as TriggerContainer).params.associate { it.id to it.defaultValue.toString() }
        logger.info("[$pipelineId]|$userId| Timer trigger [${element.name}] enable=${element.elementEnabled()}")
        /*
          在模板实例化时,有的流水线需要开启定时任务,有的流水线不需要开启,支持通过流水线变量控制定时任务的开启
          通过参数禁用定时任务,在流水线参数上配置BK_CI_TIMER_DISABLE,禁用定时触发器插件
         */
        val isParamDisable = params[PIPELINE_TIMER_DISABLE]?.toBoolean() ?: false
        if (element.elementEnabled() && !isParamDisable) {

            val eConvertExpressions = element.convertExpressions(params = params)
            if (eConvertExpressions.isEmpty()) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ILLEGAL_TIMER_CRONTAB
                )
            }
            eConvertExpressions.forEach { cron ->
                if (!CronExpression.isValidExpression(cron)) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ILLEGAL_TIMER_CRONTAB,
                        params = arrayOf(cron)
                    )
                }
                if (!CronExpressionUtils.isValidTimeInterval(cron)) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ILLEGAL_TIMER_INTERVAL_CRONTAB,
                        params = arrayOf(cron)
                    )
                }
                crontabExpressions.add(cron)
            }
        }
        val repo = getRepo(projectId = projectId, element = element, params = params, yamlInfo = yamlInfo)
        // svn仓库分支必填
        if (repo != null && repo.getScmType() == ScmType.CODE_SVN && element.branches.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_TIMER_TRIGGER_SVN_BRANCH_NOT_EMPTY
            )
        }
        if (crontabExpressions.isNotEmpty()) {
            val result = pipelineTimerService.saveTimer(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                crontabExpressions = crontabExpressions,
                channelCode = channelCode,
                repoHashId = repo?.repoHashId,
                branchs = element.branches?.toSet(),
                noScm = element.noScm
            )
            logger.info("[$pipelineId]|$userId| Update pipeline timer|crontab=$crontabExpressions")
            if (result.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ILLEGAL_TIMER_CRONTAB
                )
            }
        } else {
            pipelineTimerService.deleteTimer(projectId, pipelineId, userId)
            logger.info("[$pipelineId]|$userId| Delete pipeline timer")
        }
    }

    override fun beforeDelete(element: TimerTriggerElement, param: BeforeDeleteParam) {
        if (param.pipelineId.isNotBlank()) {
            pipelineTimerService.deleteTimer(param.projectId, param.pipelineId, param.userId)
            pipelineTimerService.deleteTimerBranch(projectId = param.projectId, pipelineId = param.pipelineId)
        }
    }

    private fun getRepo(
        projectId: String,
        element: TimerTriggerElement,
        params: Map<String, String>,
        yamlInfo: PipelineYamlVo?
    ): Repository? {
        return when {
            element.repositoryType == TriggerRepositoryType.SELF -> {
                if (yamlInfo == null || yamlInfo.repoHashId.isBlank()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TIMER_TRIGGER_NEED_ENABLE_PAC
                    )
                }
                try {
                    client.get(ServiceRepositoryResource::class).get(
                        projectId = projectId,
                        repositoryId = yamlInfo.repoHashId,
                        repositoryType = RepositoryType.ID
                    ).data
                } catch (ignored: NotFoundException) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TIMER_TRIGGER_REPO_NOT_FOUND
                    )
                }
            }

            !element.repoHashId.isNullOrBlank() || !element.repoName.isNullOrBlank() -> {
                val repositoryConfig = with(element) {
                    RepositoryConfigUtils.getRepositoryConfig(
                        repoHashId = repoHashId,
                        repoName = repoName,
                        repoType = TriggerRepositoryType.toRepositoryType(repositoryType),
                        variables = params
                    )
                }
                try {
                    client.get(ServiceRepositoryResource::class).get(
                        projectId = projectId,
                        repositoryId = repositoryConfig.getURLEncodeRepositoryId(),
                        repositoryType = repositoryConfig.repositoryType
                    ).data
                } catch (ignored: NotFoundException) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TIMER_TRIGGER_REPO_NOT_FOUND
                    )
                }
            }
            else -> null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimerTriggerElementBizPlugin::class.java)
    }
}
