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
import com.tencent.devops.common.api.constant.KEY_OS
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonSchemaUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ThreadLocalUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.LogUtils
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
import com.tencent.devops.store.pojo.common.KEY_BUILD_LESS_RUN_FLAG
import com.tencent.devops.store.pojo.common.KEY_HTML_TEMPLATE_VERSION
import com.tencent.devops.store.pojo.common.KEY_URL_SCHEME
import com.tencent.devops.store.pojo.common.KEY_YAML_FLAG
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.MarketMainItemLabel
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.QueryComponentsParam
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreDeskVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.VersionInfo
import com.tencent.devops.store.pojo.common.version.VersionModel
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Suppress("TooManyFunctions", "LargeClass")
@Service
class StoreComponentQueryServiceImpl : StoreComponentQueryService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var storeBaseQueryDao: StoreBaseQueryDao

    @Autowired
    lateinit var storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao

    @Autowired
    lateinit var storeUserService: StoreUserService

    @Autowired
    lateinit var storeBaseEnvQueryDao: StoreBaseEnvQueryDao

    @Autowired
    lateinit var storeLabelService: StoreLabelService

    @Autowired
    lateinit var storeCommentService: StoreCommentService

    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var storeVersionLogDao: StoreVersionLogDao

    @Autowired
    lateinit var storeBaseExtQueryDao: StoreBaseExtQueryDao

    @Autowired
    lateinit var classifyDao: ClassifyDao

    @Autowired
    lateinit var storeTotalStatisticService: StoreTotalStatisticService

    @Autowired
    lateinit var categoryService: CategoryService

    @Autowired
    lateinit var storeHonorService: StoreHonorService

    @Autowired
    lateinit var storeIndexManageService: StoreIndexManageService

    @Autowired
    lateinit var storeCommonService: StoreCommonService

    @Autowired
    lateinit var marketStoreQueryDao: MarketStoreQueryDao

    @Autowired
    lateinit var storeMemberService: StoreMemberService

    @Autowired
    lateinit var classifyService: ClassifyService

    @Autowired
    lateinit var storeProjectService: StoreProjectService

    @Autowired
    lateinit var storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao

    @Autowired
    lateinit var storeClassifyService: ClassifyService

    @Autowired
    lateinit var labelDao: LabelDao

    companion object {
        private val executor = Executors.newFixedThreadPool(30)
    }

    override fun getMyComponents(
        userId: String,
        storeType: String,
        name: String?,
        page: Int,
        pageSize: Int
    ): Page<MyStoreComponent>? {
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        // 获取有权限的组件代码列表
        val (count, records) = queryMyComponents(
            userId = userId,
            storeTypeEnum = storeTypeEnum,
            name = name,
            page = page,
            pageSize = pageSize
        )
        val storeProjectMap = mutableMapOf<String, String>()
        val storeIds = mutableListOf<String>()
        val storeCodes = mutableListOf<String>()
        records.forEach { record ->
            storeIds.add(record[TStoreBase.T_STORE_BASE.ID])
            val storeCode = record[TStoreBase.T_STORE_BASE.STORE_CODE] as String
            storeCodes.add(storeCode)
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeEnum
            )
            if (null != testProjectCode) {
                storeProjectMap[storeCode] = testProjectCode
            }
        }
        val storeComponents = handleComponents(
            storeType = storeTypeEnum,
            storeCodes = storeCodes,
            storeIds = storeIds,
            storeProjectMap = storeProjectMap,
            records = records
        )
        return Page(
            count = count.toLong(),
            page = page,
            pageSize = pageSize,
            records = storeComponents
        )
    }

    private fun queryMyComponents(
        userId: String,
        storeTypeEnum: StoreTypeEnum,
        name: String?,
        page: Int,
        pageSize: Int
    ): Pair<Int, List<Record>> {
        val conditions = storeBaseQueryDao.generateGetMyComponentConditions(
            userId = userId,
            storeType = storeTypeEnum,
            storeName = name
        )
        val records = storeBaseQueryDao.getMyComponents(
            dslContext = dslContext,
            conditions = conditions,
            page = page,
            pageSize = pageSize
        )
        val count = storeBaseQueryDao.countMyComponents(dslContext, conditions)
        return Pair(count, records)
    }

    override fun listComponents(userId: String, queryComponentsParam: QueryComponentsParam): Page<MyStoreComponent>? {
        val storeType = StoreTypeEnum.valueOf(queryComponentsParam.storeType)
        val classifyId = queryComponentsParam.classifyCode?.let {
            classifyDao.getClassifyByCode(
                dslContext = dslContext,
                classifyCode = it,
                type = storeType
            )
        }
        val categoryIds = queryComponentsParam.categoryCodes?.let { queryComponentsCategoryIds(storeType, it) }
        val labelIds = queryComponentsParam.labelCodes?.let {
            labelDao.getIdsByCodes(dslContext, it.split(","), storeType.type.toByte())
        }
        val count = storeBaseQueryDao.countComponents(
            dslContext = dslContext,
            queryComponentsParam = queryComponentsParam,
            classifyId = classifyId?.id,
            categoryIds = categoryIds,
            labelIds = labelIds
        )
        val records = storeBaseQueryDao.listComponents(
            dslContext = dslContext,
            queryComponentsParam = queryComponentsParam,
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
        // 查询组件初始化项目信息
        storeProjectRelDao.getStoreInitProjects(
            dslContext = dslContext,
            storeType = storeType.type.toByte(),
            descFlag = false,
            specProjectCodeList = storeCodes.toSet()
        )?.forEach { storeProjectMap[it.storeCode] = it.projectCode }
        val storeComponents = handleComponents(
            storeType = storeType,
            storeCodes = storeCodes,
            storeIds = storeIds,
            storeProjectMap = storeProjectMap,
            records = records
        )
        return Page(
            count = count.toLong(),
            page = queryComponentsParam.page,
            pageSize = queryComponentsParam.pageSize,
            records = storeComponents
        )
    }

    private fun queryComponentsCategoryIds(storeType: StoreTypeEnum, categoryCodes: String): List<String>? {
        val allCategory = categoryService.getAllCategory(storeType.type.toByte()).data
        val categoryCodeList = categoryCodes.split(",")
        return allCategory?.filter {
                category ->
            categoryCodeList.contains(category.categoryCode)
        }?.map { categoryInfo ->
            categoryInfo.id
        }
    }

    @Suppress("LongMethod")
    private fun handleComponents(
        storeType: StoreTypeEnum,
        storeCodes: List<String>,
        storeIds: List<String>,
        storeProjectMap: Map<String, String>,
        records: List<Record>
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
        val publicFlagInfoMap = storeBaseFeatureQueryDao.getComponentPublicFlagInfo(
            dslContext = dslContext,
            storeCodes = processingStoreRecords.map { it.storeCode },
            storeType = storeType
        )
        val processingVersionInfoMap = mutableMapOf<String, MutableList<StoreBaseInfo>>()
        processingStoreRecords.forEach { processingStoreRecord ->
            val storeId = processingStoreRecord[tStoreBase.ID] as String
            val version = processingStoreRecord[tStoreBase.VERSION] as String
            if (version.isBlank() || storeIds.contains(storeId)) {
                return@forEach
            }
            val storeCode = processingStoreRecord[tStoreBase.STORE_CODE] as String
            val logoUrl = processingStoreRecord[tStoreBase.LOGO_URL]
            val storeBaseInfo = StoreBaseInfo(
                storeId = storeId,
                storeCode = storeCode,
                storeName = processingStoreRecord[tStoreBase.NAME] as String,
                storeType = storeType,
                version = version,
                publicFlag = publicFlagInfoMap[storeCode] ?: false,
                status = processingStoreRecord[tStoreBase.STATUS] as String,
                logoUrl = logoUrl?.let {
                    StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(logoUrl) as? String
                },
                publisher = processingStoreRecord[tStoreBase.PUBLISHER] as String,
                classifyId = processingStoreRecord[tStoreBase.CLASSIFY_ID] as String
            )
            processingVersionInfoMap.getOrPut(storeCode) { mutableListOf() }.add(storeBaseInfo)
        }
        val projectCodeList = storeProjectMap.values
        // 根据项目Code获取对应的名称
        val projectMap =
            client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        records.forEach {
            val storeCode = it[tStoreBase.STORE_CODE] as String
            var releaseFlag = false // 是否有处于上架状态的组件版本
            val releaseStoreNum = storeBaseQueryDao.countByCondition(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType,
                status = StoreStatusEnum.RELEASED
            )
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
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        // 判断当前用户是否是组件的成员
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeEnum.type.toByte()
            )
        ) {
            throw ErrorCodeException(errorCode = StoreMessageCode.GET_INFO_NO_PERMISSION, params = arrayOf(storeCode))
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
        //
        val baseExtRecords = storeBaseExtQueryDao.getBaseExtByIds(dslContext, storeIds)
        val baseExtMap = baseExtRecords.groupBy({ it.storeId }, {
            it.fieldName to formatJson(it.fieldValue)
        }).mapValues { it.value.toMap() }
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

    @Suppress("LongMethod")
    override fun getComponentDetailInfoById(
        userId: String,
        storeType: StoreTypeEnum,
        storeId: String
    ): StoreDetailInfo? {
        val storeBaseRecord = storeBaseQueryDao.getComponentById(dslContext, storeId) ?: return null
        val storeCode = storeBaseRecord.storeCode
        val storeFeatureRecord = storeBaseFeatureQueryDao.getBaseFeatureByCode(
            dslContext = dslContext,
            storeCode = storeBaseRecord.storeCode,
            storeType = storeType
        ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeCode))
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
        var extData = baseExtRecords.associateBy({ it.fieldName }, { formatJson(it.fieldValue) })
        val baseFeatureExtRecords = storeBaseFeatureExtQueryDao.queryStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType
        )
        extData = extData.plus(baseFeatureExtRecords.associateBy({ it.fieldName }, { formatJson(it.fieldValue) }))
        val htmlTemplateVersion = extData[KEY_HTML_TEMPLATE_VERSION]
        val initProjectCode =
            if (htmlTemplateVersion != null && htmlTemplateVersion == FrontendTypeEnum.HISTORY.typeVersion) {
                ""
            } else {
                storeProjectRelDao.getInitProjectCodeByStoreCode(dslContext, storeCode, storeType.type.toByte())
            }
        val classify = classifyService.getClassify(storeBaseRecord.classifyId).data
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
        val newestComponentRecord = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType
        )
        val logoUrl = storeBaseRecord.logoUrl
        return StoreDetailInfo(
            storeId = storeId,
            storeCode = storeCode,
            storeType = storeType.name,
            name = storeBaseRecord.name,
            version = storeBaseRecord.version,
            status = storeBaseRecord.status,
            classify = classify,
            logoUrl = logoUrl?.let {
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(logoUrl) as? String
            },
            versionInfo = versionLog,
            downloads = statistic.downloads,
            score = statistic.score,
            summary = storeBaseRecord.summary,
            description = storeBaseRecord.description?.let {
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
            },
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
            rdType = storeFeatureRecord.rdType,
            userCommentInfo = userCommentInfo,
            editFlag = StoreUtils.checkEditCondition(newestComponentRecord!!.status),
            honorInfos = storeHonorService.getStoreHonor(userId, storeType, storeCode),
            indexInfos = storeIndexManageService.getStoreIndexInfosByStoreCode(storeType, storeCode),
            extData = extData
        )
    }

    override fun getComponentDetailInfoByCode(userId: String, storeType: String, storeCode: String): StoreDetailInfo? {
        return storeBaseQueryDao.getLatestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType)
        )?.let {
            getComponentDetailInfoById(userId, StoreTypeEnum.valueOf(storeType), it.id)
        }
    }

    @Suppress("LongMethod")
    override fun getMainPageComponents(
        userId: String,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean
    ): Result<List<MarketMainItem>> {
        val storeType = storeInfoQuery.storeType
        val page = storeInfoQuery.page
        val pageSize = storeInfoQuery.pageSize
        val projectCode = storeInfoQuery.projectCode
        storeInfoQuery.validate()
        val watcher = Watcher("getMainPageComponents|$userId|$storeType")
        try {
            val result = mutableListOf<MarketMainItem>()
            // 获取用户组织架构
            val userDeptList = storeUserService.getUserDeptList(userId)
            val futureList = mutableListOf<Future<Page<MarketItem>>>()
            val labelInfoList = mutableListOf<MarketMainItemLabel>()
            labelInfoList.add(
                MarketMainItemLabel(LATEST, I18nUtil.getCodeLanMessage(LATEST))
            )
            watcher.start("getMainPageComponentsBySortType")
            futureList.add(
                // 根据最新排序
                doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    storeInfoQuery = StoreInfoQuery(
                        storeType = storeType,
                        projectCode = projectCode,
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
                // 根据热度排序
                doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    storeInfoQuery = StoreInfoQuery(
                        storeType = storeType,
                        projectCode = projectCode,
                        queryProjectComponentFlag = false,
                        sortType = StoreSortTypeEnum.DOWNLOAD_COUNT,
                        page = page,
                        pageSize = pageSize
                    ),
                    urlProtocolTrim = urlProtocolTrim
                )
            )

            watcher.start("getMainPageComponentsByClassify")
            // 根据分类排序
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
                                classifyId = it.id,
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
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    override fun queryComponents(
        userId: String,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean
    ): Page<MarketItem> {
        val watcher = Watcher("queryComponents|$userId|$storeInfoQuery")
        storeInfoQuery.validate()
        // 获取用户组织架构
        watcher.start("getUserDeptList")
        val userDeptList = storeUserService.getUserDeptList(userId)
        watcher.start("queryComponentData")
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
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        val record = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeCode)
        )
        // 获取回显版本号
        val cancelFlag = record.status == StoreStatusEnum.GROUNDING_SUSPENSION.name
        val showVersion = if (cancelFlag) {
            record.version
        } else {
            storeBaseQueryDao.getNewestComponentByCode(dslContext, storeCode, storeTypeEnum)?.version
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

    override fun getComponentUpgradeVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        projectCode: String,
        instanceId: String?,
        osName: String?,
        osArch: String?
    ): VersionInfo? {
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)

        // 判断是否需要处理测试中的版本
        val isTestEnv = storeProjectRelDao.getProjectRelInfo(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum.type.toByte(),
            storeProjectType = StoreProjectTypeEnum.TEST,
            projectCode = projectCode,
            instanceId = instanceId
        )?.any() ?: false

        // 获取最新可用版本：根据环境状态构建版本状态过滤条件
        val statusList = mutableListOf(StoreStatusEnum.RELEASED.name).apply {
            if (isTestEnv) {
                // 增加测试中的状态
                addAll(StoreStatusEnum.getTestStatusList())
            }
        }
        // 查询符合条件的最新版本组件
        val validLatestVersionRecord = storeBaseQueryDao.getMaxVersionComponentByCode(
            dslContext = dslContext,
            storeType = storeTypeEnum,
            storeCode = storeCode,
            statusList = statusList
        ) ?: return null

        // 获取已安装组件关系信息：查询项目关联记录
        val installedRel = storeProjectRelDao.getProjectRelInfo(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum.type.toByte(),
            storeProjectType = StoreProjectTypeEnum.COMMON,
            projectCode = projectCode,
            instanceId = instanceId
        )?.firstOrNull()
        val installedVersion = installedRel?.version
        val validLatestBusNum = validLatestVersionRecord.busNum
        val validLatestVersion = validLatestVersionRecord.version
        // 未安装时直接返回最新版本信息
        if (installedVersion.isNullOrBlank()) {
            return createVersionInfo(validLatestVersion)
        }

        // 获取当前安装版本对应的业务号（用于版本比较）
        val currentBusNum = storeBaseQueryDao.getMaxBusNumByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum,
            version = installedVersion
        )

        // 业务号比较逻辑（决定是否需要升级）
        return when {
            // 无法获取当前业务号时返回null
            currentBusNum == null -> null
            // 最新业务号更大时需要升级
            validLatestBusNum > currentBusNum -> createVersionInfo(validLatestVersion)
            // 业务号相同，但是安装时间晚于最新版本发布时间，需要更新
            validLatestBusNum == currentBusNum && isUpdateRequired(
                storeId = validLatestVersionRecord.id,
                installedTime = installedRel.createTime,
                osName = osName,
                osArch = osArch
            ) -> createVersionInfo(validLatestVersion)
            // 其他情况不需要升级
            else -> null
        }
    }

    private fun isUpdateRequired(
        storeId: String,
        installedTime: LocalDateTime,
        osName: String?,
        osArch: String?
    ): Boolean {
        val envRecord = storeBaseEnvQueryDao.getBaseEnvsByStoreId(
            dslContext = dslContext,
            storeId = storeId,
            osName = osName,
            osArch = osArch
        )?.firstOrNull()
        return envRecord?.updateTime.let { packageTime ->
            installedTime < packageTime
        }
    }

    private fun createVersionInfo(versionValue: String, versionName: String = versionValue) =
        VersionInfo(
            versionName = versionName.takeIf { it.isNotBlank() } ?: versionValue,
            versionValue = versionValue
        )

    private fun getStoreInfos(userDeptList: List<Int>, storeInfoQuery: StoreInfoQuery): Pair<Long, List<Record>> {
        if (storeInfoQuery.getSpecQueryFlag()) {
            handleQueryStoreCodes(storeInfoQuery)
        }
        return marketStoreQueryDao.run {
            val count = count(
                dslContext = dslContext,
                userDeptList = userDeptList,
                storeInfoQuery = storeInfoQuery
            )
            val storeList = list(
                dslContext = dslContext,
                userDeptList = userDeptList,
                storeInfoQuery = storeInfoQuery
            )
            count.toLong() to storeList
        }
    }

    private fun handleQueryStoreCodes(
        storeInfoQuery: StoreInfoQuery
    ) {
        val projectCode = storeInfoQuery.projectCode!!
        val storeType = StoreTypeEnum.valueOf(storeInfoQuery.storeType)
        val queryTestFlag = storeInfoQuery.queryTestFlag
        // 查询项目下已安装的组件版本信息
        val installComponentMap = storeProjectService.getProjectComponents(
            projectCode = projectCode,
            storeType = storeType.type.toByte(),
            storeProjectTypes = listOf(
                StoreProjectTypeEnum.COMMON.type.toByte()
            ),
            instanceId = storeInfoQuery.instanceId
        ) ?: emptyMap()
        val tStoreBase = TStoreBase.T_STORE_BASE
        var testStoreCodes = emptySet<String>()
        var testComponentVersionMap: Map<String, String>? = null
        if (queryTestFlag != false) {
            // 查询项目下可调试的组件版本信息
            val testComponentMap = storeProjectService.getProjectComponents(
                projectCode = projectCode,
                storeType = storeType.type.toByte(),
                storeProjectTypes = listOf(
                    StoreProjectTypeEnum.TEST.type.toByte()
                ),
                instanceId = storeInfoQuery.instanceId
            ) ?: emptyMap()
            // 查询测试或者审核中组件最新版本信息
            testComponentVersionMap = storeBaseQueryDao.getValidComponentsByCodes(
                dslContext = dslContext,
                storeCodes = testComponentMap.keys,
                storeType = storeType,
                testComponentFlag = true
            ).intoMap({ it[tStoreBase.STORE_CODE] }, { it[tStoreBase.VERSION] })
            testStoreCodes = testComponentVersionMap.keys
        }
        val componentVersionMap = if (queryTestFlag != true) {
            // 查询非测试或者审核中组件最新发布版本信息
            val publicComponentList = storeBaseFeatureQueryDao.getAllPublicComponent(dslContext, storeType)
            val normalStoreCodes = installComponentMap.keys.plus(publicComponentList).toMutableSet()
            normalStoreCodes.removeAll(testStoreCodes)
            val normalComponentVersionMap = storeBaseQueryDao.getValidComponentsByCodes(
                dslContext = dslContext,
                storeCodes = normalStoreCodes,
                storeType = storeType,
                testComponentFlag = false
            ).intoMap({ it[tStoreBase.STORE_CODE] }, { it[tStoreBase.VERSION] }).toMutableMap()
            testComponentVersionMap?.let {
                normalComponentVersionMap += it
            }
            normalComponentVersionMap
        } else {
            testComponentVersionMap
        }

        val finalNormalStoreCodes = mutableSetOf<String>()
        val finalTestStoreCodes = mutableSetOf<String>()
        componentVersionMap?.forEach {
            val storeCode = it.key
            val version = it.value
            val updateFlag = storeInfoQuery.updateFlag
            val installed = storeInfoQuery.installed
            installed?.let {
                // 根据是否安装条件筛选组件
                if (installed != installComponentMap.containsKey(storeCode)) {
                    return@forEach
                }
            }
            val installedVersion = installComponentMap[storeCode]
            if (testStoreCodes.contains(storeCode) && updateFlag != false) {
                finalTestStoreCodes.add(storeCode)
            } else {
                // 比较当前安装的版本与组件最新版本
                val shouldAddStoreCode = when {
                    updateFlag == null -> true
                    updateFlag -> installedVersion == null || StoreUtils.isGreaterVersion(version, installedVersion)
                    else -> installedVersion != null && !StoreUtils.isGreaterVersion(version, installedVersion)
                }
                if (shouldAddStoreCode) {
                    finalNormalStoreCodes.add(storeCode)
                }
            }
        }
        storeInfoQuery.normalStoreCodes = finalNormalStoreCodes
        storeInfoQuery.testStoreCodes = finalTestStoreCodes
    }

    private fun doList(
        userId: String,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean = false
    ): Future<Page<MarketItem>> {
        val referer = BkApiUtil.getHttpServletRequest()?.getHeader(REFERER)
        return executor.submit(
            Callable {
                val watcher = Watcher(id = "doQueryDataList|$userId|$userDeptList|$storeInfoQuery")
                referer?.let {
                    ThreadLocalUtil.set(REFERER, referer)
                }
                val results: List<MarketItem>?
                watcher.start("getStoreInfos")
                // 分页查询组件信息
                val (count, storeInfos) = getStoreInfos(userDeptList, storeInfoQuery)
                try {
                    watcher.start("handleMarketItem")
                    results = handleMarketItem(
                        userId = userId,
                        userDeptList = userDeptList,
                        storeInfoQuery = storeInfoQuery,
                        urlProtocolTrim = urlProtocolTrim,
                        storeInfos = storeInfos
                    )
                } finally {
                    ThreadLocalUtil.remove(REFERER)
                }
                return@Callable Page(
                    page = storeInfoQuery.page,
                    pageSize = storeInfoQuery.pageSize,
                    count = count,
                    records = results ?: emptyList()
                )
            }
        )
    }

    @Suppress("LongMethod")
    private fun handleMarketItem(
        userId: String,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean = false,
        storeInfos: List<Record>
    ): List<MarketItem> {
        val watcher = Watcher(id = "handleMarketItem|$userId|$userDeptList|$storeInfoQuery")
        val results = mutableListOf<MarketItem>()
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
        // 获取组件可见范围
        watcher.start("generateStoreVisibleData")
        val storeVisibleData =
            storeCommonService.generateStoreVisibleData(storeCodeList, storeTypeEnum)
        // 获取组件统计信息
        watcher.start("getStatisticByCodeList")
        val storeStatisticData = storeTotalStatisticService.getStatisticByCodeList(
            storeType = storeTypeEnum.type.toByte(),
            storeCodeList = storeCodeList
        )
        // 获取用户
        watcher.start("batchListMember")
        val memberData = storeMemberService.batchListMember(storeCodeList, storeTypeEnum).data
        // 获取项目下已安装组件
        watcher.start("getInstalledComponents")
        val installedInfoMap = projectCode?.let {
            storeProjectService.getProjectComponents(
                projectCode = it,
                storeType = storeTypeEnum.type.toByte(),
                storeProjectTypes = listOf(StoreProjectTypeEnum.COMMON.type.toByte()),
                instanceId = storeInfoQuery.instanceId
            )
        }
        // 获取分类
        watcher.start("getAllClassify")
        val classifyList = classifyService.getAllClassify(storeTypeEnum.type.toByte()).data
        val classifyMap = mutableMapOf<String, String>()
        classifyList?.forEach {
            classifyMap[it.id] = it.classifyCode
        }
        // 获取组件荣誉信息及指标
        watcher.start("getHonorInfosByStoreCodes")
        val storeHonorInfoMap = storeHonorService.getHonorInfosByStoreCodes(storeTypeEnum, storeCodeList)
        val storeIndexInfosMap =
            storeIndexManageService.getStoreIndexInfosByStoreCodes(storeTypeEnum, storeCodeList)
        val categoryInfoMap = categoryService.getByRelStoreIds(storeIds)
        watcher.start("handleStoreInfos")
        storeInfos.forEach { record ->
            val storeId = record[tStoreBase.ID]
            val storeCode = record[tStoreBase.STORE_CODE]
            val statistic = storeStatisticData[storeCode]
            val version = record[tStoreBase.VERSION]
            val status = record[tStoreBase.STATUS]
            // 组件是否已安装
            val installed = storeInfoQuery.installed ?: run {
                projectCode?.let { installedInfoMap?.contains(storeCode) }
            }
            // 是否可更新
            val installedVersion = installedInfoMap?.get(storeCode)
            val updateFlag = storeInfoQuery.updateFlag ?: run {
                installedVersion == null || StoreUtils.isGreaterVersion(
                    version,
                    installedVersion
                ) || status in StoreStatusEnum.getTestStatusList()
            }
            val osList = queryComponentOsName(storeCode, storeTypeEnum)
            // 无构建环境组件是否可以在有构建环境运行
            val buildLessRunFlag = storeBaseExtQueryDao.getBaseExtByStoreId(
                dslContext = dslContext,
                storeId = storeId,
                fieldName = KEY_BUILD_LESS_RUN_FLAG
            ).firstOrNull()?.fieldValue?.toBoolean()
            val extData = getBaseExtData(storeId, storeCode, storeTypeEnum)
            val publicFlag = record[tStoreBaseFeature.PUBLIC_FLAG] ?: false
            val marketItem = MarketItem(
                id = storeId,
                name = record[tStoreBase.NAME],
                code = storeCode,
                version = version,
                status = status,
                type = StoreTypeEnum.getStoreType(record[tStoreBase.STORE_TYPE].toInt()),
                rdType = record[tStoreBaseFeature.RD_TYPE],
                classifyCode = classifyMap[record[tStoreBase.CLASSIFY_ID] as String],
                category =
                categoryInfoMap[record[tStoreBase.ID]]?.joinToString(",") { it.categoryCode },
                logoUrl = record[tStoreBase.LOGO_URL]?.let { convertLogoUrl(it, urlProtocolTrim) },
                publisher = record[tStoreBase.PUBLISHER],
                os = osList,
                downloads = statistic?.downloads ?: 0,
                score = statistic?.score ?: 0.toDouble(),
                summary = record[tStoreBase.SUMMARY],
                flag = storeCommonService.generateInstallFlag(
                    defaultFlag = publicFlag,
                    members = memberData?.get(storeCode),
                    userId = userId,
                    visibleList = storeVisibleData?.get(storeCode),
                    userDeptList = userDeptList
                ),
                publicFlag = publicFlag,
                buildLessRunFlag = buildLessRunFlag,
                docsLink = record[tStoreBase.DOCS_LINK],
                modifier = record[tStoreBase.MODIFIER],
                updateTime = DateTimeUtil.toDateTime(record[tStoreBase.UPDATE_TIME] as LocalDateTime),
                recommendFlag = record[tStoreBaseFeature.RECOMMEND_FLAG],
                yamlFlag = extData?.get(KEY_YAML_FLAG) as? Boolean,
                installed = installed,
                updateFlag = updateFlag,
                honorInfos = storeHonorInfoMap[storeCode],
                indexInfos = storeIndexInfosMap[storeCode],
                recentExecuteNum = statistic?.recentExecuteNum,
                hotFlag = statistic?.hotFlag,
                extData = extData
            )
            results.add(marketItem)
        }
        return results
    }

    private fun queryComponentOsName(storeCode: String, storeType: StoreTypeEnum): List<String>? {
        val os = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            fieldName = KEY_OS
        )?.fieldValue
        return os?.let { JsonUtil.to(it, object : TypeReference<List<String>>() {}) }
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

    private fun getBaseExtData(storeId: String, storeCode: String, storeType: StoreTypeEnum): MutableMap<String, Any>? {
        val extDataResult = storeBaseFeatureExtQueryDao.queryStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = StoreTypeEnum.getStoreTypeObj(storeType.type)
        )
        val urlScheme = storeBaseExtQueryDao.getBaseExtByStoreId(
            dslContext = dslContext,
            storeId = storeId,
            fieldName = KEY_URL_SCHEME
        ).firstOrNull()?.fieldValue
        val extData = if (extDataResult.isNotEmpty || !urlScheme.isNullOrBlank()) {
            mutableMapOf<String, Any>().apply {
                urlScheme?.let { put(KEY_URL_SCHEME, it) }
                extDataResult.forEach { record ->
                    put(record.fieldName, formatJson(record.fieldValue))
                }
            }
        } else null
        return extData
    }

    // 解析字段值为json格式字符串的数据
    private fun formatJson(str: String): Any {
        return when {
            JsonSchemaUtil.isJsonArray(str) -> {
                JsonUtil.to(str, List::class.java)
            }
            JsonSchemaUtil.isJsonObject(str) -> {
                JsonUtil.to(str, object : TypeReference<Map<String, Any>>() {})
            }
            else -> str
        }
    }
}
