/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.auth.REFERER
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_OS
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
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
import com.tencent.devops.model.store.tables.records.TStoreBaseExtRecord
import com.tencent.devops.model.store.tables.records.TStoreBaseFeatureExtRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.common.dao.MarketStoreQueryDao
import com.tencent.devops.store.common.dao.StoreBaseExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreVisibleProjectRelDao
import com.tencent.devops.store.common.service.CategoryService
import com.tencent.devops.store.common.service.ClassifyService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreComponentBaseInfoQueryService
import com.tencent.devops.store.common.service.StoreComponentMarketQueryService
import com.tencent.devops.store.common.service.StoreHonorService
import com.tencent.devops.store.common.service.StoreIndexManageService
import com.tencent.devops.store.common.service.StoreMemberService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.common.service.StoreTotalStatisticService
import com.tencent.devops.store.common.service.StoreUserService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.common.utils.StoreExtFieldUtil
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.KEY_BUILD_LESS_RUN_FLAG
import com.tencent.devops.store.pojo.common.KEY_URL_SCHEME
import com.tencent.devops.store.pojo.common.KEY_YAML_FLAG
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.MarketMainItemLabel
import com.tencent.devops.store.pojo.common.QueryGroupParam
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.category.Category
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import com.tencent.devops.store.pojo.common.statistic.StoreStatistic
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.Record
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentMarketQueryServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val marketStoreQueryDao: MarketStoreQueryDao,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao,
    private val storeBaseExtQueryDao: StoreBaseExtQueryDao,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao,
    private val storeVisibleProjectRelDao: StoreVisibleProjectRelDao,
    private val storeUserService: StoreUserService,
    private val classifyService: ClassifyService,
    private val storeProjectService: StoreProjectService,
    private val storeCommonService: StoreCommonService,
    private val storeTotalStatisticService: StoreTotalStatisticService,
    private val storeMemberService: StoreMemberService,
    private val storeHonorService: StoreHonorService,
    private val storeIndexManageService: StoreIndexManageService,
    private val categoryService: CategoryService,
    private val storeComponentBaseInfoQueryService: StoreComponentBaseInfoQueryService
) : StoreComponentMarketQueryService {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentMarketQueryServiceImpl::class.java)
        private val executor = Executors.newFixedThreadPool(30)
    }

    private val storeBusNumCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(100, TimeUnit.DAYS)
        .build<String, Long>()

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
            val classifyList = classifyService.getAllClassify(StoreTypeEnum.valueOf(storeType).type.toByte()).data
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
        try {
            storeInfoQuery.validate()
            // 查询项目维度组件时，校验用户是否为该项目成员
            val projectCode = storeInfoQuery.projectCode
            if (storeInfoQuery.getSpecQueryFlag() && !projectCode.isNullOrBlank()) {
                verifyUserProjectPermission(userId = userId, projectCode = projectCode)
            }
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
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    /**
     * 校验用户是否拥有项目查看权限。
     * 权限服务调用失败抛系统错误，避免误报为无权限。
     */
    private fun verifyUserProjectPermission(userId: String, projectCode: String) {
        val validateFlag: Boolean?
        try {
            validateFlag = client.get(ServiceProjectResource::class).verifyUserProjectPermission(
                projectCode = projectCode,
                userId = userId
            ).data
        } catch (ignored: Throwable) {
            logger.warn("verifyUserProjectPermission error, params[$userId|$projectCode]", ignored)
            throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
        }
        if (validateFlag != true) {
            throw ErrorCodeException(errorCode = StoreMessageCode.USER_QUERY_PROJECT_PERMISSION_IS_INVALID)
        }
    }

    override fun enrichMarketItems(
        userId: String,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery,
        storeInfos: List<Record>,
        urlProtocolTrim: Boolean
    ): List<MarketItem> {
        if (storeInfos.isEmpty()) {
            return emptyList()
        }
        return handleMarketItem(
            userId = userId,
            userDeptList = userDeptList,
            storeInfoQuery = storeInfoQuery,
            urlProtocolTrim = urlProtocolTrim,
            storeInfos = storeInfos
        )
    }

    override fun getComponentGroupCount(userId: String, queryGroupParam: QueryGroupParam): List<Pair<String, Int>> {
        val watcher = Watcher("getComponentGroupCount|$userId|$queryGroupParam")
        try {
            watcher.start("queryComponents")
            // 有权限的组件code
            val storeCodes = queryComponents(
                userId = userId,
                storeInfoQuery = StoreInfoQuery(
                    storeType = queryGroupParam.storeType.name,
                    page = NUM_ONE,
                    pageSize = Int.MAX_VALUE,
                    queryProjectComponentFlag = false
                )
            ).records.map { it.code }.toSet()
            watcher.start("queryComponentGroupCount")
            return storeBaseQueryDao.getComponentGroupCount(
                dslContext = dslContext,
                queryGroupParam = queryGroupParam,
                storeCodes = storeCodes
            )
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

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

    @Suppress("LongMethod", "ComplexMethod")
    private fun handleQueryStoreCodes(storeInfoQuery: StoreInfoQuery) {
        val projectCode = storeInfoQuery.projectCode!!
        val storeType = StoreTypeEnum.valueOf(storeInfoQuery.storeType)
        val queryTestFlag = storeInfoQuery.queryTestFlag
        val storeBaseTable = TStoreBase.T_STORE_BASE

        // 1. 封装可复用查询逻辑 - 获取指定类型的项目组件
        /**
         * 获取项目组件映射
         * @param projectType 项目类型枚举
         * @return 组件代码与版本的映射表
         */
        fun getComponents(projectType: StoreProjectTypeEnum): Map<String, String?> =
            storeProjectService.getProjectComponents(
                projectCode = projectCode,
                storeType = storeType.type.toByte(),
                storeProjectTypes = listOf(projectType.type.toByte()),
                instanceId = storeInfoQuery.instanceId
            ) ?: emptyMap()

        // 2. 优化测试组件查询逻辑 - 根据查询标志获取测试组件
        val installComponentMap = getComponents(StoreProjectTypeEnum.COMMON)
        val testComponentVersionMap = when (queryTestFlag ?: true) {
            true -> {
                val testMap = getComponents(StoreProjectTypeEnum.TEST)
                // 查询有效的测试组件版本信息
                storeBaseQueryDao.getValidComponentsByCodes(
                    dslContext = dslContext,
                    storeCodes = testMap.keys,
                    storeType = storeType,
                    testComponentFlag = true
                ).associate {
                    it[storeBaseTable.STORE_CODE] to it[storeBaseTable.VERSION]
                }
            }
            false -> null
        }
        val testStoreCodes = testComponentVersionMap?.keys.orEmpty()

        // 3. 版本映射合并逻辑 - 合并公共组件和测试组件版本信息
        val componentVersionMap = when {
            queryTestFlag == true -> testComponentVersionMap ?: emptyMap()
            else -> {
                // 获取所有公共组件
                val publicComponents = storeBaseFeatureQueryDao.getAllPublicComponent(dslContext, storeType)
                // 计算普通组件代码集合（排除测试组件）
                val normalStoreCodes = installComponentMap.keys + publicComponents - testStoreCodes
                // 查询普通组件版本信息
                val normalVersionMap = storeBaseQueryDao.getValidComponentsByCodes(
                    dslContext = dslContext,
                    storeCodes = normalStoreCodes,
                    storeType = storeType,
                    testComponentFlag = false
                ).associate {
                    it[storeBaseTable.STORE_CODE] to it[storeBaseTable.VERSION]
                }
                // 合并普通组件和测试组件版本映射
                normalVersionMap + testComponentVersionMap.orEmpty()
            }
        }

        // 4. 缓存处理逻辑 - 缓存业务编号避免重复计算
        val busNumCache = mutableMapOf<Pair<String, String>, Long>()
        /**
         * 获取缓存中的业务编号
         * @param storeCode 组件代码
         * @param version 组件版本
         * @return 业务编号值
         */
        fun getCachedBusNum(storeCode: String, version: String) = busNumCache.getOrPut(storeCode to version) {
            getStoreBusNum(storeType, storeCode, version) ?: 0
        }

        // 5. 过滤逻辑链式调用 - 根据查询条件过滤组件
        val filteredEntries = componentVersionMap
            .entries
            .filter { (storeCode, _) ->
                // 根据安装状态过滤组件
                storeInfoQuery.installed?.let { it == installComponentMap.containsKey(storeCode) } ?: true
            }

        // 处理测试组件过滤逻辑
        val finalTestStoreCodes = filteredEntries
            .asSequence()
            .filter { (storeCode, _) -> storeCode in testStoreCodes }
            .filter { storeInfoQuery.updateFlag != false } // 当updateFlag不为false时保留测试组件
            .map { it.key }
            .toSet()

        // 处理普通组件更新标志过滤逻辑
        val normalCandidates = filteredEntries.filterNot { it.key in finalTestStoreCodes }
        val finalNormalStoreCodes = normalCandidates
            .filter { (storeCode, version) ->
                val installedVersion = installComponentMap[storeCode]
                when (storeInfoQuery.updateFlag) {
                    // 需要更新的组件：当前版本业务编号大于已安装版本
                    true -> installedVersion?.let {
                        getCachedBusNum(storeCode, version) > getCachedBusNum(storeCode, it)
                    } ?: true
                    // 不需要更新的组件：当前版本业务编号小于等于已安装版本
                    false -> installedVersion?.let {
                        getCachedBusNum(storeCode, version) <= getCachedBusNum(storeCode, it)
                    } ?: false

                    null -> true // 不进行更新标志过滤
                }
            }
            .map { it.key }
            .toSet()

        // 6. 结果赋值 - 设置最终查询结果
        storeInfoQuery.normalStoreCodes = finalNormalStoreCodes
        storeInfoQuery.testStoreCodes = finalTestStoreCodes
    }

    private fun getStoreBusNum(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    ): Long? {
        val storeBusNumCacheKey = StoreUtils.getStoreFieldKeyPrefix(storeType, storeCode, version)
        // 尝试从缓存获取业务编号，若不存在则从数据库查询
        val busNum = storeBusNumCache.getIfPresent(storeBusNumCacheKey) ?: storeBaseQueryDao.getMaxBusNumByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            version = version
        )
        // 若获取到有效业务编号，更新缓存
        busNum?.let {
            storeBusNumCache.put(storeBusNumCacheKey, busNum)
        }
        return busNum
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
                try {
                    referer?.let {
                        ThreadLocalUtil.set(REFERER, referer)
                    }
                    watcher.start("getStoreInfos")
                    // 分页查询组件信息
                    val (count, storeInfos) = getStoreInfos(userDeptList, storeInfoQuery)
                    watcher.start("handleMarketItem")
                    val results = handleMarketItem(
                        userId = userId,
                        userDeptList = userDeptList,
                        storeInfoQuery = storeInfoQuery,
                        urlProtocolTrim = urlProtocolTrim,
                        storeInfos = storeInfos
                    )
                    return@Callable Page(
                        page = storeInfoQuery.page,
                        pageSize = storeInfoQuery.pageSize,
                        count = count,
                        records = results
                    )
                } finally {
                    watcher.stop()
                    LogUtils.printCostTimeWE(watcher)
                    ThreadLocalUtil.remove(REFERER)
                }
            }
        )
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun handleMarketItem(
        userId: String,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery,
        urlProtocolTrim: Boolean = false,
        storeInfos: List<Record>
    ): List<MarketItem> {
        val watcher = Watcher(id = "handleMarketItem|$userId|$userDeptList|$storeInfoQuery")
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
            // 获取宿主应用信息
            val ownerStoreCodes = storeInfos.mapNotNull { it[tStoreBase.OWNER_STORE_CODE] }.toSet()
            val ownerStoreInfos = storeComponentBaseInfoQueryService.getComponentBaseInfoList(
                storeType = StoreTypeEnum.DEVX,
                storeCodes = ownerStoreCodes
            ).associate { it.storeCode to it.storeName }
            // 批量查询扩展信息，避免循环内逐组件单查的N+1：
            // 组件级共享扩展(T_STORE_BASE_FEATURE_EXT)按storeCode聚合；版本级扩展(T_STORE_BASE_EXT)按storeId聚合
            watcher.start("batchQueryExtInfo")
            val featureExtRecordsByCode = if (storeCodeList.isEmpty()) {
                emptyMap()
            } else {
                storeBaseFeatureExtQueryDao.batchQueryStoreBaseFeatureExt(
                    dslContext = dslContext,
                    storeCodes = storeCodeList,
                    storeType = storeTypeEnum
                ).groupBy { it.storeCode }
            }
            val versionExtRecordsById = if (storeIds.isEmpty()) {
                emptyMap()
            } else {
                storeBaseExtQueryDao.getBaseExtByIds(dslContext, storeIds).groupBy { it.storeId }
            }
            watcher.start("handleStoreInfos")
            // 用于计算安装/可用标识(flag)，避免"因项目授权可见但flag=false导致看得到却装不了"。
            val projectVisibleStoreCodes = if (projectCode != null && storeCodeList.isNotEmpty()) {
                storeVisibleProjectRelDao.listStoreCodesByProject(
                    dslContext = dslContext,
                    storeType = storeTypeEnum.type.toByte(),
                    projectCode = projectCode,
                    storeCodes = storeCodeList
                )
            } else {
                emptySet()
            }
            val ctx = MarketItemCtx(
                tStoreBase = tStoreBase,
                tStoreBaseFeature = tStoreBaseFeature,
                storeTypeEnum = storeTypeEnum,
                projectCode = projectCode,
                userId = userId,
                userDeptList = userDeptList,
                urlProtocolTrim = urlProtocolTrim,
                storeInfoQuery = storeInfoQuery,
                projectVisibleStoreCodes = projectVisibleStoreCodes,
                storeStatisticData = storeStatisticData,
                storeVisibleData = storeVisibleData,
                memberData = memberData,
                installedInfoMap = installedInfoMap,
                classifyMap = classifyMap,
                storeHonorInfoMap = storeHonorInfoMap,
                storeIndexInfosMap = storeIndexInfosMap,
                categoryInfoMap = categoryInfoMap,
                ownerStoreInfos = ownerStoreInfos,
                featureExtRecordsByCode = featureExtRecordsByCode,
                versionExtRecordsById = versionExtRecordsById
            )
            return storeInfos.map { record -> buildMarketItem(record, ctx) }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    private class MarketItemCtx(
        val tStoreBase: TStoreBase,
        val tStoreBaseFeature: TStoreBaseFeature,
        val storeTypeEnum: StoreTypeEnum,
        val projectCode: String?,
        val userId: String,
        val userDeptList: List<Int>,
        val urlProtocolTrim: Boolean,
        val storeInfoQuery: StoreInfoQuery,
        val projectVisibleStoreCodes: Set<String>,
        val storeStatisticData: HashMap<String, StoreStatistic>,
        val storeVisibleData: HashMap<String, MutableList<Int>>?,
        val memberData: HashMap<String, MutableList<String>>?,
        val installedInfoMap: Map<String, String?>?,
        val classifyMap: Map<String, String>,
        val storeHonorInfoMap: Map<String, List<HonorInfo>>,
        val storeIndexInfosMap: Map<String, List<StoreIndexInfo>>,
        val categoryInfoMap: Map<String, List<Category>>,
        val ownerStoreInfos: Map<String, String>,
        val featureExtRecordsByCode: Map<String, List<TStoreBaseFeatureExtRecord>>,
        val versionExtRecordsById: Map<String, List<TStoreBaseExtRecord>>
    )

    @Suppress("LongMethod", "ComplexMethod")
    private fun buildMarketItem(record: Record, ctx: MarketItemCtx): MarketItem {
        val storeId = record[ctx.tStoreBase.ID]
        val storeCode = record[ctx.tStoreBase.STORE_CODE]
        val statistic = ctx.storeStatisticData[storeCode]
        val version = record[ctx.tStoreBase.VERSION]
        val status = record[ctx.tStoreBase.STATUS]
        val ownerStoreCode = record[ctx.tStoreBase.OWNER_STORE_CODE]
        // 组件是否已安装
        val installed = ctx.storeInfoQuery.installed ?: run {
            ctx.projectCode?.let { ctx.installedInfoMap?.contains(storeCode) }
        }
        // 是否可更新
        val installedVersion = ctx.installedInfoMap?.get(storeCode)
        val updateFlag = ctx.storeInfoQuery.updateFlag ?: run {
            // 当已安装版本不存在时或处于测试状态直接返回true，避免后续无效计算
            if (installedVersion == null || status in StoreStatusEnum.getTestStatusList()) true
            else {
                val currentBusNum = record[ctx.tStoreBase.BUS_NUM] ?: 0
                val installedBusNum = getStoreBusNum(ctx.storeTypeEnum, storeCode, installedVersion) ?: 0
                currentBusNum > installedBusNum
            }
        }
        // 组件级共享扩展(installPath/os/yamlFlag等)与版本级扩展，均取自循环外的批量结果
        val featureExtRecords = ctx.featureExtRecordsByCode[storeCode].orEmpty()
        val versionExtRecords = ctx.versionExtRecordsById[storeId].orEmpty()
        val osList = featureExtRecords.firstOrNull { it.fieldName == KEY_OS }?.fieldValue
            ?.let { JsonUtil.to(it, object : TypeReference<List<String>>() {}) }
        // 无构建环境组件是否可以在有构建环境运行
        val buildLessRunFlag = versionExtRecords
            .firstOrNull { it.fieldName == KEY_BUILD_LESS_RUN_FLAG }?.fieldValue?.toBoolean()
        // 组件级共享扩展：所有版本共享(如 installPath、os、yamlFlag 等)
        val featureExtData = if (featureExtRecords.isNotEmpty()) {
            featureExtRecords.associate { it.fieldName to StoreExtFieldUtil.formatJson(it.fieldValue) }
        } else {
            null
        }
        // 版本级扩展：列表保持精简，仅取 urlScheme；
        // installType/installParams 等安装明细由详情接口(StoreDetailInfo)/部署接口按需提供
        val urlScheme = versionExtRecords.firstOrNull { it.fieldName == KEY_URL_SCHEME }?.fieldValue
        val versionExtData = if (!urlScheme.isNullOrBlank()) mapOf(KEY_URL_SCHEME to urlScheme) else null
        // 向后兼容：保持 extData 为组件级+版本级合并，旧客户端仍可读取
        val mergedExt = mutableMapOf<String, Any>()
        featureExtData?.let { mergedExt.putAll(it) }
        versionExtData?.let { mergedExt.putAll(it) }
        val extData: Map<String, Any>? = mergedExt.ifEmpty { null }
        val publicFlag = record[ctx.tStoreBaseFeature.PUBLIC_FLAG] ?: false
        return MarketItem(
            id = storeId,
            name = record[ctx.tStoreBase.NAME],
            code = storeCode,
            version = version,
            status = status,
            type = StoreTypeEnum.getStoreType(record[ctx.tStoreBase.STORE_TYPE].toInt()),
            rdType = record[ctx.tStoreBaseFeature.RD_TYPE],
            classifyCode = ctx.classifyMap[record[ctx.tStoreBase.CLASSIFY_ID] as String],
            category =
            ctx.categoryInfoMap[record[ctx.tStoreBase.ID]]?.joinToString(",") { it.categoryCode },
            logoUrl = record[ctx.tStoreBase.LOGO_URL]?.let { convertLogoUrl(it, ctx.urlProtocolTrim) },
            publisher = record[ctx.tStoreBase.PUBLISHER],
            os = osList,
            downloads = statistic?.downloads ?: 0,
            score = statistic?.score ?: 0.toDouble(),
            summary = record[ctx.tStoreBase.SUMMARY],
            flag = storeCommonService.generateInstallFlag(
                defaultFlag = publicFlag,
                members = ctx.memberData?.get(storeCode),
                userId = ctx.userId,
                visibleList = ctx.storeVisibleData?.get(storeCode),
                userDeptList = ctx.userDeptList
            ) || storeCode in ctx.projectVisibleStoreCodes,
            publicFlag = publicFlag,
            buildLessRunFlag = buildLessRunFlag,
            docsLink = record[ctx.tStoreBase.DOCS_LINK],
            modifier = record[ctx.tStoreBase.MODIFIER],
            updateTime = DateTimeUtil.toDateTime(record[ctx.tStoreBase.UPDATE_TIME] as LocalDateTime),
            recommendFlag = record[ctx.tStoreBaseFeature.RECOMMEND_FLAG],
            yamlFlag = featureExtData?.get(KEY_YAML_FLAG) as? Boolean,
            installed = installed,
            updateFlag = updateFlag,
            honorInfos = ctx.storeHonorInfoMap[storeCode],
            indexInfos = ctx.storeIndexInfosMap[storeCode],
            recentExecuteNum = statistic?.recentExecuteNum,
            hotFlag = statistic?.hotFlag,
            extData = extData,
            ownerStoreName = if (ownerStoreCode.isNullOrBlank()) {
                null
            } else {
                ctx.ownerStoreInfos[ownerStoreCode]
            },
            ownerStoreCode = ownerStoreCode
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
}
