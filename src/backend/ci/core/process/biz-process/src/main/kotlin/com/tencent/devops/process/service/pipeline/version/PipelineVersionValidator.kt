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

package com.tencent.devops.process.service.pipeline.version

import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.exception.CustomMessageException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.TimerNodeType
import com.tencent.devops.common.pipeline.utils.ModelVarRefValidator
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_MAX_PIPELINE_COUNT_PER_PROJECT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_MODEL_VAR_REF_INVALID
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.service.pipeline.RuntimeResourceResolver
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineVersionValidator @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val projectCacheService: ProjectCacheService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val runtimeResourceResolver: RuntimeResourceResolver
) {

    fun validate(context: PipelineVersionCreateContext) {
        with(context) {
            validateBasicInfo()
            validateModelBasicInfo()
            validatePermission()
            validateSetting()
            validateRuntimeResource()
        }
    }

    /**
     * 发布前校验运行资源与定时触发选中的运行节点确实可用。
     *
     * 节点的可用性会随环境变动而失效：环境被清空或删除、节点被移出环境、单体资源的机器被释放。
     * 发布时不拦，问题要等到下一次定时触发才暴露，而那时只有一条触发失败记录，编排的人未必看得到。
     * 只在发布时校验，草稿保存是编写过程中的中间状态，不该被尚未定稿的节点配置挡住。
     *
     * 只在有确凿证据时拦截：查询本身失败（环境服务异常等）一律放行，不能因为问不到就挡住发布。
     */
    private fun PipelineVersionCreateContext.validateRuntimeResource() {
        if (versionAction != PipelineVersionAction.RELEASE_DRAFT) return
        if (pipelineBasicInfo.channelCode != ChannelCode.CREATIVE_STREAM) return
        val resource = pipelineSettingWithoutVersion.runtimeResource() ?: return
        // 节点列表按使用权限过滤，口径要和定时触发一致：用流水线授权人，没有授权人时才退回操作人
        val authUserId = pipelineInfo?.let {
            pipelineRepositoryService.getPipelineOauthUser(projectId, pipelineId)
        } ?: userId
        if (!resource.group) {
            // 单体资源：机器被释放后换不出 agent，发布出去也只会在派发阶段失败
            val agentHashId = runtimeResourceResolver.resolveAgentHashId(
                projectId = projectId,
                userId = authUserId,
                resource = resource
            )
            if (agentHashId.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_RUNTIME_RESOURCE_NODE_UNRESOLVED,
                    params = arrayOf(resource.resourceName ?: resource.resourceId ?: "")
                )
            }
            return
        }
        val envHashId = resource.envHashId?.takeIf { it.isNotBlank() } ?: return
        val envAgentIds = runtimeResourceResolver.listEnvAgentHashIdsOrNull(
            projectId = projectId,
            userId = authUserId,
            envHashId = envHashId
        ) ?: return
        val envName = resource.resourceName
            ?: runtimeResourceResolver.getEnvName(projectId, authUserId, envHashId)
            ?: envHashId
        if (envAgentIds.isEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_RUNTIME_RESOURCE_ENV_NO_NODE,
                params = arrayOf(envName)
            )
        }
        // 定时触发勾选的节点必须还在环境里，否则到点触发会逐个失败
        val invalidAgentId = pipelineResourceWithoutVersion.model.getTriggerContainer().elements
            .filterIsInstance<TimerTriggerElement>()
            .filter { it.elementEnabled() && it.nodeType == TimerNodeType.NODE_LIST }
            .flatMap { it.nodes.orEmpty() }
            .distinct()
            .firstOrNull { it.isNotBlank() && !envAgentIds.contains(it) }
            ?: return
        throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TIMER_TRIGGER_NODE_NOT_IN_ENV,
            params = arrayOf(
                runtimeResourceResolver.getNodeName(projectId, authUserId, invalidAgentId) ?: invalidAgentId,
                envName
            )
        )
    }

    fun PipelineVersionCreateContext.validateBasicInfo() {
        if (pipelineBasicInfo.pipelineName.isBlank()) {
            logger.warn("The pipeline name is empty")
            throw CustomMessageException("The pipeline name cannot be empty.")
        }
        val model = pipelineResourceWithoutVersion.model
        // 检查model中表达式是否合法
        val invalidRefs = ModelVarRefValidator.getInvalidRefs(model, projectId)
        if (invalidRefs.isNotEmpty()) {
            val invalidRefsMsg = ModelVarRefValidator.formatInvalidRefsMessage(invalidRefs)
            throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_MODEL_VAR_REF_INVALID,
                params = arrayOf(invalidRefsMsg)
            )
        }
        val nameExist = pipelineRepositoryService.isPipelineExist(
            projectId = projectId,
            pipelineName = pipelineBasicInfo.pipelineName,
            channelCode = pipelineBasicInfo.channelCode,
            excludePipelineId = pipelineId
        )
        if (nameExist) {
            logger.warn("The pipeline(${pipelineBasicInfo.pipelineName}) is exist")
            throw ErrorCodeException(
                statusCode = Response.Status.CONFLICT.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
            )
        }
        val existedPipeline = pipelineInfo
        if (existedPipeline == null) {
            // 检查用户流水线是否达到上限
            val projectVO = projectCacheService.getProject(projectId)
            // codecc等无需注册鉴权的渠道不受项目流水线数量上限约束
            if (ChannelCode.isNeedAuth(pipelineBasicInfo.channelCode) && projectVO?.pipelineLimit != null) {
                val preCount =
                    pipelineRepositoryService.countByProjectIds(setOf(projectId), pipelineBasicInfo.channelCode)
                if (preCount >= projectVO.pipelineLimit!!) {
                    throw OperationException(
                        MessageUtil.getMessageByLocale(
                            ERROR_MAX_PIPELINE_COUNT_PER_PROJECT,
                            I18nUtil.getLanguage(userId),
                            arrayOf("${projectVO.pipelineLimit}")
                        )
                    )
                }
            }
        } else {
            val existedChannelCode = existedPipeline.channelCode
            if (existedChannelCode != pipelineBasicInfo.channelCode) {
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                    params = arrayOf(existedChannelCode.name)
                )
            }
        }
    }

    fun PipelineVersionCreateContext.validateModelBasicInfo() {
        if (pipelineInfo != null) {
            val model = pipelineResourceWithoutVersion.model
            // 只在更新操作时检查stage数量不为1
            if (model.stages.size <= 1) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_WITH_EMPTY_STAGE, params = arrayOf()
            )
        }
    }

    fun PipelineVersionCreateContext.validatePermission() {
        if (!checkPermission) return
        val language = I18nUtil.getLanguage(userId)
        if (pipelineInfo == null) {
            val permission = AuthPermission.CREATE
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = "*",
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE, language, arrayOf(
                    userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), "*"
                )
                )
            )
        } else {
            val permission = AuthPermission.EDIT
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE, I18nUtil.getLanguage(userId), arrayOf(
                    userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), pipelineId
                )
                )
            )
        }
    }

    fun PipelineVersionCreateContext.validateSetting() {
        pipelineSettingWithoutVersion.fixSubscriptions()
        modelCheckPlugin.checkSettingIntegrity(pipelineSettingWithoutVersion, projectId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVersionValidator::class.java)
    }
}
