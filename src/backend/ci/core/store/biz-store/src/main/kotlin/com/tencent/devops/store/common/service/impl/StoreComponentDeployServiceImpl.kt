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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.records.TStoreBaseRecord
import com.tencent.devops.store.common.dao.MarketStoreQueryDao
import com.tencent.devops.store.common.dao.StoreBaseExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.StoreComponentDeployService
import com.tencent.devops.store.common.service.StoreComponentQueryService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.common.service.StoreTotalStatisticService
import com.tencent.devops.store.common.service.StoreUserService
import com.tencent.devops.store.common.utils.StoreExtFieldUtil
import com.tencent.devops.store.pojo.common.KEY_URL_SCHEME
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.deploy.ComponentDeployVersionInfo
import com.tencent.devops.store.pojo.common.deploy.UserComponentDeployInfo
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentDeployServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeUserService: StoreUserService,
    private val storeProjectService: StoreProjectService,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseExtQueryDao: StoreBaseExtQueryDao,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao,
    private val marketStoreQueryDao: MarketStoreQueryDao,
    private val storeTotalStatisticService: StoreTotalStatisticService,
    private val storeComponentQueryService: StoreComponentQueryService
) : StoreComponentDeployService {

    override fun getUserComponentDeployInfos(
        userId: String,
        storeInfoQuery: StoreInfoQuery
    ): Page<UserComponentDeployInfo> {
        val storeTypeEnum = StoreTypeEnum.valueOf(storeInfoQuery.storeType)
        // 调试组件代码：这些组件除已发布版本外，还需展示测试中/填写中/审核中的版本。
        // 即"某个实例被设置为某个组件的调试用例"。因此调试组件需按(projectCode, instanceId)匹配：
        //   - 传了instanceId(调试实例)时，仅返回被配置为该实例调试用例的组件，与getComponentUpgradeVersionInfo判定测试环境的口径一致；
        //   - 未传instanceId时，INSTANCE_ID不参与过滤，返回该项目下所有调试组件。
        val testStoreCodes = resolveTestStoreCodes(storeInfoQuery, storeTypeEnum)

        // 排序与分页策略：整列表 = "用户可见的已发布组件" 与 "纯调试组件(从未发布、仅测试中/审核中)" 的并集，
        // 按 sortType 指定字段统一排序(缺省更新时间倒序)。新增排序字段只需扩展 deployComparator / MySQL applySorting。
        storeInfoQuery.validate()
        val sortType = storeInfoQuery.sortType ?: StoreSortTypeEnum.UPDATE_TIME
        // UPGRADE 是"升级列表"场景的排序，并非组件自身字段，本接口不支持
        if (sortType == StoreSortTypeEnum.UPGRADE) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf("sortType"))
        }

        val userDeptList = storeUserService.getUserDeptList(userId)
        // 纯调试组件：被配置为当前调试用例(projectCode/instanceId)、但从未发布过(无RELEASED版本)的组件，与可见已发布组件不相交
        val debugOnlyCodes = resolveDebugOnlyCodes(testStoreCodes, storeTypeEnum)

        // 计算当前页有序条目、当前页可见组件明细、总数
        val pageData = queryPageData(
            storeInfoQuery = storeInfoQuery,
            userDeptList = userDeptList,
            debugOnlyCodes = debugOnlyCodes,
            storeTypeEnum = storeTypeEnum,
            sortType = sortType
        )
        if (pageData.pageEntries.isEmpty()) {
            return Page(
                page = storeInfoQuery.page,
                pageSize = storeInfoQuery.pageSize,
                count = pageData.totalCount,
                records = emptyList()
            )
        }

        // 富化当前页：版本列表 + 扩展信息(均按本页组件代码批量查询，富化限制在一页内)
        val records = buildPageRecords(
            pageData = pageData,
            userId = userId,
            userDeptList = userDeptList,
            storeInfoQuery = storeInfoQuery,
            testStoreCodes = testStoreCodes,
            storeTypeEnum = storeTypeEnum
        )
        return Page(
            page = storeInfoQuery.page,
            pageSize = storeInfoQuery.pageSize,
            count = pageData.totalCount,
            records = records
        )
    }

    /**
     * 当前页的有序条目：仅承载输出顺序与归属(是否纯调试组件)，富化时按 storeCode 取明细。
     */
    private data class DeployPageEntry(
        val storeCode: String,
        val debugOnly: Boolean
    )

    /**
     * 合并路径(存在纯调试组件)下参与内存归并排序的组件属性。统一承载"可见已发布组件"与"纯调试组件"的排序字段，
     * 新增排序维度时在此补充字段并在 [deployComparator] 增加分支即可，避免把排序逻辑写死。
     */
    private data class DeploySortAttr(
        val storeCode: String,
        val debugOnly: Boolean,
        val name: String?,
        val publisher: String?,
        val createTime: LocalDateTime?,
        val updateTime: LocalDateTime?,
        val downloads: Int
    )

    /**
     * 部署页面查询的分页结果，承载当前页条目、可见组件明细与总数。
     */
    private data class DeployPageData(
        val pageEntries: List<DeployPageEntry>,
        val pageVisibleRecords: List<Record>,
        val totalCount: Long
    )

    /**
     * 根据排序字段构造比较器(均为倒序，storeCode 升序兜底保证排序键相同时翻页稳定)。
     * 与 MySQL 侧 applySorting 的字段保持一致，新增排序字段时两处同步扩展即可。
     */
    private fun deployComparator(sortType: StoreSortTypeEnum): Comparator<DeploySortAttr> {
        val byField: Comparator<DeploySortAttr> = when (sortType) {
            StoreSortTypeEnum.NAME -> compareByDescending(nullsLast()) { it.name }
            StoreSortTypeEnum.PUBLISHER -> compareByDescending(nullsLast()) { it.publisher }
            StoreSortTypeEnum.CREATE_TIME -> compareByDescending(nullsLast()) { it.createTime }
            StoreSortTypeEnum.UPDATE_TIME -> compareByDescending(nullsLast()) { it.updateTime }
            StoreSortTypeEnum.DOWNLOAD_COUNT -> compareByDescending { it.downloads }
            StoreSortTypeEnum.UPGRADE ->
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("sortType")
                )
        }
        return byField.thenBy { it.storeCode }
    }

    /**
     * 解析部署查询中的测试组件代码集合：从项目中获取配置为测试类型的组件。
     */
    private fun resolveTestStoreCodes(
        storeInfoQuery: StoreInfoQuery,
        storeTypeEnum: StoreTypeEnum
    ): Set<String> {
        val projectCode = storeInfoQuery.projectCode
        return if (!projectCode.isNullOrBlank()) {
            storeProjectService.getProjectComponents(
                projectCode = projectCode,
                storeType = storeTypeEnum.type.toByte(),
                storeProjectTypes = listOf(StoreProjectTypeEnum.TEST.type.toByte()),
                instanceId = storeInfoQuery.instanceId
            )?.keys.orEmpty()
        } else {
            emptySet()
        }
    }

    /**
     * 解析"纯调试组件"代码集合：被配置为调试用例、但从未发布过(无RELEASED版本)的组件。
     */
    private fun resolveDebugOnlyCodes(
        testStoreCodes: Set<String>,
        storeTypeEnum: StoreTypeEnum
    ): Set<String> {
        if (testStoreCodes.isEmpty()) return emptySet()
        val releasedTestCodes = storeBaseQueryDao.getStoreBaseInfoByConditions(
            dslContext = dslContext,
            storeCodeList = testStoreCodes.toList(),
            storeType = storeTypeEnum,
            storeStatusList = listOf(StoreStatusEnum.RELEASED.name)
        ).map { it.storeCode }.toSet()
        return testStoreCodes - releasedTestCodes
    }

    /**
     * 查询部署页面分页数据，包含当前页条目、可见组件明细与总数。
     * - 无纯调试组件时：排序+分页完全由MySQL完成(快路径)；
     * - 有纯调试组件时：取排序字段做内存归并排序(合并路径)。
     */
    private fun queryPageData(
        storeInfoQuery: StoreInfoQuery,
        userDeptList: List<Int>,
        debugOnlyCodes: Set<String>,
        storeTypeEnum: StoreTypeEnum,
        sortType: StoreSortTypeEnum
    ): DeployPageData {
        val tStoreBase = TStoreBase.T_STORE_BASE
        if (debugOnlyCodes.isEmpty()) {
            // 快路径：整列表即用户可见的已发布组件，排序+分页完全由MySQL完成(无内存排序、无全量拉取)
            val deployQuery = storeInfoQuery.copy(sortType = sortType)
            val totalCount = marketStoreQueryDao.count(dslContext, userDeptList, deployQuery).toLong()
            val pageVisibleRecords = marketStoreQueryDao.list(dslContext, userDeptList, deployQuery).toList()
            val pageEntries = pageVisibleRecords.map {
                DeployPageEntry(it[tStoreBase.STORE_CODE] as String, debugOnly = false)
            }
            return DeployPageData(pageEntries, pageVisibleRecords, totalCount)
        }
        // 合并路径：可见组件与纯调试组件按 sortType 统一排序(storeCode升序兜底保证翻页稳定)。
        val visibleSortKeys = marketStoreQueryDao.listStoreSortKeys(dslContext, userDeptList, storeInfoQuery)
        val debugLatestByCode = storeBaseQueryDao.getStoreBaseInfoByConditions(
            dslContext = dslContext,
            storeCodeList = debugOnlyCodes.toList(),
            storeType = storeTypeEnum,
            storeStatusList = mutableListOf(StoreStatusEnum.RELEASED.name).apply {
                addAll(StoreStatusEnum.getTestStatusList())
            }
        ).groupBy { it.storeCode }.mapValues { (_, records) ->
            records.firstOrNull { it.latestFlag } ?: records.maxByOrNull { it.updateTime }
        }
        // 仅按下载量排序时才查统计数据(可见组件 + 纯调试组件)，其余排序字段无需统计
        val downloadsByCode: Map<String, Int> = if (sortType == StoreSortTypeEnum.DOWNLOAD_COUNT) {
            val codes = (visibleSortKeys.map { it.value1() } + debugOnlyCodes).distinct()
            storeTotalStatisticService.getStatisticByCodeList(
                storeType = storeTypeEnum.type.toByte(),
                storeCodeList = codes
            ).mapValues { it.value.downloads }
        } else {
            emptyMap()
        }
        val sortAttrs = buildList {
            visibleSortKeys.forEach { record ->
                add(
                    DeploySortAttr(
                        storeCode = record.value1(),
                        debugOnly = false,
                        name = record.value2(),
                        publisher = record.value3(),
                        createTime = record.value4(),
                        updateTime = record.value5(),
                        downloads = downloadsByCode[record.value1()] ?: 0
                    )
                )
            }
            debugOnlyCodes.forEach { code ->
                debugLatestByCode[code]?.let { record ->
                    add(
                        DeploySortAttr(
                            storeCode = code,
                            debugOnly = true,
                            name = record.name,
                            publisher = record.publisher,
                            createTime = record.createTime,
                            updateTime = record.updateTime,
                            downloads = downloadsByCode[code] ?: 0
                        )
                    )
                }
            }
        }.sortedWith(deployComparator(sortType))
        val totalCount = sortAttrs.size.toLong()
        val fromIndex = ((storeInfoQuery.page - 1).toLong() * storeInfoQuery.pageSize)
            .coerceIn(0L, sortAttrs.size.toLong()).toInt()
        val toIndex = (fromIndex.toLong() + storeInfoQuery.pageSize)
            .coerceIn(0L, sortAttrs.size.toLong()).toInt()
        val pageAttrs = if (fromIndex < toIndex) sortAttrs.subList(fromIndex, toIndex) else emptyList()
        val pageEntries = pageAttrs.map { DeployPageEntry(it.storeCode, it.debugOnly) }
        val pageVisibleCodes = pageAttrs.filterNot { it.debugOnly }.map { it.storeCode }
        val pageVisibleRecords = if (pageVisibleCodes.isNotEmpty()) {
            marketStoreQueryDao.listReleasedByStoreCodes(dslContext, storeTypeEnum, pageVisibleCodes).toList()
        } else {
            emptyList()
        }
        return DeployPageData(pageEntries, pageVisibleRecords, totalCount)
    }

    /**
     * 富化当前页的部署组件记录：查询版本信息、扩展信息，构造最终部署信息列表。
     */
    private fun buildPageRecords(
        pageData: DeployPageData,
        userId: String,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery,
        testStoreCodes: Set<String>,
        storeTypeEnum: StoreTypeEnum
    ): List<UserComponentDeployInfo> {
        val pageCodes = pageData.pageEntries.map { it.storeCode }
        // 版本状态：默认仅已发布；调试组件额外含测试中/填写中/审核中
        val statusList = mutableListOf(StoreStatusEnum.RELEASED.name)
        if (testStoreCodes.isNotEmpty()) {
            statusList.addAll(StoreStatusEnum.getTestStatusList())
        }
        val componentRecords = storeBaseQueryDao.getStoreBaseInfoByConditions(
            dslContext = dslContext,
            storeCodeList = pageCodes,
            storeType = storeTypeEnum,
            storeStatusList = statusList
        )
        // 非调试组件仅保留已发布版本；调试组件保留已发布+测试中/审核中版本
        val recordsByCode = componentRecords.groupBy { it.storeCode }.mapValues { (code, records) ->
            if (code in testStoreCodes) records
            else records.filter { it.status == StoreStatusEnum.RELEASED.name }
        }
        // 批量查询各版本的扩展信息(安装方式/安装参数等，跟随版本，存于extBaseInfo)
        val versionStoreIds = recordsByCode.values.flatten().map { it.id }
        val baseExtMap: Map<String, Map<String, Any>> = if (versionStoreIds.isEmpty()) {
            emptyMap()
        } else {
            storeBaseExtQueryDao.getBaseExtByIds(dslContext, versionStoreIds)
                .groupBy({ it.storeId }, { it.fieldName to StoreExtFieldUtil.formatJson(it.fieldValue) })
                .mapValues { it.value.toMap() }
        }
        // 组件级共享扩展(installPath等)按本页全部代码一次性批量查询，避免逐组件单查的N+1
        val featureExtByCode = buildFeatureExtMap(pageCodes, storeTypeEnum)
        // 可见组件复用市场富化(仅传本页记录，逐条富化被限制在一页内)；纯调试组件由DAO直接构造
        val marketItemByCode = if (pageData.pageVisibleRecords.isNotEmpty()) {
            storeComponentQueryService.enrichMarketItems(
                userId = userId,
                userDeptList = userDeptList,
                storeInfoQuery = storeInfoQuery,
                storeInfos = pageData.pageVisibleRecords,
                urlProtocolTrim = true
            ).associateBy { it.code }
        } else {
            emptyMap()
        }
        // 按统一排序顺序输出
        return pageData.pageEntries.mapNotNull { entry ->
            if (entry.debugOnly) {
                buildDebugOnlyDeployInfo(
                    storeCode = entry.storeCode,
                    storeType = storeTypeEnum,
                    records = recordsByCode[entry.storeCode].orEmpty(),
                    baseExtMap = baseExtMap,
                    featureExtData = featureExtByCode[entry.storeCode]
                )
            } else {
                marketItemByCode[entry.storeCode]?.let { marketItem ->
                    buildVisibleDeployInfo(
                        marketItem = marketItem,
                        versionRecords = recordsByCode[entry.storeCode].orEmpty(),
                        baseExtMap = baseExtMap,
                        featureExtData = featureExtByCode[entry.storeCode]
                    )
                }
            }
        }
    }

    /**
     * 批量查询组件级共享扩展(取自extBaseFeatureInfo，如installPath)，按 storeCode 聚合为 字段名->字段值 映射。
     * 仅查特性扩展表即可，版本级字段(urlScheme等)由各版本的baseExtMap承载，此处过滤掉。
     */
    private fun buildFeatureExtMap(
        storeCodes: List<String>,
        storeType: StoreTypeEnum
    ): Map<String, Map<String, Any>> {
        if (storeCodes.isEmpty()) return emptyMap()
        return storeBaseFeatureExtQueryDao.batchQueryStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCodes = storeCodes,
            storeType = storeType
        ).filter { it.fieldName != KEY_URL_SCHEME }
            .groupBy({ it.storeCode }, { it.fieldName to StoreExtFieldUtil.formatJson(it.fieldValue) })
            .mapValues { it.value.toMap() }
    }

    /**
     * 由市场富化结果(MarketItem)构造可见已发布组件的部署信息。
     */
    private fun buildVisibleDeployInfo(
        marketItem: MarketItem,
        versionRecords: List<TStoreBaseRecord>,
        baseExtMap: Map<String, Map<String, Any>>,
        featureExtData: Map<String, Any>?
    ): UserComponentDeployInfo {
        val latestVersion = marketItem.version
        val versions = versionRecords.map { record ->
            ComponentDeployVersionInfo(
                version = record.version,
                storeId = record.id,
                latestFlag = record.version == latestVersion,
                status = record.status,
                // 版本级扩展(含安装方式installType、安装参数installParams等)
                extData = baseExtMap[record.id]
            )
        }
        return UserComponentDeployInfo(
            storeCode = marketItem.code,
            storeType = marketItem.type,
            storeId = marketItem.id,
            name = marketItem.name,
            installFlag = marketItem.flag,
            latestVersion = latestVersion,
            // 组件级共享扩展(含安装路径installPath等)，由deploy自行批量查询特性扩展表得到，
            // 不再依赖MarketItem携带，保持市场POJO对外只暴露合并后的extData
            extData = featureExtData?.takeIf { it.isNotEmpty() },
            versionInfos = versions
        )
    }

    /**
     * 构造"纯调试组件"(从未发布、仅有测试中/审核中版本)的部署信息。
     * 这类组件不在用户可见的已发布列表中，但被配置为当前调试用例(projectCode/instanceId)，需在调试场景下展示其测试版本。
     */
    private fun buildDebugOnlyDeployInfo(
        storeCode: String,
        storeType: StoreTypeEnum,
        records: List<TStoreBaseRecord>,
        baseExtMap: Map<String, Map<String, Any>>,
        featureExtData: Map<String, Any>?
    ): UserComponentDeployInfo? {
        if (records.isEmpty()) {
            return null
        }
        // 取最新版本记录(优先latestFlag为true的记录，否则取创建时间最新的一条；记录已按创建时间倒序)
        val latestRecord = records.firstOrNull { it.latestFlag } ?: records.first()
        val versions = records.map { record ->
            ComponentDeployVersionInfo(
                version = record.version,
                storeId = record.id,
                latestFlag = record.id == latestRecord.id,
                status = record.status,
                extData = baseExtMap[record.id]
            )
        }
        return UserComponentDeployInfo(
            storeCode = storeCode,
            storeType = storeType.name,
            storeId = latestRecord.id,
            name = latestRecord.name,
            // 纯调试组件被配置为当前调试用例，用户具备拉取/安装(调试)该组件的权限
            installFlag = true,
            latestVersion = latestRecord.version,
            extData = featureExtData?.takeIf { it.isNotEmpty() },
            versionInfos = versions
        )
    }
}
