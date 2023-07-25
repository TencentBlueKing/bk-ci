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

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.model.store.tables.records.TTemplateRecord
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.dao.template.TemplateCategoryRelDao
import com.tencent.devops.store.dao.template.TemplateLabelRelDao
import com.tencent.devops.store.pojo.atom.MarketMainItemLabel
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_PROJECT_CODE
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.UserStoreDeptInfoRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
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
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreDeptService
import com.tencent.devops.store.service.common.StoreHonorService
import com.tencent.devops.store.service.common.StoreIndexManageService
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.template.MarketTemplateService
import com.tencent.devops.store.service.template.TemplateCategoryService
import com.tencent.devops.store.service.template.TemplateLabelService
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@Suppress("ALL")
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
    lateinit var templateLabelRelDao: TemplateLabelRelDao
    @Autowired
    lateinit var marketAtomDao: MarketAtomDao
    @Autowired
    lateinit var atomDao: AtomDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var templateCategoryService: TemplateCategoryService
    @Autowired
    lateinit var templateLabelService: TemplateLabelService
    @Autowired
    lateinit var storeTotalStatisticService: StoreTotalStatisticService
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
    lateinit var storeHonorService: StoreHonorService
    @Autowired
    lateinit var storeIndexManageService: StoreIndexManageService
    @Autowired
    lateinit var storeDeptService: StoreDeptService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var client: Client

    companion object {
        private val logger = LoggerFactory.getLogger(MarketTemplateServiceImpl::class.java)
        private val executor = Executors.newFixedThreadPool(30)
    }

    private val cache = CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Optional<StoreBaseInfo>>() {
            override fun load(atomCode: String): Optional<StoreBaseInfo> {
                val atomRecord = atomDao.getPipelineAtom(
                    dslContext = dslContext,
                    atomCode = atomCode,
                    atomStatusList = listOf(
                        AtomStatusEnum.RELEASED.status.toByte(),
                        AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                        AtomStatusEnum.UNDERCARRIAGED.status.toByte()
                    )
                )
                return if (atomRecord != null) {
                    val storeBaseInfo = StoreBaseInfo(
                        storeId = atomRecord.id,
                        storeCode = atomRecord.atomCode,
                        storeName = atomRecord.name,
                        version = atomRecord.version,
                        publicFlag = atomRecord.defaultFlag
                    )
                    Optional.of(storeBaseInfo)
                } else {
                    Optional.empty()
                }
            }
        })

    private fun getUserDeptList(userId: String): List<Int> {
        val userInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
        return if (userInfo == null) {
            listOf(0, 0, 0, 0)
        } else {
            listOf(userInfo.bgId.toInt(), userInfo.deptId.toInt(), userInfo.centerId.toInt(), userInfo.groupId.toInt())
        }
    }

    private fun getMarketTemplateList(
        userId: String,
        userDeptList: List<Int>,
        keyword: String?,
        classifyCode: String?,
        category: String?,
        labelCode: String?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        installedTemplateCodes: List<String>?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Future<MarketTemplateResp> {
        return executor.submit(Callable<MarketTemplateResp> {
            val installedTemplates = mutableListOf<MarketItem>()
            val canInstallTemplates = mutableListOf<MarketItem>()
            val cannotInstallTemplates = mutableListOf<MarketItem>()
            // 获取模版
            val categoryList = if (category.isNullOrEmpty()) listOf() else category.split(",")
            val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode.split(",")
            val count = marketTemplateDao.count(
                dslContext = dslContext,
                keyword = keyword,
                classifyCode = classifyCode,
                categoryList = categoryList,
                labelCodeList = labelCodeList,
                score = score,
                rdType = rdType
            )
            val templates = marketTemplateDao.list(
                dslContext = dslContext,
                keyword = keyword,
                classifyCode = classifyCode,
                categoryList = categoryList,
                labelCodeList = labelCodeList,
                score = score,
                rdType = rdType,
                sortType = sortType,
                desc = desc,
                page = page,
                pageSize = pageSize
            )
                ?: return@Callable MarketTemplateResp(0, page, pageSize, canInstallTemplates)
            val tTemplate = TTemplate.T_TEMPLATE
            val templateCodeList = templates.map {
                it[tTemplate.TEMPLATE_CODE] as String
            }.toList()
            val storeType = StoreTypeEnum.TEMPLATE
            // 获取可见范围
            val templateVisibleData = storeCommonService.generateStoreVisibleData(templateCodeList, storeType)

            // 获取统计数据
            val templateStatisticData = storeTotalStatisticService.getStatisticByCodeList(
                storeType = storeType.type.toByte(),
                storeCodeList = templateCodeList
            )
            val templateHonorInfoMap = storeHonorService.getHonorInfosByStoreCodes(storeType, templateCodeList)
            val templateIndexInfosMap =
                storeIndexManageService.getStoreIndexInfosByStoreCodes(storeType, templateCodeList)
            // 获取成员
            val memberData = storeMemberService.batchListMember(templateCodeList, storeType).data

            // 获取分类
            val classifyList = classifyService.getAllClassify(storeType.type.toByte()).data
            val classifyMap = mutableMapOf<String, String>()
            classifyList?.forEach {
                classifyMap[it.id] = it.classifyCode
            }

            templates.forEach {
                val code = it[tTemplate.TEMPLATE_CODE] as String
                val visibleList = templateVisibleData?.get(code)
                val statistic = templateStatisticData[code]
                val honorInfos = templateHonorInfoMap[code]
                val indexInfos = templateIndexInfosMap[code]
                val members = memberData?.get(code)
                val publicFlag = it[tTemplate.PUBLIC_FLAG] as Boolean
                val canInstall = storeCommonService.generateInstallFlag(
                    defaultFlag = publicFlag,
                    members = members,
                    userId = userId,
                    visibleList = visibleList,
                    userDeptList = userDeptList
                )
                val installed = installedTemplateCodes?.contains(code)
                val classifyId = it[tTemplate.CLASSIFY_ID] as String
                val marketItem = MarketItem(
                    id = it[tTemplate.ID] as String,
                    name = it[tTemplate.TEMPLATE_NAME] as String,
                    code = code,
                    version = it[tTemplate.VERSION] as String,
                    type = "",
                    rdType = TemplateRdTypeEnum.getTemplateRdType((it[tTemplate.TEMPLATE_RD_TYPE] as Byte).toInt()),
                    classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] else "",
                    logoUrl = it[tTemplate.LOGO_URL],
                    publisher = it[tTemplate.PUBLISHER] as String,
                    os = listOf(),
                    downloads = statistic?.downloads
                        ?: 0,
                    score = statistic?.score
                        ?: 0.toDouble(),
                    summary = it[tTemplate.SUMMARY],
                    flag = canInstall,
                    publicFlag = it[tTemplate.PUBLIC_FLAG] as Boolean,
                    buildLessRunFlag = false,
                    docsLink = "",
                    modifier = it[tTemplate.MODIFIER] as String,
                    updateTime = DateTimeUtil.toDateTime(it[tTemplate.UPDATE_TIME] as LocalDateTime),
                    installed = installed,
                    honorInfos = honorInfos,
                    indexInfos = indexInfos,
                    hotFlag = statistic?.hotFlag
                )
                when {
                    installed == true -> installedTemplates.add(marketItem)
                    canInstall -> canInstallTemplates.add(marketItem)
                    else -> cannotInstallTemplates.add(marketItem)
                }
            }

            return@Callable MarketTemplateResp(
                count = count,
                page = page,
                pageSize = pageSize,
                records = installedTemplates.plus(canInstallTemplates).plus(cannotInstallTemplates)
            )
        })
    }

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
        labelInfoList.add(
            MarketMainItemLabel(
                LATEST, I18nUtil.getCodeLanMessage(messageCode = LATEST, language = I18nUtil.getLanguage(userId))
            )
        )
        futureList.add(
            getMarketTemplateList(
                userId = userId,
                userDeptList = userDeptList,
                keyword = null,
                classifyCode = null,
                category = null,
                labelCode = null,
                score = null,
                rdType = null,
                sortType = MarketTemplateSortTypeEnum.UPDATE_TIME,
                installedTemplateCodes = null,
                desc = true,
                page = page,
                pageSize = pageSize
            )
        )
        labelInfoList.add(
            MarketMainItemLabel(
                HOTTEST,
                I18nUtil.getCodeLanMessage(HOTTEST, language = I18nUtil.getLanguage(userId))
            )
        )
        futureList.add(
            getMarketTemplateList(
                userId = userId,
                userDeptList = userDeptList,
                keyword = null,
                classifyCode = null,
                category = null,
                labelCode = null,
                score = null,
                rdType = null,
                sortType = MarketTemplateSortTypeEnum.DOWNLOAD_COUNT,
                installedTemplateCodes = null,
                desc = true,
                page = page,
                pageSize = pageSize
            )
        )
        val classifyList = classifyDao.getAllClassify(dslContext, StoreTypeEnum.TEMPLATE.type.toByte())
        classifyList.forEach {
            val classifyCode = it.classifyCode
            val classifyLanName = I18nUtil.getCodeLanMessage(
                messageCode = "${StoreTypeEnum.TEMPLATE.name}.classify.$classifyCode",
                defaultMessage = it.classifyName,
                language = I18nUtil.getLanguage(userId)
            )
            labelInfoList.add(MarketMainItemLabel(classifyCode, classifyLanName))
            futureList.add(
                getMarketTemplateList(
                    userId = userId,
                    userDeptList = userDeptList,
                    keyword = null,
                    classifyCode = classifyCode,
                    category = null,
                    labelCode = null,
                    score = null,
                    rdType = null,
                    sortType = MarketTemplateSortTypeEnum.DOWNLOAD_COUNT,
                    installedTemplateCodes = null,
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
        keyword: String?,
        classifyCode: String?,
        category: String?,
        labelCode: String?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        projectCode: String?,
        page: Int?,
        pageSize: Int?
    ): MarketTemplateResp {
        logger.info("[list]enter")
        // 获取用户组织架构
        val userDeptList = getUserDeptList(userId)
        logger.info("list userDeptList is:$userDeptList")
        var installedTemplateCodes: List<String>? = null
        run check@{
            if (!projectCode.isNullOrBlank()) {
                val installedTemplatesResult =
                    client.get(ServicePTemplateResource::class).getSrcTemplateCodes(projectCode)
                if (installedTemplatesResult.isNotOk()) {
                    throw RemoteServiceException("Failed to get project($projectCode) installedTemplates")
                }
                logger.info("get project($projectCode) installedTemplates :$installedTemplatesResult")
                installedTemplateCodes = installedTemplatesResult.data
            }
        }

        return getMarketTemplateList(
            userId = userId,
            userDeptList = userDeptList,
            keyword = keyword,
            classifyCode = classifyCode,
            category = category,
            labelCode = labelCode,
            score = score,
            rdType = rdType,
            sortType = sortType,
            installedTemplateCodes = installedTemplateCodes,
            desc = true,
            page = page,
            pageSize = pageSize
        ).get()
    }

    override fun getTemplateDetailByCode(userId: String, templateCode: String): Result<TemplateDetail?> {
        logger.info("getTemplateDetailByCode userId is :$userId, templateCode is :$templateCode")
        val templateRecord = marketTemplateDao.getLatestTemplateByCode(dslContext, templateCode)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(templateCode),
                language = I18nUtil.getLanguage(userId)
            )
        return getTemplateDetail(templateRecord, userId)
    }

    override fun getTemplateDetailById(userId: String, templateId: String): Result<TemplateDetail?> {
        logger.info("getTemplateDetailById userId is :$userId, templateId is :$templateId")
        val templateRecord = marketTemplateDao.getTemplate(dslContext, templateId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(templateId),
                language = I18nUtil.getLanguage(userId)
            )
        return getTemplateDetail(templateRecord, userId)
    }

    private fun getTemplateDetail(templateRecord: TTemplateRecord, userId: String): Result<TemplateDetail?> {
        val templateCode = templateRecord.templateCode
        val templateClassify = classifyService.getClassify(templateRecord.classifyId).data
        val storeStatistic = storeTotalStatisticService.getStatisticByCode(
            userId = userId,
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE.type.toByte()
        )
        val templateHonorInfos = storeHonorService.getStoreHonor(userId, StoreTypeEnum.TEMPLATE, templateCode)
        val templateIndexInfos =
            storeIndexManageService.getStoreIndexInfosByStoreCode(StoreTypeEnum.TEMPLATE, templateCode)
        // 查找范畴列表
        val categoryList = templateCategoryService.getCategorysByTemplateId(templateRecord.id).data
        val labelList = templateLabelService.getLabelsByTemplateId(templateRecord.id).data // 查找标签列表
        val publicFlag = templateRecord.publicFlag // 是否为公共模板
        val installFlag = storeUserService.isCanInstallStoreComponent(
            defaultFlag = publicFlag,
            userId = userId,
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE
        ) // 是否能安装
        var releaseFlag = false // 是否有处于上架状态的模板版本
        val count = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
        if (count > 0) {
            releaseFlag = true
        }
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(
            userId = userId,
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE
        )
        return Result(TemplateDetail(
            templateId = templateRecord.id,
            templateCode = templateCode,
            templateName = templateRecord.templateName,
            logoUrl = templateRecord.logoUrl,
            classifyCode = templateClassify?.classifyCode,
            classifyName = templateClassify?.classifyName,
            downloads = storeStatistic.downloads,
            score = storeStatistic.score,
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
            userCommentInfo = userCommentInfo,
            honorInfos = templateHonorInfos,
            indexInfos = templateIndexInfos
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
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(templateCode),
                language = I18nUtil.getLanguage(userId)
            )
        }

        val releasedCnt = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
        if (releasedCnt > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_TEMPLATE_RELEASED,
                params = arrayOf(templateCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        logger.info("releasedCnt: $releasedCnt")

        // 如果已经被安装到其他项目下使用，不能删除关联
        val installedCnt = storeProjectRelDao.countInstalledProject(dslContext, templateCode, type)
        if (installedCnt > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_TEMPLATE_USED,
                params = arrayOf(templateCode),
                language = I18nUtil.getLanguage(userId)
            )
        }

        dslContext.transaction { t ->
            val context = DSL.using(t)

            client.get(ServicePTemplateResource::class).updateStoreFlag(userId, templateCode, false)
            storeCommonService.deleteStoreInfo(context, templateCode, StoreTypeEnum.TEMPLATE.type.toByte())
            templateCategoryRelDao.deleteByTemplateCode(context, templateCode)
            templateLabelRelDao.deleteByTemplateCode(context, templateCode)
            marketTemplateDao.delete(context, templateCode)
        }

        return Result(true)
    }

    /**
     * 安装模板到项目
     */
    override fun installTemplate(
        userId: String,
        channelCode: ChannelCode,
        installTemplateReq: InstallTemplateReq
    ): Result<Boolean> {
        logger.info("installTemplate userId: $userId,channelCode: $channelCode,installTemplateReq: $installTemplateReq")
        val templateCode = installTemplateReq.templateCode
        val projectCodeList = installTemplateReq.projectCodeList
        val template = marketTemplateDao.getLatestTemplateByCode(dslContext, templateCode)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(templateCode),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        // 校验用户是否在模板下插件的可见范围之内和模板的可见范围是否都在其下面的插件可见范围之内
        val validateResult = validateUserTemplateComponentVisibleDept(userId, templateCode, projectCodeList)
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
        val addMarketTemplateResult = client.get(ServicePTemplateResource::class)
            .addMarketTemplate(userId = userId, addMarketTemplateRequest = addMarketTemplateRequest)
        logger.info("addMarketTemplateResult is $addMarketTemplateResult")
        if (addMarketTemplateResult.isNotOk()) {
            // 抛出错误提示
            return Result(addMarketTemplateResult.status, addMarketTemplateResult.message ?: "")
        }

        val addMarketTemplateResultKeys = addMarketTemplateResult.data?.keys ?: emptySet()
        projectCodeList.removeAll(addMarketTemplateResultKeys)
        // 更新生成的模板的红线规则
        copyQualityRule(userId, templateCode, addMarketTemplateResultKeys, addMarketTemplateResult.data ?: mapOf())
        val installStoreComponentResult = storeProjectService.installStoreComponent(
            userId = userId,
            projectCodeList = ArrayList(addMarketTemplateResultKeys),
            storeId = template.id,
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE,
            publicFlag = template.publicFlag,
            channelCode = channelCode
        )
        return if (projectCodeList.isNullOrEmpty()) {
            installStoreComponentResult
        } else {
            I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_INSTALL_TEMPLATE_CODE_IS_INVALID,
                params = arrayOf(
                    template.templateName,
                    projectCodeList.joinToString(",")
                ),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
    }

    override fun validateUserTemplateComponentVisibleDept(
        userId: String,
        templateCode: String,
        projectCodeList: ArrayList<String>
    ): Result<Boolean> {
        val templateDetailResult = client.get(ServicePTemplateResource::class).getTemplateDetailInfo(templateCode)
        if (templateDetailResult.isNotOk()) {
            // 抛出错误提示
            return Result(templateDetailResult.status, templateDetailResult.message ?: "")
        }
        val templateDetail = templateDetailResult.data
        val templateModel = templateDetail?.templateModel
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
        return verificationModelComponentVisibleDept(
            userId = userId,
            model = templateModel,
            projectCodeList = projectCodeList,
            templateCode = templateDetail.templateCode
        )
    }

    override fun verificationModelComponentVisibleDept(
        userId: String,
        model: Model,
        projectCodeList: ArrayList<String>,
        templateCode: String?
    ): Result<Boolean> {

        val invalidImageList = mutableListOf<String>()
        val invalidAtomList = mutableListOf<String>()
        val needInstallImageMap = mutableMapOf<String, StoreBaseInfo>()
        val needInstallAtomMap = mutableMapOf<String, StoreBaseInfo>()
        val userDeptIdList = storeUserService.getUserDeptList(userId) // 获取用户的机构ID信息
        val stageList = model.stages
        // 获取模板下镜像的机构信息
        val templateImageDeptMap = storeDeptService.getTemplateImageDeptMap(stageList)
        // 获取每个模板下插件的机构信息
        val templateAtomDeptMap = storeDeptService.getTemplateAtomDeptMap(stageList)
        projectCodeList.forEach { projectCode ->
            // 获取可用的镜像标识列表
            val validImageCodes = getValidStoreCodes(
                projectCode = projectCode,
                storeCodes = templateImageDeptMap.keys,
                storeType = StoreTypeEnum.IMAGE
            )
            // 获取可用的插件标识列表
            val validAtomCodes = getValidStoreCodes(
                projectCode = projectCode,
                storeCodes = templateAtomDeptMap.keys,
                storeType = StoreTypeEnum.ATOM
            )
            stageList.forEach { stage ->
                val containerList = stage.containers
                containerList.forEach { container ->
                    // 判断用户的组织架构是否在镜像的可见范围之内
                    validateUserImageVisible(
                        container = container,
                        templateImageDeptMap = templateImageDeptMap,
                        userId = userId,
                        userDeptIdList = userDeptIdList,
                        validImageCodes = validImageCodes,
                        invalidImageList = invalidImageList,
                        needInstallImageMap = needInstallImageMap
                    )
                    val elementList = container.elements
                    elementList.forEach { element ->
                        // 判断用户的组织架构是否在插件的可见范围之内
                        validateUserAtomVisible(
                            element = element,
                            userId = userId,
                            userDeptIdList = userDeptIdList,
                            templateAtomDeptMap = templateAtomDeptMap,
                            validAtomCodes = validAtomCodes,
                            invalidAtomList = invalidAtomList,
                            needInstallAtomMap = needInstallAtomMap
                        )
                    }
                }
            }
            logger.info("validateUserTemplateComponentVisibleDept invalidImageList:$invalidImageList")
            if (invalidImageList.isNotEmpty()) {
                // 存在用户不在镜像的可见范围内的镜像，给出错误提示
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_IMAGE_PROJECT_IS_INVALID,
                    params = arrayOf(JsonUtil.toJson(invalidImageList), projectCode)
                )
            }
            logger.info("validateUserTemplateComponentVisibleDept invalidAtomList:$invalidAtomList")
            if (invalidAtomList.isNotEmpty()) {
                // 存在用户不在插件的可见范围内的插件，给出错误提示
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_ATOM_VISIBLE_DEPT_IS_INVALID,
                    params = arrayOf(JsonUtil.toJson(invalidAtomList))
                )
            }
            if (!templateCode.isNullOrBlank()) {
                val validateTempleAtomVisibleResult = validateTemplateVisibleDept(
                    templateCode = templateCode,
                    templateModel = model,
                    validImageCodes = validImageCodes,
                    validAtomCodes = validAtomCodes
                )
                if (validateTempleAtomVisibleResult.isNotOk()) {
                    return validateTempleAtomVisibleResult
                }
            }

            // 安装镜像
            installStoreComponent(needInstallImageMap, userId, projectCode, StoreTypeEnum.IMAGE)
            // 安装插件
            installStoreComponent(needInstallAtomMap, userId, projectCode, StoreTypeEnum.ATOM)
        }
        return Result(data = true)
    }

    private fun validateUserImageVisible(
        container: Container,
        templateImageDeptMap: Map<String, List<DeptInfo>?>,
        userId: String,
        userDeptIdList: List<Int>,
        validImageCodes: List<String>,
        invalidImageList: MutableList<String>,
        needInstallImageMap: MutableMap<String, StoreBaseInfo>
    ) {
        val storeType = StoreTypeEnum.IMAGE.name
        if (container is VMBuildContainer && container.dispatchType is StoreDispatchType) {
            val dispatchType = container.dispatchType as StoreDispatchType
            val imageCode = dispatchType.imageCode
            val imageName = dispatchType.imageName
            // 可用的镜像无需再检查
            if (!imageCode.isNullOrBlank() && !validImageCodes.contains(imageCode)) {
                val storeCommonDao = try {
                    SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
                } catch (ignored: Throwable) {
                    logger.warn("StoreCommonDao is not exist", ignored)
                    null
                }
                if (storeCommonDao != null) {
                    val storeBaseInfo = storeCommonDao.getNewestStoreBaseInfoByCode(
                        dslContext = dslContext,
                        storeCode = imageCode,
                        storeStatus = ImageStatusEnum.RELEASED.status.toByte()
                    )
                        ?: throw ErrorCodeException(
                            errorCode = StoreMessageCode.USER_TEMPLATE_IMAGE_IS_INVALID,
                            params = arrayOf(imageName ?: imageCode)
                        )
                    val storeDepInfoList = templateImageDeptMap[imageCode]
                    // 判断用户是否有权限使用该镜像
                    val validFlag = checkUserInvalidVisibleStoreInfo(
                        UserStoreDeptInfoRequest(
                            userId = userId,
                            userDeptIdList = userDeptIdList,
                            storeCode = imageCode,
                            storeType = StoreTypeEnum.IMAGE,
                            publicFlag = storeBaseInfo.publicFlag,
                            storeDepInfoList = storeDepInfoList
                        )
                    )
                    if (!validFlag) invalidImageList.add(imageName ?: imageCode)
                    if (!storeBaseInfo.publicFlag && validFlag) needInstallImageMap[imageCode] = storeBaseInfo
                }
            }
        }
    }

    private fun installStoreComponent(
        needInstallStoreMap: MutableMap<String, StoreBaseInfo>,
        userId: String,
        projectCode: String,
        storeType: StoreTypeEnum
    ) {
        val totalStoreCodes = needInstallStoreMap.keys
        totalStoreCodes.forEach {
            val storeBaseInfo = needInstallStoreMap[it]
            if (storeBaseInfo != null) {
                storeProjectService.installStoreComponent(
                    userId = userId,
                    projectCodeList = arrayListOf(projectCode),
                    storeId = storeBaseInfo.storeId,
                    storeCode = storeBaseInfo.storeCode,
                    storeType = storeType,
                    publicFlag = storeBaseInfo.publicFlag,
                    channelCode = ChannelCode.BS
                )
            }
        }
    }

    private fun getValidStoreCodes(
        projectCode: String,
        storeCodes: Collection<String>,
        storeType: StoreTypeEnum
    ): List<String> {
        return storeProjectRelDao.getValidStoreCodesByProject(
            dslContext = dslContext,
            projectCode = projectCode,
            storeCodes = storeCodes,
            storeType = storeType
        )?.map { it.value1() } ?: emptyList()
    }

    private fun validateUserAtomVisible(
        element: Element,
        userId: String,
        userDeptIdList: List<Int>,
        templateAtomDeptMap: Map<String, List<DeptInfo>?>?,
        validAtomCodes: List<String>?,
        invalidAtomList: MutableList<String>,
        needInstallAtomMap: MutableMap<String, StoreBaseInfo>
    ) {
        val atomCode = element.getAtomCode()
        // 已安装的插件无需再检查
        if (validAtomCodes?.contains(atomCode) == true) {
            return
        }
        // 判断插件是否为默认插件
        val storeBaseInfoOptional = cache.get(atomCode)
        val storeBaseInfo = if (storeBaseInfoOptional.isPresent) {
            storeBaseInfoOptional.get()
        } else {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_TEMPLATE_ATOM_IS_INVALID,
                params = arrayOf(element.name)
            )
        }
        val storeDepInfoList = templateAtomDeptMap?.get(atomCode)
        val validFlag = checkUserInvalidVisibleStoreInfo(
            UserStoreDeptInfoRequest(
                userId = userId,
                userDeptIdList = userDeptIdList,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM,
                publicFlag = storeBaseInfo.publicFlag,
                storeDepInfoList = storeDepInfoList
            )
        )
        if (!validFlag) invalidAtomList.add(element.name)
        if (!storeBaseInfo.publicFlag && validFlag) needInstallAtomMap[atomCode] = storeBaseInfo
    }

    abstract fun checkUserInvalidVisibleStoreInfo(userStoreDeptInfoRequest: UserStoreDeptInfoRequest): Boolean

    abstract fun validateTemplateVisibleDept(
        templateCode: String,
        templateModel: Model,
        validImageCodes: List<String>? = null,
        validAtomCodes: List<String>? = null
    ): Result<Boolean>

    private fun copyQualityRule(
        userId: String,
        templateCode: String,
        projectCodeList: Collection<String>,
        projectTemplateMap: Map<String, String>
    ) {
        try {
            logger.info("start to copy the quality rule for template: $templateCode")
            val sourceTemplate = client.get(ServicePTemplateResource::class).listTemplateById(
                setOf(templateCode), null, null).data?.templates!!.getValue(templateCode)
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
            projectCodeList.add(it[KEY_PROJECT_CODE] as String)
        }
        val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        val templateList = mutableListOf<MyTemplateItem>()
        val tTemplate = TTemplate.T_TEMPLATE
        records?.forEach {
            val templateCode = it[tTemplate.TEMPLATE_CODE] as String
            var releaseFlag = false // 是否有处于上架状态的模板版本
            val count = marketTemplateDao.countReleaseTemplateByCode(dslContext, templateCode)
            if (count > 0) {
                releaseFlag = true
            }
            val templateStatus = TemplateStatusEnum.getTemplateStatus((it[tTemplate.TEMPLATE_STATUS] as Byte).toInt())
            templateList.add(
                MyTemplateItem(
                    templateId = it[tTemplate.ID] as String,
                    templateCode = templateCode,
                    templateName = it[tTemplate.TEMPLATE_NAME] as String,
                    logoUrl = it[tTemplate.LOGO_URL],
                    version = it[tTemplate.VERSION] as String,
                    templateStatus = templateStatus,
                    projectCode = it[KEY_PROJECT_CODE] as String,
                    projectName = projectMap?.get(it[KEY_PROJECT_CODE] as String) as String,
                    releaseFlag = releaseFlag,
                    creator = it[tTemplate.CREATOR] as String,
                    modifier = it[tTemplate.MODIFIER] as String,
                    createTime = (it[tTemplate.CREATE_TIME] as LocalDateTime).timestampmilli(),
                    updateTime = (it[tTemplate.UPDATE_TIME] as LocalDateTime).timestampmilli()
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
     * 根据模板ID和模板代码判断模板是否存在
     */
    override fun judgeTemplateExistByIdAndCode(templateId: String, templateCode: String): Result<Boolean> {
        logger.info("the templateId is:$templateId, templateCode is:$templateCode")
        val count = marketTemplateDao.countByIdAndCode(dslContext, templateId, templateCode)
        if (count < 1) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("templateId:$templateId,templateCode:$templateCode"),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        return Result(true)
    }
}
