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
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_NULL
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
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
import com.tencent.devops.store.pojo.common.honor.I18nHonorInfoDTO
import com.tencent.devops.store.pojo.common.honor.StoreHonorManageInfo
import com.tencent.devops.store.pojo.common.honor.StoreHonorRel
import org.jooq.DSLContext
import org.jooq.Record
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
                    storeType = StoreTypeEnum.getStoreTypeObj((it[STORE_TYPE] as Byte).toInt()),
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
            // 删除组件荣誉关联信息
            storeHonorDao.batchDeleteStoreHonorRel(context, storeHonorRelList)
            // 查看是否有被其他组件所关联
            val honorIds = storeHonorDao.getByIds(context, delHonorIds)
            // 删除没有被其他组件关联的荣誉信息
            storeHonorDao.batchDeleteStoreHonorInfo(context, delHonorIds.subtract(honorIds).toList())
        }
        // 同时清理掉请求相关联组件的国际化数据
        storeHonorRelList.forEach { storeHonorRel ->
            val key = buildHonorKey(
                storeType = storeHonorRel.storeType,
                storeCode = storeHonorRel.storeCode,
                honorId = storeHonorRel.honorId,
                appendTrailingDot = true
            )
            client.get(ServiceI18nMessageResource::class)
                .deleteI18nMessage(
                    userId = userId,
                    key = key,
                    moduleCode = SystemModuleEnum.STORE.name,
                    language = null
                )
        }
        return true
    }

    override fun add(userId: String, addStoreHonorRequest: AddStoreHonorRequest): Result<Boolean> {
        val i18nHonorInfoList = addStoreHonorRequest.i18nHonorInfoList
        val id = UUIDUtil.generate()

        val i18nHonorInfo = i18nHonorInfoList
            .find { it.language == commonConfig.devopsDefaultLocaleLanguage }
        if (i18nHonorInfo == null) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("honorTitle")
            )
        }
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


        val i18nMessages = i18nHonorInfoList.flatMap { honorInfo ->
            listOf(
                "honorTitle" to honorInfo.honorTitle,
                "honorName" to honorInfo.honorName
            ).flatMap { (fieldName, value) ->
                addStoreHonorRequest.storeCodes.map { storeCode ->
                    I18nMessage(
                        moduleCode = SystemModuleEnum.STORE.name,
                        language = honorInfo.language,
                        key = buildHonorKey(addStoreHonorRequest.storeType, storeCode, id, "honorInfo.$fieldName"),
                        value = value
                    )
                }
            }
        }
        try {
            client.get(ServiceI18nMessageResource::class).batchAddI18nMessage(userId, i18nMessages)
        } catch (ignore: Throwable) {
            logger.warn("add i18n message error", ignore)
        }
        return Result(true)
    }

    override fun getStoreHonor(userId: String, storeType: StoreTypeEnum, storeCode: String): List<HonorInfo> {
        val honorInfos = storeHonorDao.getHonorByStoreCode(dslContext, storeType, storeCode)
        if (honorInfos.isEmpty()) {
            return emptyList()
        }
        val userLanguage = I18nUtil.getLanguage(I18nUtil.getRequestUserId() ?: userId)
        val isDefaultLanguage = userLanguage == I18nUtil.getDefaultLocaleLanguage()
        val i18nValueMap = fetchRemoteI18nResources(
            records = honorInfos,
            storeType = storeType,
            userLanguage = userLanguage,
            storeCodeParam = storeCode,
            isDefaultLanguage = isDefaultLanguage
        )
        return buildHonorInfo(
            records = honorInfos,
            i18nValueMap = i18nValueMap,
            storeCode = storeCode,
            storeType = storeType,
            isDefaultLanguage = isDefaultLanguage
        )
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
        storeCodes: List<String>,
        userId: String?
    ): Map<String, List<HonorInfo>> {
        logger.info("getHonorInfosByStoreCodes storeCodes: $storeCodes")
        val records = storeHonorDao.getHonorInfosByStoreCodes(dslContext, storeType, storeCodes)
        if (records.isEmpty()) {
            return emptyMap()
        }
        val userLanguage = I18nUtil.getLanguage(I18nUtil.getRequestUserId() ?: userId)
        val isDefaultLanguage = userLanguage == commonConfig.devopsDefaultLocaleLanguage
        val i18nValueMap = fetchRemoteI18nResources(
            isDefaultLanguage = isDefaultLanguage,
            userLanguage = userLanguage,
            storeType = storeType,
            records = records
        )
        return records.groupBy { record ->
            record[STORE_CODE].toString()
        }.mapValues { (storeCode, groupRecords) ->
            buildHonorInfo(
                storeCode = storeCode,
                records = groupRecords,
                i18nValueMap = i18nValueMap,
                isDefaultLanguage = isDefaultLanguage,
                storeType = storeType
            )
        }
    }

    private fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    override fun batchFillHonorTranslations(userId: String, honorI18nDTOList: List<I18nHonorInfoDTO>): Boolean {

        // 校验所有DTO的国际化字段是否为空
        honorI18nDTOList.forEach { dto ->
            if (dto.honorTitleI18n.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = PARAMETER_IS_NULL,
                    params = arrayOf("honorTitleI18n")
                )
            }
            if (dto.honorNameI18n.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = PARAMETER_IS_NULL,
                    params = arrayOf("honorNameI18n")
                )
            }
        }
        // 按honorTitle分组，减少数据库查询次数
        val groupedDTOs = honorI18nDTOList.groupBy { it.honorTitle }
        // 批量查询与荣誉信息对应的插件信息
        val honorRelMap = mutableMapOf<String, List<Record>>()
        groupedDTOs.keys.forEach { title ->
            val honorRels = storeHonorDao.getHonorStoreInfos(dslContext, title)
            if (honorRels.isNotEmpty) {
                honorRelMap[title] = honorRels
            }
        }
        val i18nMessages = groupedDTOs.flatMap { (title, dtos) ->
            // 获取与荣誉信息对应的插件信息
            val honorRels = honorRelMap[title] ?: emptyList()
            // 根据插件信息，给每个插件批量构建不同语言的国际化荣誉信息
            honorRels.flatMap { honorRel ->
                val storeType = StoreTypeEnum.getStoreTypeObj((honorRel[STORE_TYPE] as Byte).toInt())
                val storeCode = honorRel[STORE_CODE].toString()
                val honorId = honorRel[STORE_HONOR_ID].toString()
                dtos.flatMap { dto ->
                    listOf(
                        I18nMessage(
                            moduleCode = SystemModuleEnum.STORE.name,
                            language = dto.language,
                            key = buildHonorKey(storeType, storeCode, honorId, "honorInfo.honorTitle"),
                            value = dto.honorTitleI18n!!
                        ),
                        I18nMessage(
                            moduleCode = SystemModuleEnum.STORE.name,
                            language = dto.language,
                            key = buildHonorKey(storeType, storeCode, honorId, "honorInfo.honorName"),
                            value = dto.honorNameI18n!!
                        )
                    )
                }
            }
        }

        // 保存荣誉国际化信息
        return try {
            client.get(ServiceI18nMessageResource::class).batchAddI18nMessage(userId, i18nMessages)
            true
        } catch (e: Throwable) {
            logger.warn("add i18n message error", e)
            false
        }
    }


    private fun buildHonorKey(
        storeType: StoreTypeEnum,
        storeCode: String,
        honorId: String,
        suffix: String = "",
        appendTrailingDot: Boolean = false
    ): String {
        val baseKey = "${storeType.name}.$storeCode.$honorId"
        val fullKey = if (suffix.isNotBlank()) "$baseKey.$suffix" else baseKey
        return if (appendTrailingDot) "$fullKey." else fullKey
    }

    private fun fetchRemoteI18nResources(
        isDefaultLanguage: Boolean,
        records: List<Record>,
        storeCodeParam: String? = null,
        storeType: StoreTypeEnum,
        userLanguage: String,
    ): Map<String, String> {
        return if (!isDefaultLanguage) {
            // 收集所有需要翻译的键
            val allI18nKeys = records.flatMap { record ->
                val storeCode = storeCodeParam ?: record[STORE_CODE].toString()
                val honorId = record[STORE_HONOR_ID].toString()
                listOf(
                    buildHonorKey(storeType, storeCode, honorId, "honorInfo.honorTitle"),
                    buildHonorKey(storeType, storeCode, honorId, "honorInfo.honorName")
                )
            }.distinct()
            // 调用国际化服务获取翻译值
            try {
                client.get(ServiceI18nMessageResource::class)
                    .getI18nMessages(
                        keys = allI18nKeys,
                        moduleCode = SystemModuleEnum.STORE.name,
                        language = userLanguage
                    ).data?.associate { it.key to it.value } ?: emptyMap()
            } catch (e: Throwable) {
                logger.warn("Failed to get i18n messages for keys: $allI18nKeys", e)
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    private fun buildHonorInfo(
        records: List<Record>,
        storeCode: String,
        storeType: StoreTypeEnum,
        i18nValueMap: Map<String, String>,
        isDefaultLanguage: Boolean
    ): List<HonorInfo> {
        return records.map { record ->
            val honorId = record[STORE_HONOR_ID].toString()
            val originalTitle = record[STORE_HONOR_TITLE]?.toString() ?: ""
            val originalName = record[STORE_HONOR_NAME]?.toString() ?: ""
            val mountFlag = record[STORE_HONOR_MOUNT_FLAG] as Boolean
            val createTime = record[CREATE_TIME] as LocalDateTime

            // 根据是否为默认语言决定使用原始值还是翻译值
            val title = if (isDefaultLanguage) originalTitle else i18nValueMap[
                buildHonorKey(storeType, storeCode, honorId, "honorInfo.honorTitle")
            ] ?: originalTitle

            val name = if (isDefaultLanguage) originalName else i18nValueMap[
                buildHonorKey(storeType, storeCode, honorId, "honorInfo.honorName")
            ] ?: originalName

            HonorInfo(
                honorId = honorId,
                honorTitle = title,
                honorName = name,
                mountFlag = mountFlag,
                createTime = createTime
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StoreHonorServiceImpl::class.java)
    }

}
