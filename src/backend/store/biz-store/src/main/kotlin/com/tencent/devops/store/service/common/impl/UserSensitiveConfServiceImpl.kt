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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.SensitiveConfDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.SensitiveConfReq
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.UserSensitiveConfService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

@Service
class UserSensitiveConfServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val sensitiveConfDao: SensitiveConfDao,
    private val storeMemberDao: StoreMemberDao
) : UserSensitiveConfService {

    private val logger = LoggerFactory.getLogger(UserSensitiveConfServiceImpl::class.java)

    @Value("\${aes.aesKey}")
    private lateinit var aesKey: String

    @Value("\${aes.aesMock}")
    private lateinit var aesMock: String

    /**
     * 判断是否有权限操作敏感配置
     */
    override fun checkRight(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Boolean {
        return storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())
    }

    /**
     * 新增敏感配置
     */
    override fun create(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean> {
        logger.info("create: $storeType | $storeCode | $sensitiveConfReq")

        // 判断是否有权限新增
        val optRight = checkRight(userId, storeType, storeCode)
        logger.info("create right: $optRight")
        if (!optRight) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PERMISSION_DENIED,
            arrayOf(userId),
            false
        )

        val fieldName = sensitiveConfReq.fieldName
        if (fieldName.isEmpty()) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf(fieldName),
            false
        )
        val fieldValue = sensitiveConfReq.fieldValue
        if (fieldValue.isEmpty()) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf(fieldValue),
            false
        )

        // 判断同名
        val isNameExist = sensitiveConfDao.check(dslContext, storeCode, storeType.type.toByte(), fieldName, null)
        logger.info("fieldName: $fieldName, isNameExist: $isNameExist")
        if (isNameExist) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_SENSITIVE_CONF_EXIST,
                arrayOf(fieldName),
                false
            )
        }

        // 对字段值进行加密
        val fieldValueEncrypted = AESUtil.encrypt(aesKey, fieldValue)
        logger.info("fieldValue: $fieldValueEncrypted")

        sensitiveConfDao.create(
            dslContext = dslContext,
            userId = userId,
            id = UUIDUtil.generate(),
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            fieldName = fieldName,
            fieldValue = fieldValueEncrypted,
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
        logger.info("update: $storeType | $storeCode | $id | $sensitiveConfReq")

        // 判断是否有权限编辑
        val optRight = checkRight(userId, storeType, storeCode)
        logger.info("update right: $optRight")
        if (!optRight) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PERMISSION_DENIED,
            arrayOf(userId),
            false
        )

        val fieldName = sensitiveConfReq.fieldName
        if (fieldName.isEmpty()) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf(fieldName),
            false
        )
        val fieldValue = sensitiveConfReq.fieldValue
        if (fieldValue.isEmpty()) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf(fieldValue),
            false
        )

        // 判断同名
        val isNameExist = sensitiveConfDao.check(dslContext, storeCode, storeType.type.toByte(), fieldName, id)
        logger.info("fieldName: $fieldName, isNameExist: $isNameExist")
        if (isNameExist) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_SENSITIVE_CONF_EXIST,
                arrayOf(fieldName),
                false
            )
        }

        // 对字段值进行加密
        val fieldValueEncrypted = if (fieldValue == aesMock) {
            null
        } else {
            AESUtil.encrypt(aesKey, fieldValue)
        }
        logger.info("fieldValue: $fieldValueEncrypted")

        sensitiveConfDao.update(
            dslContext = dslContext,
            userId = userId,
            id = id,
            fieldName = fieldName,
            fieldValue = fieldValueEncrypted,
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
        logger.info("delete: $userId | $storeType | $storeCode | $ids")

        // 判断是否有权限删除
        val optRight = checkRight(userId, storeType, storeCode)
        logger.info("delete right: $optRight")
        if (!optRight) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PERMISSION_DENIED,
            arrayOf(userId),
            false
        )

        sensitiveConfDao.batchDelete(dslContext, storeType.type.toByte(), storeCode, ids.split(","))

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
        // 判断是否有权限
        val optRight = checkRight(userId, storeType, storeCode)
        logger.info("get right: $optRight")
        if (!optRight) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PERMISSION_DENIED,
            arrayOf(userId),
            null
        )

        val record = sensitiveConfDao.getById(dslContext, id)
        logger.info("the record is :$record")
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return Result(
            if (null != record) {
                SensitiveConfResp(
                    fieldId = record["ID"] as String,
                    fieldName = record["FIELD_NAME"] as String,
                    fieldValue = aesMock,
                    fieldDesc = record["FIELD_DESC"] as? String,
                    creator = record["CREATOR"] as String,
                    modifier = record["MODIFIER"] as String,
                    createTime = df.format(record["CREATE_TIME"] as TemporalAccessor),
                    updateTime = df.format(record["UPDATE_TIME"] as TemporalAccessor)
                )
            } else {
                null
            }
        )
    }

    /**
     * 获取列表
     */
    override fun list(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        isDecrypt: Boolean
    ): Result<List<SensitiveConfResp>?> {
        if (!isDecrypt) {
            // 判断是否有权限
            val optRight = checkRight(userId, storeType, storeCode)
            logger.info("list right: $optRight")
            if (!optRight) return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PERMISSION_DENIED,
                arrayOf(userId),
                null
            )
        }
        val records = sensitiveConfDao.list(dslContext, storeType.type.toByte(), storeCode)
        val sensitiveConfRespList = mutableListOf<SensitiveConfResp>()
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        records?.forEach {
            val fieldValue = if (isDecrypt) AESUtil.decrypt(aesKey, it["FIELD_VALUE"] as String) else aesMock
            sensitiveConfRespList.add(
                SensitiveConfResp(
                    fieldId = it["ID"] as String,
                    fieldName = it["FIELD_NAME"] as String,
                    fieldValue = fieldValue,
                    fieldDesc = it["FIELD_DESC"] as? String,
                    creator = it["CREATOR"] as String,
                    modifier = it["MODIFIER"] as String,
                    createTime = df.format(it["CREATE_TIME"] as TemporalAccessor),
                    updateTime = df.format(it["UPDATE_TIME"] as TemporalAccessor)
                )
            )
        }
        logger.info("getSensitiveConfRespList storeType: $storeType,storeCode: $storeCode,sensitiveConfRespList: $sensitiveConfRespList")
        return Result(sensitiveConfRespList)
    }
}