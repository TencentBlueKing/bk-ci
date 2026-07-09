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

import com.tencent.devops.model.store.tables.records.TStoreBaseRecord
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.StoreComponentBaseInfoQueryService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentBaseInfoQueryServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao
) : StoreComponentBaseInfoQueryService {

    override fun getComponentBaseInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        version: String?
    ): StoreBaseInfo? {
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        val baseRecord = if (version.isNullOrBlank()) {
            storeBaseQueryDao.getLatestComponentByCode(
                dslContext = dslContext, storeCode = storeCode, storeType = storeTypeEnum
            )
        } else {
            storeBaseQueryDao.getComponent(
                dslContext = dslContext, storeCode = storeCode, version = version, storeType = storeTypeEnum
            )
        } ?: return null
        val publicFlag = storeBaseFeatureQueryDao.isPublic(
            dslContext = dslContext, storeCode = storeCode, storeType = storeTypeEnum
        )
        return StoreBaseInfo(
            storeId = baseRecord.id,
            storeCode = storeCode,
            storeName = baseRecord.name,
            storeType = storeTypeEnum,
            version = baseRecord.version,
            publicFlag = publicFlag,
            status = baseRecord.status,
            logoUrl = baseRecord.logoUrl?.let {
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
            },
            publisher = baseRecord.publisher,
            classifyId = baseRecord.classifyId
        )
    }

    override fun getComponentBaseInfo(
        storeType: StoreTypeEnum,
        storeCode: String?,
        storeId: String?,
        storeStatus: StoreStatusEnum?,
        ownerStoreCode: String?,
        keywork: String?
    ): StoreBaseInfo? {
        if (storeId.isNullOrEmpty() && storeCode.isNullOrEmpty()) {
            throw IllegalArgumentException("storeCode or storeId can not be null")
        }
        val baseRecord = storeCode?.let {
            storeBaseQueryDao.getLatestComponentByCode(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType,
                ownerStoreCode = ownerStoreCode,
                storeStatus = storeStatus,
                keyword = keywork
            )
        } ?: storeId?.let {
            storeBaseQueryDao.getComponentById(
                dslContext = dslContext,
                storeId = it
            )
        }
        return baseRecord?.convertStoreBaseInfo()
    }

    override fun getComponentBaseInfoList(
        storeType: StoreTypeEnum,
        storeCodes: Set<String>?
    ) = storeBaseQueryDao.getLatestComponentByCodes(
        dslContext = dslContext,
        storeCodes = storeCodes,
        storeType = storeType
    ).map {
        it.convertStoreBaseInfo()
    }

    private fun TStoreBaseRecord.convertStoreBaseInfo() = StoreBaseInfo(
        storeId = id,
        storeCode = storeCode,
        storeName = name,
        storeType = StoreTypeEnum.getStoreTypeObj(storeType.toInt()),
        version = version,
        status = status,
        logoUrl = logoUrl,
        publisher = publisher,
        classifyId = classifyId
    )
}
