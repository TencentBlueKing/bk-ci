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
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.dao.common.StoreEnvVarDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_ENCRYPT_FLAG
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_SCOPE
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_TYPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.KEY_VAR_DESC
import com.tencent.devops.store.pojo.common.KEY_VAR_NAME
import com.tencent.devops.store.pojo.common.KEY_VAR_VALUE
import com.tencent.devops.store.pojo.common.StoreEnvChangeLogInfo
import com.tencent.devops.store.pojo.common.StoreEnvVarInfo
import com.tencent.devops.store.pojo.common.StoreEnvVarRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreEnvVarService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class StoreEnvVarServiceImpl @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val storeEnvVarDao: StoreEnvVarDao
) : StoreEnvVarService {

    private val logger = LoggerFactory.getLogger(StoreEnvVarServiceImpl::class.java)

    @Value("\${aes.aesKey}")
    private lateinit var aesKey: String

    @Value("\${aes.aesMock}")
    private lateinit var aesMock: String

    override fun create(userId: String, storeEnvVarRequest: StoreEnvVarRequest): Result<Boolean> {
        logger.info("storeEnvVar create userId:$userId,storeEnvVarRequest:$storeEnvVarRequest")
        val storeCode = storeEnvVarRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(storeEnvVarRequest.storeType).type.toByte()
        if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(storeEnvVarRequest.storeCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (storeEnvVarDao.queryEnvironmentVariable(
                dslContext = dslContext,
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                scope = storeEnvVarRequest.scope,
                varName = storeEnvVarRequest.varName
        ) != null) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SENSITIVE_CONF_EXIST,
                params = arrayOf(storeEnvVarRequest.varName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val lockKey = "$storeCode:$storeType:${storeEnvVarRequest.varName}"
        val lock = RedisLock(redisOperation, lockKey, 60)
        try {
            if (lock.tryLock()) {
                // 查询该环境变量在数据库中最大的版本
                val maxVersion = storeEnvVarDao.getEnvVarMaxVersion(
                    dslContext = dslContext,
                    storeType = storeType,
                    storeCode = storeCode,
                    varName = storeEnvVarRequest.varName
                ) ?: 0
                storeEnvVarDao.create(
                    dslContext = dslContext,
                    userId = userId,
                    version = maxVersion + 1,
                    storeEnvVarRequest = storeEnvVarRequest
                )
            }
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|addEnvVar|$storeEnvVarRequest|error=${ignored.message}", ignored)
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)

            )
        } finally {
            lock.unlock()
        }
        return Result(true)
    }

    override fun update(userId: String, variableId: String, storeEnvVarRequest: StoreEnvVarRequest): Result<Boolean> {
        logger.info("storeEnvVar update userId:$userId,storeEnvVarRequest:$storeEnvVarRequest")
        val storeCode = storeEnvVarRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(storeEnvVarRequest.storeType).type.toByte()
        if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storeEnvVarRequest.storeCode)
            )
        }
        // 查询该环境变量在数据库中最大的版本的一行记录
        val envVarOne = storeEnvVarDao.getNewEnvVar(
            dslContext = dslContext,
            storeType = storeType,
            storeCode = storeCode,
            variableId = variableId
        )
        val maxVersionData = StoreEnvVarInfo(
            id = envVarOne?.get(KEY_ID) as String,
            storeCode = envVarOne[KEY_STORE_CODE] as String,
            storeType = StoreTypeEnum.getStoreType((envVarOne[KEY_STORE_TYPE] as Byte).toInt()),
            varName = envVarOne[KEY_VAR_NAME] as String,
            varValue = envVarOne[KEY_VAR_VALUE] as String,
            varDesc = envVarOne[KEY_VAR_DESC] as? String,
            encryptFlag = envVarOne[KEY_ENCRYPT_FLAG] as Boolean,
            scope = envVarOne[KEY_SCOPE] as String,
            version = envVarOne[KEY_VERSION] as Int,
            creator = envVarOne[KEY_CREATOR] as String,
            modifier = envVarOne[KEY_MODIFIER] as String,
            createTime = DateTimeUtil.toDateTime(envVarOne[KEY_CREATE_TIME] as LocalDateTime),
            updateTime = DateTimeUtil.toDateTime(envVarOne[KEY_UPDATE_TIME] as LocalDateTime)
        )
        if (storeEnvVarRequest.scope != maxVersionData.scope && storeEnvVarDao.queryEnvironmentVariable(
                dslContext = dslContext,
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                scope = storeEnvVarRequest.scope,
                varName = storeEnvVarRequest.varName
            ) != null
        ) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SENSITIVE_CONF_EXIST,
                params = arrayOf(storeEnvVarRequest.varName),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val lockKey = "$storeCode:$storeType:${storeEnvVarRequest.varName}"
        val lock = RedisLock(redisOperation, lockKey, 60)
        try {
            if (lock.tryLock()) {
                // 判断是否修改变量环境或变量名
                if (storeEnvVarRequest.scope != maxVersionData.scope || storeEnvVarRequest.varName != maxVersionData.varName) {
                    storeEnvVarDao.updateVariableEnvironment(
                            dslContext = dslContext,
                            userId = userId,
                            storeType = storeType,
                            storeCode = storeCode,
                            pastScope = maxVersionData.scope,
                            scope = storeEnvVarRequest.scope,
                            pastName = maxVersionData.varName,
                            varName = storeEnvVarRequest.varName
                        )
                }
                // 如变量值变更，则添加新记录
                if (storeEnvVarRequest.varValue != maxVersionData.varValue && storeEnvVarRequest.varValue != "******") {
                    storeEnvVarDao.create(
                        dslContext = dslContext,
                        userId = userId,
                        version = maxVersionData.version + 1,
                        storeEnvVarRequest = storeEnvVarRequest
                    )
                } else {
                    // 判断变量值是否需要进行加密或解密
                    val value = if (storeEnvVarRequest.encryptFlag != maxVersionData.encryptFlag) {
                        if (storeEnvVarRequest.encryptFlag)
                            AESUtil.encrypt(aesKey, maxVersionData.varValue)
                        else AESUtil.decrypt(aesKey, maxVersionData.varValue)
                    } else maxVersionData.varValue
                    storeEnvVarDao.updateVariable(
                        dslContext = dslContext,
                        storeType = storeType,
                        storeCode = storeCode,
                        variableId = maxVersionData.id,
                        varValue = value,
                        varDesc = storeEnvVarRequest.varDesc ?: "",
                        encryptFlag = storeEnvVarRequest.encryptFlag
                    )
                }
            }
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|updateEnvVar|$storeEnvVarRequest|error=${ignored.message}", ignored)
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)

            )
        } finally {
            lock.unlock()
        }
        return Result(true)
    }

    override fun delete(
        userId: String,
        storeType: String,
        storeCode: String,
        scope: String,
        varNames: String
    ): Result<Boolean> {
        logger.info("storeEnvVar delete userId:$userId,storeType:$storeType,storeCode:$storeCode,varNames:$varNames")
        val storeTypeObj = StoreTypeEnum.valueOf(storeType)
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeObj.type.toByte()
            )
        ) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storeCode)
            )
        }
        storeEnvVarDao.batchDelete(
            dslContext = dslContext,
            storeType = storeTypeObj.type.toByte(),
            storeCode = storeCode,
            scope = scope,
            varNameList = varNames.split(",")
        )
        return Result(true)
    }

    override fun getLatestEnvVarList(
        userId: String,
        storeType: String,
        storeCode: String,
        scopes: String?,
        varName: String?,
        isDecrypt: Boolean,
        checkPermissionFlag: Boolean
    ): Result<List<StoreEnvVarInfo>?> {
        logger.info("storeEnvVar getLatestEnvVarList userId:$userId,storeType:$storeType,storeCode:$storeCode")
        val storeTypeObj = StoreTypeEnum.valueOf(storeType)
        val scopeList = if (scopes.isNullOrEmpty()) listOf("ALL", "PRD", "TEST") else scopes.split(",")
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeObj.type.toByte()
            )
        ) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storeCode)
            )
        }
        val latestEnvVarRecords = storeEnvVarDao.getLatestEnvVarList(
            dslContext = dslContext,
            storeType = storeTypeObj.type.toByte(),
            storeCode = storeCode,
            scopeList = scopeList,
            varName = varName
        )
        return if (latestEnvVarRecords != null) {
            val storeEnvVarList = mutableListOf<StoreEnvVarInfo>()
            latestEnvVarRecords.forEach {
                val encryptFlag = it[KEY_ENCRYPT_FLAG] as Boolean
                val varValue = if (encryptFlag) {
                    if (isDecrypt) AESUtil.decrypt(aesKey, it[KEY_VAR_VALUE] as String) else aesMock
                } else {
                    it[KEY_VAR_VALUE] as String
                }
                storeEnvVarList.add(
                    StoreEnvVarInfo(
                        id = it[KEY_ID] as String,
                        storeCode = it[KEY_STORE_CODE] as String,
                        storeType = StoreTypeEnum.getStoreType((it[KEY_STORE_TYPE] as Byte).toInt()),
                        varName = it[KEY_VAR_NAME] as String,
                        varValue = varValue,
                        varDesc = it[KEY_VAR_DESC] as? String,
                        encryptFlag = encryptFlag,
                        scope = it[KEY_SCOPE] as String,
                        version = it[KEY_VERSION] as Int,
                        creator = it[KEY_CREATOR] as String,
                        modifier = it[KEY_MODIFIER] as String,
                        createTime = DateTimeUtil.toDateTime(it[KEY_CREATE_TIME] as LocalDateTime),
                        updateTime = DateTimeUtil.toDateTime(it[KEY_UPDATE_TIME] as LocalDateTime)
                    )
                )
            }
            Result(data = storeEnvVarList)
        } else {
            Result(data = null)
        }
    }

    override fun getEnvVarChangeLogList(
        userId: String,
        storeType: String,
        storeCode: String,
        scope: String,
        varName: String
    ): Result<List<StoreEnvChangeLogInfo>?> {
        logger.info("storeEnvVar getEnvVarChangeLogList params:[$userId|$storeType|$storeCode|$varName]")
        val storeTypeObj = StoreTypeEnum.valueOf(storeType)
        if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeTypeObj.type.toByte())) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storeCode)
            )
        }
        val storeEnvVarRecords = storeEnvVarDao.getEnvVarList(
            dslContext = dslContext,
            storeType = storeTypeObj.type.toByte(),
            storeCode = storeCode,
            scope = scope,
            varName = varName
        )
        return if (storeEnvVarRecords != null && storeEnvVarRecords.size > 1) {
            val storeEnvChangeLogList = mutableListOf<StoreEnvChangeLogInfo>()
            for (i in storeEnvVarRecords.indices) {
                if (i < storeEnvVarRecords.size - 1) {
                    val nowStoreEnvRecord = storeEnvVarRecords[i]
                    val pastStoreEnvRecord = storeEnvVarRecords[i + 1]
                    storeEnvChangeLogList.add(
                        StoreEnvChangeLogInfo(
                            varName = varName,
                            beforeVarValue = if (pastStoreEnvRecord.encryptFlag) {
                                aesMock
                            } else pastStoreEnvRecord.varValue,
                            afterVarValue = if (nowStoreEnvRecord.encryptFlag) {
                                aesMock
                            } else nowStoreEnvRecord.varValue,
                            modifier = nowStoreEnvRecord.modifier,
                            updateTime = DateTimeUtil.toDateTime(nowStoreEnvRecord.updateTime)
                        )
                    )
                }
            }
            Result(data = storeEnvChangeLogList)
        } else {
            Result(data = null)
        }
    }
}
