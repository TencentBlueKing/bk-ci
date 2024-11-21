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

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.plugin.trigger.util.CronExpressionUtils
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.utils.PIPELINE_TIMER_DISABLE
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import org.quartz.CronExpression
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线定时服务
 * @version 1.0
 */
@Service
open class PipelineTimerTriggerTaskService @Autowired constructor(
    private val client: Client,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService
) {
    fun getParams(model: Model) = model.getTriggerContainer()
        .params.associate { it.id to it.defaultValue.toString() }

    fun getCrontabExpressions(params: Map<String, String>, element: TimerTriggerElement): Set<String> {
        val crontabExpressions = mutableSetOf<String>()
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
        return crontabExpressions
    }

    fun getRepo(
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

    fun getRepo(
        projectId: String,
        pipelineId: String,
        element: TimerTriggerElement,
        params: Map<String, String>,
        latestVersion: Int
    ): Repository? {
        val pipelineYamlVo = pipelineYamlFacadeService.getPipelineYamlInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            version = latestVersion
        )
        return getRepo(
            projectId = projectId,
            element = element,
            params = params,
            yamlInfo = pipelineYamlVo
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTimerTriggerTaskService::class.java)
    }
}
