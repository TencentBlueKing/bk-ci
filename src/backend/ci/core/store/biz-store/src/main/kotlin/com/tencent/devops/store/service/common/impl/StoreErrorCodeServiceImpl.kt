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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreDockingPlatformDao
import com.tencent.devops.store.dao.common.StoreErrorCodeInfoDao
import com.tencent.devops.store.pojo.common.ErrorCodeInfo
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.enums.ErrorCodeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreErrorCodeService
import org.jooq.DSLContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StoreErrorCodeServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeErrorCodeInfoDao: StoreErrorCodeInfoDao,
    private val storeDockingPlatformDao: StoreDockingPlatformDao
) : StoreErrorCodeService {

    @Value("\${store.defaultAtomErrorCodeLength:6}")
    private var defaultAtomErrorCodeLength: Int = 6

    @Value("\${store.defaultAtomErrorCodePrefix:8}")
    private lateinit var defaultAtomErrorCodePrefix: String

    override fun createGeneralErrorCode(
        userId: String,
        errorCodeInfo: ErrorCodeInfo
    ): Result<Boolean> {
        // 检查错误码是否规范
        checkErrorCode(ErrorCodeTypeEnum.GENERAL, listOf("${errorCodeInfo.errorCode}"))
        storeErrorCodeInfoDao.batchUpdateErrorCodeInfo(
            dslContext = dslContext,
            userId = userId,
            storeErrorCodeInfo = StoreErrorCodeInfo(
                storeCode = null,
                storeType = null,
                errorCodes = setOf(errorCodeInfo.errorCode)
            )
        )
        return Result(true)
    }

    override fun isComplianceErrorCode(
        storeCode: String,
        storeType: StoreTypeEnum,
        errorCode: Int,
        errorCodeType: ErrorCodeTypeEnum
    ): Boolean {
        try {
            checkErrorCode(errorCodeType, listOf("$errorCode"))
        } catch (e: ErrorCodeException) {
            logger.warn("errorCode Non-compliance {${e.message}}")
            return false
        }
        if (errorCodeType == ErrorCodeTypeEnum.PLATFORM) {
            val errorCodePrefix = "$errorCode".substring(0, 3)
            val record =
                storeDockingPlatformDao.getStoreDockingPlatformByErrorCode(dslContext, errorCodePrefix.toInt())
            return record?.let {
                storeDockingPlatformDao.getPlatformErrorCode(
                    dslContext,
                    record.platformCode,
                    errorCode
                ) != null
            } ?: false
        }
        return storeErrorCodeInfoDao.getStoreErrorCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            errorCode = errorCode
        ).isNotEmpty
    }

    @Suppress("ComplexMethod")
    private fun checkErrorCode(errorCodeType: ErrorCodeTypeEnum, errorCodeInfos: List<String>) {
        val invalidErrorCodes = mutableListOf<String>()
        errorCodeInfos.forEach { errorCode ->
            if (errorCode.length != defaultAtomErrorCodeLength) invalidErrorCodes.add(errorCode)
            val errorCodePrefix = errorCode.substring(0, 3)
            when (errorCodeType) {
                ErrorCodeTypeEnum.ATOM -> {
                    if (!errorCodePrefix.startsWith(defaultAtomErrorCodePrefix)) {
                        invalidErrorCodes.add(errorCode)
                    }
                }
                ErrorCodeTypeEnum.GENERAL -> {
                    if (!errorCodePrefix.startsWith("100")) {
                        invalidErrorCodes.add(errorCode)
                    }
                }
                ErrorCodeTypeEnum.PLATFORM -> {
                    if (errorCodePrefix.toInt() !in 101..599) {
                        invalidErrorCodes.add(errorCode)
                    }
                }
            }
        }
        if (invalidErrorCodes.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ERROR_CODE_INVALID,
                params = arrayOf("[${invalidErrorCodes.joinToString(",")}]")
            )
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(StoreErrorCodeServiceImpl::class.java)
    }
}
