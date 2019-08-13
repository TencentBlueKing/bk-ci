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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.service.template

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.records.TPipelineSettingRecord
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.pipeline.PipelineResource
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.process.pojo.template.CopyTemplateReq
import com.tencent.devops.process.pojo.template.OptionalTemplate
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.SaveAsTemplateReq
import com.tencent.devops.process.pojo.template.TemplateCompareModel
import com.tencent.devops.process.pojo.template.TemplateCompareModelResult
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateOperationMessage
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.TemplateVersion
import com.tencent.devops.process.service.ParamService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.template.dao.PTemplateDao
import com.tencent.devops.process.template.dao.TemplatePipelineDao
import com.tencent.devops.process.util.DateTimeUtils
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

@Service
class TemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: PTemplateDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineService: PipelineService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val pipelineResDao: PipelineResDao,
    private val pipelineGroupService: PipelineGroupService,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val paramService: ParamService,
    private val modelCheckPlugin: ModelCheckPlugin
) {

    fun createTemplate(projectId: String, userId: String, template: Model): String {
        logger.info("Start to create the template $template by user $userId")
        checkPermission(projectId, userId)
        checkTemplate(template)
        val templateId = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(context, template.name, projectId, templateId)
            updateContainerId(template)
            val version = templateDao.create(
                context,
                projectId,
                templateId,
                template.name,
                INIT_TEMPLATE_NAME,
                userId,
                objectMapper.writeValueAsString(template),
                false
            )

            pipelineSettingDao.insertNewSetting(context, projectId, templateId, template.name, true)
            logger.info("Get the template version $version")
        }

        return templateId
    }

    fun copyTemplate(
        userId: String,
        projectId: String,
        srcTemplateId: String,
        copyTemplateReq: CopyTemplateReq
    ): String {
        logger.info("Start to copy the template, $srcTemplateId | $userId | $copyTemplateReq")

        checkPermission(projectId, userId)

        var latestTemplate = templateDao.getLatestTemplate(dslContext, projectId, srcTemplateId)
        val template = latestTemplate
        if (latestTemplate.type == TemplateType.CONSTRAINT.name) {
            latestTemplate = templateDao.getLatestTemplate(dslContext, latestTemplate.srcTemplateId)
        }
        val newTemplateId = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(context, copyTemplateReq.templateName, projectId, newTemplateId)
            val version = templateDao.createTemplate(
                dslContext = context,
                projectId = projectId,
                templateId = newTemplateId,
                templateName = copyTemplateReq.templateName,
                versionName = INIT_TEMPLATE_NAME,
                userId = userId,
                template = latestTemplate.template,
                type = TemplateType.CUSTOMIZE.name,
                category = template.category,
                logoUrl = template.logoUrl,
                srcTemplateId = srcTemplateId,
                storeFlag = false,
                weight = 0
            )

            if (copyTemplateReq.isCopySetting) {
                val setting = copySetting(
                    getTemplateSetting(projectId, userId, srcTemplateId),
                    newTemplateId,
                    copyTemplateReq.templateName
                )
                saveTemplatePipelineSetting(userId, setting, true)
            } else {
                pipelineSettingDao.insertNewSetting(
                    context,
                    projectId,
                    newTemplateId,
                    copyTemplateReq.templateName,
                    true
                )
            }

            logger.info("Get the template version $version")
        }

        return newTemplateId
    }

    /**
     * 流水线另存为模版
     */
    fun saveAsTemplate(
        userId: String,
        projectId: String,
        saveAsTemplateReq: SaveAsTemplateReq
    ): String {
        logger.info("Start to saveAsTemplate, $userId | $projectId | $saveAsTemplateReq")

        checkPermission(projectId, userId)

        val template = pipelineResDao.getLatestVersionModelString(dslContext, saveAsTemplateReq.pipelineId)
            ?: throw OperationException("流水线不存在")

        val templateId = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(context, saveAsTemplateReq.templateName, projectId, templateId)
            val version = templateDao.create(
                dslContext = context,
                projectId = projectId,
                templateId = templateId,
                templateName = saveAsTemplateReq.templateName,
                versionName = INIT_TEMPLATE_NAME,
                userId = userId,
                template = template,
                storeFlag = false
            )

            if (saveAsTemplateReq.isCopySetting) {
                val setting = copySetting(
                    getTemplateSetting(projectId, userId, saveAsTemplateReq.pipelineId),
                    templateId,
                    saveAsTemplateReq.templateName
                )
                saveTemplatePipelineSetting(userId, setting, true)
            } else {
                pipelineSettingDao.insertNewSetting(
                    context,
                    projectId,
                    templateId,
                    saveAsTemplateReq.templateName,
                    true
                )
            }

            logger.info("Get the template version $version")
        }

        return templateId
    }

    fun deleteTemplate(projectId: String, userId: String, templateId: String): Boolean {
        logger.info("Start to delete the template $templateId by user $userId")
        checkPermission(projectId, userId)
        val template = templateDao.getLatestTemplate(dslContext, templateId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val pipelines = templatePipelineDao.listPipeline(context, templateId)
            if (pipelines.isNotEmpty) {
                throw OperationException("已有流水线实例使用当前模版，不能删除")
            }
            if (template.type == TemplateType.CUSTOMIZE.name && template.storeFlag == true) {
                throw OperationException("已关联到研发商店，请先下架再删除")
            }
            if (template.type == TemplateType.CUSTOMIZE.name &&
                templateDao.isExistInstalledTemplate(context, templateId)
            ) {
                throw OperationException("已安装到其他项目下使用，不能删除")
            }

            templateDao.delete(context, templateId)
            pipelineSettingDao.delete(context, templateId)
            if (template.type == TemplateType.CONSTRAINT.name) {
                client.get(ServiceStoreResource::class).uninstall(
                    template.srcTemplateId, StoreTypeEnum.TEMPLATE, template
                        .projectId
                )
            }
        }
        return true
    }

    fun deleteTemplate(projectId: String, userId: String, templateId: String, version: Long): Boolean {
        logger.info("Start to delete the template [$projectId|$userId|$templateId|$version]")
        checkPermission(projectId, userId)
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val pipelines = templatePipelineDao.listPipeline(context, templateId, version)
            if (pipelines.isNotEmpty) {
                logger.warn("There are ${pipelines.size} pipeline attach to $templateId of version $version")
                throw OperationException("模板还存在实例，不允许删除")
            }
            templateDao.delete(dslContext, templateId, version) == 1
        }
    }

    fun updateTemplate(
        projectId: String,
        userId: String,
        templateId: String,
        versionName: String,
        template: Model
    ): Boolean {
        logger.info("Start to update the template $templateId by user $userId - ($template)")
        checkPermission(projectId, userId)
        checkTemplate(template)
        val latestTemplate = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(context, template.name, projectId, templateId)
            updateContainerId(template)
            val version = templateDao.createTemplate(
                dslContext = context,
                projectId = projectId,
                templateId = templateId,
                templateName = template.name,
                versionName = versionName,
                userId = userId,
                template = objectMapper.writeValueAsString(template),
                type = latestTemplate.type,
                category = latestTemplate.category,
                logoUrl = latestTemplate.logoUrl,
                srcTemplateId = latestTemplate.srcTemplateId,
                storeFlag = latestTemplate.storeFlag,
                weight = latestTemplate.weight
            )
            logger.info("Get the update template version $version")
        }

        if (latestTemplate.storeFlag) {
            // 将更新信息推送给使用模版的项目管理员 -- todo
        }

        return true
    }

    fun updateTemplateSetting(
        projectId: String,
        userId: String,
        templateId: String,
        setting: PipelineSetting
    ): Boolean {
        logger.info("Start to update the template setting - [$projectId|$userId|$templateId]")
        checkPermission(projectId, userId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            checkTemplateName(context, setting.pipelineName, projectId, templateId)
            saveTemplatePipelineSetting(userId, setting, true)
        }
        return true
    }

    fun getTemplateSetting(projectId: String, userId: String, templateId: String): PipelineSetting {

        val setting = pipelineSettingDao.getSetting(dslContext, templateId)
        if (setting == null) {
            logger.warn("Fail to get the template setting - [$projectId|$userId|$templateId]")
            throw OperationException("流水线模板设置不存在")
        }

        val hasPermission = hasManagerPermission(projectId, userId)

        val groups = pipelineGroupService.getGroups(userId, projectId, templateId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        return setting.map {
            with(TPipelineSetting.T_PIPELINE_SETTING) {
                val successType = it.get(SUCCESS_TYPE).split(",").filter { i -> i.isNotBlank() }
                    .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
                val failType = it.get(FAIL_TYPE).split(",").filter { i -> i.isNotBlank() }
                    .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
                PipelineSetting(
                    projectId,
                    templateId,
                    it.get(NAME),
                    it.get(DESC),
                    PipelineRunLockType.valueOf(it.get(RUN_LOCK_TYPE)),
                    Subscription(
                        successType,
                        it.get(SUCCESS_GROUP).split(",").toSet(),
                        it.get(SUCCESS_RECEIVER),
                        it.get(SUCCESS_WECHAT_GROUP_FLAG),
                        it.get(SUCCESS_WECHAT_GROUP),
                        it.get(SUCCESS_DETAIL_FLAG),
                        it.get(SUCCESS_CONTENT) ?: ""
                    ),
                    Subscription(
                        failType,
                        it.get(FAIL_GROUP).split(",").toSet(),
                        it.get(FAIL_RECEIVER),
                        it.get(FAIL_WECHAT_GROUP_FLAG),
                        it.get(FAIL_WECHAT_GROUP),
                        it.get(FAIL_DETAIL_FLAG),
                        it.get(FAIL_CONTENT) ?: ""
                    ),
                    labels,
                    DateTimeUtils.secondToMinute(it.get(WAIT_QUEUE_TIME_SECOND)),
                    it.get(MAX_QUEUE_SIZE),
                    hasPermission
                )
            }
        }
    }

    fun listTemplate(
        projectId: String,
        userId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): TemplateListModel {
        logger.info("[$projectId|$userId|$templateType|$storeFlag|$page|$pageSize] List template")
        val hasManagerPermission = hasManagerPermission(projectId, userId)
        val result = ArrayList<TemplateModel>()
        val count = templateDao.countTemplate(dslContext, projectId, null, templateType, null, storeFlag)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val templates =
                templateDao.listTemplate(context, projectId, null, templateType, null, storeFlag, page, pageSize)
            logger.info("after get templates")

            val constrainedTemplateList = mutableListOf<String>()
            val templateIdList = mutableSetOf<String>()
            templates?.forEach { template ->
                if ((template["templateType"] as String) == TemplateType.CONSTRAINT.name) {
                    constrainedTemplateList.add(template["srcTemplateId"] as String)
                }
                templateIdList.add(template["templateId"] as String)
            }
            val srcTemplateRecords =
                templateDao.listTemplate(context, null, null, null, constrainedTemplateList, null, null, null)
            val srcTemplates = srcTemplateRecords?.associateBy { it["templateId"] as String }
            logger.info("after get srcTemplates")

            val settings = pipelineSettingDao.getSettings(context, templateIdList).map { it.pipelineId to it }.toMap()
            templates?.forEach { record ->
                val templateId = record["templateId"] as String
                val type = record["templateType"] as String

                val templateRecord = if (type == TemplateType.CONSTRAINT.name) {
                    val srcTemplateId = record["srcTemplateId"] as String
                    srcTemplates?.get(srcTemplateId)
                } else {
                    record
                }

                if (templateRecord == null) {
                    throw OperationException("模板不存在")
                } else {
                    val modelStr = templateRecord["template"] as String
                    val version = templateRecord["version"] as Long

                    val model: Model = objectMapper.readValue(modelStr)
                    val associateCodes = listOf<String>()
                    val associatePipeline =
                        templatePipelineDao.listPipeline(context, templateId)

                    val pipelineIds = associatePipeline.map { PipelineId(it.pipelineId) }

                    var hasInstances2Upgrade = false

                    run lit@{
                        associatePipeline.forEach {
                            if (it.version < version) {
                                logger.info("The pipeline ${it.pipelineId} need to upgrade from ${it.version} to $version")
                                hasInstances2Upgrade = true
                                return@lit
                            }
                        }
                    }

                    val setting = settings[templateId]
                    val logoUrl = record["logoUrl"] as? String
                    result.add(
                        TemplateModel(
                            name = setting?.name ?: model.name,
                            templateId = templateId,
                            version = version,
                            versionName = templateRecord["versionName"] as String,
                            templateType = type,
                            templateTypeDesc = TemplateType.getTemplateTypeDesc(type),
                            logoUrl = logoUrl ?: "",
                            storeFlag = record["storeFlag"] as Boolean,
                            associateCodes = associateCodes,
                            associatePipelines = pipelineIds,
                            hasInstance2Upgrade = hasInstances2Upgrade,
                            hasPermission = hasManagerPermission
                        )
                    )
                }
            }
        }
        return TemplateListModel(projectId, hasManagerPermission, result, count)
    }

    @Suppress("UNCHECKED_CAST")
    fun listAllTemplate(
        projectId: String,
        userId: String,
        templateType: TemplateType?,
        page: Int?,
        pageSize: Int?
    ): OptionalTemplateList {
        logger.info("[$projectId|$userId|$templateType|$page|$pageSize] List template")
        val result = mutableMapOf<String, OptionalTemplate>()
        val templateCount = templateDao.countTemplate(dslContext, projectId, true, templateType, null, null)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val templates = templateDao.listTemplate(context, projectId, true, templateType, null, null, page, pageSize)

            val constrainedTemplateList = mutableListOf<String>()
            val templateIdList = mutableSetOf<String>()
            templates?.forEach { tempTemplate ->
                if ((tempTemplate["templateType"] as String) == TemplateType.CONSTRAINT.name) {
                    constrainedTemplateList.add(tempTemplate["srcTemplateId"] as String)
                }
                templateIdList.add(tempTemplate["templateId"] as String)
            }
            val srcTemplateRecords =
                templateDao.listTemplate(context, null, null, null, constrainedTemplateList, null, null, null)
            val srcTemplates = srcTemplateRecords?.associateBy { it["templateId"] as String }

            val settings = pipelineSettingDao.getSettings(context, templateIdList).map { it.pipelineId to it }.toMap()
            templates?.forEach { record ->
                val templateId = record["templateId"] as String
                val type = record["templateType"] as String

                val templateRecord = if (type == TemplateType.CONSTRAINT.name) {
                    val srcTemplateId = record["srcTemplateId"] as String
                    srcTemplates?.get(srcTemplateId)
                } else {
                    record
                }

                if (templateRecord != null) {
                    val modelStr = templateRecord["template"] as String
                    val version = templateRecord["version"] as Long

                    val model: Model = objectMapper.readValue(modelStr)
                    val setting = settings[templateId]
                    val logoUrl = record["logoUrl"] as? String
                    val categoryStr = record["category"] as? String
                    val key =
                        if (type == TemplateType.CONSTRAINT.name) record["srcTemplateId"] as String else templateId
                    result[key] = OptionalTemplate(
                        name = setting?.name ?: model.name,
                        templateId = templateId,
                        version = version,
                        versionName = templateRecord["versionName"] as String,
                        templateType = type,
                        templateTypeDesc = TemplateType.getTemplateTypeDesc(type),
                        logoUrl = logoUrl ?: "",
                        category = if (!categoryStr.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
                            categoryStr,
                            List::class.java
                        ) as List<String> else listOf(),
                        stages = model.stages
                    )
                }
            }
        }

        return OptionalTemplateList(
            count = templateCount,
            page = page,
            pageSize = pageSize,
            templates = result
        )
    }

    fun getTemplate(projectId: String, userId: String, templateId: String, version: Long?): TemplateModelDetail {
        var templates = templateDao.listTemplate(dslContext, projectId, templateId)
        if (templates.isEmpty()) {
            logger.warn("The template $templateId of project $projectId is not exist")
            throw OperationException("模板不存在")
        }
        var latestTemplate = templates[0]
        val constrainedTemplate = latestTemplate
        val isConstrainedFlag = latestTemplate.type == TemplateType.CONSTRAINT.name

        if (isConstrainedFlag) {
            templates = templateDao.listTemplateByIds(dslContext, listOf(latestTemplate.srcTemplateId))
            if (templates.isEmpty()) {
                logger.warn("The src template ${latestTemplate.srcTemplateId} is not exist")
                throw OperationException("源模板不存在")
            }
            latestTemplate = templates[0]
        }

        val setting = pipelineSettingDao.getSetting(dslContext, templateId)
        if (setting == null) {
            logger.warn("The template setting is not exist [$projectId|$userId|$templateId]")
            throw OperationException("模板设置不存在")
        }

        val latestVersion = TemplateVersion(
            latestTemplate.version,
            latestTemplate.versionName,
            latestTemplate.createdTime.timestampmilli(),
            latestTemplate.creator
        )
        val versionNames = templates.groupBy { it.versionName }
        val versions = versionNames.map {
            val temp = it.value.maxBy { t -> t.version }!!
            TemplateVersion(temp.version, temp.versionName, temp.createdTime.timestampmilli(), temp.creator)
        }.toList()

        var template: TTemplateRecord? = null
        if (version == null) {
            template = templates[0]
        } else {
            run lit@{
                templates.forEach {
                    if (it.version == version) {
                        template = it
                        return@lit
                    }
                }
            }
        }
        if (template == null) {
            logger.warn("The template $templateId of project $projectId with version $version is not exist")
            throw OperationException("模板不存在")
        }
        val currentVersion = TemplateVersion(
            template!!.version,
            template!!.versionName,
            template!!.createdTime.timestampmilli(),
            template!!.creator
        )
        val model: Model = if (isConstrainedFlag) {
            objectMapper.readValue(latestTemplate.template)
        } else {
            objectMapper.readValue(template!!.template)
        }
        model.name = setting.name
        model.desc = setting.desc
        val groups = pipelineGroupService.getGroups(userId, projectId, templateId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        model.labels = labels
        return TemplateModelDetail(
            versions = versions,
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            templateName = setting.name,
            description = setting.desc ?: "",
            creator = if (isConstrainedFlag) constrainedTemplate.creator else template!!.creator,
            template = instanceParamModel(userId, projectId, model),
            templateType = if (isConstrainedFlag) constrainedTemplate.type else template!!.type,
            logoUrl = if (isConstrainedFlag) constrainedTemplate.logoUrl ?: "" else {
                if (template!!.logoUrl.isNullOrEmpty()) "" else template!!.logoUrl
            },
            hasPermission = hasManagerPermission(projectId, userId)
        )
    }

    private fun listTemplateVersions(projectId: String, templateId: String): List<TemplateVersion> {
        val templates = templateDao.listTemplate(dslContext, projectId, templateId)
        if (templates.isEmpty()) {
            logger.warn("The template $templateId of project $projectId is not exist")
            throw OperationException("模板不存在")
        }

        val versionNames = templates.groupBy { it.versionName }
        return versionNames.map {
            val temp = it.value.maxBy { t -> t.version }!!
            TemplateVersion(temp.version, temp.versionName, temp.createdTime.timestampmilli(), temp.creator)
        }.toList()
    }

    /**
     * 差异对比
     * 比较主要是对当前流水线对应的模板版本和选中的版本进行比较
     * 需要注意：
     * 1. 更新前的变量是流水线的变量
     * 2. 更新后的变量是流水线的变量和对应的模板的变量的一个对比
     */
    fun compareTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        pipelineId: String,
        version: Long
    ): TemplateCompareModelResult {
        logger.info("Compare the template instances - [$projectId|$userId|$templateId|$pipelineId|$version]")
        val templatePipelineRecord = templatePipelineDao.get(dslContext, pipelineId)
        val template: Model =
            objectMapper.readValue(templateDao.getTemplate(dslContext, templatePipelineRecord.version).template)
        val v1Model: Model = instanceCompareModel(
            objectMapper.readValue(
                pipelineResDao.getVersionModelString(dslContext, pipelineId, null)
                    ?: throw OperationException("流水线编排不存在")
            ),
            template
        )

        val v2Model = getTemplateModel(version)
        val v1Containers = getContainers(v1Model)
        val v2Containers = getContainers(v2Model)
        logger.info("Get the containers - [$v1Containers] - [$v2Containers]")

        compareContainer(v1Containers, v2Containers)
        val srcTemplate = templateDao.getTemplate(dslContext, version)
        val versions = listTemplateVersions(srcTemplate.projectId, srcTemplate.id)
        return compareModel(versions, v1Model, v2Model)
    }

    fun compareModel(versions: List<TemplateVersion>, v1Model: Model, v2Model: Model): TemplateCompareModelResult {
        val v1TriggerContainer = v1Model.stages[0].containers[0] as TriggerContainer
        val v2TriggerContainer = v2Model.stages[0].containers[0] as TriggerContainer
        return TemplateCompareModelResult(
            versions,
            TemplateCompareModel(
                v1TriggerContainer.buildNo,
                v1TriggerContainer.params,
                v1Model
            ),
            TemplateCompareModel(
                v2TriggerContainer.buildNo,
                getCompareModelParam(v1TriggerContainer.params, v2TriggerContainer),
                v2Model
            )
        )
    }

    private fun getCompareModelParam(
        instanceParam: List<BuildFormProperty>,
        triggerContainer: TriggerContainer
    ): List<BuildFormProperty> {
        return pipelineService.mergeProperties(instanceParam, triggerContainer.params)
    }

    fun compareContainer(v1Containers: Map<String, Container>, v2Containers: Map<String, Container>) {

        v1Containers.forEach { (id, container) ->
            val v2Container = v2Containers[id]
            if (v2Container == null) {
                logger.warn("The container is not exist of $id")
                return@forEach
            }
            container.elements.forEach e@{ element ->
                v2Container.elements.forEach { v2Element ->
                    if (element.id == v2Element.id) {
                        val modify = elementModify(element, v2Element)
                        if (modify) {
                            element.templateModify = true
                            v2Element.templateModify = true
                        }
                        return@e
                    }
                }
            }
        }
    }

    private fun getTemplateModel(version: Long): Model {
        return objectMapper.readValue(templateDao.getTemplate(dslContext, version).template)
    }

    fun elementModify(e1: Element, e2: Element): Boolean {
        if (e1::class != e2::class) {
            return true
        }

        val v1Properties = e1.javaClass.kotlin.declaredMemberProperties
        val v2Properties = e2.javaClass.kotlin.declaredMemberProperties
        if (v1Properties.size != v2Properties.size) {
            return true
        }

        val v1Map = v1Properties.map {
            it.isAccessible = true
            it.name to it.get(e1)
        }.toMap()

        val v2Map = v2Properties.map {
            it.isAccessible = true
            it.name to it.get(e2)
        }.toMap()

        if (v1Map.size != v2Map.size) {
            return true
        }

        for ((key, value) in v1Map) {
            if (!v2Map.containsKey(key)) {
                return true
            }
            if (v2Map[key] != value) {
                return true
            }
        }
        return false
    }

    private fun getContainers(model: Model): Map<String/*ID*/, Container> {
        val containers = HashMap<String, Container>()

        model.stages.forEach { stage ->
            stage.containers.forEach c@{
                if (it.containerId == null) {
                    logger.warn("The container id is null of $it")
                    return@c
                }
                containers[it.containerId!!] = it
            }
        }

        return containers
    }

    /**
     * 通过流水线ID获取流水线的参数，已经和模板的参数做了Merge操作
     */
    fun listTemplateInstancesParams(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        pipelineIds: Set<String>
    ): Map<String, TemplateInstanceParams> {
        try {
            val template = templateDao.getTemplate(dslContext, version)
            val model: Model = objectMapper.readValue(template.template)
            val triggerContainer = model.stages[0].containers[0] as TriggerContainer
            val buildNo = triggerContainer.buildNo
            val params = triggerContainer.params
            val pipelines = listLatestModel(pipelineIds)
            logger.info("[$userId|$projectId|$templateId|$version] Get the pipelines - $pipelines")
            val settings = pipelineSettingDao.getSettings(dslContext, pipelineIds)

            return pipelines.map {
                val pipelineId = it.key
                val m: Model = objectMapper.readValue(it.value)
                val container = m.stages[0].containers[0] as TriggerContainer
                val param = paramService.filterParams(userId, projectId, removeProperties(params, container.params))
                logger.info("[$userId|$projectId|$templateId|$version] Get the param ($param)")
                val no = if (container.buildNo != null) {
                    container.buildNo
                } else {
                    buildNo
                }
                pipelineId to TemplateInstanceParams(
                    pipelineId,
                    getPipelineName(settings, pipelineId) ?: model.name,
                    no,
                    param
                )
            }.toMap()
        } catch (t: Throwable) {
            logger.warn("Fail to list pipeline params - [$projectId|$userId|$templateId|$version]", t)
            throw OperationException("列举流水线参数失败")
        }
    }

    fun createTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet {
        logger.info("Create the new template instance [$projectId|$userId|$templateId|$version|$useTemplateSettings]")
        val template = templateDao.getTemplate(dslContext, version)

        val successPipelines = ArrayList<String>()
        val failurePipelines = ArrayList<String>()
        val messages = HashMap<String, String>()

        instances.forEach { instance ->
            try {
                val pipelineName = instance.pipelineName
                val buildNo = instance.buildNo
                val param = instance.param
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    // TODO 事务
                    val instanceModel =
                        pipelineService.instanceModel(
                            objectMapper.readValue(template.template),
                            pipelineName,
                            buildNo,
                            param,
                            true
                        )
                    val pipelineId =
                        pipelineService.createPipeline(userId, projectId, instanceModel, ChannelCode.BS, true)
                    templatePipelineDao.create(
                        context,
                        pipelineId,
                        template.version,
                        template.versionName,
                        templateId,
                        userId,
                        if (buildNo == null) {
                            null
                        } else {
                            objectMapper.writeValueAsString(buildNo)
                        },
                        objectMapper.writeValueAsString(param)
                    )
                    if (useTemplateSettings) {
                        val setting = copySetting(
                            getTemplateSetting(projectId, userId, templateId),
                            pipelineId,
                            instance.pipelineName
                        )
                        saveTemplatePipelineSetting(userId, setting)
                    } else {
                        pipelineSettingDao.insertNewSetting(context, projectId, pipelineId, pipelineName)
                    }
                    successPipelines.add(instance.pipelineName)
                }
            } catch (t: DuplicateKeyException) {
                logger.warn("Fail to update the pipeline $instance of project $projectId by user $userId", t)
                failurePipelines.add(instance.pipelineName)
                messages[instance.pipelineName] = "流水线已经存在"
            } catch (t: Throwable) {
                logger.warn("Fail to update the pipeline $instance of project $projectId by user $userId", t)
                failurePipelines.add(instance.pipelineName)
                messages[instance.pipelineName] = t.message ?: "创建流水线失败"
            }
        }

        return TemplateOperationRet(0, TemplateOperationMessage(successPipelines, failurePipelines, messages), "")
    }

    /**
     * 批量更新模板实例
     */
    fun updateTemplateInstances(
        projectId: String,
        userId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet {
        logger.info("Update the template instance [$projectId|$userId|$templateId|$version|$instances|$useTemplateSettings]")

        val successPipelines = ArrayList<String>()
        val failurePipelines = ArrayList<String>()
        val messages = HashMap<String, String>()

        val template = templateDao.getTemplate(dslContext, version)

        instances.forEach {
            try {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    templatePipelineDao.update(
                        context,
                        template.version, template.versionName,
                        userId, it
                    )
                    val templateModel: Model = objectMapper.readValue(template.template)
                    val labels = if (useTemplateSettings) {
                        templateModel.labels
                    } else {
                        val tmpLabels = ArrayList<String>()
                        pipelineGroupService.getGroups(
                            userId,
                            projectId,
                            it.pipelineId
                        ).forEach { group ->
                            tmpLabels.addAll(group.labels)
                        }
                        tmpLabels
                    }
                    pipelineService.editPipeline(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = it.pipelineId,
                        model = pipelineService.instanceModel(
                            templateModel = templateModel,
                            pipelineName = it.pipelineName,
                            buildNo = it.buildNo,
                            param = it.param,
                            instanceFromTemplate = true,
                            labels = labels
                        ),
                        channelCode = ChannelCode.BS,
                        checkPermission = true,
                        checkTemplate = false
                    )

                    if (useTemplateSettings) {
                        val setting = copySetting(
                            getTemplateSetting(projectId, userId, templateId),
                            it.pipelineId,
                            it.pipelineName
                        )
                        saveTemplatePipelineSetting(userId, setting)
                    }
                    successPipelines.add(it.pipelineName)
                }
            } catch (t: DuplicateKeyException) {
                logger.warn("Fail to update the pipeline $it of project $projectId by user $userId", t)
                failurePipelines.add(it.pipelineName)
                messages[it.pipelineName] = "流水线已经存在"
            } catch (t: Throwable) {
                logger.warn("Fail to update the pipeline $it of project $projectId by user $userId", t)
                failurePipelines.add(it.pipelineName)
                messages[it.pipelineName] = t.message ?: "更新流水线失败"
            }
        }

        return TemplateOperationRet(0, TemplateOperationMessage(successPipelines, failurePipelines, messages), "")
    }

    fun copySetting(setting: PipelineSetting, pipelineId: String, templateName: String): PipelineSetting {
        with(setting) {
            return PipelineSetting(
                projectId, pipelineId, templateName, desc,
                runLockType,
                successSubscription,
                failSubscription,
                labels,
                waitQueueTimeMinute,
                maxQueueSize,
                hasPermission
            )
        }
    }

    /**
     * 删除流水线参数中的流水线常量
     */
    private fun instanceCompareModel(
        instance: Model,
        template: Model
    ): Model {
        val templateParams = (template.stages[0].containers[0] as TriggerContainer).templateParams
        if (templateParams == null || templateParams.isEmpty()) {
            return instance
        }
        val triggerContainer = instance.stages[0].containers[0] as TriggerContainer
        val finalParams = ArrayList<BuildFormProperty>()
        val params = triggerContainer.params
        params.forEach { param ->
            var exist = false
            run lit@{
                templateParams.forEach { template ->
                    if (template.id == param.id) {
                        // 流水线参数中的这个参数是模板常量， 要剔除
                        exist = true
                        return@lit
                    }
                }
            }
            if (!exist) {
                finalParams.add(param)
            }
        }

        val finalTriggerContainer = with(triggerContainer) {
            TriggerContainer(
                id,
                name,
                elements,
                status,
                startEpoch,
                systemElapsed,
                elementElapsed,
                finalParams,
                templateParams,
                buildNo,
                canRetry,
                containerId
            )
        }

        val stages = ArrayList<Stage>()

        instance.stages.forEachIndexed { index, stage ->
            if (index == 0) {
                stages.add(Stage(listOf(finalTriggerContainer), null))
            } else {
                stages.add(stage)
            }
        }

        return Model(instance.name, "", stages, instance.labels, true)
    }

    private fun saveTemplatePipelineSetting(
        userId: String,
        setting: PipelineSetting,
        isTemplate: Boolean = false
    ): Int {
        pipelineGroupService.updatePipelineLabel(userId, setting.pipelineId, setting.labels)
        pipelineInfoDao.update(dslContext, setting.pipelineId, userId, false, setting.pipelineName, setting.desc)
        logger.info("Save the template pipeline setting - ($setting)")
        return pipelineSettingDao.saveSetting(dslContext, setting, isTemplate)
    }

    private fun instanceParamModel(userId: String, projectId: String, model: Model): Model {
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val params = paramService.filterParams(userId, projectId, triggerContainer.params)
        val templateParams =
            if (triggerContainer.templateParams == null || triggerContainer.templateParams!!.isEmpty()) {
                triggerContainer.templateParams
            } else {
                paramService.filterParams(userId, projectId, triggerContainer.templateParams!!)
            }
        val rewriteContainer = TriggerContainer(
            null,
            triggerContainer.name,
            triggerContainer.elements,
            null, null, null, null,
            params, templateParams,
            triggerContainer.buildNo,
            triggerContainer.canRetry,
            triggerContainer.containerId
        )

        val stages = ArrayList<Stage>()

        model.stages.forEachIndexed { index, stage ->
            if (index == 0) {
                stages.add(Stage(listOf(rewriteContainer), null))
            } else {
                stages.add(stage)
            }
        }

        return Model(model.name, "", stages, model.labels, false)
    }

    /**
     * 只有管理员权限才能对模板操作
     */
    private fun checkPermission(projectId: String, userId: String) {
        val isProjectUser = hasManagerPermission(projectId = projectId, userId = userId)
        val errMsg = "用户${userId}没有模板操作权限"
        if (!isProjectUser) {
            logger.warn("The manager users is empty of project $projectId")
            throw OperationException(errMsg)
        }
    }

    fun hasManagerPermission(projectId: String, userId: String): Boolean =
        pipelinePermissionService.isProjectUser(userId = userId, projectId = projectId, group = BkAuthGroup.MANAGER)

    /**
     * 删除模板的参数， 如果模板中没有这个参数，那么流水线中应该删除掉
     */
    fun removeProperties(
        templateParams: List<BuildFormProperty>,
        pipelineParams: List<BuildFormProperty>
    ): List<BuildFormProperty> {
        if (templateParams.isEmpty()) {
            return pipelineParams
        }

        val result = ArrayList<BuildFormProperty>()

        templateParams.forEach outside@{ template ->
            pipelineParams.forEach { pipeline ->
                if (pipeline.id == template.id) {
                    result.add(pipeline)
                    return@outside
                }
            }
            result.add(template)
        }

        return result
    }

    /**
     * 检查模板是不是合法
     */
    private fun checkTemplate(template: Model) {
        if (template.name.isBlank()) {
            throw OperationException("模板名不能为空字符串")
        }
        modelCheckPlugin.checkModelIntegrity(model = template)
        checkPipelineParam(template)
    }

    /**
     * 模板的流水线变量和模板常量不能相同
     */
    private fun checkPipelineParam(template: Model) {
        val triggerContainer = template.stages[0].containers[0] as TriggerContainer

        if (triggerContainer.params.isEmpty()) {
            return
        }

        if (triggerContainer.templateParams == null || triggerContainer.templateParams!!.isEmpty()) {
            return
        }

        triggerContainer.params.forEach { param ->
            triggerContainer.templateParams!!.forEach { template ->
                if (param.id == template.id) {
                    throw OperationException("流水线变量参数和常量重名")
                }
            }
        }
    }

    private fun getPipelineName(records: Result<TPipelineSettingRecord>, pipelineId: String): String? {
        records.forEach {
            if (it.pipelineId == pipelineId)
                return it.name
        }
        return null
    }

    private fun updateContainerId(model: Model) {
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                if (container.containerId.isNullOrBlank()) {
                    container.containerId = UUIDUtil.generate()
                }

                container.elements.forEach { e ->
                    if (e.id.isNullOrBlank()) {
                        e.id = modelTaskIdGenerator.getNextId()
                    }
                }
            }
        }
    }

    private fun checkTemplateName(
        dslContext: DSLContext,
        name: String,
        projectId: String,
        templateId: String
    ) {
        val count = pipelineSettingDao.getSettingByName(
            dslContext,
            name,
            projectId,
            templateId,
            true
        )?.value1() ?: 0
        if (count > 0) {
            throw OperationException("模板名已经存在")
        }
    }

    fun listLatestModel(pipelineIds: Set<String>): Map<String/*Pipeline ID*/, String/*Model*/> {
        val modelResource = pipelineResDao.listModelResource(dslContext, pipelineIds).map {
            PipelineResource(it.pipelineId, it.version, it.model)
        }.groupBy { it.pipelineId }
        return modelResource.map { map ->
            map.key to map.value.maxBy { it.version }!!.model
        }.toMap()
    }

    fun addMarketTemplate(
        userId: String,
        addMarketTemplateRequest: AddMarketTemplateRequest
    ): com.tencent.devops.common.api.pojo.Result<Boolean> {
        logger.info("the userId is:$userId,addMarketTemplateRequest is:$addMarketTemplateRequest")
        val templateCode = addMarketTemplateRequest.templateCode
        val category = JsonUtil.toJson(addMarketTemplateRequest.categoryCodeList ?: listOf<String>())
        val projectCodeList = addMarketTemplateRequest.projectCodeList

        val customizeTemplateRecord = templateDao.getLatestTemplate(dslContext, templateCode)
        logger.info("the customizeTemplateRecord is:$customizeTemplateRecord")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            projectCodeList.forEach {
                val templateId = UUIDUtil.generate()
                templateDao.createTemplate(
                    dslContext = context,
                    projectId = it,
                    templateId = templateId,
                    templateName = addMarketTemplateRequest.templateName,
                    versionName = customizeTemplateRecord.versionName,
                    userId = userId,
                    template = null,
                    type = TemplateType.CONSTRAINT.name,
                    category = category,
                    logoUrl = addMarketTemplateRequest.logoUrl,
                    srcTemplateId = templateCode,
                    storeFlag = true,
                    weight = 0
                )
                pipelineSettingDao.insertNewSetting(
                    context,
                    it,
                    templateId,
                    addMarketTemplateRequest.templateName,
                    true
                )
            }
        }
        return com.tencent.devops.common.api.pojo.Result(true)
    }

    fun updateMarketTemplateReference(
        userId: String,
        updateMarketTemplateRequest: AddMarketTemplateRequest
    ): com.tencent.devops.common.api.pojo.Result<Boolean> {
        logger.info("the userId is:$userId,updateMarketTemplateReference Request is:$updateMarketTemplateRequest")
        val templateCode = updateMarketTemplateRequest.templateCode
        val category = JsonUtil.toJson(updateMarketTemplateRequest.categoryCodeList ?: listOf<String>())
        val referenceList = templateDao.listTemplateReference(dslContext, templateCode).map { it["ID"] as String }
        if (referenceList.isNotEmpty()) {
            pipelineSettingDao.updateSettingName(dslContext, referenceList, updateMarketTemplateRequest.templateName)
            templateDao.updateTemplateReference(
                dslContext,
                templateCode,
                updateMarketTemplateRequest.templateName,
                category,
                updateMarketTemplateRequest.logoUrl
            )
        }
        return com.tencent.devops.common.api.pojo.Result(true)
    }

    fun updateTemplateStoreFlag(
        userId: String,
        templateId: String,
        storeFlag: Boolean
    ): com.tencent.devops.common.api.pojo.Result<Boolean> {
        templateDao.updateStoreFlag(dslContext, userId, templateId, storeFlag)
        return com.tencent.devops.common.api.pojo.Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateService::class.java)
        private const val INIT_TEMPLATE_NAME = "init"
    }
}