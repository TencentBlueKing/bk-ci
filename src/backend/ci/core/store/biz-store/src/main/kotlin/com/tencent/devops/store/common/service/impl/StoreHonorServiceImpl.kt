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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.ThreadLocalUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.service.ServiceI18nMessageResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TStoreHonorInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreHonorRelRecord
import com.tencent.devops.store.common.dao.AbstractStoreCommonDao
import com.tencent.devops.store.common.dao.StoreHonorDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.service.StoreHonorService
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.pojo.common.CREATE_TIME
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
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.honor.AddStoreHonorRequest
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.honor.StoreHonorManageInfo
import com.tencent.devops.store.pojo.common.honor.StoreHonorRel
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoreHonorServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeHonorDao: StoreHonorDao,
    private val storeMemberDao: StoreMemberDao,
    private val client: Client,
    private val commonConfig: CommonConfig,
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
                    storeType = StoreTypeEnum.getStoreType((it[STORE_TYPE] as Byte).toInt()),
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
        val i18nHonorInfoList = addStoreHonorRequest.i18nHonorInfoList
        val id = UUIDUtil.generate()
        i18nHonorInfoList.forEach { i18nHonorInfo ->
            if (i18nHonorInfo.language.locale == commonConfig.devopsDefaultLocaleLanguage) {
                val honorTitleCount = storeHonorDao.countByhonorTitle(dslContext, i18nHonorInfo.honorTitle)
                if (honorTitleCount > 0) {
                    return I18nUtil.generateResponseDataObject(
                        messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                        params = arrayOf(i18nHonorInfo.honorTitle)
                    )
                }
                val storeHonorInfo = TStoreHonorInfoRecord()
                storeHonorInfo.id = id
                storeHonorInfo.honorTitle = i18nHonorInfo.honorTitle
                storeHonorInfo.honorName = i18nHonorInfo.honorName
                storeHonorInfo.storeType = addStoreHonorRequest.storeType.type.toByte()
                storeHonorInfo.creator = userId
                storeHonorInfo.modifier = userId
                storeHonorInfo.createTime = LocalDateTime.now()
                storeHonorInfo.updateTime = LocalDateTime.now()
                val tStoreHonorRelList = addStoreHonorRequest.storeCodes.map {
                    val atomName =
                        getStoreCommonDao(addStoreHonorRequest.storeType.name).getStoreNameByCode(dslContext, it)
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
            }
        }

        val i18nMessages = i18nHonorInfoList.flatMap { honorInfo ->
            listOf(
                "honorTitle" to honorInfo.honorTitle,
                "honorName" to honorInfo.honorName
            ).flatMap { (fieldName, value) ->
                addStoreHonorRequest.storeCodes.map { storeCode ->
                    I18nMessage(
                        moduleCode = SystemModuleEnum.STORE.name,
                        language = honorInfo.language.locale,
                        key = "${addStoreHonorRequest.storeType.name}.${storeCode}.${id}.honorInfo.$fieldName",
                        value = value
                    )
                }
            }
        }
        try {
            client.get(ServiceI18nMessageResource::class).batchAddI18nMessage(userId, i18nMessages)
        } catch (ignore: Throwable) {
            logger.warn("add i18n message error:$ignore")
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
        logger.info("getHonorInfosByStoreCodes storeCodes: $storeCodes")
        val records = storeHonorDao.getHonorInfosByStoreCodes(dslContext, storeType, storeCodes)
        if (records.isEmpty()) {
            return emptyMap()
        }
        val defaultLanguage = commonConfig.devopsDefaultLocaleLanguage
        val userLanguage = ThreadLocalUtil.get(AUTH_HEADER_USER_ID)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: I18nUtil.getLanguage(I18nUtil.getRequestUserId())
        val isDefaultLanguage = userLanguage == defaultLanguage
        val honorInfoMap = mutableMapOf<String, MutableList<HonorInfo>>()
        val i18nValueMap = if (!isDefaultLanguage) {
            val allI18nKeys = records.flatMap { record ->
                val storeCode = record[STORE_CODE]?.toString() ?: return@flatMap emptyList()
                val honorId = record[STORE_HONOR_ID]?.toString() ?: return@flatMap emptyList()
                listOf(
                    "${storeType.name}.$storeCode.$honorId.honorInfo.honorTitle",
                    "${storeType.name}.$storeCode.$honorId.honorInfo.honorName"
                )
            }.distinct()

            try {
                client.get(ServiceI18nMessageResource::class)
                    .getI18nMessages(
                        keys = allI18nKeys,
                        moduleCode = "STORE",
                        language = userLanguage
                    )
                    .data
                    ?.associate { it.key to it.value }
                    ?: emptyMap()
            } catch (e: Throwable) {
                logger.warn("Failed to get i18n messages for keys: $allI18nKeys", e)
                emptyMap()
            }
        } else {
            emptyMap()
        }

        records.forEach { record ->
            val storeCode = record[STORE_CODE]?.toString() ?: return@forEach
            val honorId = record[STORE_HONOR_ID]?.toString() ?: return@forEach
            val originalTitle = record[STORE_HONOR_TITLE]?.toString() ?: ""
            val originalName = record[STORE_HONOR_NAME]?.toString() ?: ""
            val mountFlag = record[STORE_HONOR_MOUNT_FLAG] as? Boolean ?: false
            val createTime = record[CREATE_TIME] as? LocalDateTime ?: LocalDateTime.MIN

            // 构建国际化后的标题和名称（非默认语言时使用翻译值）
            val (title, name) = if (isDefaultLanguage) {
                Pair(originalTitle, originalName)
            } else {
                val titleKey = "${storeType.name}.$storeCode.$honorId.honorInfo.honorTitle"
                val nameKey = "${storeType.name}.$storeCode.$honorId.honorInfo.honorName"
                Pair(
                    i18nValueMap[titleKey] ?: originalTitle,
                    i18nValueMap[nameKey] ?: originalName
                )
            }

            val honorInfo = HonorInfo(
                honorId = honorId,
                honorTitle = title,
                honorName = name,
                mountFlag = mountFlag,
                createTime = createTime
            )
            honorInfoMap.getOrPut(storeCode) { mutableListOf() }.add(honorInfo)
        }

        return honorInfoMap.mapValues { it.value.toList() }
    }

    private fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StoreHonorServiceImpl::class.java)
    }
}
