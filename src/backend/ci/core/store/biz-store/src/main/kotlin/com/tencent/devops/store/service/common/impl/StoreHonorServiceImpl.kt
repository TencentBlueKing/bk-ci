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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TStoreHonorInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreHonorRelRecord
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.dao.common.StoreHonorDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.AddStoreHonorRequest
import com.tencent.devops.store.pojo.common.CREATE_TIME
import com.tencent.devops.store.pojo.common.HonorInfo
import com.tencent.devops.store.pojo.common.STORE_CODE
import com.tencent.devops.store.pojo.common.STORE_CREATE_TIME
import com.tencent.devops.store.pojo.common.STORE_CREATOR
import com.tencent.devops.store.pojo.common.STORE_HONOR_ID
import com.tencent.devops.store.pojo.common.STORE_HONOR_MOUNT_FLAG
import com.tencent.devops.store.pojo.common.STORE_HONOR_NAME
import com.tencent.devops.store.pojo.common.STORE_HONOR_TITLE
import com.tencent.devops.store.pojo.common.STORE_MODIFIER
import com.tencent.devops.store.pojo.common.STORE_NAME
import com.tencent.devops.store.pojo.common.STORE_TYPE
import com.tencent.devops.store.pojo.common.STORE_UPDATE_TIME
import com.tencent.devops.store.pojo.common.StoreHonorManageInfo
import com.tencent.devops.store.pojo.common.StoreHonorRel
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreHonorService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreHonorServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeHonorDao: StoreHonorDao,
    private val storeMemberDao: StoreMemberDao
) : StoreHonorService {

    override fun list(userId: String, keyWords: String?, page: Int, pageSize: Int): Page<StoreHonorManageInfo> {
        val records = storeHonorDao.list(
            dslContext = dslContext,
            keyWords = keyWords,
            page = page,
            pageSize = pageSize
        )
        return Page(
            count = storeHonorDao.count(dslContext, keyWords),
            page = page,
            pageSize = pageSize,
            records = records.map {
                StoreHonorManageInfo(
                    storeCode = it[STORE_CODE] as String,
                    storeName = it[STORE_NAME] as String,
                    storeType = StoreTypeEnum.getStoreTypeObj((it[STORE_TYPE] as Byte).toInt())!!,
                    honorId = it[STORE_HONOR_ID] as String,
                    honorTitle = it[STORE_HONOR_TITLE] as String,
                    honorName = it[STORE_HONOR_NAME] as String,
                    creator = it[STORE_CREATOR] as String,
                    modifier = it[STORE_MODIFIER] as String,
                    createTime = it[STORE_CREATE_TIME] as LocalDateTime,
                    updateTime = it[STORE_UPDATE_TIME] as LocalDateTime
                )
            }
        )
    }

    override fun batchDelete(userId: String, storeHonorRelList: List<StoreHonorRel>): Boolean {
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

    override fun add(userId: String, addStoreHonorRequest: AddStoreHonorRequest): Result<Boolean> {
        logger.info("create storeHonor userid:$userId||honorTitle:${addStoreHonorRequest.honorTitle}")
        val honorTitleCount = storeHonorDao.countByhonorTitle(dslContext, addStoreHonorRequest.honorTitle)
        if (honorTitleCount > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(addStoreHonorRequest.honorTitle)
            )
        }
        val id = UUIDUtil.generate()
        val storeHonorInfo = TStoreHonorInfoRecord()
        storeHonorInfo.id = id
        storeHonorInfo.honorTitle = addStoreHonorRequest.honorTitle
        storeHonorInfo.honorName = addStoreHonorRequest.honorName
        storeHonorInfo.storeType = addStoreHonorRequest.storeType.type.toByte()
        storeHonorInfo.creator = userId
        storeHonorInfo.modifier = userId
        storeHonorInfo.createTime = LocalDateTime.now()
        storeHonorInfo.updateTime = LocalDateTime.now()
        val tStoreHonorRelList = addStoreHonorRequest.storeCodes.map {
            val atomName = getStoreCommonDao(addStoreHonorRequest.storeType.name).getStoreNameByCode(dslContext, it)
            if (atomName.isNullOrBlank()) {
                return I18nUtil.generateResponseDataObject(
                    CommonMessageCode.ERROR_INVALID_PARAM_,
                    arrayOf("${addStoreHonorRequest.storeType.name}:$it")
                )
            }
            val tStoreHonorRelRecord = TStoreHonorRelRecord()
            tStoreHonorRelRecord.id = UUIDUtil.generate()
            tStoreHonorRelRecord.storeCode = it
            tStoreHonorRelRecord.storeName = atomName
            tStoreHonorRelRecord.storeType = addStoreHonorRequest.storeType.type.toByte()
            tStoreHonorRelRecord.honorId = id
            tStoreHonorRelRecord.creator = userId
            tStoreHonorRelRecord.modifier = userId
            tStoreHonorRelRecord.createTime = LocalDateTime.now()
            tStoreHonorRelRecord.updateTime = LocalDateTime.now()
            tStoreHonorRelRecord
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeHonorDao.createStoreHonorInfo(context, userId, storeHonorInfo)
            storeHonorDao.batchCreateStoreHonorRel(context, tStoreHonorRelList)
        }
        return Result(true)
    }

    override fun getStoreHonor(userId: String, storeType: StoreTypeEnum, storeCode: String): List<HonorInfo> {
        return storeHonorDao.getHonorByStoreCode(dslContext, storeType, storeCode).map {
            HonorInfo(
                honorTitle = it[STORE_HONOR_TITLE] as String,
                honorName = it[STORE_HONOR_NAME] as String,
                honorId = it[STORE_HONOR_ID] as String,
                mountFlag = it[STORE_HONOR_MOUNT_FLAG] as Boolean,
                createTime = it[CREATE_TIME] as LocalDateTime
            )
        }
    }

    override fun installStoreHonor(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        honorId: String
    ): Boolean {

        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeType = storeType.type.toByte(),
                storeCode = storeCode
            )
        ) {
            throw ErrorCodeException(
                errorCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
        storeHonorDao.installStoreHonor(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            honorId = honorId
        )
        return true
    }

    override fun getHonorInfosByStoreCodes(
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ): Map<String, List<HonorInfo>> {
        logger.info("getHonorInfosByStoreCodes storeCodes is {$storeCodes}")
        val records = storeHonorDao.getHonorInfosByStoreCodes(dslContext, storeType, storeCodes)
        val storeHonorInfoMap = mutableMapOf<String, List<HonorInfo>>()
        records.forEach {
            val storeCode = it[STORE_CODE] as String
            val honorInfo = HonorInfo(
                honorId = it[STORE_HONOR_ID] as String,
                honorTitle = it[STORE_HONOR_TITLE] as String,
                honorName = it[STORE_HONOR_NAME] as String,
                mountFlag = it[STORE_HONOR_MOUNT_FLAG] as Boolean,
                createTime = it[CREATE_TIME] as LocalDateTime
            )
            if (storeHonorInfoMap[storeCode].isNullOrEmpty()) {
                storeHonorInfoMap[storeCode] = listOf(honorInfo)
            } else {
                val honorInfos = storeHonorInfoMap[storeCode]!!.toMutableList()
                honorInfos.add(honorInfo)
                storeHonorInfoMap[storeCode] = honorInfos
            }
        }
        return storeHonorInfoMap
    }

    private fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StoreHonorServiceImpl::class.java)
    }
}
