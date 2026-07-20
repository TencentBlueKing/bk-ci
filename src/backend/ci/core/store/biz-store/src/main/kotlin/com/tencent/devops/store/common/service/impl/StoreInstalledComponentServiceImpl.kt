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
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.service.StoreComponentBaseInfoQueryService
import com.tencent.devops.store.common.service.StoreInstalledComponentExtHandler
import com.tencent.devops.store.common.service.StoreInstalledComponentService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.pojo.common.InstalledComponentInfo
import com.tencent.devops.store.pojo.common.InstalledComponentQueryReq
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StoreInstalledComponentServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeComponentBaseInfoQueryService: StoreComponentBaseInfoQueryService,
    private val classifyDao: ClassifyDao,
    storeInstalledComponentExtHandlers: List<StoreInstalledComponentExtHandler>
) : StoreInstalledComponentService {

    @Value("\${store.maxQueryInstanceNum:10}")
    private var maxQueryInstanceNum: Int = DEFAULT_MAX_QUERY_INSTANCE_NUM

    /**
     * storeType -> 个性化扩展点，由Spring自动织入所有实现，未注册的类型走默认公共逻辑
     */
    private val extHandlerMap: Map<StoreTypeEnum, StoreInstalledComponentExtHandler> =
        storeInstalledComponentExtHandlers.associateBy { it.getStoreType() }

    override fun getInstalledComponents(
        userId: String,
        queryReq: InstalledComponentQueryReq
    ): Page<InstalledComponentInfo> {
        val instanceIds = queryReq.instanceIds
        // 实例ID数量限制校验(可配置)
        if (!instanceIds.isNullOrEmpty() && instanceIds.size > maxQueryInstanceNum) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("instanceIds(${instanceIds.size}) exceeds the maximum limit $maxQueryInstanceNum")
            )
        }
        val storeType = queryReq.storeType
        val page = queryReq.page
        val pageSize = queryReq.pageSize
        logger.info(
            "getInstalledComponents|$userId|${queryReq.projectCode}|$storeType|" +
                "${queryReq.storeProjectType}|instanceIds=$instanceIds|$page|$pageSize"
        )
        val count = storeProjectRelDao.countInstalledComponents(
            dslContext = dslContext,
            projectCode = queryReq.projectCode,
            storeType = storeType.type.toByte(),
            instanceIds = instanceIds,
            storeProjectType = queryReq.storeProjectType
        )
        if (count == 0L) {
            return Page(page = page, pageSize = pageSize, count = 0L, records = emptyList())
        }
        // 查询安装记录(数据源：T_STORE_PROJECT_REL)
        val relRecords = storeProjectRelDao.listInstalledComponents(
            dslContext = dslContext,
            projectCode = queryReq.projectCode,
            storeType = storeType.type.toByte(),
            instanceIds = instanceIds,
            storeProjectType = queryReq.storeProjectType,
            page = page,
            pageSize = pageSize
        )
        // 批量获取组件公共信息(与storeType无关，复用既有查询逻辑)
        val storeCodes = relRecords.map { it.storeCode }.toSet()
        val baseInfoMap = storeComponentBaseInfoQueryService.getComponentBaseInfoList(
            storeType = storeType,
            storeCodes = storeCodes
        ).associateBy { it.storeCode }
        // 批量解析分类信息(classifyCode + 国际化后的classifyName)
        val classifyIds = baseInfoMap.values.mapNotNull { it.classifyId.takeIf { id -> id.isNotBlank() } }
        val classifyInfoMap = classifyDao.getClassifyInfosByIds(dslContext, classifyIds)
        // 组装公共字段
        val components = relRecords.map { record ->
            val baseInfo = baseInfoMap[record.storeCode]
            val classifyInfo = baseInfo?.classifyId?.let { classifyInfoMap[it] }
            InstalledComponentInfo(
                storeId = baseInfo?.storeId ?: "",
                storeCode = record.storeCode,
                storeName = baseInfo?.storeName ?: record.storeCode,
                storeType = storeType,
                installedVersion = record.version,
                logoUrl = baseInfo?.logoUrl?.let {
                    StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
                },
                classifyCode = classifyInfo?.first,
                classifyName = classifyInfo?.second,
                publisher = baseInfo?.publisher,
                installer = record.creator,
                installTime = record.createTime?.let { DateTimeUtil.toDateTime(it) },
                installType = StoreProjectTypeEnum.getProjectType(record.type.toInt()),
                instanceId = record.instanceId,
                instanceName = record.instanceName
            )
        }
        // storeType个性化富化(扩展点)，未注册扩展点的类型直接返回公共数据
        val records = extHandlerMap[storeType]
            ?.enrich(userId = userId, queryReq = queryReq, components = components)
            ?: components
        return Page(page = page, pageSize = pageSize, count = count, records = records)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StoreInstalledComponentServiceImpl::class.java)
        private const val DEFAULT_MAX_QUERY_INSTANCE_NUM = 10
    }
}
