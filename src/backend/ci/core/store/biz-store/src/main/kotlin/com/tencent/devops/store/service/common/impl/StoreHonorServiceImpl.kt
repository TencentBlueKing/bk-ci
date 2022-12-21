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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.records.TStoreHonorRelRecord
import com.tencent.devops.store.dao.common.StoreHonorDao
import com.tencent.devops.store.pojo.common.AddStoreHonorRequest
import com.tencent.devops.store.pojo.common.StoreHonorInfo
import com.tencent.devops.store.pojo.common.StoreHonorRel
import com.tencent.devops.store.service.common.StoreHonorService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoreHonorServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeHonorDao: StoreHonorDao
) : StoreHonorService {

    override fun list(userId: String, keyWords: String?, page: Int, pageSize: Int): Page<StoreHonorInfo> {
        // 权限校验


        return Page(
            count = storeHonorDao.count(dslContext, keyWords),
            page = page,
            pageSize = pageSize,
            records = storeHonorDao.list(
                dslContext = dslContext,
                keyWords = keyWords,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun batchDelete(userId: String, storeHonorRelList: List<StoreHonorRel>): Boolean {
        // 权限校验

        if (storeHonorRelList.isEmpty()) {
            return false
        }
        val delHonorIds = storeHonorRelList.map { it.honorId }.toMutableList()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeHonorDao.batchDeleteStoreHonorRel(context, storeHonorRelList)
            val honorIds = storeHonorDao.getByIds(context, delHonorIds)
            storeHonorDao.batchDeleteStoreHonorInfo(context, delHonorIds.subtract(honorIds).toList())
        }
        return true
    }

    override fun add(userId: String, addStoreHonorRequest: AddStoreHonorRequest): Boolean {
        // 权限校验


        val id = UUIDUtil.generate()
        val storeHonorInfo = StoreHonorInfo(
            id = id,
            honorTitle = addStoreHonorRequest.honorTitle,
            honorName = addStoreHonorRequest.honorName,
            storeType = addStoreHonorRequest.storeType,
            creator = userId,
            modifier = userId,
            createTime = LocalDateTime.now(),
            updateTime = LocalDateTime.now()
        )
        val tStoreHonorRelList = addStoreHonorRequest.storeCodes.split(",").map {
            val tStoreHonorRelRecord = TStoreHonorRelRecord()
            tStoreHonorRelRecord.id = UUIDUtil.generate()
            tStoreHonorRelRecord.storeCode = it
            tStoreHonorRelRecord.storeType = addStoreHonorRequest.storeType.type.toByte()
            tStoreHonorRelRecord.honorId = id
            tStoreHonorRelRecord.creator = userId
            tStoreHonorRelRecord.modifier = userId
            tStoreHonorRelRecord.createTime = LocalDateTime.now()
            tStoreHonorRelRecord.updateTime = LocalDateTime.now()
            tStoreHonorRelRecord
        }
        dslContext.transaction { t -

        }
        storeHonorDao.createStoreHonorInfo(dslContext, userId, storeHonorInfo)
        storeHonorDao.batchCreateStoreHonorRel(dslContext,tStoreHonorRelList)
        return true
    }
}