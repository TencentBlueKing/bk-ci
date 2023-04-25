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

package com.tencent.devops.prebuild.v2.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.pojo.StreamCommonVariables
import com.tencent.devops.prebuild.service.CommonPreBuildService
import com.tencent.devops.prebuild.v2.component.PreCIYAMLValidatorV2
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.yaml.modelCreate.ModelCreate
import com.tencent.devops.process.yaml.modelCreate.inner.ExtraParam
import com.tencent.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import com.tencent.devops.process.yaml.modelCreate.inner.PipelineInfo
import com.tencent.devops.process.yaml.modelCreate.inner.PreCIData
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v2.parsers.template.models.GetTemplateParam
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Service
class PreBuildV2Service @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val prebuildProjectDao: PrebuildProjectDao,
    private val modelCreate: ModelCreate,
    private val preCIYAMLValidatorV2: PreCIYAMLValidatorV2,
    private val redisOperation: RedisOperation
) : CommonPreBuildService(client, dslContext, prebuildProjectDao) {
    companion object {
        private val logger = LoggerFactory.getLogger(PreBuildV2Service::class.java)
        private const val TEMPLATE_DIR = ".ci/templates/"
        private const val VARIABLE_PREFIX = "variables."
        private const val REDIS_KEY_GIT_TOKEN_LOCK_PREFIX = "preci:git:token:lock:"
        private const val REDIS_KEY_GIT_TOKEN_PREFIX = "preci:git:token:"
        private const val REDIS_KEY_CREATE_PIPELINE_LOCK_PREFIX = "preci:create:pipeline:lock"
        private val GIT_TOKEN_EXPIRE_TIME = TimeUnit.HOURS.toSeconds(7)
    }

    /**
     * 检验yaml合法性
     *
     * @param originYaml 源yaml
     * @return web api result
     */
    fun checkYamlSchema(originYaml: String): Result<String> {
        return try {
            preCIYAMLValidatorV2.check(originYaml, null, true)
            Result("OK")
        } catch (e: Throwable) {
            logger.error("Check yaml schema failed.", e)
            Result(1, "Invalid yaml: ${e.message}")
        }
    }

    /**
     * 生成流水线并执行
     *
     * @param userId 用户Id
     * @param pipelineName 流水线名称
     * @param startUpReq 启动构建相关参数
     * @param agentInfo agent相关信息
     * @return 构建Id
     */
    fun startBuild(
        userId: String,
        pipelineName: String,
        startUpReq: StartUpReq,
        agentInfo: ThirdPartyAgentStaticInfo
    ): BuildId {
        // 1.校验yaml合法性
        try {
            preCIYAMLValidatorV2.check(startUpReq.yaml, null, true)
        } catch (e: Throwable) {
            throw CustomException(Response.Status.BAD_REQUEST, "Invalid yaml: ${e.message}")
        }

        // 2.标准化处理
        val scriptBuildYaml = ScriptYmlUtils.normalizePreCiYaml(
            YamlTemplate(
                filePath = "",
                yamlObject = YamlUtil.getObjectMapper()
                        .readValue(startUpReq.yaml, PreTemplateScriptBuildYaml::class.java),
                extraParameters = null,
                getTemplateMethod = ::getTemplate,
                nowRepo = null,
                repo = null,
                resourcePoolMapExt = null
            ).replace()
        )

        // 若流水线不存在，则创建空流水线，服务于红线质量
        var pipelineId = createPipelineIfNoExist(userId, pipelineName)
        val preCIData = PreCIData(
            agentId = agentInfo.agentId,
            workspace = startUpReq.workspace,
            userId = userId,
            extraParam = ExtraParam(
                codeccScanPath = startUpReq.extraParam?.codeccScanPath,
                incrementFileList = startUpReq.extraParam?.incrementFileList,
                ideVersion = startUpReq.extraParam?.ideVersion,
                pluginVersion = startUpReq.extraParam?.pluginVersion
            )
        )
        val projectId = getUserProjectId(userId)
        val modelCreateEvent = ModelCreateEvent(
            userId = userId,
            projectCode = projectId,
            pipelineInfo = PipelineInfo(pipelineId),
            preCIData = preCIData,
            gitData = null,
            streamData = null,
            changeSet = null,
            jobTemplateAcrossInfo = null
        )

        // 3.生成流水线编排
        val pipelineModel = modelCreate.createPipelineModel(
            modelName = pipelineName,
            event = modelCreateEvent,
            yaml = scriptBuildYaml,
            pipelineParams = getPipelineParams(scriptBuildYaml.variables, userId, pipelineName),
            asCodeSettings = PipelineAsCodeSettings(true)
        ).model

        // 若是本机构建，需后置填充调度信息
        pipelineModel.stages.forEach { stage ->
            stage.containers.forEach { container ->
                if (container is VMBuildContainer && container.dispatchType is ThirdPartyAgentIDDispatchType) {
                    val dispatchType: ThirdPartyAgentIDDispatchType =
                        (container.dispatchType as ThirdPartyAgentIDDispatchType)
                    dispatchType.workspace = startUpReq.workspace
                    dispatchType.displayName = agentInfo.agentId
                }
            }
        }

        // 4.存储
        updatePipeline(userId, pipelineName, startUpReq, pipelineModel, pipelineId)

        // 5.启动流水线
        val startupResp =
            client.get(ServiceBuildResource::class).manualStartup(userId, projectId, pipelineId, mapOf(), channelCode)

        if (startupResp.isNotOk() || startupResp.data == null) {
            logger.error("Failed to start pipeline: $pipelineId, remote message: ${startupResp.message}")
            throw RemoteServiceException("Failed to start pipeline")
        }

        return BuildId(startupResp.data!!.id)
    }

    private fun createPipelineIfNoExist(
        userId: String,
        pipelineName: String
    ): String {
        var pipelineId = getPipelineId(userId, pipelineName)
        if (!pipelineId.isNullOrBlank()) {
            return pipelineId
        }

        val keyForLock = "$REDIS_KEY_CREATE_PIPELINE_LOCK_PREFIX:$userId:$pipelineName"
        val redisLock = RedisLock(redisOperation, keyForLock, 5)
        redisLock.use {
            redisLock.lock()

            pipelineId = getPipelineId(userId, pipelineName)
            if (!pipelineId.isNullOrBlank()) {
                return pipelineId!!
            }

            return createEmptyPipeline(userId, pipelineName)
        }
    }

    private fun getTemplate(
        param: GetTemplateParam<Any?>
    ): String {
        if (param.targetRepo == null) {
            throw CustomException(Response.Status.BAD_REQUEST, "PreCI仅支持远程模板")
        }

        if (param.targetRepo?.repository.isNullOrBlank() || param.targetRepo?.name.isNullOrBlank()
        ) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "远程仓关键字不能为空: repository, name"
            )
        }

        val content = getTemplateCntFromGit(
            personalToken = param.targetRepo?.credentials?.personalAccessToken,
            gitProjectId = param.targetRepo?.repository!!,
            ref = param.targetRepo?.ref,
            fileName = TEMPLATE_DIR + param.path
        )

        val templateRelPath = TEMPLATE_DIR + param.path
        try {
            preCIYAMLValidatorV2.check(content, param.templateType, false)
        } catch (e: Throwable) {
            logger.error("template yaml check fail, [$param.templateType?.text|$templateRelPath|$e.message")
            throw CustomException(Response.Status.BAD_REQUEST, "template yaml check fail: ${e.message}")
        }

        return ScriptYmlUtils.formatYaml(content)
    }

    private fun getTemplateCntFromGit(
        personalToken: String?,
        gitProjectId: String,
        fileName: String,
        ref: String?
    ): String {
        logger.info("getTemplateCntFromGit: [$gitProjectId|$fileName|$ref]")
        try {
            var useAccessToken = false
            var token = personalToken
            if (token.isNullOrBlank()) {
                token = getGitAccessToken(gitProjectId)
                useAccessToken = true
            }

            return client.getScm(ServiceGitCiResource::class).getGitCIFileContent(
                gitProjectId = gitProjectId,
                filePath = fileName,
                token = token,
                ref = getTriggerBranch(ref, token, useAccessToken, gitProjectId),
                useAccessToken = useAccessToken
            ).data!!
        } catch (e: Throwable) {
            logger.error("get yaml template error from git, $gitProjectId", e)
            throw CustomException(Response.Status.BAD_REQUEST, "get yaml template error from git")
        }
    }

    private fun getTriggerBranch(
        branch: String?,
        token: String,
        useAccessToken: Boolean,
        gitProjectId: String
    ): String {
        return when {
            branch != null && branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch != null && branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else ->
                branch ?: getGitProjectDefaultBranch(token, useAccessToken, gitProjectId) ?: "master"
        }
    }

    /**
     * 获取超级token
     */
    private fun getGitAccessToken(gitProjectId: String): String {
        val keyForVal = REDIS_KEY_GIT_TOKEN_PREFIX + gitProjectId
        var token = redisOperation.get(keyForVal)

        if (!token.isNullOrBlank()) {
            return token
        }

        val keyForLock = REDIS_KEY_GIT_TOKEN_LOCK_PREFIX + gitProjectId
        val redisLock = RedisLock(redisOperation, keyForLock, 5)
        redisLock.use {
            redisLock.lock()

            token = redisOperation.get(keyForVal)
            if (!token.isNullOrBlank()) {
                return token!!
            }

            val newToken = client.getScm(ServiceGitCiResource::class).getToken(gitProjectId).data!!.accessToken
            logger.info("PRECI|getToken|gitProjectId=$gitProjectId|newToken=$newToken")
            redisOperation.set(keyForVal, newToken, GIT_TOKEN_EXPIRE_TIME)

            return newToken
        }
    }

    /**
     * 获取流水线参数
     */
    private fun getPipelineParams(
        variables: Map<String, Variable>?,
        userId: String,
        pipelineName: String
    ): List<BuildFormProperty> {
        if (variables.isNullOrEmpty()) {
            return emptyList()
        }

        val retList = mutableListOf<BuildFormProperty>()
        val startParams = mutableMapOf<String, String>()
        startParams[StreamCommonVariables.CI_PIPELINE_NAME] = pipelineName
        startParams[StreamCommonVariables.CI_ACTOR] = userId
        startParams[StreamCommonVariables.CI_BRANCH] = "PRECI_VIRTUAL_BRANCH"

        variables.forEach { (key, variable) ->
            startParams[VARIABLE_PREFIX + key] =
                variable.copy(value = formatVariablesValue(variable.value, startParams)).value ?: ""
        }

        startParams.forEach {
            val property = BuildFormProperty(
                id = it.key,
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = it.value,
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            )
            retList.add(property)
        }

        return retList
    }

    /**
     * 格式化构建参数
     */
    private fun formatVariablesValue(
        varValue: String?,
        startParams: MutableMap<String, String>
    ): String? {
        if (varValue.isNullOrEmpty()) {
            return ""
        }

        val settingMap = mutableMapOf<String, String>().apply {
            putAll(startParams)
        }

        return ScriptYmlUtils.parseVariableValue(varValue, settingMap)
    }

    /**
     * 获取git项目设定的默认分支
     */
    private fun getGitProjectDefaultBranch(
        token: String,
        useAccessToken: Boolean,
        gitProjectId: String
    ): String? {
        return client.getScm(ServiceGitCiResource::class).getProjectInfo(
            accessToken = token,
            useAccessToken = useAccessToken,
            gitProjectId = gitProjectId
        ).data?.defaultBranch
    }
}
