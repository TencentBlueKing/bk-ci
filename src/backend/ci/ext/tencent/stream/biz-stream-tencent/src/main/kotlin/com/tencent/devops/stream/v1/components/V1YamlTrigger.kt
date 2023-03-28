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

package com.tencent.devops.stream.v1.components

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.stream.constant.StreamMessageCode.GIT_CI_NO_RECOR
import com.tencent.devops.stream.constant.StreamMessageCode.MIRROR_VERSION_NOT_AVAILABLE
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.v1.dao.V1GitCIServicesConfDao
import com.tencent.devops.stream.v1.dao.V1GitCISettingDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.pojo.V1EnvironmentVariables
import com.tencent.devops.stream.v1.pojo.V1GitRequestEventForHandle
import com.tencent.devops.stream.v1.pojo.V1StreamTriggerContext
import com.tencent.devops.stream.v1.service.V1GitCIEventService
import com.tencent.devops.stream.v1.service.V1GitRepositoryConfService
import com.tencent.devops.stream.v1.utils.V1GitCIWebHookMatcher
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.StringReader
import javax.ws.rs.core.Response

@Component
class V1YamlTrigger @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitCISettingDao: V1GitCISettingDao,
    private val gitServicesConfDao: V1GitCIServicesConfDao,
    private val gitRequestEventBuildDao: V1GitRequestEventBuildDao,
    private val repositoryConfService: V1GitRepositoryConfService,
    private val gitCIEventSaveService: V1GitCIEventService,
    private val yamlBuild: V1YamlBuild
) {

    fun triggerBuild(
        context: V1StreamTriggerContext
    ): Boolean {
        // TODO: 暂时先全部展开，后续函数全替换为上下文参数即可去掉
        val (event, gitRequestEventForHandle, _, gitProjectPipeline, originYaml, _) = context

        val yamlObject = prepareCIBuildYaml(
            gitRequestEventForHandle = gitRequestEventForHandle,
            isMr = (event is GitMergeRequestEvent),
            originYaml = originYaml,
            filePath = gitProjectPipeline.filePath,
            pipelineId = gitProjectPipeline.pipelineId,
            pipelineName = gitProjectPipeline.displayName
        ) ?: return false

        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        logger.info("normalize yaml: $normalizedYaml")

        // 若是Yaml格式没问题，则取Yaml中的流水线名称，并修改当前流水线名称
        if (!gitRequestEventForHandle.checkRepoTrigger) {
            gitProjectPipeline.displayName = if (!yamlObject.name.isNullOrBlank()) {
                yamlObject.name!!
            } else {
                gitProjectPipeline.filePath.removeSuffix(
                    ".yml"
                )
            }
        }

        if (isMatch(event, gitRequestEventForHandle, yamlObject).first) {
            logger.info(
                "Matcher is true, display the event, gitProjectId: ${gitRequestEventForHandle.gitProjectId}, " +
                    "eventId: ${gitRequestEventForHandle.id}, dispatched pipeline: $gitProjectPipeline"
            )
            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEventForHandle.id!!,
                originYaml = originYaml,
                parsedYaml = originYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEventForHandle.gitProjectId,
                branch = gitRequestEventForHandle.branch,
                objectKind = gitRequestEventForHandle.gitRequestEvent.objectKind,
                commitMsg = gitRequestEventForHandle.gitRequestEvent.commitMsg,
                triggerUser = gitRequestEventForHandle.userId,
                sourceGitProjectId = gitRequestEventForHandle.gitRequestEvent.sourceGitProjectId,
                buildStatus = BuildStatus.RUNNING,
                version = null
            )
            try {
                yamlBuild.gitStartBuild(
                    pipeline = gitProjectPipeline,
                    event = gitRequestEventForHandle.gitRequestEvent,
                    yaml = yamlObject,
                    gitBuildId = gitBuildId
                )
            } catch (e: Throwable) {
                logger.warn("Fail to start the git ci build($gitRequestEventForHandle)", e)
            }
            if (!gitRequestEventForHandle.checkRepoTrigger) {
                repositoryConfService.updateGitCISetting(gitRequestEventForHandle.gitProjectId)
            }
        } else {
            logger.warn(
                "Matcher is false, return, gitProjectId: ${gitRequestEventForHandle.gitProjectId}, " +
                    "eventId: ${gitRequestEventForHandle.id}"
            )
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEventForHandle.userId,
                eventId = gitRequestEventForHandle.id!!,
                pipelineId = gitProjectPipeline.pipelineId.ifBlank { null },
                pipelineName = gitProjectPipeline.displayName,
                filePath = gitProjectPipeline.filePath,
                originYaml = originYaml,
                normalizedYaml = normalizedYaml,
                reason = TriggerReason.TRIGGER_NOT_MATCH.name,
                reasonDetail = TriggerReason.TRIGGER_NOT_MATCH.detail,
                gitProjectId = gitRequestEventForHandle.gitProjectId,
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = null,
                branch = gitRequestEventForHandle.branch
            )
        }

        return true
    }

    fun isMatch(
        event: GitEvent,
        gitRequestEventForHandle: V1GitRequestEventForHandle,
        ymlObject: CIBuildYaml
    ): Pair<Boolean, Boolean> {
        val matcher = V1GitCIWebHookMatcher(event)
        return Pair(matcher.isMatch(ymlObject.trigger!!, ymlObject.mr!!), false)
    }

    fun prepareCIBuildYaml(
        gitRequestEventForHandle: V1GitRequestEventForHandle,
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
            createCIBuildYaml(originYaml, gitRequestEventForHandle.gitProjectId)
        } catch (e: Throwable) {
            logger.warn("git ci yaml is invalid", e)
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEventForHandle.userId,
                eventId = gitRequestEventForHandle.id!!,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                filePath = filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_INVALID.name,
                reasonDetail = TriggerReason.CI_YAML_INVALID.detail.format(e.message.toString()),
                gitProjectId = gitRequestEventForHandle.gitProjectId,
                // V1不发送通知
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = null,
                branch = gitRequestEventForHandle.branch
            )
            return null
        }

        return yamlObject
    }

    fun checkYamlSchema(userId: String, yaml: String): Result<String> {
        return Result("OK")
    }

    fun createCIBuildYaml(yamlStr: String, gitProjectId: Long? = null): CIBuildYaml {
        logger.info("input yamlStr: $yamlStr")

        var yaml = CiYamlUtils.formatYaml(yamlStr)
        yaml = replaceEnv(yaml, gitProjectId)
        val yamlObject = YamlUtil.getObjectMapper().readValue(yaml, CIBuildYaml::class.java)

        // 检测services镜像
        if (yamlObject.services != null) {
            yamlObject.services!!.forEachIndexed { _, it ->
                // 判断镜像格式是否合法
                val (imageName, imageTag) = it.parseImage()
                val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
                    ?: throw CustomException(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        MessageUtil.getMessageByLocale(
                            messageCode = GIT_CI_NO_RECOR,
                            language = I18nUtil.getLanguage()
                        ) + ". ${it.image}"
                    )
                if (!record.enable) {
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, MessageUtil.getMessageByLocale(
                        messageCode = MIRROR_VERSION_NOT_AVAILABLE,
                        language = I18nUtil.getLanguage()
                    ) + ". ${it.image}")
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

    private fun getEnvValue(env: List<V1EnvironmentVariables>, key: String): String? {
        env.forEach {
            if (it.name == key) {
                return it.value
            }
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(V1YamlTrigger::class.java)
    }
}
