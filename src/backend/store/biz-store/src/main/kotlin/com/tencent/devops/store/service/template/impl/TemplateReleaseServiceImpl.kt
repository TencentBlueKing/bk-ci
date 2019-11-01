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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TTemplateRecord
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.dao.template.TemplateCategoryRelDao
import com.tencent.devops.store.dao.template.TemplateLabelRelDao
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.template.TemplateNotifyService
import com.tencent.devops.store.service.template.TemplateReleaseService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
import java.time.LocalDateTime

abstract class TemplateReleaseServiceImpl @Autowired constructor() : TemplateReleaseService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var marketTemplateDao: MarketTemplateDao
    @Autowired
    lateinit var templateLabelRelDao: TemplateLabelRelDao
    @Autowired
    lateinit var templateCategoryRelDao: TemplateCategoryRelDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var storeReleaseDao: StoreReleaseDao
    @Autowired
    lateinit var templateNotifyService: TemplateNotifyService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(TemplateReleaseServiceImpl::class.java)

    override fun addMarketTemplate(userId: String, templateCode: String, marketTemplateRelRequest: MarketTemplateRelRequest): Result<Boolean> {
        logger.info("the userId is :$userId,templateCode is :$templateCode,marketTemplateRelRequest is :$marketTemplateRelRequest")
        // 判断模板代码是否存在
        val codeCount = marketTemplateDao.countByCode(dslContext, templateCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(templateCode), false)
        }
        val templateName = marketTemplateRelRequest.templateName
        // 判断模板名称是否存在
        val nameCount = marketTemplateDao.countByName(dslContext, templateName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(templateName), false)
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val templateId = UUIDUtil.generate()
            marketTemplateDao.addMarketTemplate(context, userId, templateId, templateCode, marketTemplateRelRequest)
            // 添加模板与项目关联关系，type为0代表新增模板时关联的项目
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = templateCode,
                projectCode = marketTemplateRelRequest.projectCode,
                type = StoreProjectTypeEnum.INIT.type.toByte(),
                storeType = StoreTypeEnum.TEMPLATE.type.toByte()
            )
            // 默认给关联模板的人赋予管理员权限
            storeMemberDao.addStoreMember(context, userId, templateCode, userId, 0, StoreTypeEnum.TEMPLATE.type.toByte())
            storeMemberDao.addStoreMember(
                dslContext = context,
                userId = userId,
                storeCode = templateCode,
                userName = userId,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.TEMPLATE.type.toByte()
            )
            client.get(ServiceTemplateResource::class).updateStoreFlag(userId, templateCode, true)
        }
        return Result(true)
    }

    override fun updateMarketTemplate(userId: String, marketTemplateUpdateRequest: MarketTemplateUpdateRequest): Result<String?> {
        logger.info("the userId is :$userId,marketTemplateUpdateRequest is :$marketTemplateUpdateRequest")
        val templateCode = marketTemplateUpdateRequest.templateCode
        val templateRecords = marketTemplateDao.getTemplatesByTemplateCode(dslContext, templateCode)
        logger.info("the templateRecords is :$templateRecords")
        if (null != templateRecords && templateRecords.size > 0) {
            val templateName = marketTemplateUpdateRequest.templateName
            // 判断更新的名称是否已存在
            val count = marketTemplateDao.countByName(dslContext, templateName)
            if (validateNameIsExist(count, templateRecords, templateName)) return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf(templateName)
            )
            val templateRecord = templateRecords[0]
            if (templateRecords.size > 1) {
                // 判断最近一个模板版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许添加新的版本
                val templateFinalStatusList = listOf(
                    TemplateStatusEnum.AUDIT_REJECT.status.toByte(),
                    TemplateStatusEnum.RELEASED.status.toByte(),
                    TemplateStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                    TemplateStatusEnum.UNDERCARRIAGED.status.toByte()
                )
                if (!templateFinalStatusList.contains(templateRecord.templateStatus)) {
                    return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_TEMPLATE_VERSION_IS_NOT_FINISH, arrayOf(templateRecord.templateName, templateRecord.version))
                }
            }
            // todo 检查源模板模型的合法性
            val isNormalUpgrade = getNormalUpgradeFlag(templateRecord.templateCode, templateRecord.templateStatus.toInt())
            logger.info("updateMarketTemplate isNormalUpgrade is:$isNormalUpgrade")
            val templateStatus = if (isNormalUpgrade) TemplateStatusEnum.RELEASED.status.toByte() else TemplateStatusEnum.AUDITING.status.toByte()
            var templateId = UUIDUtil.generate()
            dslContext.transaction { t ->
                val context = DSL.using(t)
                if (1 == templateRecords.size) {
                    if (StringUtils.isEmpty(templateRecord.version)) {
                        // 首次创建版本
                        templateId = templateRecord.id
                        marketTemplateDao.updateMarketTemplate(context, userId, templateId, "1", marketTemplateUpdateRequest)
                        // 插入标签
                        val labelIdList = marketTemplateUpdateRequest.labelIdList
                        if (null != labelIdList) {
                            templateLabelRelDao.deleteByTemplateId(context, templateId)
                            if (labelIdList.isNotEmpty())
                            templateLabelRelDao.batchAdd(context, userId, templateId, labelIdList)
                        }
                        // 插入范畴
                        templateCategoryRelDao.deleteByTemplateId(context, templateId)
                        templateCategoryRelDao.batchAdd(context, userId, templateId, marketTemplateUpdateRequest.categoryIdList)
                    } else {
                        // 升级模板
                        upgradeMarketTemplate(templateRecord, context, userId, templateId, templateStatus, marketTemplateUpdateRequest)
                    }
                } else {
                    // 升级模板
                    upgradeMarketTemplate(templateRecord, context, userId, templateId, templateStatus, marketTemplateUpdateRequest)
                }
            }
            return Result(templateId)
        } else {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(templateCode))
        }
    }

    private fun upgradeMarketTemplate(
        templateRecord: TTemplateRecord,
        context: DSLContext,
        userId: String,
        templateId: String,
        templateStatus: Byte,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ) {
        val dbVersion = templateRecord.version
        val version = (dbVersion.toInt() + 1).toString() // 每次升级模板版本号加1
        marketTemplateDao.upgradeMarketTemplate(
            dslContext = context,
            userId = userId,
            templateId = templateId,
            templateStatus = templateStatus,
            version = version,
            templateRecord = templateRecord,
            marketTemplateUpdateRequest = marketTemplateUpdateRequest
        )
        // 插入标签
        val labelIdList = marketTemplateUpdateRequest.labelIdList
        if (null != labelIdList) {
            templateLabelRelDao.deleteByTemplateId(context, templateId)
            if (labelIdList.isNotEmpty())
            templateLabelRelDao.batchAdd(context, userId, templateId, labelIdList)
        }
        // 插入范畴
        templateCategoryRelDao.deleteByTemplateId(context, templateId)
        templateCategoryRelDao.batchAdd(context, userId, templateId, marketTemplateUpdateRequest.categoryIdList)
        if (templateStatus == TemplateStatusEnum.RELEASED.status.toByte()) {
            // 普通升级无需审核
            val upgradeTemplateRecord = marketTemplateDao.getTemplate(dslContext, templateId)!!
            handleTemplateRelease(
                context = context,
                userId = userId,
                approveResult = PASS,
                template = upgradeTemplateRecord,
                templateStatus = templateStatus,
                templateStatusMsg = ""
                )
            // 发通知消息
            templateNotifyService.sendTemplateReleaseAuditNotifyMessage(templateId, AuditTypeEnum.AUDIT_SUCCESS)
        }
    }

    override fun handleTemplateRelease(
        context: DSLContext,
        userId: String,
        approveResult: String,
        template: TTemplateRecord,
        templateStatus: Byte,
        templateStatusMsg: String
    ) {
        val latestFlag = approveResult == PASS
        var pubTime: LocalDateTime? = null
        if (latestFlag) {
            // 清空旧版本LATEST_FLAG
            marketTemplateDao.cleanLatestFlag(context, template.templateCode)
            pubTime = LocalDateTime.now()
            // 记录发布信息
            storeReleaseDao.addStoreReleaseInfo(
                dslContext = context,
                userId = userId,
                storeReleaseCreateRequest = StoreReleaseCreateRequest(
                    storeCode = template.templateCode,
                    storeType = StoreTypeEnum.TEMPLATE,
                    latestUpgrader = template.creator,
                    latestUpgradeTime = pubTime
                )
            )
        }
        // 入库信息，并设置当前版本的LATEST_FLAG
        marketTemplateDao.updateTemplateStatusInfo(context, userId, template.id, templateStatus, templateStatusMsg, latestFlag, pubTime)
        if (approveResult == PASS) {
            val categoryRecords = templateCategoryRelDao.getCategorysByTemplateId(dslContext, template.id)
            val categoryCodeList = mutableListOf<String>()
            categoryRecords?.forEach {
                categoryCodeList.add(it["categoryCode"] as String)
            }
            val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(dslContext, template.templateCode, StoreTypeEnum.TEMPLATE.type.toByte())
            val addMarketTemplateRequest = AddMarketTemplateRequest(
                projectCodeList = arrayListOf(projectCode!!),
                templateCode = template.templateCode,
                templateName = template.templateName,
                logoUrl = template.logoUrl,
                categoryCodeList = categoryCodeList,
                publicFlag = template.publicFlag,
                publisher = template.publisher
            )
            logger.info("addMarketTemplateRequest is $addMarketTemplateRequest")
            val updateMarketTemplateReferenceResult = client.get(ServiceTemplateResource::class).updateMarketTemplateReference("system", addMarketTemplateRequest)
            logger.info("updateMarketTemplateReferenceResult is $updateMarketTemplateReferenceResult")
        }
    }

    private fun validateNameIsExist(
        count: Int,
        templateRecords: org.jooq.Result<TTemplateRecord>,
        templateName: String
    ): Boolean {
        var flag = false
        if (count > 0) {
            for (item in templateRecords) {
                if (templateName == item.templateName) {
                    flag = true
                    break
                }
            }
            if (!flag) {
                return true
            }
        }
        return false
    }

    /**
     * 获取发布进度
     */
    override fun getProcessInfo(userId: String, templateId: String): Result<StoreProcessInfo> {
        logger.info("getProcessInfo templateId: $templateId")
        val record = marketTemplateDao.getTemplate(dslContext, templateId)
        logger.info("getProcessInfo record: $record")
        return if (null == record) {
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(templateId))
        } else {
            val status = record.templateStatus.toInt()
            val templateCode = record.templateCode
            // 判断用户是否有查询权限
            val queryFlag = storeMemberDao.isStoreMember(dslContext, userId, templateCode, StoreTypeEnum.TEMPLATE.type.toByte())
            if (!queryFlag) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
            }
            // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
            val isNormalUpgrade = getNormalUpgradeFlag(templateCode, status)
            logger.info("getProcessInfo isNormalUpgrade: $isNormalUpgrade")
            val processInfo = handleProcessInfo(isNormalUpgrade, status)
            val storeProcessInfo = storeCommonService.generateStoreProcessInfo(
                userId = userId,
                storeId = templateId,
                storeCode = templateCode,
                storeType = StoreTypeEnum.TEMPLATE,
                modifier = record.modifier,
                processInfo = processInfo
            )
            logger.info("getProcessInfo storeProcessInfo: $storeProcessInfo")
            Result(storeProcessInfo)
        }
    }

    private fun getNormalUpgradeFlag(templateCode: String, status: Int): Boolean {
        val releaseTotalNum = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
        val currentNum = if (status == TemplateStatusEnum.RELEASED.status) 1 else 0
        return releaseTotalNum > currentNum
    }

    abstract fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem>

    /**
     * 取消发布
     */
    override fun cancelRelease(userId: String, templateId: String): Result<Boolean> {
        logger.info("the userId is:$userId, templateId is:$templateId")
        val status = TemplateStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val templateRecord = marketTemplateDao.getTemplate(dslContext, templateId)
        logger.info("templateRecord is $templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(templateId), false)
        }
        val templateCode = templateRecord.templateCode
        val creator = templateRecord.creator
        val templateStatus = templateRecord.templateStatus
        // 处于已发布状态的模板不允许取消发布
        if (templateStatus == TemplateStatusEnum.RELEASED.status.toByte()) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_TEMPLATE_RELEASE_STEPS_ERROR, false)
        }
        // 判断用户是否有权限
        if (! (storeMemberDao.isStoreAdmin(dslContext, userId, templateCode, StoreTypeEnum.TEMPLATE.type.toByte()) || creator == userId)) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, false)
        }
        marketTemplateDao.updateTemplateStatusById(dslContext, templateId, status, userId, "cancel release")
        return Result(true)
    }

    /**
     * 下架模板
     */
    override fun offlineTemplate(userId: String, templateCode: String, version: String?, reason: String?): Result<Boolean> {
        logger.info("offlineTemplate userId is:$userId, templateCode is:$templateCode,version is:$version")
        // 判断用户是否有权限下架模板
        if (! storeMemberDao.isStoreAdmin(dslContext, userId, templateCode, StoreTypeEnum.TEMPLATE.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        if (!version.isNullOrEmpty()) {
            val templateRecord = marketTemplateDao.getTemplate(dslContext, templateCode, version!!.trim())
            logger.info("templateRecord is $templateRecord")
            if (null == templateRecord) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("$templateCode:$version"), false)
            }
            if (TemplateStatusEnum.RELEASED.status.toByte() != templateRecord.templateStatus) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                val releaseTemplateRecords = marketTemplateDao.getReleaseTemplatesByCode(context, templateCode)
                if (null != releaseTemplateRecords && releaseTemplateRecords.size > 0) {
                    marketTemplateDao.updateTemplateStatusInfo(
                        dslContext = context,
                        userId = userId,
                        templateId = templateRecord.id,
                        templateStatus = TemplateStatusEnum.UNDERCARRIAGED.status.toByte(),
                        templateStatusMsg = reason ?: "",
                        latestFlag = false
                    )
                    val newestReleaseTemplateRecord = releaseTemplateRecords[0]
                    if (newestReleaseTemplateRecord.id == templateRecord.id) {
                        if (releaseTemplateRecords.size == 1) {
                            val newestUndercarriagedTemplate = marketTemplateDao.getNewestUndercarriagedTemplatesByCode(context, templateCode)
                            if (null != newestUndercarriagedTemplate) {
                                marketTemplateDao.updateTemplateStatusById(
                                    dslContext = context,
                                    templateId = newestUndercarriagedTemplate.id,
                                    templateStatus = TemplateStatusEnum.UNDERCARRIAGED.status.toByte(),
                                    userId = userId,
                                    latestFlag = true
                                )
                            }
                        } else {
                            // 把前一个发布的版本的latestFlag置为true
                            val tmpTemplateRecord = releaseTemplateRecords[1]
                            marketTemplateDao.updateTemplateStatusById(
                                dslContext = context,
                                templateId = tmpTemplateRecord.id,
                                templateStatus = TemplateStatusEnum.RELEASED.status.toByte(),
                                userId = userId,
                                latestFlag = true
                            )
                        }
                    }
                }
            }
        } else {
            // 把IDE插件所有已发布的版本全部下架
            dslContext.transaction { t ->
                val context = DSL.using(t)
                marketTemplateDao.updateTemplateStatusByCode(
                    dslContext = context,
                    templateCode = templateCode,
                    templateOldStatus = TemplateStatusEnum.RELEASED.status.toByte(),
                    templateNewStatus = TemplateStatusEnum.UNDERCARRIAGED.status.toByte(),
                    userId = userId,
                    msg = "undercarriage",
                    latestFlag = false
                )
                val newestUndercarriagedTemplate = marketTemplateDao.getNewestUndercarriagedTemplatesByCode(context, templateCode)
                if (null != newestUndercarriagedTemplate) {
                    // 把发布时间最晚的下架版本latestFlag置为true
                    marketTemplateDao.updateTemplateStatusById(
                        dslContext = context,
                        templateId = newestUndercarriagedTemplate.id,
                        templateStatus = TemplateStatusEnum.UNDERCARRIAGED.status.toByte(),
                        userId = userId,
                        latestFlag = true
                    )
                }
            }
        }
        return Result(true)
    }
}
