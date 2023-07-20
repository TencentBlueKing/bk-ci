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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TTemplateRecord
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.USER_TEMPLATE_IMAGE_IS_INVALID
import com.tencent.devops.store.constant.StoreMessageCode.VERSION_PUBLISHED
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.dao.common.StoreStatisticTotalDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.dao.template.TemplateCategoryRelDao
import com.tencent.devops.store.dao.template.TemplateLabelRelDao
import com.tencent.devops.store.pojo.common.CLOSE
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.OPEN
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
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@Suppress("ALL")
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
    lateinit var storeStatisticTotalDao: StoreStatisticTotalDao
    @Autowired
    lateinit var templateNotifyService: TemplateNotifyService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(TemplateReleaseServiceImpl::class.java)

    @Value("\${store.templateApproveSwitch}")
    protected lateinit var templateApproveSwitch: String

    override fun addMarketTemplate(
        userId: String,
        templateCode: String,
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean> {
        logger.info("addMarketTemplate params:[$userId|$templateCode|$marketTemplateRelRequest]")
        // 判断模板代码是否存在
        val codeCount = marketTemplateDao.countByCode(dslContext, templateCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(templateCode),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val templateName = marketTemplateRelRequest.templateName
        // 判断模板名称是否存在
        val nameCount = marketTemplateDao.countByName(dslContext, templateName)
        if (nameCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(templateName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val projectCode = marketTemplateRelRequest.projectCode
        // 校验模板是否合法
        val checkResult = client.get(ServicePTemplateResource::class).checkTemplate(
            userId = userId,
            projectId = projectCode,
            templateId = templateCode
        )
        if (checkResult.isNotOk()) {
            return checkResult
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val templateId = UUIDUtil.generate()
            marketTemplateDao.addMarketTemplate(
                dslContext = context,
                userId = userId,
                templateId = templateId,
                templateCode = templateCode,
                marketTemplateRelRequest = marketTemplateRelRequest
            )
            // 添加模板与项目关联关系，type为0代表新增模板时关联的项目
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = templateCode,
                projectCode = projectCode,
                type = StoreProjectTypeEnum.INIT.type.toByte(),
                storeType = StoreTypeEnum.TEMPLATE.type.toByte()
            )
            // 默认给关联模板的人赋予管理员权限
            storeMemberDao.addStoreMember(
                dslContext = context,
                userId = userId,
                storeCode = templateCode,
                userName = userId,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.TEMPLATE.type.toByte()
            )
            // 初始化统计表数据
            storeStatisticTotalDao.initStatisticData(
                dslContext = context,
                storeCode = templateCode,
                storeType = StoreTypeEnum.TEMPLATE.type.toByte()
            )
            client.get(ServicePTemplateResource::class).updateStoreFlag(userId, templateCode, true)
        }
        return Result(true)
    }

    override fun updateMarketTemplate(
        userId: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?> {
        logger.info("updateMarketTemplate params:[$userId|$marketTemplateUpdateRequest]")
        val templateCode = marketTemplateUpdateRequest.templateCode
        val templateCount = marketTemplateDao.countByCode(dslContext, templateCode)
        val releaseResult = client.get(ServicePTemplateResource::class).checkImageReleaseStatus(userId, templateCode)
        val imageCode = releaseResult.data
        if (!imageCode.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = USER_TEMPLATE_IMAGE_IS_INVALID,
                params = arrayOf(imageCode)
            )
        }
        if (templateCount > 0) {
            val templateName = marketTemplateUpdateRequest.templateName
            // 判断更新的名称是否已存在
            if (validateNameIsExist(templateCode, templateName)) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(templateName),
                    language = I18nUtil.getLanguage(userId)
                )
            }
            val templateRecord = marketTemplateDao.getUpToDateTemplateByCode(dslContext, templateCode)!!
            // 判断最近一个模板版本的状态，如果不是首次发布，则只有处于审核驳回、已发布、上架中止和已下架的插件状态才允许添加新的版本
            val templateFinalStatusList = mutableListOf(
                    TemplateStatusEnum.AUDIT_REJECT.status.toByte(),
                    TemplateStatusEnum.RELEASED.status.toByte(),
                    TemplateStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                    TemplateStatusEnum.UNDERCARRIAGED.status.toByte()
            )
            if (templateCount == 1) {
                // 如果是首次发布，处于初始化的模板状态也允许添加新的版本
                templateFinalStatusList.add(TemplateStatusEnum.INIT.status.toByte())
            }
            if (!templateFinalStatusList.contains(templateRecord.templateStatus)) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = StoreMessageCode.USER_TEMPLATE_VERSION_IS_NOT_FINISH,
                    params = arrayOf(templateRecord.templateName, templateRecord.version),
                    language = I18nUtil.getLanguage(userId)
                )
            }
            val isNormalUpgrade = getNormalUpgradeFlag(
                templateCode = templateRecord.templateCode,
                status = templateRecord.templateStatus.toInt()
            )
            logger.info("updateMarketTemplate isNormalUpgrade is:$isNormalUpgrade")
            val templateStatus = if (isNormalUpgrade) {
                TemplateStatusEnum.RELEASED.status.toByte()
            } else TemplateStatusEnum.AUDITING.status.toByte()
            var templateId = UUIDUtil.generate()
            dslContext.transaction { t ->
                val context = DSL.using(t)
                if (1 == templateCount) {
                    if (templateRecord.version.isNullOrBlank()) {
                        // 首次创建版本
                        templateId = templateRecord.id
                        marketTemplateDao.updateMarketTemplate(
                            dslContext = context,
                            userId = userId,
                            templateId = templateId,
                            version = "1",
                            marketTemplateUpdateRequest = marketTemplateUpdateRequest
                        )
                        // 插入标签
                        val labelIdList = marketTemplateUpdateRequest.labelIdList
                        if (null != labelIdList) {
                            templateLabelRelDao.deleteByTemplateId(context, templateId)
                            if (labelIdList.isNotEmpty()) {
                                templateLabelRelDao.batchAdd(context, userId, templateId, labelIdList)
                            }
                        }
                        // 插入范畴
                        templateCategoryRelDao.deleteByTemplateId(context, templateId)
                        templateCategoryRelDao.batchAdd(
                            dslContext = context,
                            userId = userId,
                            templateId = templateId,
                            categoryIdList = marketTemplateUpdateRequest.categoryIdList
                        )
                        if (templateApproveSwitch == CLOSE) {
                            passTemplateReleaseAndNotify(
                                context = context,
                                userId = userId,
                                templateId = templateId,
                                templateStatus = templateStatus
                            )
                        }
                    } else {
                        // 升级模板
                        upgradeMarketTemplate(
                            templateRecord = templateRecord,
                            context = context,
                            userId = userId,
                            templateId = templateId,
                            templateStatus = templateStatus,
                            marketTemplateUpdateRequest = marketTemplateUpdateRequest
                        )
                    }
                } else {
                    // 升级模板
                    upgradeMarketTemplate(
                        templateRecord = templateRecord,
                        context = context,
                        userId = userId,
                        templateId = templateId,
                        templateStatus = templateStatus,
                        marketTemplateUpdateRequest = marketTemplateUpdateRequest
                    )
                }
            }
            return Result(templateId)
        } else {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(templateCode),
                language = I18nUtil.getLanguage(userId)
            )
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
        logger.info("upgradeMarketTemplate： templateId=$templateId,templateStatus=$templateStatus")
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
            if (labelIdList.isNotEmpty()) {
                templateLabelRelDao.batchAdd(context, userId, templateId, labelIdList)
            }
        }
        // 插入范畴
        templateCategoryRelDao.deleteByTemplateId(context, templateId)
        templateCategoryRelDao.batchAdd(context, userId, templateId, marketTemplateUpdateRequest.categoryIdList)
        if (templateStatus == TemplateStatusEnum.RELEASED.status.toByte()) {
            // 普通升级无需审核
            passTemplateReleaseAndNotify(
                context = context,
                userId = userId,
                templateId = templateId,
                templateStatus = templateStatus
            )
        }
    }

    private fun passTemplateReleaseAndNotify(
        context: DSLContext,
        userId: String,
        templateId: String,
        templateStatus: Byte
    ) {
        val record = marketTemplateDao.getTemplate(dslContext, templateId)!!
        handleTemplateRelease(
            context = context,
            userId = userId,
            approveResult = PASS,
            template = record,
            templateStatus = templateStatus,
            templateStatusMsg = ""
        )
        // 发通知消息
        templateNotifyService.sendTemplateReleaseAuditNotifyMessage(templateId, AuditTypeEnum.AUDIT_SUCCESS)
    }

    override fun handleTemplateRelease(
        context: DSLContext,
        userId: String,
        approveResult: String,
        template: TTemplateRecord,
        templateStatus: Byte,
        templateStatusMsg: String
    ) {
        logger.info("handleTemplateRelease: userId=$userId,templateStatus=$templateStatus")
        val latestFlag = approveResult == PASS
        var pubTime: LocalDateTime? = null
        if (latestFlag) {
            // 判断模板和组件的合法性
            validateTemplateVisibleDept(template.templateCode)
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
        marketTemplateDao.updateTemplateStatusInfo(
            dslContext = context,
            userId = userId,
            templateId = template.id,
            templateStatus = templateStatus,
            templateStatusMsg = templateStatusMsg,
            latestFlag = latestFlag,
            pubTime = pubTime
        )
        if (approveResult == PASS) {
            val categoryRecords = templateCategoryRelDao.getCategorysByTemplateId(dslContext, template.id)
            val categoryCodeList = mutableListOf<String>()
            categoryRecords?.forEach {
                categoryCodeList.add(it[KEY_CATEGORY_CODE] as String)
            }
            val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext = dslContext,
                storeCode = template.templateCode,
                storeType = StoreTypeEnum.TEMPLATE.type.toByte()
            )
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
            val updateMarketTemplateReferenceResult = client.get(ServicePTemplateResource::class)
                .updateMarketTemplateReference("system", addMarketTemplateRequest)
            logger.info("updateMarketTemplateReferenceResult is $updateMarketTemplateReferenceResult")
        }
    }

    abstract fun validateTemplateVisibleDept(templateCode: String)

    private fun validateNameIsExist(
        templateCode: String,
        templateName: String
    ): Boolean {
        var flag = false
        val count = marketTemplateDao.countByName(dslContext, templateName)
        if (count > 0) {
            // 判断模板名称是否重复（模板升级允许名称一样）
            flag = marketTemplateDao.countByName(
                dslContext = dslContext,
                templateCode = templateCode,
                templateName = templateName
            ) < count
        }
        return flag
    }

    /**
     * 获取发布进度
     */
    override fun getProcessInfo(userId: String, templateId: String): Result<StoreProcessInfo> {
        logger.info("getProcessInfo templateId: $templateId")
        val record = marketTemplateDao.getTemplate(dslContext, templateId)
        return if (null == record) {
            I18nUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(templateId),
                language = I18nUtil.getLanguage(userId)
            )
        } else {
            val status = record.templateStatus.toInt()
            val templateCode = record.templateCode
            // 判断用户是否有查询权限
            val queryFlag = storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = templateCode,
                storeType = StoreTypeEnum.TEMPLATE.type.toByte()
            )
            if (!queryFlag) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = GET_INFO_NO_PERMISSION,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(templateCode)
                )
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
                creator = record.creator,
                processInfo = processInfo
            )
            logger.info("getProcessInfo storeProcessInfo: $storeProcessInfo")
            Result(storeProcessInfo)
        }
    }

    private fun getNormalUpgradeFlag(templateCode: String, status: Int): Boolean {
        logger.info("templateApproveSwitch: $templateApproveSwitch")
        return if (templateApproveSwitch == OPEN) {
            val releaseTotalNum = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
            val currentNum = if (status == TemplateStatusEnum.RELEASED.status) 1 else 0
            releaseTotalNum > currentNum
        } else {
            logger.info("no approve required")
            true
        }
    }

    abstract fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem>

    /**
     * 取消发布
     */
    override fun cancelRelease(userId: String, templateId: String): Result<Boolean> {
        logger.info("cancelRelease userId is:$userId, templateId is:$templateId")
        val status = TemplateStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val templateRecord = marketTemplateDao.getTemplate(dslContext, templateId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(templateId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        val templateCode = templateRecord.templateCode
        val creator = templateRecord.creator
        val templateStatus = templateRecord.templateStatus
        // 处于已发布状态的模板不允许取消发布
        if (templateStatus == TemplateStatusEnum.RELEASED.status.toByte()) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_TEMPLATE_RELEASE_STEPS_ERROR,
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 判断用户是否有权限
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, templateCode, StoreTypeEnum.TEMPLATE.type.toByte()) ||
            creator == userId) {
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                data = false,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(templateCode)
            )
        }
        marketTemplateDao.updateTemplateStatusById(dslContext, templateId, status, userId, "cancel release")
        return Result(true)
    }

    /**
     * 下架模板
     */
    override fun offlineTemplate(
        userId: String,
        templateCode: String,
        version: String?,
        reason: String?
    ): Result<Boolean> {
        logger.info("offlineTemplate userId is:$userId, templateCode is:$templateCode,version is:$version")
        // 判断用户是否有权限下架模板
        if (! storeMemberDao.isStoreAdmin(dslContext, userId, templateCode, StoreTypeEnum.TEMPLATE.type.toByte())) {
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(templateCode)
            )
        }
        if (!version.isNullOrEmpty()) {
            val templateRecord = marketTemplateDao.getTemplate(dslContext, templateCode, version.trim())
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("$templateCode:$version"),
                    data = false,
                    language = I18nUtil.getLanguage(userId)
                )
            if (TemplateStatusEnum.RELEASED.status.toByte() != templateRecord.templateStatus) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = VERSION_PUBLISHED,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(templateCode, version)
                )
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
                            val newestUndercarriagedTemplate =
                                marketTemplateDao.getNewestUndercarriagedTemplatesByCode(context, templateCode)
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
            // 把所有已发布的版本全部下架
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
                val newestUndercarriagedTemplate =
                    marketTemplateDao.getNewestUndercarriagedTemplatesByCode(context, templateCode)
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
