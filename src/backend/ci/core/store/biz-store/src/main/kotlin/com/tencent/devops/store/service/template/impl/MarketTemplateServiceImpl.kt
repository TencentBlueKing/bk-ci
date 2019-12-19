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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.model.store.tables.records.TTemplateRecord
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.dao.template.TemplateCategoryRelDao
import com.tencent.devops.store.pojo.atom.MarketMainItemLabel
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.MyTemplateItem
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.template.MarketTemplateService
import com.tencent.devops.store.service.template.MarketTemplateStatisticService
import com.tencent.devops.store.service.template.TemplateCategoryService
import com.tencent.devops.store.service.template.TemplateLabelService
import com.tencent.devops.store.service.template.TemplateModelService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
    lateinit var templateCategoryRelDao: TemplateCategoryRelDao
    @Autowired
    lateinit var marketAtomDao: MarketAtomDao
    @Autowired
    lateinit var atomDao: AtomDao
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
    @Qualifier("templateMemberService")
    lateinit var storeMemberService: StoreMemberService
    @Autowired
    lateinit var classifyService: ClassifyService
    @Autowired
    lateinit var templateModelService: TemplateModelService
    @Autowired
    lateinit var client: Client

    companion object {
        private val logger = LoggerFactory.getLogger(MarketTemplateServiceImpl::class.java)
        private val executor = Executors.newFixedThreadPool(30)
    }

    private fun getUserDeptList(userId: String): List<Int> {
        val userInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
        return if (userInfo == null) {
            listOf(0, 0, 0, 0)
        } else {
            listOf(userInfo.bgId.toInt(), userInfo.deptId.toInt(), userInfo.centerId.toInt(), userInfo.groupId.toInt())
        }
    }

    private fun doList(
        userId: String,
        userDeptList: List<Int>,
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
            val count = marketTemplateDao.count(dslContext, name, classifyCode, categoryList, labelCodeList, score, rdType)
            val templates = marketTemplateDao.list(dslContext, name, classifyCode, categoryList, labelCodeList, score, rdType, sortType, desc, page, pageSize)
                ?: return@Callable MarketTemplateResp(0, page, pageSize, results)
            logger.info("[list]get templates: $templates")

            val templateCodeList = templates.map {
                it["TEMPLATE_CODE"] as String
            }.toList()

            // 获取可见范围
            val templateVisibleData = generateTemplateVisibleData(templateCodeList, StoreTypeEnum.TEMPLATE).data
            logger.info("[list]get templateVisibleData")

            // 获取统计数据
            val templateStatisticData = marketTemplateStatisticService.getStatisticByCodeList(templateCodeList).data
            logger.info("[list]get statisticData")

            // 获取成员
            val memberData = storeMemberService.batchListMember(templateCodeList, StoreTypeEnum.TEMPLATE).data

            // 获取分类
            val classifyList = classifyService.getAllClassify(StoreTypeEnum.TEMPLATE.type.toByte()).data
            val classifyMap = mutableMapOf<String, String>()
            classifyList?.forEach {
                classifyMap[it.id] = it.classifyCode
            }

            templates.forEach {
                val code = it["TEMPLATE_CODE"] as String
                val visibleList = templateVisibleData?.get(code)
                val statistic = templateStatisticData?.get(code)
                val members = memberData?.get(code)
                val publicFlag = it["PUBLIC_FLAG"] as Boolean
                val flag = generateInstallFlag(publicFlag, members, userId, visibleList, userDeptList)
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
                        os = listOf(),
                        downloads = statistic?.downloads
                            ?: 0,
                        score = statistic?.score
                            ?: 0.toDouble(),
                        summary = it["SUMMARY"] as? String,
                        flag = flag,
                        publicFlag = it["PUBLIC_FLAG"] as Boolean,
                        buildLessRunFlag = false,
                        docsLink = ""
                    )
                )
            }

            logger.info("[list]end")
            return@Callable MarketTemplateResp(count, page, pageSize, results)
        })
    }

    abstract fun generateInstallFlag(
        defaultFlag: Boolean,
        members: MutableList<String>?,
        userId: String,
        visibleList: MutableList<Int>?,
        userDeptList: List<Int>
    ): Boolean

    abstract fun generateTemplateVisibleData(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<Int>>?>

    /**
     * 模版市场，首页
     */
    override fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<MarketTemplateMain>> {
        val result = mutableListOf<MarketTemplateMain>()
        // 获取用户组织架构
        val userDeptList = getUserDeptList(userId)
        logger.info("mainPageList userDeptList is:$userDeptList")
        val futureList = mutableListOf<Future<MarketTemplateResp>>()
        val labelInfoList = mutableListOf<MarketMainItemLabel>()
        labelInfoList.add(MarketMainItemLabel(LATEST, MessageCodeUtil.getCodeLanMessage(LATEST)))
        futureList.add(
            doList(
                userId = userId, userDeptList = userDeptList, name = null, classifyCode = null, category = null,
                labelCode = null, score = null, rdType = null, sortType = MarketTemplateSortTypeEnum.UPDATE_TIME,
                desc = true, page = page, pageSize = pageSize
            )
        )
        labelInfoList.add(MarketMainItemLabel(HOTTEST, MessageCodeUtil.getCodeLanMessage(HOTTEST)))
        futureList.add(
            doList(
                userId = userId, userDeptList = userDeptList, name = null, classifyCode = null, category = null,
                labelCode = null, score = null, rdType = null, sortType = MarketTemplateSortTypeEnum.DOWNLOAD_COUNT,
                desc = true, page = page, pageSize = pageSize
            )
        )
        val classifyList = classifyDao.getAllClassify(dslContext, StoreTypeEnum.TEMPLATE.type.toByte())
        classifyList.forEach {
            val classifyCode = it.classifyCode
            val classifyLanName = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                defaultMessage = it.classifyName
            )
            labelInfoList.add(MarketMainItemLabel(classifyCode, classifyLanName))
            futureList.add(
                doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    name = null,
                    classifyCode = classifyCode,
                    category = null,
                    labelCode = null,
                    score = null,
                    rdType = null,
                    sortType = MarketTemplateSortTypeEnum.DOWNLOAD_COUNT,
                    desc = true,
                    page = page,
                    pageSize = pageSize
                )
            )
        }
        for (index in futureList.indices) {
            val labelInfo = labelInfoList[index]
            result.add(
                MarketTemplateMain(
                    key = labelInfo.key,
                    label = labelInfo.label,
                    records = futureList[index].get().records
                )
            )
        }
        return Result(result)
    }

    /**
     * 模版市场，查询模版列表
     */
    override fun list(
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
    ): MarketTemplateResp {
        logger.info("[list]enter")
        // 获取用户组织架构
        val userDeptList = getUserDeptList(userId)
        logger.info("list userDeptList is:$userDeptList")
        return doList(
            userId = userId,
            userDeptList = userDeptList,
            name = name,
            classifyCode = classifyCode,
            category = category,
            labelCode = labelCode,
            score = score,
            rdType = rdType,
            sortType = sortType,
            desc = true,
            page = page,
            pageSize = pageSize
        ).get()
    }

    override fun getTemplateDetailByCode(userId: String, templateCode: String): Result<TemplateDetail?> {
        logger.info("getTemplateDetailByCode userId is :$userId, templateCode is :$templateCode")
        val templateRecord = marketTemplateDao.getLatestTemplateByCode(dslContext, templateCode)
        logger.info("the templateRecord is :$templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(templateCode))
        }
        return getTemplateDetail(templateRecord, userId)
    }

    override fun getTemplateDetailById(userId: String, templateId: String): Result<TemplateDetail?> {
        logger.info("getTemplateDetailById userId is :$userId, templateId is :$templateId")
        val templateRecord = marketTemplateDao.getTemplate(dslContext, templateId)
        logger.info("the templateRecord is :$templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(templateId))
        }
        return getTemplateDetail(templateRecord, userId)
    }

    private fun getTemplateDetail(templateRecord: TTemplateRecord, userId: String): Result<TemplateDetail?> {
        val templateCode = templateRecord.templateCode
        val templateClassify = classifyService.getClassify(templateRecord.classifyId).data
        val templateStatisticRecord = storeStatisticDao.getStatisticByStoreCode(
            dslContext = dslContext,
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE.type.toByte()
        )
        val downloads = templateStatisticRecord.value1()?.toInt()
        val comments = templateStatisticRecord.value2()?.toInt()
        val score = templateStatisticRecord.value3()?.toDouble()
        val avgScore: Double = if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble() // 计算平均分
        val categoryList = templateCategoryService.getCategorysByTemplateId(templateRecord.id).data // 查找范畴列表
        val labelList = templateLabelService.getLabelsByTemplateId(templateRecord.id).data // 查找标签列表
        val publicFlag = templateRecord.publicFlag // 是否为公共模板
        val installFlag = storeUserService.isCanInstallStoreComponent(publicFlag, userId, templateCode, StoreTypeEnum.TEMPLATE) // 是否能安装
        var releaseFlag = false // 是否有处于上架状态的模板版本
        val count = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
        if (count > 0) {
            releaseFlag = true
        }
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, templateCode, StoreTypeEnum.TEMPLATE)
        return Result(TemplateDetail(
            templateId = templateRecord.id,
            templateCode = templateCode,
            templateName = templateRecord.templateName,
            logoUrl = templateRecord.logoUrl,
            classifyCode = templateClassify?.classifyCode,
            classifyName = templateClassify?.classifyName,
            downloads = downloads ?: 0,
            score = String.format("%.1f", avgScore).toDouble(),
            summary = templateRecord.summary,
            templateStatus = TemplateStatusEnum.getTemplateStatus(templateRecord.templateStatus.toInt()),
            description = templateRecord.description,
            version = templateRecord.version,
            templateRdType = TemplateRdTypeEnum.getTemplateRdType(templateRecord.templateRdType.toInt()),
            categoryList = categoryList,
            labelList = labelList,
            latestFlag = templateRecord.latestFlag,
            publisher = templateRecord.publisher,
            pubDescription = templateRecord.pubDescription,
            flag = installFlag,
            releaseFlag = releaseFlag,
            userCommentInfo = userCommentInfo
        ))
    }

    /**
     * 删除模版关联关系
     */
    override fun delete(userId: String, templateCode: String): Result<Boolean> {
        logger.info("to delete, userId: $userId | templateCode: $templateCode")
        val type = StoreTypeEnum.TEMPLATE.type.toByte()
        val isOwner = storeMemberDao.isStoreAdmin(dslContext, userId, templateCode, type)
        if (!isOwner) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, arrayOf(templateCode))
        }

        val releasedCnt = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
        if (releasedCnt > 0) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_TEMPLATE_RELEASED, arrayOf(templateCode))
        }
        logger.info("releasedCnt: $releasedCnt")

        // 如果已经被安装到其他项目下使用，不能删除关联
        val installedCnt = storeProjectRelDao.countInstalledProject(dslContext, templateCode, type)
        if (installedCnt > 0) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_TEMPLATE_USED, arrayOf(templateCode))
        }

        dslContext.transaction { t ->
            val context = DSL.using(t)

            client.get(ServiceTemplateResource::class).updateStoreFlag(userId, templateCode, false)

            storeMemberDao.deleteAll(context, templateCode, type)
            storeProjectRelDao.deleteAllRel(context, templateCode, type)
            marketTemplateDao.delete(context, templateCode)
        }

        return Result(true)
    }

    /**
     * 安装模板到项目
     */
    override fun installTemplate(userId: String, channelCode: ChannelCode, installTemplateReq: InstallTemplateReq): Result<Boolean> {
        logger.info("installTemplate userId is: $userId")
        logger.info("installTemplate channelCode is: $channelCode, installTemplateReq is: $installTemplateReq")
        val templateCode = installTemplateReq.templateCode
        val projectCodeList = installTemplateReq.projectCodeList
        val template = marketTemplateDao.getLatestTemplateByCode(dslContext, templateCode)
        logger.info("template is: $template")
        if (null == template) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(templateCode), false)
        }
        // 校验用户是否在模板下插件的可见范围之内和模板的可见范围是否都在其下面的插件可见范围之内
        val validateResult = validateUserTemplateAtomVisibleDept(userId, templateCode, projectCodeList)
        if (validateResult.isNotOk()) {
            // 抛出错误提示
            return Result(validateResult.status, validateResult.message ?: "")
        }
        logger.info("validateResult is: $validateResult")
        val validateInstallResult = storeProjectService.validateInstallPermission(
            publicFlag = template.publicFlag,
            userId = userId,
            storeCode = template.templateCode,
            storeType = StoreTypeEnum.TEMPLATE,
            projectCodeList = projectCodeList,
            channelCode = channelCode
        )
        logger.info("validateInstallResult is: $validateInstallResult")
        if (validateInstallResult.isNotOk()) {
            return validateInstallResult
        }
        val categoryRecords = templateCategoryRelDao.getCategorysByTemplateId(dslContext, template.id)
        val categoryCodeList = mutableListOf<String>()
        categoryRecords?.forEach {
            categoryCodeList.add(it[KEY_CATEGORY_CODE] as String)
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
        val addMarketTemplateResult = client.get(ServiceTemplateResource::class).addMarketTemplate(userId, addMarketTemplateRequest)
        logger.info("addMarketTemplateResult is $addMarketTemplateResult")
        if (addMarketTemplateResult.isNotOk()) {
            // 抛出错误提示
            return Result(addMarketTemplateResult.status, addMarketTemplateResult.message ?: "")
        }

        // 更新生成的模板的红线规则
        copyQualityRule(userId, templateCode, projectCodeList, addMarketTemplateResult.data ?: mapOf())

        return storeProjectService.installStoreComponent(
            userId = userId,
            projectCodeList = projectCodeList,
            storeId = template.id,
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE,
            publicFlag = template.publicFlag,
            channelCode = channelCode
        )
    }

    private fun validateUserTemplateAtomVisibleDept(userId: String, templateCode: String, projectCodeList: List<String>?): Result<Boolean> {
        logger.info("validateUserTemplateAtomVisibleDept userId is :$userId,templateCode is :$templateCode,projectCodeList is :$projectCodeList")
        val templateModelResult = templateModelService.getTemplateModel(templateCode)
        logger.info("the templateModelResult is :$templateModelResult")
        if (templateModelResult.isNotOk()) {
            // 抛出错误提示
            return Result(templateModelResult.status, templateModelResult.message ?: "")
        }
        val templateModel = templateModelResult.data
        logger.info("the templateModel is :$templateModel")
        if (null == templateModel) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
        var invalidAtomList = emptyList<String>()
        val needInstallAtomMap = mutableMapOf<String, TAtomRecord>()
        val stageList = templateModel.stages
        stageList.forEach { stage ->
            val containerList = stage.containers
            containerList.forEach { container ->
                val elementList = container.elements
                elementList.forEach { element ->
                    // 判断用户的组织架构是否在原子插件的可见范围之内
                    val atomCode = element.getAtomCode()
                    val atomVersion = element.version
                    logger.info("the atomCode is:$atomCode，atomVersion is:$atomVersion")
                    val atomRecord = if (atomVersion.isNotEmpty()) {
                        atomDao.getPipelineAtom(dslContext, atomCode, atomVersion.replace("*", ""))
                    } else {
                        marketAtomDao.getLatestAtomByCode(dslContext, atomCode) // 兼容历史存量原子插件的情况
                    }
                    logger.info("the atomRecord is:$atomRecord")
                    if (null == atomRecord) {
                        return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode))
                    }
                    invalidAtomList = generateUserAtomInvalidVisibleAtom(atomCode, userId, atomRecord, element)
                    if (!atomRecord.defaultFlag) needInstallAtomMap[atomCode] = atomRecord
                }
            }
        }
        if (invalidAtomList.isNotEmpty()) {
            // 存在用户不在插件的可见范围内的插件，给出错误提示
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_ATOM_VISIBLE_DEPT_IS_INVALID, arrayOf(JsonUtil.toJson(invalidAtomList)), false)
        }
        val validateTempleAtomVisibleResult = validateTempleAtomVisible(templateCode, templateModel)
        if (validateTempleAtomVisibleResult.isNotOk()) {
            return validateTempleAtomVisibleResult
        }
        if (projectCodeList != null && projectCodeList.isNotEmpty()) {
            logger.info("the needInstallAtomList is:$needInstallAtomMap")
            // 判断插件是否已安装
            projectCodeList.forEach { projectCode ->
                needInstallAtomMap.forEach {
                    val atomCode = it.key
                    val atomRecord = it.value
                    val installFlag = storeProjectRelDao.isInstalledByProject(
                        dslContext = dslContext,
                        projectCode = projectCode,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM.type.toByte()
                    )
                    if (!installFlag) {
                        return storeProjectService.installStoreComponent(
                            userId = userId,
                            projectCodeList = arrayListOf(projectCode),
                            storeId = atomRecord.id,
                            storeCode = atomRecord.atomCode,
                            storeType = StoreTypeEnum.ATOM,
                            publicFlag = atomRecord.defaultFlag,
                            channelCode = ChannelCode.BS
                        )
                    }
                }
            }
        }
        return Result(true)
    }

    abstract fun generateUserAtomInvalidVisibleAtom(
        atomCode: String,
        userId: String,
        atomRecord: TAtomRecord,
        element: Element
    ): List<String>

    abstract fun validateTempleAtomVisible(templateCode: String, templateModel: Model): Result<Boolean>

    private fun copyQualityRule(userId: String, templateCode: String, projectCodeList: Collection<String>, projectTemplateMap: Map<String, String>) {
        try {
            logger.info("start to copy the quality rule for template: $templateCode")
            val sourceTemplate = client.get(ServiceTemplateResource::class).listTemplateById(
                setOf(templateCode), null).data?.templates!!.getValue(templateCode)
            projectCodeList.forEach { projectCode ->
                client.get(ServiceQualityRuleResource::class).copyRule(CopyRuleRequest(
                    sourceTemplate.projectId,
                    templateCode,
                    projectCode,
                    projectTemplateMap[projectCode] ?: "",
                    userId
                ))
            }
        } catch (e: Exception) {
            logger.error("fail to copy the quality rule for template: $templateCode", e)
        }
    }

    override fun getMyTemplates(userId: String, templateName: String?, page: Int, pageSize: Int): Result<Page<MyTemplateItem>?> {
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
            templateList.add(MyTemplateItem(
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
            ))
        }
        val templateCount = marketTemplateDao.getMyTemplatesCount(dslContext, userId, templateName)
        val totalPages = PageUtil.calTotalPage(pageSize, templateCount)
        return Result(Page(count = templateCount, page = page, pageSize = pageSize, totalPages = totalPages, records = templateList))
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
                arrayOf("templateId:$templateId,templateCode:$templateCode"),
                false
            )
        }
        return Result(true)
    }
}