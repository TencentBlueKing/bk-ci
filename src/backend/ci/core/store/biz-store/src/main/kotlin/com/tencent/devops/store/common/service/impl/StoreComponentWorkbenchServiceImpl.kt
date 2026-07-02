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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.LabelDao
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.service.CategoryService
import com.tencent.devops.store.common.service.StoreComponentWorkbenchService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.QueryComponentsParam
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentWorkbenchServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseEnvQueryDao: StoreBaseEnvQueryDao,
    private val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val classifyDao: ClassifyDao,
    private val labelDao: LabelDao,
    private val categoryService: CategoryService
) : StoreComponentWorkbenchService {

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
}
