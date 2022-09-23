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

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.JsonParseException
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.PipelineAlreadyExistException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelUpdate
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ILLEGAL_PIPELINE_MODEL_JSON
import com.tencent.devops.process.constant.ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION
import com.tencent.devops.process.engine.compatibility.BuildPropertyCompatibilityTools
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.jmx.pipeline.PipelineBean
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.DeletePipelineResult
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.store.api.template.ServiceTemplateResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Suppress("ALL")
@Service
class PipelineInfoFacadeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val projectCacheService: ProjectCacheService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val stageTagService: StageTagService,
    private val templateService: TemplateService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineBean: PipelineBean,
    private val processJmxApi: ProcessJmxApi,
    private val client: Client,
    private val pipelineInfoDao: PipelineInfoDao,
    private val redisOperation: RedisOperation
) {

    @Value("\${process.deletedPipelineStoreDays:30}")
    private val deletedPipelineStoreDays: Int = 30

    // pipeline对应的channel为静态数据, 基本不会变
    private val pipelineChannelCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String/*pipelineId*/, ChannelCode>()

    fun exportPipeline(userId: String, projectId: String, pipelineId: String): Response {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT,
            message = "用户($userId)无权限在工程($projectId)下导出流水线"
        )

        val settingInfo = pipelineRepositoryService.getSetting(projectId, pipelineId)
            ?: throw OperationException(MessageCodeUtil.getCodeLanMessage(ILLEGAL_PIPELINE_MODEL_JSON))
        val model = pipelineRepositoryService.getModel(projectId, pipelineId)
            ?: throw OperationException(MessageCodeUtil.getCodeLanMessage(ILLEGAL_PIPELINE_MODEL_JSON))

        val modelAndSetting = PipelineModelAndSetting(model = model, setting = settingInfo)
        logger.info("exportPipeline |$pipelineId | $projectId| $userId")
        return exportModelToFile(modelAndSetting, settingInfo.pipelineName)
    }

    fun uploadPipeline(userId: String, projectId: String, pipelineModelAndSetting: PipelineModelAndSetting): String {
        val permissionCheck = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        if (!permissionCheck) {
            logger.warn("$userId|$projectId uploadPipeline permission check fail")
            throw PermissionForbiddenException(
                MessageCodeUtil.getCodeMessage(USER_NEED_PIPELINE_X_PERMISSION, arrayOf(AuthPermission.CREATE.value))
            )
        }
        val model = pipelineModelAndSetting.model
        modelCheckPlugin.clearUpModel(model)
        if (model.srcTemplateId.isNullOrBlank()) {
            val validateRet = client.get(ServiceTemplateResource::class)
                .validateModelComponentVisibleDept(
                    userId = userId,
                    model = model,
                    projectCode = projectId
                )
            if (validateRet.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = validateRet.status.toString(),
                    defaultMessage = validateRet.message
                )
            }
        }
        val newPipelineId = createPipeline(
            userId = userId,
            projectId = projectId,
            model = model,
            channelCode = ChannelCode.BS,
            checkPermission = true
        )

        val newSetting = pipelineSettingFacadeService.rebuildSetting(
            oldSetting = pipelineModelAndSetting.setting,
            projectId = projectId,
            newPipelineId = newPipelineId,
            pipelineName = model.name
        )
        // setting pipeline需替换成新流水线的
        pipelineSettingFacadeService.saveSetting(
            userId = userId,
            setting = newSetting,
            checkPermission = true,
            dispatchPipelineUpdateEvent = false
        )
        return newPipelineId
    }

    private fun exportModelToFile(modelAndSetting: PipelineModelAndSetting, pipelineName: String): Response {
        // 流式下载
        val fileStream = StreamingOutput { output ->
            val sb = StringBuilder()
            sb.append(JsonUtil.toJson(modelAndSetting))
            output.write(sb.toString().toByteArray())
            output.flush()
        }
        val fileName = URLEncoder.encode("$pipelineName.json", "UTF-8")
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $fileName")
            .header("Cache-Control", "no-cache")
            .build()
    }

    fun getPipelineNameVersion(projectId: String, pipelineId: String): Pair<String, Int> {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        return Pair(pipelineInfo?.pipelineName ?: "", pipelineInfo?.version ?: 0)
    }

    fun createPipeline(
        userId: String,
        projectId: String,
        model: Model,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        fixPipelineId: String? = null,
        instanceType: String? = PipelineInstanceTypeEnum.FREEDOM.type,
        buildNo: BuildNo? = null,
        param: List<BuildFormProperty>? = null,
        fixTemplateVersion: Long? = null,
        useTemplateSettings: Boolean? = false
    ): String {
        val watcher =
            Watcher(id = "createPipeline|$projectId|$userId|$channelCode|$checkPermission|$instanceType|$fixPipelineId")
        var success = false
        try {

            if (checkPermission) {
                watcher.start("perm_v_perm")
                pipelinePermissionService.validPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = "*",
                    permission = AuthPermission.CREATE,
                    message = "用户($userId)无权限在工程($projectId)下创建流水线"
                )
                watcher.stop()
            }

            if (isPipelineExist(
                    projectId = projectId,
                    pipelineId = fixPipelineId,
                    name = model.name,
                    channelCode = channelCode
                )
            ) {
                logger.warn("The pipeline(${model.name}) is exist")
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS,
                    defaultMessage = "流水线名称已被使用"
                )
            }

            val templateId = model.templateId
            if (templateId != null) {
                // 如果是根据模板创建的流水线需为model设置srcTemplateId
                model.srcTemplateId = templateDao.getSrcTemplateId(
                    dslContext = dslContext,
                    projectId = projectId,
                    templateId = templateId,
                    type = TemplateType.CONSTRAINT.name
                )
            }

            // 检查用户是否有插件的使用权限
            if (model.srcTemplateId != null) {
                watcher.start("store_template_perm")
                val validateRet = client.get(ServiceTemplateResource::class)
                    .validateUserTemplateComponentVisibleDept(
                        userId = userId,
                        templateCode = model.srcTemplateId as String,
                        projectCode = projectId
                    )
                if (validateRet.isNotOk()) {
                    throw OperationException(validateRet.message ?: "模版下存在无权限的插件")
                }
                watcher.stop()
            }

            watcher.start("project_v_pipeline")
            // 检查用户流水线是否达到上限
            val projectVO = projectCacheService.getProject(projectId)
            if (projectVO?.pipelineLimit != null) {
                val preCount = pipelineRepositoryService.countByProjectIds(setOf(projectId), ChannelCode.BS)
                if (preCount >= projectVO.pipelineLimit!!) {
                    throw OperationException("该项目最多只能创建${projectVO.pipelineLimit}条流水线")
                }
            }
            watcher.stop()

            var pipelineId: String? = null
            try {
                val instance = if (instanceType == PipelineInstanceTypeEnum.FREEDOM.type) {
                    // 将模版常量变更实例化为流水线变量
                    val triggerContainer = model.stages[0].containers[0] as TriggerContainer
                    PipelineUtils.instanceModel(
                        templateModel = model,
                        pipelineName = model.name,
                        buildNo = triggerContainer.buildNo,
                        param = triggerContainer.params,
                        instanceFromTemplate = false,
                        labels = model.labels,
                        defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
                    )
                } else {
                    model
                }
                watcher.start("deployPipeline")
                pipelineId = pipelineRepositoryService.deployPipeline(
                    model = instance,
                    projectId = projectId,
                    signPipelineId = fixPipelineId,
                    userId = userId,
                    channelCode = channelCode,
                    create = true,
                    useTemplateSettings = useTemplateSettings,
                    templateId = model.templateId
                ).pipelineId
                watcher.stop()

                // 先进行模板关联操作
                if (templateId != null) {
                    watcher.start("addLabel")
                    if (useTemplateSettings == true) {
                        val groups = pipelineGroupService.getGroups(userId, projectId, templateId)
                        val labels = ArrayList<String>()
                        groups.forEach {
                            labels.addAll(it.labels)
                        }
                        pipelineGroupService.updatePipelineLabel(
                            userId = userId,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            labelIds = labels
                        )
                    }
                    watcher.stop()
                    watcher.start("createTemplate")
                    templateService.createRelationBtwTemplate(
                        userId = userId,
                        projectId = projectId,
                        templateId = templateId,
                        pipelineId = pipelineId,
                        instanceType = instanceType!!,
                        buildNo = buildNo,
                        param = param,
                        fixTemplateVersion = fixTemplateVersion
                    )
                    watcher.stop()
                }

                // 模板关联操作成功后再创建流水线相关资源
                if (checkPermission) {
                    watcher.start("perm_c_perm")
                    try {
                        pipelinePermissionService.createResource(
                            userId = userId,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            pipelineName = model.name
                        )
                    } catch (ignored: Throwable) {
                        if (fixPipelineId != pipelineId) {
                            throw ignored
                        }
                    }
                    watcher.stop()
                }
                pipelineGroupService.addPipelineLabel(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    labelIds = model.labels
                )

                success = true
                return pipelineId
            } catch (duplicateKeyException: DuplicateKeyException) {
                logger.info("duplicateKeyException: ${duplicateKeyException.message}")
                if (pipelineId != null) {
                    pipelineRepositoryService.deletePipeline(projectId, pipelineId, userId, channelCode, true)
                }
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_IS_EXISTS,
                    defaultMessage = "流水线已经存在或未找到对应模板"
                )
            } catch (ignored: Throwable) {
                if (pipelineId != null) {
                    pipelineRepositoryService.deletePipeline(projectId, pipelineId, userId, channelCode, true)
                }
                throw ignored
            } finally {
                if (!success) {
                    val beforeDeleteParam = BeforeDeleteParam(
                        userId = userId, projectId = projectId, pipelineId = pipelineId ?: "", channelCode = channelCode
                    )
                    modelCheckPlugin.beforeDeleteElementInExistsModel(
                        existModel = model,
                        sourceModel = null,
                        param = beforeDeleteParam
                    )
                }
            }
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
            pipelineBean.create(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_CREATE, watcher.totalTimeMillis)
        }
    }

    /**
     * 还原已经删除的流水线
     */
    fun restorePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ) {
        val watcher = Watcher(id = "restorePipeline|$pipelineId|$userId")
        try {
            watcher.start("isProjectManager")
            // 判断用户是否为项目管理员
            if (!pipelinePermissionService.checkProjectManager(userId, projectId)) {
                val defaultMessage = "管理员"
                val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${BkAuthGroup.MANAGER.value}",
                    defaultMessage = defaultMessage
                )
                throw ErrorCodeException(
                    statusCode = Response.Status.FORBIDDEN.statusCode,
                    errorCode = USER_NEED_PIPELINE_X_PERMISSION,
                    defaultMessage = defaultMessage,
                    params = arrayOf(permissionMsg)
                )
            }

            watcher.start("restorePipeline")
            val model = pipelineRepositoryService.restorePipeline(
                projectId = projectId, pipelineId = pipelineId, userId = userId,
                channelCode = channelCode, days = deletedPipelineStoreDays.toLong()
            )
            watcher.start("createResource")
            pipelinePermissionService.createResource(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = model.name
            )
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    /**
     * Copy the exist pipeline
     */
    fun copyPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        name: String,
        desc: String?,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): String {

        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在"
            )

        logger.info("Start to copy the pipeline $pipelineId")
        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT,
                message = "用户无流水线编辑权限"
            )
//            pipelinePermissionService.validPipelinePermission(
//                userId = userId,
//                projectId = projectId,
//                pipelineId = "*",
//                permission = AuthPermission.CREATE,
//                message = "用户($userId)无权限在工程($projectId)下创建流水线"
//            )
            if (!pipelinePermissionService.checkPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    permission = AuthPermission.CREATE
                )
            ) {
                throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下创建流水线")
            }
        }

        if (pipeline.channelCode != channelCode) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                defaultMessage = "指定要复制的流水线渠道来源${pipeline.channelCode}不符合$channelCode",
                params = arrayOf(pipeline.channelCode.name)
            )
        }

        val model = pipelineRepositoryService.getModel(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS,
                defaultMessage = "指定要复制的流水线-模型不存在"
            )
        try {
            val copyMode = Model(name, desc ?: model.desc, model.stages)
            modelCheckPlugin.clearUpModel(copyMode)
            val newPipelineId = createPipeline(userId, projectId, copyMode, channelCode)
            val settingInfo = pipelineSettingFacadeService.getSettingInfo(projectId, pipelineId)
            if (settingInfo != null) {
                // setting pipeline需替换成新流水线的
                val newSetting = pipelineSettingFacadeService.rebuildSetting(
                    oldSetting = settingInfo,
                    projectId = projectId,
                    newPipelineId = newPipelineId,
                    pipelineName = name
                )
                // 复制setting到新流水线
                pipelineSettingFacadeService.saveSetting(
                    userId = userId,
                    setting = newSetting,
                    dispatchPipelineUpdateEvent = false
                )
            }
            return newPipelineId
        } catch (e: JsonParseException) {
            logger.error("Parse process($pipelineId) fail", e)
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ILLEGAL_PIPELINE_MODEL_JSON,
                defaultMessage = "非法的流水线"
            )
        } catch (e: PipelineAlreadyExistException) {
            throw ErrorCodeException(
                statusCode = Response.Status.CONFLICT.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS,
                defaultMessage = "流水线名称已被使用"
            )
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", e)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.OPERATE_PIPELINE_FAIL,
                defaultMessage = "非法的流水线",
                params = arrayOf(e.message ?: "")
            )
        }
    }

    fun editPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        checkTemplate: Boolean = true,
        updateLastModifyUser: Boolean? = true
    ): DeployPipelineResult {
        if (checkTemplate && templateService.isTemplatePipeline(projectId, pipelineId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_TEMPLATE_CAN_NOT_EDIT,
                defaultMessage = "模板流水线不支持编辑"
            )
        }
        val apiStartEpoch = System.currentTimeMillis()
        var success = false

        try {
            if (checkPermission) {
                pipelinePermissionService.validPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = AuthPermission.EDIT,
                    message = "用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)"
                )
            }

            if (isPipelineExist(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    name = model.name,
                    channelCode = channelCode
                )
            ) {
                logger.warn("The pipeline(${model.name}) is exist")
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS,
                    defaultMessage = "流水线名称已被使用"
                )
            }

            val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                    defaultMessage = "流水线不存在"
                )

            if (pipeline.channelCode != channelCode) {
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                    defaultMessage = "指定要复制的流水线渠道来源${pipeline.channelCode}不符合$channelCode",
                    params = arrayOf(pipeline.channelCode.name)
                )
            }

            val existModel = pipelineRepositoryService.getModel(projectId, pipelineId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS,
                    defaultMessage = "指定要复制的流水线-模型不存在"
                )
            // 对已经存在的模型做处理
            val param = BeforeDeleteParam(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode
            )
            modelCheckPlugin.beforeDeleteElementInExistsModel(existModel, model, param)
            val deployResult = pipelineRepositoryService.deployPipeline(
                model = model,
                projectId = projectId,
                signPipelineId = pipelineId,
                userId = userId,
                channelCode = channelCode,
                create = false,
                updateLastModifyUser = updateLastModifyUser
            )
            if (checkPermission) {
                pipelinePermissionService.modifyResource(projectId, pipelineId, model.name)
            }
            success = true
            return deployResult
        } finally {
            pipelineBean.edit(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_EDIT, System.currentTimeMillis() - apiStartEpoch)
            logger.info("EDIT_PIPELINE|$pipelineId|$channelCode|p=$checkPermission|u=$userId")
        }
    }

    fun renamePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        name: String,
        channelCode: ChannelCode
    ) {
        val setting = pipelineSettingFacadeService.userGetSetting(userId, projectId, pipelineId, channelCode)
        setting.pipelineName = name
        pipelineSettingFacadeService.saveSetting(
            userId = userId,
            setting = setting,
            checkPermission = true,
            dispatchPipelineUpdateEvent = true
        )
    }

    fun saveAll(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        setting: PipelineSetting,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        checkTemplate: Boolean = true
    ): DeployPipelineResult {
        val pipelineResult = editPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = model,
            channelCode = channelCode,
            checkPermission = checkPermission,
            checkTemplate = checkTemplate
        )
        if (setting.projectId.isBlank()) {
            setting.projectId = projectId
        }
        setting.pipelineId = pipelineResult.pipelineId // fix 用户端可能不传入pipelineId的问题，或者传错的问题
        pipelineSettingFacadeService.saveSetting(
            userId = userId,
            setting = setting,
            checkPermission = false,
            version = pipelineResult.version,
            dispatchPipelineUpdateEvent = false
        )
        return pipelineResult
    }

    fun getPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        version: Int? = null,
        checkPermission: Boolean = true
    ): Model {

        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = "用户($userId)无权限在工程($projectId)下获取流水线($pipelineId)"
            )
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在"
            )

        if (pipelineInfo.channelCode != channelCode) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                defaultMessage = "指定要复制的流水线渠道来源${pipelineInfo.channelCode}不符合$channelCode",
                params = arrayOf(pipelineInfo.channelCode.name)
            )
        }

        val model = pipelineRepositoryService.getModel(projectId, pipelineId, version)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS,
                defaultMessage = "指定要复制的流水线-模型不存在"
            )

        try {
            val triggerContainer = model.stages[0].containers[0] as TriggerContainer
            val buildNo = triggerContainer.buildNo
            if (buildNo != null) {
                buildNo.buildNo = pipelineRepositoryService.getBuildNo(projectId = projectId, pipelineId = pipelineId)
                    ?: buildNo.buildNo
            }
            // 兼容性处理
            BuildPropertyCompatibilityTools.fix(triggerContainer.params)

            // 获取流水线labels
            val groups = pipelineGroupService.getGroups(userId = userId, projectId = projectId, pipelineId = pipelineId)
            val labels = mutableListOf<String>()
            groups.forEach {
                labels.addAll(it.labels)
            }
            model.labels = labels
            model.name = pipelineInfo.pipelineName
            model.desc = pipelineInfo.pipelineDesc
            model.pipelineCreator = pipelineInfo.creator

            val defaultTagId by lazy { stageTagService.getDefaultStageTag().data?.id } // 优化
            model.stages.forEach {
                if (it.name.isNullOrBlank()) it.name = it.id
                if (it.tag == null) it.tag = defaultTagId?.let { self -> listOf(self) }
                it.resetBuildOption()
            }

            // 部分老的模板实例没有templateId，需要手动加上
            if (model.instanceFromTemplate == true) {
                model.templateId = templateService.getTemplateIdByPipeline(projectId, pipelineId)
            }
            // 将当前最新版本号传给前端
            model.latestVersion = pipelineInfo.version
            return model
        } catch (e: Exception) {
            logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", e)
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.OPERATE_PIPELINE_FAIL,
                defaultMessage = "Fail to get the pipeline",
                params = arrayOf(e.message ?: "unknown")
            )
        }
    }

    fun deletePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode? = null,
        checkPermission: Boolean = true,
        delete: Boolean = false
    ): DeletePipelineResult {
        val watcher = Watcher(id = "deletePipeline|$pipelineId|$userId")
        var success = false
        try {
            if (checkPermission) {
                watcher.start("perm_v_perm")
                pipelinePermissionService.validPipelinePermission(
                    userId = userId, projectId = projectId, pipelineId = pipelineId,
                    permission = AuthPermission.DELETE, message = "用户($userId)无权限在工程($projectId)下删除流水线($pipelineId)"
                )
                watcher.stop()
            }

            val existModel = pipelineRepositoryService.getModel(projectId, pipelineId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS,
                    defaultMessage = "指定要复制的流水线-模型不存在"
                )
            // 对已经存在的模型做删除前处理
            val param = BeforeDeleteParam(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode ?: ChannelCode.BS
            )
            modelCheckPlugin.beforeDeleteElementInExistsModel(existModel, null, param)

            watcher.start("s_r_pipeline_del")
            val deletePipelineResult = pipelineRepositoryService.deletePipeline(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                channelCode = channelCode,
                delete = delete
            )
            watcher.stop()

            if (checkPermission) {
                watcher.start("perm_d_perm")
                pipelinePermissionService.deleteResource(projectId = projectId, pipelineId = pipelineId)
                watcher.stop()
            }
            success = true
            return deletePipelineResult
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher, warnThreshold = 2000)
            pipelineBean.delete(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_DELETE, watcher.totalTimeMillis)
            logger.info("DEL_PIPELINE|$pipelineId|$channelCode|p=$checkPermission|u=$userId|del=$delete")
        }
    }

    fun isPipelineExist(
        projectId: String,
        pipelineId: String? = null,
        name: String,
        channelCode: ChannelCode
    ): Boolean {
        return pipelineRepositoryService.isPipelineExist(
            projectId = projectId, pipelineName = name, channelCode = channelCode, excludePipelineId = pipelineId
        )
    }

    fun batchUpdatePipelineNamePinYin(userId: String) {
        logger.info("$userId batchUpdatePipelineNamePinYin")
        val redisLock = RedisLock(redisOperation, "process:batchUpdatePipelineNamePinYin", 5 * 60)
        if (redisLock.tryLock()) {
            try {
                pipelineInfoDao.batchUpdatePipelineNamePinYin(dslContext)
            } finally {
                redisLock.unlock()
                logger.info("$userId batchUpdatePipelineNamePinYin finished")
            }
        }
    }

    fun getPipelineChannel(projectId: String, pipelineId: String): ChannelCode? {
        if (pipelineChannelCache.getIfPresent(pipelineId) != null) {
            return pipelineChannelCache.getIfPresent(pipelineId)
        }
        val pipelineInfo = pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId) ?: return null
        val channelCode = ChannelCode.getChannel(pipelineInfo.channel)
        if (channelCode != null) {
            pipelineChannelCache.put(pipelineId, channelCode)
        }
        return channelCode
    }

    fun batchUpdateModelName(modelUpdateList: List<ModelUpdate>): List<ModelUpdate> {
        val failUpdateModels = mutableListOf<ModelUpdate>()
        modelUpdateList.forEach {
            try {
                val pipelineExist = isPipelineExist(
                    projectId = it.projectId,
                    name = it.name,
                    channelCode = ChannelCode.GIT,
                    pipelineId = it.pipelineId
                )
                if (!pipelineExist) {
                    pipelineRepositoryService.updateModelName(
                        pipelineId = it.pipelineId,
                        projectId = it.projectId,
                        modelName = it.name,
                        userId = it.updateUserId
                    )
                } else {
                    it.updateResultMessage = "pipeline name exist"
                    failUpdateModels.add(it)
                }
            } catch (e: Exception) {
                it.updateResultMessage = "some wrong happen in dao update,error message:${e.message}"
                failUpdateModels.add(it)
            }
        }
        return failUpdateModels
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineInfoFacadeService::class.java)
    }
}
