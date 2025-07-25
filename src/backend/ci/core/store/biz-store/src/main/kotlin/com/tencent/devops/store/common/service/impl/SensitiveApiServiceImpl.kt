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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SHA_CONTENT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_FILE_SHA_CONTENT
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.PageUtil.DEFAULT_PAGE_SIZE
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.store.common.dao.BusinessConfigDao
import com.tencent.devops.store.common.dao.SensitiveApiDao
import com.tencent.devops.store.common.dao.StoreBaseEnvExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.SensitiveApiService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.ApiLevelEnum
import com.tencent.devops.store.pojo.common.enums.ApiStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiApplyReq
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiApproveReq
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiConfig
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiCreateDTO
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiInfo
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiNameInfo
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiSearchDTO
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiUpdateDTO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SensitiveApiServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val sensitiveApiDao: SensitiveApiDao,
    private val businessConfigDao: BusinessConfigDao,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseEnvQueryDao: StoreBaseEnvQueryDao,
    private val storeBaseEnvExtQueryDao: StoreBaseEnvExtQueryDao
) : SensitiveApiService {

    companion object {
        private const val BUSINESS_CONFIG_FEATURE = "api"
        private const val BUSINESS_CONFIG_VALUE = "sensitiveApi"
    }

    override fun unApprovalApiList(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        language: String
    ): Result<List<SensitiveApiNameInfo>> {
        val sensitiveApiConfigList = getSensitiveApiConfig(storeType)
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

    private fun getSensitiveApiConfig(storeType: StoreTypeEnum): List<SensitiveApiConfig> {
        val businessConfigRecord = businessConfigDao.get(
            dslContext = dslContext,
            business = storeType.name,
            feature = BUSINESS_CONFIG_FEATURE,
            businessValue = BUSINESS_CONFIG_VALUE
        ) ?: return emptyList()
        return JsonUtil.to(
            json = businessConfigRecord.configValue,
            typeReference = object : TypeReference<List<SensitiveApiConfig>>() {}
        ).filter { !it.hideFlag }
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
            val sensitiveApiNameMap = getSensitiveApiConfig(storeType).associateBy { it.apiName }
            val sensitiveApiCreateDTOs =
                apiNameList.filter { it.isNotBlank() }
                    .map { apiName ->
                        val config = sensitiveApiNameMap[apiName]
                            ?: throw ErrorCodeException(
                                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                                params = arrayOf(apiName)
                            )
                        SensitiveApiCreateDTO(
                            id = UUIDUtil.generate(),
                            userId = userId,
                            storeType = storeType,
                            storeCode = storeCode,
                            apiName = apiName,
                            aliasName = config.aliasNames?.get(language) ?: apiName,
                            applyDesc = applyDesc,
                            apiStatus = if (config.needReview) ApiStatusEnum.WAIT else ApiStatusEnum.PASS,
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
        signFileName: String?,
        fileShaContent: String?,
        osName: String?,
        osArch: String?,
        storeType: StoreTypeEnum,
        storeCode: String,
        apiName: String,
        version: String?
    ): Result<Boolean> {
        if (storeType == StoreTypeEnum.DEVX) {
            if (version.isNullOrBlank()) {
                throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_NEED_PARAM_, params = arrayOf(KEY_VERSION))
            }
            if (fileShaContent.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                    params = arrayOf(AUTH_HEADER_DEVOPS_SHA_CONTENT)
                )
            }
            // 判断请求中的sha1算法摘要值和db中的sha1算法摘要值是否能匹配
            val storeId = storeBaseQueryDao.getComponentId(
                dslContext = dslContext,
                storeCode = storeCode,
                version = version,
                storeType = storeType
            ) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$storeType:$storeCode:$version")
            )
            val baseEnvRecord = storeBaseEnvQueryDao.getBaseEnvsByStoreId(
                dslContext = dslContext, storeId = storeId, osName = osName, osArch = osArch
            )?.getOrNull(0) ?: storeBaseEnvQueryDao.getDefaultBaseEnvInfo(
                dslContext = dslContext, storeId = storeId, osName = osName
            ) ?: storeBaseEnvQueryDao.getDefaultBaseEnvInfo(dslContext, storeId) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf("$osName:$osArch")
            )
            val dbFileShaContent = storeBaseEnvExtQueryDao.getBaseExtEnvsByEnvId(
                dslContext = dslContext,
                envId = baseEnvRecord.id,
                "${KEY_FILE_SHA_CONTENT}_$signFileName"
            )?.getOrNull(0)?.fieldValue ?: baseEnvRecord.shaContent
            if (fileShaContent.lowercase() != dbFileShaContent) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_VALIDATE_ERROR,
                    params = arrayOf(AUTH_HEADER_DEVOPS_SHA_CONTENT, "wrong sha1 content")
                )
            }
        }
        // 判断组件是否有使用该API接口的权限
        val record = sensitiveApiDao.getByApiName(
            dslContext = dslContext,
            storeType = storeType,
            storeCode = storeCode,
            apiName = apiName
        )
        return Result(record != null && record.apiStatus == ApiStatusEnum.PASS.name)
    }
}
