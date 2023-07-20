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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.AtomRuntimeUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.BUILD_VISIT_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.dao.common.SensitiveConfDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.SensitiveConfReq
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.enums.FieldTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.UserSensitiveConfService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
@RefreshScope
class UserSensitiveConfServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val sensitiveConfDao: SensitiveConfDao,
    private val storeMemberDao: StoreMemberDao
) : UserSensitiveConfService {

    private val logger = LoggerFactory.getLogger(UserSensitiveConfServiceImpl::class.java)

    @Value("\${aes.aesKey}")
    private lateinit var aesKey: String

    @Value("\${aes.aesMock}")
    private lateinit var aesMock: String

    /**
     * 新增敏感配置
     */
    override fun create(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean> {
        logger.info("createSensitiveConf params: [$userId | $storeType | $storeCode | $sensitiveConfReq]")
        checkUserAuthority(userId, storeCode, storeType)
        val fieldName = sensitiveConfReq.fieldName
        if (fieldName.isEmpty()) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf(fieldName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val fieldValue = sensitiveConfReq.fieldValue
        if (fieldValue.isEmpty()) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf(fieldValue),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 判断同名
        val isNameExist = sensitiveConfDao.check(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            fieldName = fieldName,
            id = null
        )
        if (isNameExist) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SENSITIVE_CONF_EXIST,
                params = arrayOf(fieldName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val fieldType = sensitiveConfReq.fieldType
        val finalFieldValue = if (fieldType == FieldTypeEnum.BACKEND.name) {
            // 字段如果只是给后端使用需要对字段值进行加密
            AESUtil.encrypt(aesKey, fieldValue)
        } else {
            fieldValue
        }
        sensitiveConfDao.create(
            dslContext = dslContext,
            userId = userId,
            id = UUIDUtil.generate(),
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            fieldName = fieldName,
            fieldType = fieldType,
            fieldValue = finalFieldValue,
            fieldDesc = sensitiveConfReq.fieldDesc
        )
        return Result(true)
    }

    /**
     * 更新配置信息
     */
    override fun update(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        id: String,
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean> {
        logger.info("updateSensitiveConf params: [$storeType | $storeCode | $id | $sensitiveConfReq]")
        checkUserAuthority(userId, storeCode, storeType)
        val sensitiveConfRecord = sensitiveConfDao.getById(dslContext, id)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(id),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        val fieldName = sensitiveConfReq.fieldName
        val fieldValue = sensitiveConfReq.fieldValue
        // 判断同名
        val isNameExist = sensitiveConfDao.check(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            fieldName = fieldName,
            id = id
        )
        if (isNameExist) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SENSITIVE_CONF_EXIST,
                params = arrayOf(fieldName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val fieldType = sensitiveConfReq.fieldType
        val finalFieldValue = if (fieldValue == aesMock) {
            val dbFieldType = sensitiveConfRecord.fieldType
            if (dbFieldType == FieldTypeEnum.BACKEND.name && dbFieldType != fieldType) {
                // 如果字段类型由BACKEND改为其它，需把数据库里字段内容解密存储
                AESUtil.decrypt(aesKey, sensitiveConfRecord.fieldValue)
            } else {
                null
            }
        } else {
            if (fieldType == FieldTypeEnum.BACKEND.name) {
                AESUtil.encrypt(aesKey, fieldValue)
            } else {
                fieldValue
            }
        }
        sensitiveConfDao.update(
            dslContext = dslContext,
            userId = userId,
            id = id,
            fieldName = fieldName,
            fieldType = fieldType,
            fieldValue = finalFieldValue,
            fieldDesc = sensitiveConfReq.fieldDesc
        )
        return Result(true)
    }

    /**
     * 删除
     */
    override fun delete(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        ids: String
    ): Result<Boolean> {
        logger.info("deleteSensitiveConf params: [$userId | $storeType | $storeCode | $ids]")
        checkUserAuthority(userId, storeCode, storeType)
        sensitiveConfDao.batchDelete(dslContext = dslContext,
            storeType = storeType.type.toByte(),
            storeCode = storeCode,
            idList = ids.split(","))
        return Result(true)
    }

    /**
     * 获取单个数据
     */
    override fun get(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        id: String
    ): Result<SensitiveConfResp?> {
        checkUserAuthority(userId, storeCode, storeType)
        val record = sensitiveConfDao.getById(dslContext, id)
        return Result(if (null != record) {
            val fieldType = record.fieldType
            SensitiveConfResp(
                fieldId = record.id,
                fieldName = record.fieldName,
                fieldType = fieldType,
                fieldValue = if (fieldType == FieldTypeEnum.BACKEND.name) aesMock else record.fieldValue,
                fieldDesc = record.fieldDesc,
                creator = record.creator,
                modifier = record.modifier,
                createTime = DateTimeUtil.toDateTime(record.createTime),
                updateTime = DateTimeUtil.toDateTime(record.updateTime)
            )
        } else {
            null
        })
    }

    /**
     * 获取列表
     */
    override fun list(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        isDecrypt: Boolean,
        types: String?
    ): Result<List<SensitiveConfResp>?> {
        val filedTypeList = if (!types.isNullOrBlank()) types.split(",") else null
        // 私有配置为后端类型时才需要做权限校验
        if (userId.isNotBlank() && filedTypeList?.contains(FieldTypeEnum.BACKEND.name) == true) {
            checkUserAuthority(userId, storeCode, storeType)
        }
        val records = sensitiveConfDao.list(dslContext, storeType.type.toByte(), storeCode, filedTypeList)
        val sensitiveConfRespList = mutableListOf<SensitiveConfResp>()
        records?.forEach {
            val fieldType = it.fieldType
            val fieldValue = if (fieldType == FieldTypeEnum.BACKEND.name) {
                if (isDecrypt) AESUtil.decrypt(aesKey, it.fieldValue) else aesMock
            } else {
                it.fieldValue
            }
            sensitiveConfRespList.add(
                SensitiveConfResp(
                    fieldId = it.id,
                    fieldName = it.fieldName,
                    fieldType = fieldType,
                    fieldValue = fieldValue,
                    fieldDesc = it.fieldDesc,
                    creator = it.creator,
                    modifier = it.modifier,
                    createTime = DateTimeUtil.toDateTime(it.createTime),
                    updateTime = DateTimeUtil.toDateTime(it.updateTime)
                )
            )
        }
        return Result(sensitiveConfRespList)
    }

    override fun checkOperationAuthority(
        buildId: String,
        vmSeqId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ) {
        if (storeType == StoreTypeEnum.ATOM) {
            val runningAtomCode = AtomRuntimeUtil.getRunningAtomValue(
                redisOperation = redisOperation, buildId = buildId, vmSeqId = vmSeqId
            )?.first
            if (runningAtomCode != storeCode) {
                // build类接口需要校验storeCode是否为正在运行的storeCode，防止越权查询storeCode信息
                throw ErrorCodeException(
                    errorCode = BUILD_VISIT_NO_PERMISSION,
                    params = arrayOf(storeCode)
                )
            }
        }
    }

    private fun checkUserAuthority(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ) {
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte())
        ) {
            throw ErrorCodeException(
                errorCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
    }
}
