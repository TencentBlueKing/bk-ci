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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.store.dao.common.StoreMediaInfoDao
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMediaService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreMediaServiceImpl : StoreMediaService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var storeMediaInfoDao: StoreMediaInfoDao

    override fun add(userId: String, type: StoreTypeEnum, storeMediaInfo: StoreMediaInfoRequest): Result<Boolean> {
        logger.info("addMedia input: userId[$userId] type[$type] storeMediaInfo:[$storeMediaInfo]")
        storeMediaInfoDao.add(
            dslContext = dslContext,
            id = UUIDUtil.generate(),
            userId = userId,
            type = type.type.toByte(),
            storeMediaInfoReq = storeMediaInfo
        )
        return Result(true)
    }

    override fun update(
        userId: String,
        id: String,
        storeMediaInfo: StoreMediaInfoRequest
    ): Result<Boolean> {
        logger.info("updateMedia input: userId[$userId] id[$id] storeMediaInfo:[$storeMediaInfo]")
        storeMediaInfoDao.updateById(
            dslContext = dslContext,
            id = id,
            userId = userId,
            storeMediaInfoReq = storeMediaInfo
        )
        return Result(true)
    }

    override fun deleteByStoreCode(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        logger.info("deleteByStoreCode input: userId[$userId] storeCode[$storeCode] storeType:[${storeType.type}]")
        storeMediaInfoDao.deleteByStoreCode(dslContext, storeCode, storeType.type.toByte())
        return Result(true)
    }

    override fun get(userId: String, id: String): Result<StoreMediaInfo?> {
        logger.info("getMedia input: userId[$userId] id[$id] ")
        val storeMediaRecord = storeMediaInfoDao.getMediaInfo(
            dslContext = dslContext,
            id = id
        )
        return Result(
            if (storeMediaRecord == null) {
                null
            } else {
                storeMediaInfoDao.convert(storeMediaRecord)
            }
        )
    }

    override fun getByCode(storeCode: String, storeType: StoreTypeEnum): Result<List<StoreMediaInfo>?> {
        logger.info("getMedia input: storeCode[$storeCode] storeType[$storeType] ")
        val storeMediaInfoList = mutableListOf<StoreMediaInfo>()
        val storeMediaRecord = storeMediaInfoDao.getMediaInfoByStoreCode(
            dslContext = dslContext,
            storeCode = storeCode,
            type = storeType.type.toByte()
        )
        storeMediaRecord?.forEach {
            storeMediaInfoList.add(storeMediaInfoDao.convert(it))
        }
        return Result(storeMediaInfoList)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}