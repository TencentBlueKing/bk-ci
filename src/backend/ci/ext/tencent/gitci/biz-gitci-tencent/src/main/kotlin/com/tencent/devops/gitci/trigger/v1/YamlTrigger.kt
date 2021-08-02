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

package com.tencent.devops.gitci.trigger.v1

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.pojo.EnvironmentVariables
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.service.GitRepositoryConfService
import com.tencent.devops.gitci.trigger.YamlTriggerInterface
import com.tencent.devops.gitci.utils.GitCIWebHookMatcher
import com.tencent.devops.gitci.trigger.GitCIEventService
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.StringReader
import javax.ws.rs.core.Response

@Component
class YamlTrigger @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCISettingDao,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val repositoryConfService: GitRepositoryConfService,
    private val gitCIEventSaveService: GitCIEventService,
    private val yamlBuild: YamlBuild
) : YamlTriggerInterface<CIBuildYaml> {

    override fun triggerBuild(
        gitToken: GitToken,
        forkGitToken: GitToken?,
        gitRequestEvent: GitRequestEvent,
        gitProjectPipeline: GitProjectPipeline,
        event: GitEvent,
        originYaml: String?,
        filePath: String
    ): Boolean {
        val yamlObject = prepareCIBuildYaml(
            gitToken = gitToken,
            forkGitToken = forkGitToken,
            gitRequestEvent = gitRequestEvent,
            isMr = (event is GitMergeRequestEvent),
            originYaml = originYaml,
            filePath = filePath,
            pipelineId = gitProjectPipeline.pipelineId,
            pipelineName = gitProjectPipeline.displayName
        ) ?: return false

        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        logger.info("normalize yaml: $normalizedYaml")

        // 若是Yaml格式没问题，则取Yaml中的流水线名称，并修改当前流水线名称
        gitProjectPipeline.displayName =
            if (!yamlObject.name.isNullOrBlank()) yamlObject.name!! else filePath.removeSuffix(".yml")

        if (isMatch(event, gitRequestEvent, yamlObject).first) {
            logger.info("Matcher is true, display the event, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                "eventId: ${gitRequestEvent.id}, dispatched pipeline: $gitProjectPipeline")
            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml!!,
                parsedYaml = originYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = gitRequestEvent.objectKind,
                description = gitRequestEvent.commitMsg,
                triggerUser = gitRequestEvent.userId,
                sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
                buildStatus = BuildStatus.RUNNING,
                version = null
            )
            try {
                yamlBuild.gitStartBuild(
                    pipeline = gitProjectPipeline,
                    event = gitRequestEvent,
                    yaml = yamlObject,
                    gitBuildId = gitBuildId
                )
            } catch (e: Throwable) {
                logger.error("Fail to start the git ci build($gitRequestEvent)", e)
            }
            repositoryConfService.updateGitCISetting(gitRequestEvent.gitProjectId)
        } else {
            logger.warn("Matcher is false, return, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                "eventId: ${gitRequestEvent.id}")
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = if (gitProjectPipeline.pipelineId.isBlank()) null else gitProjectPipeline.pipelineId,
                pipelineName = gitProjectPipeline.displayName,
                filePath = gitProjectPipeline.filePath,
                originYaml = originYaml,
                normalizedYaml = normalizedYaml,
                reason = TriggerReason.TRIGGER_NOT_MATCH.name,
                reasonDetail = TriggerReason.TRIGGER_NOT_MATCH.detail,
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = null
            )
        }

        return true
    }

    override fun isMatch(
        event: GitEvent,
        gitRequestEvent: GitRequestEvent,
        ymlObject: CIBuildYaml
    ): Pair<Boolean, Boolean> {
        val matcher = GitCIWebHookMatcher(event)
        return Pair(matcher.isMatch(ymlObject.trigger!!, ymlObject.mr!!), false)
    }

    override fun prepareCIBuildYaml(
        gitToken: GitToken,
        forkGitToken: GitToken?,
        gitRequestEvent: GitRequestEvent,
        isMr: Boolean,
        originYaml: String?,
        filePath: String,
        pipelineId: String?,
        pipelineName: String?
    ): CIBuildYaml? {

        if (originYaml.isNullOrBlank()) {
            return null
        }

        val yamlObject = try {
            createCIBuildYaml(originYaml, gitRequestEvent.gitProjectId)
        } catch (e: Throwable) {
            logger.error("git ci yaml is invalid", e)
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                filePath = filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_INVALID.name,
                reasonDetail = TriggerReason.CI_YAML_INVALID.detail.format(e.message.toString()),
                gitProjectId = gitRequestEvent.gitProjectId,
                // V1不发送通知
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = null
            )
            return null
        }

        return yamlObject
    }

    override fun checkYamlSchema(userId: String, yaml: String): Result<String> {
        return Result("OK")
    }

    fun createCIBuildYaml(yamlStr: String, gitProjectId: Long? = null): CIBuildYaml {
        logger.info("input yamlStr: $yamlStr")

        var yaml = CiYamlUtils.formatYaml(yamlStr)
        yaml = replaceEnv(yaml, gitProjectId)
        val yamlObject = YamlUtil.getObjectMapper().readValue(yaml, CIBuildYaml::class.java)

        // 检测services镜像
        if (yamlObject.services != null) {
            yamlObject.services!!.forEachIndexed { index, it ->
                // 判断镜像格式是否合法
                val (imageName, imageTag) = it.parseImage()
                val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
                    ?: throw CustomException(Response.Status.INTERNAL_SERVER_ERROR,
                        "Git CI没有此镜像版本记录. ${it.image}")
                if (!record.enable) {
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "镜像版本不可用. ${it.image}")
                }
            }
        }

        return CiYamlUtils.normalizeGitCiYaml(yamlObject)
    }

    private fun replaceEnv(yaml: String, gitProjectId: Long?): String {
        if (gitProjectId == null) {
            return yaml
        }
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: return yaml
        logger.info("gitProjectConf: $gitProjectConf")
        if (null == gitProjectConf.env) {
            return yaml
        }

        val sb = StringBuilder()
        val br = BufferedReader(StringReader(yaml))
        val envRegex = Regex("\\\$env:\\w+")
        var line: String? = br.readLine()
        while (line != null) {
            val envMatches = envRegex.find(line)
            envMatches?.groupValues?.forEach {
                logger.info("envKeyPrefix: $it")
                val envKeyPrefix = it
                val envKey = envKeyPrefix.removePrefix("\$env:")
                val envValue = getEnvValue(gitProjectConf.env!!, envKey)
                logger.info("envKey: $envKey, envValue: $envValue")
                line = if (null != envValue) {
                    envRegex.replace(line!!, envValue)
                } else {
                    envRegex.replace(line!!, "null")
                }
                logger.info("line: $line")
            }

            sb.append(line).append("\n")
            line = br.readLine()
        }

        return sb.toString()
    }

    private fun getEnvValue(env: List<EnvironmentVariables>, key: String): String? {
        env.forEach {
            if (it.name == key) {
                return it.value
            }
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(YamlTrigger::class.java)
    }
}
