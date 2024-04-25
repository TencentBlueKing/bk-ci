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

package com.tencent.devops.store.common.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.auth.REFERER
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonSchemaUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ThreadLocalUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.util.RegexUtils
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.TStoreBaseFeature
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.LabelDao
import com.tencent.devops.store.common.dao.MarketStoreQueryDao
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.CategoryService
import com.tencent.devops.store.common.service.ClassifyService
import com.tencent.devops.store.common.service.StoreCommentService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreComponentQueryService
import com.tencent.devops.store.common.service.StoreDailyStatisticService
import com.tencent.devops.store.common.service.StoreHonorService
import com.tencent.devops.store.common.service.StoreIndexManageService
import com.tencent.devops.store.common.service.StoreLabelService
import com.tencent.devops.store.common.service.StoreMemberService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.common.service.StoreTotalStatisticService
import com.tencent.devops.store.common.service.StoreUserService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.KEY_HTML_TEMPLATE_VERSION
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.ListComponentsQuery
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.MarketMainItemLabel
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.classify.Classify
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.statistic.StoreDailyStatistic
import com.tencent.devops.store.pojo.common.version.StoreDeskVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.VersionModel
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.jooq.DSLContext
import org.jooq.Record
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentQueryServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao,
    private val storeUserService: StoreUserService,
    private val storeBaseEnvQueryDao: StoreBaseEnvQueryDao,
    private val storeLabelService: StoreLabelService,
    private val storeCommentService: StoreCommentService,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeVersionLogDao: StoreVersionLogDao,
    private val storeBaseExtQueryDao: StoreBaseExtQueryDao,
    private val classifyDao: ClassifyDao,
    private val storeTotalStatisticService: StoreTotalStatisticService,
    private val categoryService: CategoryService,
    private val storeHonorService: StoreHonorService,
    private val storeIndexManageService: StoreIndexManageService,
    private val storeCommonService: StoreCommonService,
    private val marketStoreQueryDao: MarketStoreQueryDao,
    private val storeMemberService: StoreMemberService,
    private val classifyService: ClassifyService,
    private val storeProjectService: StoreProjectService,
    private val storeDailyStatisticService: StoreDailyStatisticService,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao,
    private val storeClassifyService: ClassifyService,
    private val labelDao: LabelDao
): StoreComponentQueryService {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentQueryServiceImpl::class.java)
        private val executor = Executors.newFixedThreadPool(30)
    }

    override fun getMyComponents(
        userId: String,
        storeType: String,
        name: String?,
        page: Int,
        pageSize: Int
    ): Page<MyStoreComponent>? {
        logger.info("getMyComponents params:[$userId|$storeType|$name|$page|$pageSize]")
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        // 获取有权限的组件代码列表
        val records = storeBaseQueryDao.getMyComponents(
            dslContext = dslContext,
            userId = userId,
            storeType = StoreTypeEnum.valueOf(storeType),
            name = name,
            page = page,
            pageSize = pageSize
        )
        val count = storeBaseQueryDao.countMyComponents(
            dslContext = dslContext,
            userId = userId,
            storeType = StoreTypeEnum.valueOf(storeType),
            name = name
        )
        val storeProjectMap = mutableMapOf<String, String>()
        val tStoreBase = TStoreBase.T_STORE_BASE
        val storeIds = mutableListOf<String>()
        val projectCodeList = mutableListOf<String>()
        val storeCodes = mutableListOf<String>()
        records?.forEach { record ->
            storeIds.add(record[tStoreBase.ID])
            val storeCode = record[tStoreBase.STORE_CODE] as String
            storeCodes.add(storeCode)
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeEnum
            )
            if (null != testProjectCode) {
                projectCodeList.add(testProjectCode)
                storeProjectMap[storeCode] = testProjectCode
            }
        }
        val storeComponents = records?.let {
            handleComponents(
                storeType = storeTypeEnum,
                storeCodes = storeCodes,
                storeIds = storeIds,
                projectCodeList = projectCodeList,
                storeProjectMap = storeProjectMap,
                records = it
            )
        }
        return Page(
            count = count.toLong(),
            page = page,
            pageSize = pageSize,
            records = storeComponents ?: emptyList()
        )
    }

    override fun listComponents(userId: String, listComponentsQuery: ListComponentsQuery): Page<MyStoreComponent>? {
        val storeType = StoreTypeEnum.valueOf(listComponentsQuery.storeType)
        val classifyId = listComponentsQuery.classifyCode?.let {
            classifyDao.getClassifyByCode(
                dslContext = dslContext,
                classifyCode = it,
                type = storeType
            )
        }
        val categoryIds = listComponentsQuery.categoryCodes?.let {
            val allCategory = categoryService.getAllCategory(storeType.type.toByte()).data
            val categoryCodes = it.split(",")
            allCategory?.filter { category -> categoryCodes.contains(category.categoryCode)}?.map { categoryInfo ->
                categoryInfo.id
            }
        }
        val labelIds = listComponentsQuery.labelCodes?.let {
            labelDao.getIdsByCodes(
                dslContext = dslContext,
                labelCodes = it.split(","),
                type = storeType.type.toByte()
            )
        }
        val count = storeBaseQueryDao.countComponents(
            dslContext = dslContext,
            listComponentsQuery = listComponentsQuery,
            classifyId = classifyId?.id,
            categoryIds = categoryIds,
            labelIds = labelIds
        )
        val records = storeBaseQueryDao.listComponents(
            dslContext = dslContext,
            listComponentsQuery = listComponentsQuery,
            classifyId = classifyId?.id,
            categoryIds = categoryIds,
            labelIds = labelIds
        )
        val storeProjectMap = mutableMapOf<String, String>()
        val tStoreBase = TStoreBase.T_STORE_BASE
        val storeIds = mutableListOf<String>()
        val storeCodes = mutableListOf<String>()

        records.forEach { record ->
            storeIds.add(record[tStoreBase.ID])
            val storeCode = record[tStoreBase.STORE_CODE] as String
            storeCodes.add(storeCode)
        }
        storeProjectRelDao.getStoreInitProjects(
            dslContext = dslContext,
            storeType = storeType.type.toByte(),
            descFlag = false,
            specProjectCodeList = storeCodes.toSet()
        )?.forEach { storeProjectMap[it.storeCode] = it.projectCode }
        val projectCodeList = storeProjectMap.values
        val storeComponents = handleComponents(
            storeType = storeType,
            storeCodes = storeCodes,
            storeIds = storeIds,
            projectCodeList = projectCodeList.toList(),
            storeProjectMap = storeProjectMap,
            records = records
        )
        return Page(
            count = count.toLong(),
            page = listComponentsQuery.page,
            pageSize = listComponentsQuery.pageSize,
            records = storeComponents
        )
    }

    private fun handleComponents(
        storeType: StoreTypeEnum,
        storeCodes: List<String>,
        storeIds: List<String>,
        projectCodeList: List<String>,
        storeProjectMap:  Map<String, String>,
        records: org.jooq.Result<out Record>
    ): List<MyStoreComponent> {

        val tStoreBase = TStoreBase.T_STORE_BASE
        // 获取项目ID对应的名称
        val storeComponentList = mutableListOf<MyStoreComponent>()
        val languageMap =
            storeBaseEnvQueryDao.batchQueryStoreLanguage(dslContext, storeIds).intoMap({ it.value1() }, { it.value2() })
        // 获取组件的流程信息
        val processingStoreRecords = storeBaseQueryDao.getStoreBaseInfoByConditions(
            dslContext = dslContext,
            storeType = storeType,
            storeCodeList = storeCodes,
            storeStatusList = StoreStatusEnum.getProcessingStatusList()
        )
        val processingVersionInfoMap = mutableMapOf<String, MutableList<StoreBaseInfo>>()
        processingStoreRecords.forEach { processingAtomRecord ->
            val version = processingAtomRecord[tStoreBase.VERSION] as String
            if (version == INIT_VERSION || version.isBlank()) {
                return@forEach
            }
            val storeCode = processingAtomRecord[tStoreBase.STORE_CODE] as String
            val storeBaseInfo = StoreBaseInfo(
                storeId = processingAtomRecord[tStoreBase.ID] as String,
                storeCode = storeCode,
                storeName = processingAtomRecord[tStoreBase.NAME] as String,
                storeType = StoreTypeEnum.valueOf(processingAtomRecord[tStoreBase.STORE_TYPE] as String),
                version = version,
                publicFlag = processingAtomRecord[TStoreBaseFeature.T_STORE_BASE_FEATURE.PUBLIC_FLAG] as Boolean,
                status = processingAtomRecord[tStoreBase.STATUS] as String,
                logoUrl = processingAtomRecord[tStoreBase.LOGO_URL],
                publisher = processingAtomRecord[tStoreBase.PUBLISHER] as String,
                classifyId = processingAtomRecord[tStoreBase.CLASSIFY_ID] as String,
            )
            processingVersionInfoMap.getOrPut(storeCode) { mutableListOf() }.add(storeBaseInfo)
        }
        // 根据项目Code获取对应的名称
        val projectMap =
            client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        records.forEach {
            val storeCode = it[tStoreBase.STORE_CODE] as String
            var releaseFlag = false // 是否有处于上架状态的组件版本
            val releaseStoreNum = storeBaseQueryDao.countReleaseStoreByCode(dslContext, storeCode, storeType)
            if (releaseStoreNum > 0) {
                releaseFlag = true
            }
            val logoUrl = it[tStoreBase.LOGO_URL]
            val storeId = it[tStoreBase.ID]
            storeComponentList.add(
                MyStoreComponent(
                    storeId = storeId,
                    storeCode = it[tStoreBase.STORE_CODE] as String,
                    storeType = storeType.name,
                    name = it[tStoreBase.NAME] as String,
                    language = languageMap[storeId],
                    logoUrl = logoUrl?.let {
                        StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(logoUrl) as? String
                    },
                    version = it[tStoreBase.VERSION] as String,
                    status = it[tStoreBase.STATUS] as String,
                    projectName = projectMap?.get(storeProjectMap[storeCode]) ?: "",
                    releaseFlag = releaseFlag,
                    creator = it[tStoreBase.CREATOR] as String,
                    modifier = it[tStoreBase.MODIFIER] as String,
                    createTime = DateTimeUtil.toDateTime(it[tStoreBase.CREATE_TIME] as LocalDateTime),
                    updateTime = DateTimeUtil.toDateTime(it[tStoreBase.UPDATE_TIME] as LocalDateTime),
                    processingVersionInfos = processingVersionInfoMap[storeCode]
                )
            )
        }
        return storeComponentList
    }

    override fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int,
        checkPermissionFlag: Boolean
    ): Page<StoreDeskVersionItem> {
        logger.info("getComponentVersionsByCode:Input:($userId,$storeCode,$page,$pageSize)")
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        // 判断当前用户是否是组件的成员
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeEnum.type.toByte()
            )
        ) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
        val count = storeBaseQueryDao.countByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum
        )
        val records = storeBaseQueryDao.getComponentsByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum,
            page = page,
            pageSize = pageSize
        )
        val storeIds = records.map { it.id }
        val versionMap = storeVersionLogDao.getStoreVersions(
            dslContext = dslContext,
            storeIds = storeIds,
            getTestVersionFlag = true
        )?.associateBy({ it.storeId }, { it.content }) ?: emptyMap()
        val baseExtRecords = storeBaseExtQueryDao.getBaseExtByIds(dslContext, storeIds)
        val baseExtMap = baseExtRecords.groupBy({ it.storeId }, { it.fieldName to it.fieldValue })
            .mapValues { it.value.toMap().toMutableMap() }
        val storeVersionInfos = records.map {
            StoreDeskVersionItem(
                storeId = it.id,
                storeCode = it.storeCode,
                storeType = StoreTypeEnum.getStoreType(it.storeType.toInt()),
                name = it.name,
                version = it.version,
                versionContent = versionMap[it.id],
                status = it.status,
                creator = it.creator,
                createTime = DateTimeUtil.toDateTime(it.createTime),
                extData = baseExtMap[it.id]
            )
        }
        return Page(
            page = page,
            pageSize = pageSize,
            count = count.toLong(),
            records = storeVersionInfos
        )
    }

    override fun getComponentDetailInfoById(
        userId: String,
        storeType: StoreTypeEnum,
        storeId: String
    ): StoreDetailInfo? {
        logger.info("getComponentDetailInfoById:Input:($userId,$storeType,$storeId)")
        val storeBaseRecord = storeBaseQueryDao.getComponentById(dslContext, storeId) ?: return null
        val storeCode = storeBaseRecord.storeCode
        val storeFeatureRecord = storeBaseFeatureQueryDao.getComponentFeatureDataByCode(
            dslContext = dslContext,
            storeCode = storeBaseRecord.storeCode,
            storeType = storeType
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeCode)
        )
        // 用户是否可安装组件
        val installFlag = storeUserService.isCanInstallStoreComponent(
            defaultFlag = storeFeatureRecord.publicFlag,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
        val labels = storeLabelService.getLabelsByStoreId(storeBaseRecord.id)
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, storeCode, storeType)

        val baseExtRecords = storeBaseExtQueryDao.getBaseExtByIds(dslContext, listOf(storeId))
        val extData = baseExtRecords.associateBy(
            { it.fieldName },
            {
                when {
                    JsonSchemaUtil.isJsonArray(it.fieldValue) -> {JsonUtil.to(it.fieldValue, List::class.java)}
                    JsonSchemaUtil.isJsonObject(it.fieldValue) -> {
                        JsonUtil.to(it.fieldValue, object : TypeReference<Map<String, Any>>() {})
                    }
                    else -> it.fieldValue
                }
            }
        )
        val htmlTemplateVersion = extData[KEY_HTML_TEMPLATE_VERSION]
        val initProjectCode =
            if (htmlTemplateVersion != null && htmlTemplateVersion == FrontendTypeEnum.HISTORY.typeVersion) {
                ""
            } else {
                storeProjectRelDao.getInitProjectCodeByStoreCode(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte()
                )
            }
        val classify = classifyDao.getClassify(dslContext, storeBaseRecord.classifyId)
        val versionLog =
            storeVersionLogDao.getStoreVersions(dslContext, listOf(storeId), true)?.firstOrNull()?.let {
                VersionModel(
                    publisher = storeBaseRecord.publisher,
                    releaseType = ReleaseTypeEnum.getReleaseTypeObj(it.releaseType.toInt())!!,
                    version = storeBaseRecord.version,
                    versionContent = it.content
                )
            }
        val statistic = storeTotalStatisticService.getStatisticByCode(userId, storeType.type.toByte(), storeCode)
        return StoreDetailInfo(
            storeId = storeId,
            storeCode = storeCode,
            storeType = storeType.name,
            name = storeBaseRecord.name,
            version = storeBaseRecord.version,
            status = storeBaseRecord.status,
            classify = classify?.let {
                Classify(
                    id = classify.id,
                    classifyCode = classify.classifyCode,
                    classifyName = I18nUtil.getCodeLanMessage(
                        messageCode = "${StoreTypeEnum.getStoreType(classify.type.toInt())}.classify.$classify.classifyCode",
                        defaultMessage = classify.classifyName
                    ),
                    classifyType =  StoreTypeEnum.getStoreType(classify.type.toInt()),
                    weight = classify.weight,
                    createTime = classify.createTime.timestampmilli(),
                    updateTime = classify.updateTime.timestampmilli()
                )
            },
            logoUrl = storeBaseRecord.logoUrl,
            versionInfo = versionLog,
            downloads = statistic.downloads,
            score = statistic.score,
            summary = storeBaseRecord.summary,
            description = storeBaseRecord.description,
            testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType
            ),
            initProjectCode = initProjectCode,
            categoryList = categoryService.getByRelStoreId(storeId),
            labelList = labels,
            latestFlag = storeBaseRecord.latestFlag,
            installFlag = installFlag,
            publicFlag = storeFeatureRecord.publicFlag,
            recommendFlag = storeFeatureRecord.recommendFlag,
            certificationFlag = storeFeatureRecord.certificationFlag,
            type = storeFeatureRecord.type,
            userCommentInfo = userCommentInfo,
            editFlag = storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            ),
            honorInfos = storeHonorService.getStoreHonor(userId, storeType, storeCode),
            indexInfos = storeIndexManageService.getStoreIndexInfosByStoreCode(storeType, storeCode),
            extData = extData
        )
    }

    override fun getComponentDetailInfoByCode(userId: String, storeType: String, storeCode: String): StoreDetailInfo? {
        logger.info("getComponentDetailInfoByCode:Input:($userId,$storeCode,$storeType)")
        return storeBaseQueryDao.getLatestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType)
        )?.let {
            getComponentDetailInfoById(userId, StoreTypeEnum.valueOf(storeType), it.id)
        }
    }

    override fun getMainPageComponents(
        userId: String,
        storeType: String,
        projectCode: String?,
        page: Int,
        pageSize: Int,
        urlProtocolTrim: Boolean
    ): Result<List<MarketMainItem>> {
        logger.info("getMainPageComponents:Input:($userId,$storeType,$page,$pageSize)")
        val result = mutableListOf<MarketMainItem>()
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        val futureList = mutableListOf<Future<Page<MarketItem>>>()
        val labelInfoList = mutableListOf<MarketMainItemLabel>()
        labelInfoList.add(
            MarketMainItemLabel(LATEST, I18nUtil.getCodeLanMessage(LATEST))
        )
        futureList.add(
            doList(
                userId = userId,
                userDeptList = userDeptList,
                storeInfoQuery = StoreInfoQuery(
                    storeType = storeType,
                    projectCode = projectCode,
                    keyword = null,
                    classifyId = null,
                    labelId = null,
                    score = null,
                    rdType = null,
                    categoryId = null,
                    recommendFlag = null,
                    installed = null,
                    updateFlag = null,
                    queryProjectComponentFlag = false,
                    sortType = StoreSortTypeEnum.UPDATE_TIME,
                    page = page,
                    pageSize = pageSize
                ),
                urlProtocolTrim = urlProtocolTrim
            )
        )
        labelInfoList.add(
            MarketMainItemLabel(
                HOTTEST,
                I18nUtil.getCodeLanMessage(HOTTEST)
            )
        )
        futureList.add(
            doList(
                userId = userId,
                userDeptList = userDeptList,
                storeInfoQuery = StoreInfoQuery(
                    storeType = storeType,
                    projectCode = projectCode,
                    keyword = null,
                    classifyId = null,
                    labelId = null,
                    score = null,
                    rdType = null,
                    categoryId = null,
                    recommendFlag = null,
                    installed = null,
                    updateFlag = null,
                    queryProjectComponentFlag = false,
                    sortType = StoreSortTypeEnum.DOWNLOAD_COUNT,
                    page = page,
                    pageSize = pageSize
                ),
                urlProtocolTrim = urlProtocolTrim
            )
        )

        val classifyList = storeClassifyService.getAllClassify(StoreTypeEnum.valueOf(storeType).type.toByte()).data
        classifyList?.forEach {
            val classifyCode = it.classifyCode
            if (classifyCode != "trigger") {
                val classifyLanName = I18nUtil.getCodeLanMessage(
                    messageCode = "$storeType.classify.$classifyCode",
                    defaultMessage = it.classifyName,
                    language = I18nUtil.getLanguage(userId)
                )
                labelInfoList.add(MarketMainItemLabel(classifyCode, classifyLanName))
                futureList.add(
                    doList(
                        userId = userId,
                        userDeptList = userDeptList,
                        storeInfoQuery = StoreInfoQuery(
                            storeType = storeType,
                            projectCode = projectCode,
                            keyword = null,
                            classifyId = it.id,
                            labelId = null,
                            score = null,
                            rdType = null,
                            categoryId = null,
                            recommendFlag = null,
                            installed = null,
                            updateFlag = null,
                            queryProjectComponentFlag = false,
                            sortType = StoreSortTypeEnum.DOWNLOAD_COUNT,
                            page = page,
                            pageSize = pageSize
                        ),
                        urlProtocolTrim = urlProtocolTrim
                    )
                )
            }
        }
        if (futureList.isEmpty()) {
            return Result(result)
        }
        for (index in futureList.indices) {
            val labelInfo = labelInfoList[index]
            result.add(
                MarketMainItem(
                    key = labelInfo.key,
                    label = labelInfo.label,
                    records = futureList[index].get().records
                )
            )
        }
        return Result(result)
    }

    override fun queryComponents(
        userId: String,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean
    ): Page<MarketItem> {
        logger.info("queryComponents:Input:" +
                "($userId,${storeInfoQuery.storeType},${storeInfoQuery.page},${storeInfoQuery.pageSize})")
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        return doList(
            userId = userId,
            userDeptList = userDeptList,
            storeInfoQuery = storeInfoQuery,
            urlProtocolTrim = urlProtocolTrim
        ).get()
    }

    override fun getComponentShowVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String
    ): StoreShowVersionInfo {
        logger.info("getComponentShowVersionInfo:Input:($userId,$storeCode,$storeType)")
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        val record = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeCode)
        )
        val cancelFlag = record.status == StoreStatusEnum.GROUNDING_SUSPENSION.name
        val showVersion = if (cancelFlag) {
            record.version
        } else {
            storeBaseQueryDao.getMaxVersionComponentByCode(dslContext, storeCode, storeTypeEnum)?.version
        }
        val releaseType = if (record.status == StoreStatusEnum.INIT.name) {
            null
        } else {
            storeVersionLogDao.getStoreVersion(dslContext, record.id)?.releaseType
        }
        val showReleaseType = if (releaseType != null) {
            ReleaseTypeEnum.getReleaseTypeObj(releaseType.toInt())
        } else {
            null
        }
        return storeCommonService.getStoreShowVersionInfo(cancelFlag, showReleaseType, showVersion)
    }

    private fun getStoreInfos(storeInfoQuery: StoreInfoQuery): Pair<Long, List<Record>> {
        val count = marketStoreQueryDao.count(
            dslContext = dslContext,
            storeInfoQuery = storeInfoQuery
        )
        val storeInfos = marketStoreQueryDao.list(
            dslContext = dslContext,
            storeInfoQuery = storeInfoQuery
        )

        return Pair(count.toLong(), storeInfos)
    }

    private fun doList(
        userId: String,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean = false
    ): Future<Page<MarketItem>> {
        logger.info("doList|storeType:${storeInfoQuery.storeType}")
        val referer = BkApiUtil.getHttpServletRequest()?.getHeader(REFERER)
        return executor.submit(Callable<Page<MarketItem>> {
            referer?.let {
                ThreadLocalUtil.set(REFERER, referer)
            }
            val results = mutableListOf<MarketItem>()

            // 调用拆分出的getStoreInfos函数获取商品信息
            var (count, storeInfos) = getStoreInfos(storeInfoQuery)
            try {
                val storeCodeList = mutableListOf<String>()
                val storeIds = mutableListOf<String>()
                val storeTypeEnum = StoreTypeEnum.valueOf(storeInfoQuery.storeType)
                val tStoreBase = TStoreBase.T_STORE_BASE
                val tStoreBaseFeature = TStoreBaseFeature.T_STORE_BASE_FEATURE
                val projectCode = storeInfoQuery.projectCode
                storeInfos.forEach {
                    storeCodeList.add(it[tStoreBase.STORE_CODE] as String)
                    storeIds.add(it[tStoreBaseFeature.ID] as String)
                }
                val storeVisibleData =
                    storeCommonService.generateStoreVisibleData(storeCodeList, storeTypeEnum)
                val storeStatisticData = storeTotalStatisticService.getStatisticByCodeList(
                    storeType = storeTypeEnum.type.toByte(),
                    storeCodeList = storeCodeList
                )
                // 获取用户
                val memberData = storeMemberService.batchListMember(storeCodeList, storeTypeEnum).data
                val installedInfoMap = projectCode?.let {
                    storeProjectService.getInstalledComponent(it, storeTypeEnum.type.toByte())
                }
                // 获取分类
                val classifyList = classifyService.getAllClassify(storeTypeEnum.type.toByte()).data
                val classifyMap = mutableMapOf<String, String>()
                classifyList?.forEach {
                    classifyMap[it.id] = it.classifyCode
                }
                val storeHonorInfoMap = storeHonorService.getHonorInfosByStoreCodes(storeTypeEnum, storeCodeList)
                val storeIndexInfosMap =
                    storeIndexManageService.getStoreIndexInfosByStoreCodes(storeTypeEnum, storeCodeList)
                val categoryInfoMap = categoryService.getByRelStoreIds(storeIds)
                storeInfos.forEach { record ->
                    val storeId = record[tStoreBase.ID]
                    val storeCode = record[tStoreBase.STORE_CODE]
                    val statistic = storeStatisticData[storeCode]
                    val version = record[tStoreBase.VERSION]
                    val installed = projectCode?.let { installedInfoMap?.contains(storeCode) }
                    val updateFlag = if (installed == true && installedInfoMap?.get(storeCode) != null) {
                        StoreUtils.isGreaterVersion(version, installedInfoMap[storeCode]!!)
                    } else null
                    if (storeInfoQuery.installed == true && installed != true) {
                        count -= 1
                        return@forEach
                    }
                    if (storeInfoQuery.updateFlag == true && updateFlag != true) {
                        count -= 1
                        return@forEach
                    }
                    val osList = mutableListOf<String>()
                    storeBaseEnvQueryDao.getBaseEnvsByStoreId(dslContext, storeId)?.forEach {
                        it.osName?.let { osName -> osList.add(osName) }
                    }
                    val marketItem =MarketItem(
                        id = storeId,
                        name = record[tStoreBase.NAME],
                        code = storeCode,
                        version = version,
                        type = StoreTypeEnum.getStoreType(record[tStoreBase.STORE_TYPE].toInt()),
                        rdType = record[tStoreBaseFeature.RD_TYPE],
                        classifyCode = classifyMap[record[tStoreBase.CLASSIFY_ID] as String],
                        category =
                        categoryInfoMap[record[tStoreBase.ID]]?.joinToString(",") { it.categoryCode },
                        logoUrl = record[tStoreBase.LOGO_URL]?.let { convertLogoUrl(it, urlProtocolTrim) },
                        publisher = record[tStoreBase.PUBLISHER],
                        os = if (osList.isEmpty()) null else osList,
                        downloads = statistic?.downloads ?: 0,
                        score = statistic?.score ?: 0.toDouble(),
                        summary = record[tStoreBase.SUMMARY],
                        flag = storeCommonService.generateInstallFlag(
                            defaultFlag = record[tStoreBaseFeature.PUBLIC_FLAG],
                            members = memberData?.get(storeCode),
                            userId = userId,
                            visibleList = storeVisibleData?.get(storeCode),
                            userDeptList = userDeptList
                        ),
                        publicFlag = record[tStoreBaseFeature.PUBLIC_FLAG],
                        buildLessRunFlag = null,
                        docsLink = record[tStoreBase.DOCS_LINK],
                        modifier = record[tStoreBase.MODIFIER],
                        updateTime = DateTimeUtil.toDateTime(record[tStoreBase.UPDATE_TIME] as LocalDateTime),
                        recommendFlag = record[tStoreBaseFeature.RECOMMEND_FLAG],
                        yamlFlag = null,
                        installed = installed,
                        updateFlag = updateFlag,
                        dailyStatisticList = getRecentDailyStatisticList(storeCode, storeTypeEnum),
                        honorInfos = storeHonorInfoMap[storeCode],
                        indexInfos = storeIndexInfosMap[storeCode],
                        recentExecuteNum = statistic?.recentExecuteNum,
                        hotFlag = statistic?.hotFlag,
                        extData = getExtData(storeCode, storeTypeEnum)
                    )
                    results.add(marketItem)
                }
            } finally {
                ThreadLocalUtil.remove(REFERER)
            }
            return@Callable Page(
                page = storeInfoQuery.page,
                pageSize = storeInfoQuery.pageSize,
                count = count,
                records = results
            )
        })
    }

    private fun getRecentDailyStatisticList(
        storeCode: String,
        storeType: StoreTypeEnum
    ): List<StoreDailyStatistic>? {
        // 统计昨天为截止日期的最近一周的数据
        val endTime = DateTimeUtil.convertDateToFormatLocalDateTime(
            date = DateTimeUtil.getFutureDateFromNow(Calendar.DAY_OF_MONTH, -1),
            format = "yyyy-MM-dd"
        )
        return storeDailyStatisticService.getDailyStatisticListByCode(
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            startTime = DateTimeUtil.convertDateToLocalDateTime(
                DateTimeUtil.getFutureDate(
                    localDateTime = endTime,
                    unit = Calendar.DAY_OF_MONTH,
                    timeSpan = -6
                )
            ),
            endTime = endTime
        )
    }

    private fun convertLogoUrl(url: String, urlProtocolTrim: Boolean): String? {
        var logoUrl = StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(url) as? String
        logoUrl = if (logoUrl?.contains("?") == true) {
            logoUrl.plus("&logo=true")
        } else {
            logoUrl?.plus("?logo=true")
        }
        if (urlProtocolTrim) { // #4796 LogoUrl跟随主站协议
            logoUrl = RegexUtils.trimProtocol(logoUrl)
        }
        return logoUrl
    }

    private fun getExtData(storeCode: String, storeType: StoreTypeEnum): MutableMap<String, Any>? {
        val extDataResult = storeBaseFeatureExtQueryDao.queryStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = StoreTypeEnum.getStoreTypeObj(storeType.type)
        )

        val extData = if (extDataResult.isEmpty()) {
            null
        } else {
            mutableMapOf<String, Any>()
        }

        extData?.let {
            extDataResult.forEach { record ->
                extData[record.fieldName] =
                when {
                    JsonSchemaUtil.isJsonArray(record.fieldValue) -> {JsonUtil.to(record.fieldValue, List::class.java)}
                    JsonSchemaUtil.isJsonObject(record.fieldValue) -> {
                        JsonUtil.to(record.fieldValue, object : TypeReference<Map<String, Any>>() {})
                    }
                    else -> record.fieldValue
                }
            }
        }
        return extData
    }
}
