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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.PageUtil.DEFAULT_PAGE_SIZE
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.SensitiveApiDao
import com.tencent.devops.store.pojo.common.SensitiveApiApplyReq
import com.tencent.devops.store.pojo.common.SensitiveApiApproveReq
import com.tencent.devops.store.pojo.common.SensitiveApiConfig
import com.tencent.devops.store.pojo.common.SensitiveApiCreateDTO
import com.tencent.devops.store.pojo.common.SensitiveApiInfo
import com.tencent.devops.store.pojo.common.SensitiveApiNameInfo
import com.tencent.devops.store.pojo.common.SensitiveApiSearchDTO
import com.tencent.devops.store.pojo.common.SensitiveApiUpdateDTO
import com.tencent.devops.store.pojo.common.enums.ApiLevelEnum
import com.tencent.devops.store.pojo.common.enums.ApiStatusEnum
import com.tencent.devops.store.pojo.common.enums.BusinessEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.SensitiveApiService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SensitiveApiServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val sensitiveApiDao: SensitiveApiDao,
    private val businessConfigDao: BusinessConfigDao
) : SensitiveApiService {

    companion object {
        private const val BUSINESS_CONFIG_FEATURE = "api"
        private const val BUSINESS_CONFIG_VALUE = "sensitiveApi"
        private val logger = LoggerFactory.getLogger(SensitiveApiServiceImpl::class.java)
    }

    override fun unApprovalApiList(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        language: String
    ): Result<List<SensitiveApiNameInfo>> {
        val sensitiveApiConfigList = getSensitiveApiConfig()
        val approvedApiList = sensitiveApiDao.getApprovedApiNameList(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType
        )
        return Result(
            sensitiveApiConfigList.filter { !approvedApiList.contains(it.apiName) }
                .map { SensitiveApiNameInfo(it.apiName, it.aliasNames?.get(language) ?: it.apiName) }
                .toList()
        )
    }

    private fun getSensitiveApiConfig(): List<SensitiveApiConfig> {
        val businessConfigRecord = businessConfigDao.get(
            dslContext = dslContext,
            business = BusinessEnum.ATOM.name,
            feature = BUSINESS_CONFIG_FEATURE,
            businessValue = BUSINESS_CONFIG_VALUE
        ) ?: return emptyList()
        return JsonUtil.to(
            json = businessConfigRecord.configValue,
            typeReference = object : TypeReference<List<SensitiveApiConfig>>() {}
        )
    }

    override fun apply(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        sensitiveApiApplyReq: SensitiveApiApplyReq
    ): Result<Boolean> {
        with(sensitiveApiApplyReq) {
            if (sensitiveApiApplyReq.applyDesc.isBlank()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("applyDesc")
                )
            }
            val sensitiveApiNameMap = getSensitiveApiConfig().associateBy { it.apiName }
            val sensitiveApiCreateDTOs =
                apiNameList.filter { it.isNotBlank() }
                    .filter { sensitiveApiNameMap.containsKey(it) }
                    .map { apiName ->
                        SensitiveApiCreateDTO(
                            id = UUIDUtil.generate(),
                            userId = userId,
                            storeType = storeType,
                            storeCode = storeCode,
                            apiName = apiName,
                            aliasName = sensitiveApiNameMap[apiName]!!.aliasNames?.get(language) ?: apiName,
                            applyDesc = applyDesc,
                            apiStatus = ApiStatusEnum.WAIT,
                            apiLevel = ApiLevelEnum.SENSITIVE
                        )
                    }
            sensitiveApiDao.create(
                dslContext = dslContext,
                sensitiveApiCreateDTOs = sensitiveApiCreateDTOs
            )
        }
        return Result(true)
    }

    override fun list(
        page: Int?,
        pageSize: Int?,
        sensitiveApiSearchDTO: SensitiveApiSearchDTO
    ): Result<Page<SensitiveApiInfo>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: DEFAULT_PAGE_SIZE
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val count = sensitiveApiDao.count(
            dslContext = dslContext,
            sensitiveApiSearchDTO = sensitiveApiSearchDTO
        )
        val record = sensitiveApiDao.list(
            dslContext = dslContext,
            offset = limit.offset,
            limit = limit.limit,
            sensitiveApiSearchDTO = sensitiveApiSearchDTO
        ).map { sensitiveApiDao.convert(it) }
        return Result(Page(pageNotNull, pageSizeNotNull, count, record))
    }

    override fun cancel(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        id: String
    ): Result<Boolean> {
        val record = sensitiveApiDao.get(
            dslContext = dslContext,
            id = id
        ) ?: throw ErrorCodeException(
            errorCode = StoreMessageCode.SENSITIVE_API_NOT_EXIST,
            params = arrayOf(id)
        )
        if (record.apiStatus == ApiStatusEnum.PASS.name) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.SENSITIVE_API_PASSED_IS_NOT_ALLOW_CANCEL
            )
        }
        val updateDTO = SensitiveApiUpdateDTO(
            id = id,
            userId = userId,
            apiStatus = ApiStatusEnum.CANCEL
        )
        sensitiveApiDao.updateApiStatus(
            dslContext = dslContext,
            sensitiveApiUpdateDTO = updateDTO
        )
        return Result(true)
    }

    override fun approve(
        userId: String,
        sensitiveApiApproveReq: SensitiveApiApproveReq
    ): Result<Boolean> {
        with(sensitiveApiApproveReq) {
            val record = sensitiveApiDao.get(
                dslContext = dslContext,
                id = id
            ) ?: throw ErrorCodeException(
                errorCode = StoreMessageCode.SENSITIVE_API_NOT_EXIST,
                params = arrayOf(id)
            )
            if (record.apiStatus == ApiStatusEnum.CANCEL.name) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.SENSITIVE_API_APPROVED_IS_NOT_ALLOW_PASS
                )
            }
            val updateDTO = SensitiveApiUpdateDTO(
                id = id,
                userId = userId,
                apiStatus = apiStatus,
                approveMsg = approveMsg
            )
            sensitiveApiDao.updateApiStatus(
                dslContext = dslContext,
                sensitiveApiUpdateDTO = updateDTO
            )
        }
        return Result(true)
    }

    override fun verifyApi(
        storeType: StoreTypeEnum,
        storeCode: String,
        apiName: String
    ): Result<Boolean> {
        val record = sensitiveApiDao.getByApiName(
            dslContext = dslContext,
            storeType = storeType,
            storeCode = storeCode,
            apiName = apiName
        )
        return Result(record != null && record.apiStatus == ApiStatusEnum.PASS.name)
    }
}
