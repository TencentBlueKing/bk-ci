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
import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.PipelineAlreadyExistException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_MAX_PIPELINE_COUNT_PER_PROJECT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_PERMISSION_PLUGIN_IN_TEMPLATE
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
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.classify.PipelineViewBulkAdd
import com.tencent.devops.process.pojo.pipeline.DeletePipelineResult
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.store.api.template.ServiceTemplateResource
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineInfoFacadeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val projectCacheService: ProjectCacheService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val stageTagService: StageTagService,
    private val templateService: TemplateService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineBean: PipelineBean,
    private val processJmxApi: ProcessJmxApi,
    private val client: Client,
    private val pipelineInfoDao: PipelineInfoDao,
    private val redisOperation: RedisOperation,
    private val pipelineRecentUseService: PipelineRecentUseService
) {

    @Value("\${process.deletedPipelineStoreDays:30}")
    private val deletedPipelineStoreDays: Int = 30

    // pipeline对应的channel为静态数据, 基本不会变
    private val pipelineChannelCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String/*pipelineId*/, ChannelCode>()

    fun exportPipeline(userId: String, projectId: String, pipelineId: String): Response {
        val language = I18nUtil.getLanguage(userId)
        val permission = AuthPermission.EDIT
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                language,
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )

        val settingInfo = pipelineRepositoryService.getSetting(projectId, pipelineId)
            ?: throw OperationException(
                I18nUtil.getCodeLanMessage(ILLEGAL_PIPELINE_MODEL_JSON, language = I18nUtil.getLanguage(userId))
            )
        val model = pipelineRepositoryService.getModel(projectId, pipelineId)
            ?: throw OperationException(
                I18nUtil.getCodeLanMessage(ILLEGAL_PIPELINE_MODEL_JSON, language = I18nUtil.getLanguage(userId))
            )

        // 适配兼容老数据
        model.stages.forEach {
            it.transformCompatibility()
        }
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
                MessageUtil.getMessageByLocale(
                    messageCode = USER_NEED_PIPELINE_X_PERMISSION,
                    params = arrayOf(AuthPermission.CREATE.getI18n(I18nUtil.getLanguage(userId))),
                    language = I18nUtil.getLanguage(userId)
                )
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
                val language = I18nUtil.getLanguage(userId)
                val permission = AuthPermission.CREATE
                pipelinePermissionService.validPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = "*",
                    permission = permission,
                    message = MessageUtil.getMessageByLocale(
                        USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                        language,
                        arrayOf(
                            userId,
                            projectId,
                            permission.getI18n(I18nUtil.getLanguage(userId)),
                            "*"
                        )
                    )
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
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
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
                    throw OperationException(
                        validateRet.message ?: MessageUtil.getMessageByLocale(
                            ERROR_NO_PERMISSION_PLUGIN_IN_TEMPLATE,
                            I18nUtil.getLanguage(userId)
                        )
                    )
                }
                watcher.stop()
            }

            watcher.start("project_v_pipeline")
            // 检查用户流水线是否达到上限
            val projectVO = projectCacheService.getProject(projectId)
            if (projectVO?.pipelineLimit != null) {
                val preCount = pipelineRepositoryService.countByProjectIds(setOf(projectId), ChannelCode.BS)
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

                // 添加标签
                pipelineGroupService.addPipelineLabel(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    labelIds = model.labels
                )

                // 添加到静态分组
                val bulkAdd = PipelineViewBulkAdd(pipelineIds = listOf(pipelineId), viewIds = model.staticViews)
                pipelineViewGroupService.bulkAdd(userId, projectId, bulkAdd)

                // 添加到动态分组
                pipelineViewGroupService.updateGroupAfterPipelineCreate(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId
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
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_IS_EXISTS
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
    ): DeployPipelineResult {
        val watcher = Watcher(id = "restorePipeline|$pipelineId|$userId")
        try {
            watcher.start("isProjectManager")
            // 判断用户是否为项目管理员
            if (!pipelinePermissionService.checkProjectManager(userId, projectId)) {
                val defaultMessage = "admin"
                val permissionMsg = I18nUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${BkAuthGroup.MANAGER.value}",
                    defaultMessage = defaultMessage,
                    language = I18nUtil.getLanguage(userId)
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
            return DeployPipelineResult(pipelineId, pipelineName = model.name, version = model.latestVersion)
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
        pipelineCopy: PipelineCopy,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): String {
        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )

        logger.info("Start to copy the pipeline $pipelineId")
        if (checkPermission) {
            val permission = AuthPermission.EDIT
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
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
                throw PermissionForbiddenException(
                    MessageUtil.getMessageByLocale(
                        USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                        I18nUtil.getLanguage(userId),
                        arrayOf(
                            userId,
                            projectId,
                            AuthPermission.CREATE.getI18n(I18nUtil.getLanguage(userId)),
                            "*"
                        )
                    )
                )
            }
        }

        if (pipeline.channelCode != channelCode) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                params = arrayOf(pipeline.channelCode.name)
            )
        }

        val model = pipelineRepositoryService.getModel(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )
        try {
            val copyMode = Model(
                name = pipelineCopy.name,
                desc = pipelineCopy.desc ?: model.desc,
                stages = model.stages,
                staticViews = pipelineCopy.staticViews,
                labels = pipelineCopy.labels
            )
            modelCheckPlugin.clearUpModel(copyMode)
            val newPipelineId = createPipeline(userId, projectId, copyMode, channelCode)
            val settingInfo = pipelineSettingFacadeService.getSettingInfo(projectId, pipelineId)
            if (settingInfo != null) {
                // setting pipeline需替换成新流水线的
                val newSetting = pipelineSettingFacadeService.rebuildSetting(
                    oldSetting = settingInfo,
                    projectId = projectId,
                    newPipelineId = newPipelineId,
                    pipelineName = pipelineCopy.name
                )
                // 复制setting到新流水线
                pipelineSettingFacadeService.saveSetting(
                    userId = userId,
                    setting = newSetting,
                    dispatchPipelineUpdateEvent = false,
                    updateLabels = false
                )
            }
            return newPipelineId
        } catch (e: JsonParseException) {
            logger.error("Parse process($pipelineId) fail", e)
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ILLEGAL_PIPELINE_MODEL_JSON
            )
        } catch (e: PipelineAlreadyExistException) {
            throw ErrorCodeException(
                statusCode = Response.Status.CONFLICT.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
            )
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", e)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.OPERATE_PIPELINE_FAIL,
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
                errorCode = ProcessMessageCode.ERROR_PIPELINE_TEMPLATE_CAN_NOT_EDIT
            )
        }
        val apiStartEpoch = System.currentTimeMillis()
        var success = false

        try {
            if (checkPermission) {
                val permission = AuthPermission.EDIT
                pipelinePermissionService.validPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = permission,
                    message = MessageUtil.getMessageByLocale(
                        USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                        I18nUtil.getLanguage(userId),
                        arrayOf(
                            userId,
                            projectId,
                            permission.getI18n(I18nUtil.getLanguage(userId)),
                            pipelineId
                        )
                    )
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
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
                )
            }

            val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
                )

            if (pipeline.channelCode != channelCode) {
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                    params = arrayOf(pipeline.channelCode.name)
                )
            }

            val existModel = pipelineRepositoryService.getModel(projectId, pipelineId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
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
            val permission = AuthPermission.VIEW
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )

        if (pipelineInfo.channelCode != channelCode) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                params = arrayOf(pipelineInfo.channelCode.name)
            )
        }

        val model = pipelineRepositoryService.getModel(projectId, pipelineId, version)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
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
                it.transformCompatibility()
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
                val permission = AuthPermission.DELETE
                pipelinePermissionService.validPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = permission,
                    message = MessageUtil.getMessageByLocale(
                        USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                        I18nUtil.getLanguage(userId),
                        arrayOf(
                            userId,
                            projectId,
                            permission.getI18n(I18nUtil.getLanguage(userId)),
                            pipelineId
                        )
                    )
                )
                watcher.stop()
            }

            val existModel = pipelineRepositoryService.getModel(projectId, pipelineId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
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
