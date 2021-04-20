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

package com.tencent.devops.gitci.v2.service

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.yaml.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.yaml.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.service.GitRepositoryConfService
import com.tencent.devops.gitci.service.trigger.RequestTriggerInterface
import com.tencent.devops.gitci.v2.listener.V2GitCIRequestDispatcher
import com.tencent.devops.gitci.pojo.V2GitCIRequestTriggerEvent
import com.tencent.devops.gitci.v2.utils.V2WebHookMatcher
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class V2RequestTrigger @Autowired constructor (
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCISettingDao,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val repositoryConfService: GitRepositoryConfService,
    private val rabbitTemplate: RabbitTemplate
) : RequestTriggerInterface<ScriptBuildYaml> {

    override fun triggerBuild(
        gitRequestEvent: GitRequestEvent,
        gitProjectPipeline: GitProjectPipeline,
        event: GitEvent,
        originYaml: String?,
        filePath: String
    ): Boolean {
        val yamlObject= prepareCIBuildYaml(gitRequestEvent, originYaml, filePath, gitProjectPipeline.pipelineId) ?: return false

        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        logger.info("normalize yaml: $normalizedYaml")

        // 若是Yaml格式没问题，则取Yaml中的流水线名称，并修改当前流水线名称
        gitProjectPipeline.displayName = if (!yamlObject.name.isNullOrBlank()) yamlObject.name!! else filePath.removeSuffix(".yml")

        if (isMatch(event, yamlObject)) {
            logger.info("Matcher is true, display the event, gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}, dispatched pipeline: $gitProjectPipeline")
            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml!!,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = gitRequestEvent.objectKind,
                description = gitRequestEvent.commitMsg,
                triggerUser = gitRequestEvent.userId,
                sourceGitProjectId = gitRequestEvent.sourceGitProjectId
            )
            V2GitCIRequestDispatcher.dispatch(rabbitTemplate,
                V2GitCIRequestTriggerEvent(
                    pipeline = gitProjectPipeline,
                    event = gitRequestEvent,
                    yaml = yamlObject,
                    originYaml = originYaml,
                    normalizedYaml = normalizedYaml,
                    gitBuildId = gitBuildId
                )
            )
            repositoryConfService.updateGitCISetting(gitRequestEvent.gitProjectId)
        } else {
            logger.warn("Matcher is false, return, gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}")
            gitRequestEventNotBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                pipelineId = if (gitProjectPipeline.pipelineId.isBlank()) null else gitProjectPipeline.pipelineId,
                filePath = gitProjectPipeline.filePath,
                originYaml = originYaml,
                normalizedYaml = normalizedYaml,
                reason = TriggerReason.TRIGGER_NOT_MATCH.name,
                reasonDetail = TriggerReason.TRIGGER_NOT_MATCH.detail,
                gitProjectId = gitRequestEvent.gitProjectId
            )
        }

        return true
    }


    override fun isMatch(event: GitEvent, ymlObject: ScriptBuildYaml): Boolean {
        return V2WebHookMatcher(event).isMatch(ymlObject.triggerOn!!)
    }

    override fun prepareCIBuildYaml(
        gitRequestEvent: GitRequestEvent,
        originYaml: String?,
        filePath: String?,
        pipelineId: String?
    ): ScriptBuildYaml? {
        if (originYaml.isNullOrBlank()) {
            return null
        }

        val yamlObject = try {
            createCIBuildYaml(originYaml, gitRequestEvent.gitProjectId)
        } catch (e: Throwable) {
            logger.error("git ci yaml is invalid", e)
            gitRequestEventNotBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                pipelineId = pipelineId,
                filePath = filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.GIT_CI_YAML_INVALID.name,
                reasonDetail = e.message.toString(),
                gitProjectId = gitRequestEvent.gitProjectId
            )
            return null
        }

        return yamlObject
    }

    fun createCIBuildYaml(yamlStr: String, gitProjectId: Long? = null): ScriptBuildYaml {
        logger.info("input yamlStr: $yamlStr")

        val yaml = ScriptYmlUtils.formatYaml(yamlStr)
        val preYamlObject = YamlUtil.getObjectMapper().readValue(yaml, PreScriptBuildYaml::class.java)

        return ScriptYmlUtils.normalizeGitCiYaml(preYamlObject)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(V2RequestTrigger::class.java)
    }
}
