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
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.ING
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TTemplateRecord
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.project.api.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.dao.template.TemplateCategoryRelDao
import com.tencent.devops.store.dao.template.TemplateLabelRelDao
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.enums.TemplateStatusEnum
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.pojo.template.MyTemplateItem
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.TemplateProcessInfo
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateTypeEnum
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.template.MarketTemplateService
import com.tencent.devops.store.service.template.MarketTemplateStatisticService
import com.tencent.devops.store.service.template.TemplateCategoryService
import com.tencent.devops.store.service.template.TemplateLabelService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

abstract class MarketTemplateServiceImpl @Autowired constructor() : MarketTemplateService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var marketTemplateDao: MarketTemplateDao
    @Autowired
    lateinit var classifyDao: ClassifyDao
    @Autowired
    lateinit var templateLabelRelDao: TemplateLabelRelDao
    @Autowired
    lateinit var templateCategoryRelDao: TemplateCategoryRelDao
    @Autowired
    lateinit var storeStatisticDao: StoreStatisticDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var templateCategoryService: TemplateCategoryService
    @Autowired
    lateinit var templateLabelService: TemplateLabelService
    @Autowired
    lateinit var marketTemplateStatisticService: MarketTemplateStatisticService
    @Autowired
    lateinit var storeUserService: StoreUserService
    @Autowired
    lateinit var storeProjectService: StoreProjectService
    @Autowired
    lateinit var storeCommentService: StoreCommentService
    @Autowired
    lateinit var classifyService: ClassifyService
    @Autowired
    lateinit var client: Client

    companion object {
        private val logger = LoggerFactory.getLogger(MarketTemplateServiceImpl::class.java)
        private val executor = Executors.newFixedThreadPool(30)
    }

    protected fun doList(
        name: String?,
        classifyCode: String?,
        category: String?,
        labelCode: String?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Future<MarketTemplateResp> {
        return executor.submit(Callable<MarketTemplateResp> {
            val results = mutableListOf<MarketItem>()
            // 获取模版
            val categoryList = if (category.isNullOrEmpty()) listOf() else category?.split(",")
            val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")
            val count =
                marketTemplateDao.count(dslContext, name, classifyCode, categoryList, labelCodeList, score, rdType)
            val templates = marketTemplateDao.list(
                dslContext, name, classifyCode, categoryList,
                labelCodeList, score, rdType, sortType, desc, page, pageSize
            )
                ?: return@Callable MarketTemplateResp(0, page, pageSize, results)
            logger.info("[list]get templates: $templates")

            val templateCodeList = templates.map {
                it["TEMPLATE_CODE"] as String
            }.toList()

            // 获取统计数据
            val templateStatisticData = marketTemplateStatisticService.getStatisticByCodeList(templateCodeList).data
            logger.info("[list]get statisticData")

            // 获取分类
            val classifyList = classifyService.getAllClassify(StoreTypeEnum.TEMPLATE.type.toByte()).data
            val classifyMap = mutableMapOf<String, String>()
            classifyList?.forEach {
                classifyMap[it.id] = it.classifyCode
            }

            templates.forEach {
                val code = it["TEMPLATE_CODE"] as String
                val statistic = templateStatisticData?.get(code)

                val classifyId = it["CLASSIFY_ID"] as String
                results.add(
                    MarketItem(
                        id = it["ID"] as String,
                        name = it["TEMPLATE_NAME"] as String,
                        code = code,
                        type = "",
                        rdType = TemplateRdTypeEnum.getTemplateRdType((it["TEMPLATE_RD_TYPE"] as Byte).toInt()),
                        classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] else "",
                        logoUrl = it["LOGO_URL"] as? String,
                        publisher = it["PUBLISHER"] as String,
                        os = null,
                        downloads = statistic?.downloads
                            ?: 0,
                        score = statistic?.score
                            ?: 0.toDouble(),
                        summary = it["SUMMARY"] as? String,
                        flag = null,
                        publicFlag = it["PUBLIC_FLAG"] as Boolean,
                        buildLessRunFlag = null,
                        docsLink = null
                    )
                )
            }

            logger.info("[list]end")
            return@Callable MarketTemplateResp(count, page, pageSize, results)
        })
    }

    /**
     * 模版市场，首页
     */
    abstract override fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<MarketTemplateMain>>

    abstract override fun list(
        userId: String,
        name: String?,
        classifyCode: String?,
        category: String?,
        labelCode: String?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): MarketTemplateResp

    override fun getTemplateDetailByCode(userId: String, templateCode: String): Result<TemplateDetail?> {
        logger.info("the templateCode is :$templateCode")
        val templateRecord = marketTemplateDao.getLatestTemplateByCode(dslContext, templateCode)
        logger.info("the templateRecord is :$templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(templateCode)
            )
        }
        val templateDetail = getTemplateDetail(templateRecord, userId)
        return Result(templateDetail)
    }

    override fun getTemplateDetailById(userId: String, templateId: String): Result<TemplateDetail?> {
        logger.info("the templateId is :$templateId")
        val templateRecord = marketTemplateDao.getTemplate(dslContext, templateId)
        logger.info("the templateRecord is :$templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(templateId)
            )
        }
        val templateDetail = getTemplateDetail(templateRecord, userId)
        return Result(templateDetail)
    }

    private fun getTemplateDetail(templateRecord: TTemplateRecord, userId: String): TemplateDetail {
        val templateCode = templateRecord.templateCode
        val templateClassifyRecord = classifyDao.getClassify(dslContext, templateRecord.classifyId)
        val templateStatisticRecord = storeStatisticDao.getStatisticByStoreCode(
            dslContext, templateCode,
            StoreTypeEnum.TEMPLATE.type.toByte()
        )
        val downloads = templateStatisticRecord.value1()?.toInt()
        val comments = templateStatisticRecord.value2()?.toInt()
        val score = templateStatisticRecord.value3()?.toDouble()
        val avgScore: Double =
            if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble() // 计算平均分
        val categoryList = templateCategoryService.getCategorysByTemplateId(templateRecord.id).data // 查找范畴列表
        val labelList = templateLabelService.getLabelsByTemplateId(templateRecord.id).data // 查找标签列表
        val publicFlag = templateRecord.publicFlag // 是否为公共模板
        val installFlag = storeUserService.isCanInstallStoreComponent(
            publicFlag,
            userId,
            templateCode,
            StoreTypeEnum.TEMPLATE
        ) // 是否能安装
        var releaseFlag = false // 是否有处于上架状态的模板版本
        val count = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
        if (count > 0) {
            releaseFlag = true
        }
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, templateCode, StoreTypeEnum.TEMPLATE)
        return TemplateDetail(
            templateId = templateRecord.id,
            templateCode = templateCode,
            templateName = templateRecord.templateName,
            logoUrl = templateRecord.logoUrl,
            classifyCode = templateClassifyRecord?.classifyCode,
            classifyName = templateClassifyRecord?.classifyName,
            downloads = downloads ?: 0,
            score = String.format("%.1f", avgScore).toDouble(),
            summary = templateRecord.summary,
            templateStatus = TemplateStatusEnum.getTemplateStatus(templateRecord.templateStatus.toInt()),
            description = templateRecord.description,
            version = templateRecord.version,
            templateType = TemplateTypeEnum.getTemplateType(templateRecord.templateType.toInt()),
            templateRdType = TemplateRdTypeEnum.getTemplateRdType(templateRecord.templateRdType.toInt()),
            categoryList = categoryList,
            labelList = labelList,
            latestFlag = templateRecord.latestFlag,
            publisher = templateRecord.publisher,
            pubDescription = templateRecord.pubDescription,
            flag = installFlag,
            releaseFlag = releaseFlag,
            userCommentInfo = userCommentInfo
        )
    }

    override fun addMarketTemplate(
        userId: String,
        templateCode: String,
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean> {
        logger.info("userId :$userId,templateCode :$templateCode,marketTemplateRelRequest :$marketTemplateRelRequest")
        // 判断模板代码是否存在
        val codeCount = marketTemplateDao.countByCode(dslContext, templateCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(templateCode), false
            )
        }
        val templateName = marketTemplateRelRequest.templateName
        // 判断模板名称是否存在
        val nameCount = marketTemplateDao.countByName(dslContext, templateName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(templateName), false
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val templateId = UUIDUtil.generate()
            marketTemplateDao.addMarketTemplate(context, userId, templateId, templateCode, marketTemplateRelRequest)
            storeProjectRelDao.addStoreProjectRel(
                context, userId, templateCode, marketTemplateRelRequest.projectCode,
                0, StoreTypeEnum.TEMPLATE.type.toByte()
            ) // 添加模板与项目关联关系，type为0代表新增插件时关联的项目
            storeMemberDao.addStoreMember(
                context, userId, templateCode, userId, 0,
                StoreTypeEnum.TEMPLATE.type.toByte()
            ) // 默认给关联模板的人赋予管理员权限

            client.get(ServicePTemplateResource::class).updateStoreFlag(userId, templateCode, true)
        }
        return Result(true)
    }

    override fun updateMarketTemplate(
        userId: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?> {
        logger.info("the userId is :$userId,marketTemplateUpdateRequest is :$marketTemplateUpdateRequest")
        val templateCode = marketTemplateUpdateRequest.templateCode
        val templateRecords = marketTemplateDao.getTemplatesByTemplateCode(dslContext, templateCode)
        logger.info("the templateRecords is :$templateRecords")
        if (null != templateRecords && templateRecords.size > 0) {
            val templateName = marketTemplateUpdateRequest.templateName
            // 判断更新的名称是否已存在
            val count = marketTemplateDao.countByName(dslContext, templateName)
            if (validateNameIsExist(
                    count,
                    templateRecords,
                    templateName
                )
            ) return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(templateName)
            )
            val templateRecord = templateRecords[0]
            if (templateRecords.size > 1) {
                // 判断最近一个模板版本的状态，只有处于已发布、上架中止和已下架的状态才允许添加新的版本
                val templateFinalStatusList = listOf(
                    TemplateStatusEnum.RELEASED.status.toByte(),
                    TemplateStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                    TemplateStatusEnum.UNDERCARRIAGED.status.toByte()
                )
                if (!templateFinalStatusList.contains(templateRecord.templateStatus)) {
                    return MessageCodeUtil.generateResponseDataObject(
                        StoreMessageCode.USER_TEMPLATE_VERSION_IS_NOT_FINISH,
                        arrayOf(templateRecord.templateName, templateRecord.version)
                    )
                }
            }

            var templateId = UUIDUtil.generate()
            dslContext.transaction { t ->
                val context = DSL.using(t)
                if (1 == templateRecords.size) {
                    if (StringUtils.isEmpty(templateRecord.version)) {
                        // 首次创建版本
                        templateId = templateRecord.id
                        marketTemplateDao.updateMarketTemplate(
                            context,
                            userId,
                            templateId,
                            "1",
                            marketTemplateUpdateRequest
                        )
                        // 插入标签
                        templateLabelRelDao.deleteByTemplateId(context, templateId)
                        templateLabelRelDao.batchAdd(
                            context,
                            userId,
                            templateId,
                            marketTemplateUpdateRequest.labelIdList
                        )
                        // 插入范畴
                        templateCategoryRelDao.deleteByTemplateId(context, templateId)
                        templateCategoryRelDao.batchAdd(
                            context,
                            userId,
                            templateId,
                            marketTemplateUpdateRequest.categoryIdList
                        )
                    } else {
                        // 升级模板
                        upgradeMarketTemplate(templateRecord, context, userId, templateId, marketTemplateUpdateRequest)
                    }
                } else {
                    // 升级模板
                    upgradeMarketTemplate(templateRecord, context, userId, templateId, marketTemplateUpdateRequest)
                }
            }
            return Result(templateId)
        } else {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(templateCode)
            )
        }
    }

    /**
     * 删除模版关联关系
     */
    override fun delete(userId: String, templateCode: String): Result<Boolean> {
        logger.info("to delete, userId: $userId | templateCode: $templateCode")
        val templateRecord = marketTemplateDao.getLatestTemplateByCode(dslContext, templateCode)
        logger.info("the templateRecord is :$templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(templateCode)
            )
        }
        val type = StoreTypeEnum.TEMPLATE.type.toByte()
        val isOwner = storeMemberDao.isStoreAdmin(dslContext, userId, templateCode, type)
        if (!isOwner) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PERMISSION_DENIED,
                arrayOf(templateRecord.templateName)
            )
        }
        val releasedCnt = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
        if (releasedCnt > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_TEMPLATE_RELEASED,
                arrayOf(templateRecord.templateName)
            )
        }
        logger.info("releasedCnt: $releasedCnt")
        // 如果已经被安装到其他项目下使用，不能删除关联
        val installedCnt = storeProjectRelDao.countInstalledProject(dslContext, templateCode, type)
        if (installedCnt > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_TEMPLATE_USED,
                arrayOf(templateCode)
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            client.get(ServicePTemplateResource::class).updateStoreFlag(userId, templateCode, false)
            storeMemberDao.deleteAll(context, templateCode, type)
            storeProjectRelDao.deleteAllRel(context, templateCode, type)
            marketTemplateDao.delete(context, templateCode)
        }
        return Result(true)
    }

    private fun upgradeMarketTemplate(
        templateRecord: TTemplateRecord,
        context: DSLContext,
        userId: String,
        templateId: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ) {
        val dbVersion = templateRecord.version
        val version = (dbVersion.toInt() + 1).toString() // 每次升级模板版本号加1

        marketTemplateDao.cleanLatestFlag(dslContext, templateRecord.templateCode)

        marketTemplateDao.upgradeMarketTemplate(
            context,
            userId,
            templateId,
            version,
            templateRecord,
            marketTemplateUpdateRequest
        )
        // 插入标签
        templateLabelRelDao.deleteByTemplateId(context, templateId)
        templateLabelRelDao.batchAdd(context, userId, templateId, marketTemplateUpdateRequest.labelIdList)
        // 插入范畴
        templateCategoryRelDao.deleteByTemplateId(context, templateId)
        templateCategoryRelDao.batchAdd(context, userId, templateId, marketTemplateUpdateRequest.categoryIdList)
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
    override fun getProcessInfo(TemplateId: String): Result<TemplateProcessInfo> {
        val record = marketTemplateDao.getTemplate(dslContext, TemplateId)
        return if (null == record) {
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(TemplateId))
        } else {
            val status = record["TEMPLATE_STATUS"].toString().toInt()
            val processInfo = handleProcessInfo(status)
            val atomProcessInfo = TemplateProcessInfo(processInfo)
            Result(atomProcessInfo)
        }
    }

    abstract fun handleProcessInfo(status: Int): List<ReleaseProcessItem>

    /**
     * 设置进度
     */
    protected fun setProcessInfo(
        processInfo: List<ReleaseProcessItem>,
        totalStep: Int,
        currStep: Int,
        status: String
    ): Boolean {
        for (item in processInfo) {
            if (item.step < currStep) {
                item.status = SUCCESS
            } else if (item.step == currStep) {
                if (currStep == totalStep) {
                    item.status = SUCCESS
                } else {
                    item.status = status
                    item.name += if (status == DOING) MessageCodeUtil.getCodeLanMessage(ING) else MessageCodeUtil.getCodeLanMessage(
                        FAIL
                    )
                }
            }
        }
        return true
    }

    /**
     * 安装模板到项目
     */
    override fun installTemplate(
        accessToken: String,
        userId: String,
        projectCodeList: ArrayList<String>,
        templateCode: String
    ): Result<Boolean> {
        logger.info("templateCode is $templateCode")
        val template = marketTemplateDao.getLatestTemplateByCode(dslContext, templateCode)
        logger.info("template is $template")
        if (null == template) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(templateCode),
                false
            )
        }

        val categoryRecords = templateCategoryRelDao.getCategorysByTemplateId(dslContext, template.id)
        val categoryCodeList = mutableListOf<String>()
        categoryRecords?.forEach {
            categoryCodeList.add(it["categoryCode"] as String)
        }
        val addMarketTemplateRequest = AddMarketTemplateRequest(
            projectCodeList = projectCodeList,
            templateCode = templateCode,
            templateName = template.templateName,
            logoUrl = template.logoUrl,
            categoryCodeList = categoryCodeList,
            publicFlag = template.publicFlag,
            publisher = template.publisher
        )
        val addMarketTemplateResult =
            client.get(ServicePTemplateResource::class).addMarketTemplate(userId, addMarketTemplateRequest)
        logger.info("addMarketTemplateResult is $addMarketTemplateResult")
        if (addMarketTemplateResult.isNotOk()) {
            // 抛出错误提示
            return Result(addMarketTemplateResult.status, addMarketTemplateResult.message ?: "")
        }
        return storeProjectService.installStoreComponent(
            accessToken, userId, projectCodeList, template.id,
            template.templateCode, StoreTypeEnum.TEMPLATE, template.publicFlag
        )
    }

    override fun getMyTemplates(
        userId: String,
        templateName: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<MyTemplateItem>?> {
        val records = marketTemplateDao.getMyTemplates(dslContext, userId, templateName, page, pageSize)
        // 获取项目代码对应的名称
        val projectCodeList = mutableListOf<String>()
        records?.forEach {
            projectCodeList.add(it["projectCode"] as String)
        }
        val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        val templateList = mutableListOf<MyTemplateItem>()
        records?.forEach {
            val templateCode = it["templateCode"] as String
            var releaseFlag = false // 是否有处于上架状态的模板版本
            val count = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
            if (count > 0) {
                releaseFlag = true
            }
            templateList.add(
                MyTemplateItem(
                    templateId = it["templateId"] as String,
                    templateCode = templateCode,
                    templateName = it["templateName"] as String,
                    logoUrl = it["logoUrl"] as? String,
                    version = it["version"] as String,
                    templateStatus = TemplateStatusEnum.getTemplateStatus((it["templateStatus"] as Byte).toInt()),
                    projectCode = it["projectCode"] as String,
                    projectName = projectMap?.get(it["projectCode"] as String) as String,
                    releaseFlag = releaseFlag,
                    creator = it["creator"] as String,
                    modifier = it["modifier"] as String,
                    createTime = (it["createTime"] as LocalDateTime).timestampmilli(),
                    updateTime = (it["updateTime"] as LocalDateTime).timestampmilli()
                )
            )
        }
        val templateCount = marketTemplateDao.getMyTemplatesCount(dslContext, userId, templateName)
        val totalPages = PageUtil.calTotalPage(pageSize, templateCount)
        return Result(
            Page(
                count = templateCount,
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                records = templateList
            )
        )
    }

    /**
     * 取消发布
     */
    override fun cancelRelease(userId: String, templateId: String): Result<Boolean> {
        logger.info("the userId is:$userId, templateId is:$templateId")
        val status = TemplateStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val templateRecord = marketTemplateDao.getTemplate(dslContext, templateId)
        logger.info("templateRecord is $templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(templateId),
                false
            )
        }
        val templateCode = templateRecord.templateCode
        val creator = templateRecord.creator
        val templateStatus = templateRecord.templateStatus
        // 处于已发布状态的模板不允许取消发布
        if (templateStatus == TemplateStatusEnum.RELEASED.status.toByte()) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_TEMPLATE_RELEASE_STEPS_ERROR, false)
        }
        // 判断用户是否有权限
        if (!(storeMemberDao.isStoreAdmin(
                dslContext,
                userId,
                templateCode,
                StoreTypeEnum.TEMPLATE.type.toByte()
            ) || creator == userId)
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, false)
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
        logger.info("the userId is:$userId, templateCode is:$templateCode,version is:$version")
        // 判断用户是否有权限下架模板
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, templateCode, StoreTypeEnum.TEMPLATE.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        if (!version.isNullOrEmpty()) {
            val templateRecord = marketTemplateDao.getTemplate(dslContext, templateCode, version!!.trim())
            logger.info("templateRecord is $templateRecord")
            if (null == templateRecord) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_INVALID,
                    arrayOf("$templateCode:$version"),
                    false
                )
            }
            marketTemplateDao.updateTemplateStatusById(
                dslContext,
                templateRecord.id,
                TemplateStatusEnum.UNDERCARRIAGED.status.toByte(),
                userId,
                reason
            )
        } else {
            // 把模板所有已发布的版本全部下架
            marketTemplateDao.updateTemplateStatusByCode(
                dslContext, templateCode, TemplateStatusEnum.RELEASED.status.toByte(),
                TemplateStatusEnum.UNDERCARRIAGED.status.toByte(), userId, "undercarriage"
            )
        }

        return Result(true)
    }

    /**
     * 根据模板ID和模板代码判断模板是否存在
     */
    override fun judgeTemplateExistByIdAndCode(templateId: String, templateCode: String): Result<Boolean> {
        logger.info("the templateId is:$templateId, templateCode is:$templateCode")
        val count = marketTemplateDao.countByIdAndCode(dslContext, templateId, templateCode)
        if (count < 1) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf("templateId:$templateId,templateCode:$templateCode"), false
            )
        }
        return Result(true)
    }
}