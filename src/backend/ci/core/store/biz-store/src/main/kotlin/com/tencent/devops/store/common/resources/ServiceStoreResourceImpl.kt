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

package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.service.ClassifyService
import com.tencent.devops.store.common.service.StoreBuildService
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.common.service.StoreErrorCodeService
import com.tencent.devops.store.common.service.StoreMemberService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.common.service.UserSensitiveConfService
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.classify.Classify
import com.tencent.devops.store.pojo.common.enums.ErrorCodeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBuildResultRequest
import com.tencent.devops.store.pojo.common.sensitive.SensitiveConfResp
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("LongParameterList")
class ServiceStoreResourceImpl @Autowired constructor(
    private val storeProjectService: StoreProjectService,
    private val sensitiveConfService: UserSensitiveConfService,
    private val storeBuildService: StoreBuildService,
    private val storeErrorCodeService: StoreErrorCodeService,
    private val storeMemberService: StoreMemberService,
    private val classifyService: ClassifyService,
    private val storeComponentManageService: StoreComponentManageService,
    private val storeInnerPipelineConfig: StoreInnerPipelineConfig
) : ServiceStoreResource {

    override fun uninstall(storeCode: String, storeType: StoreTypeEnum, projectCode: String): Result<Boolean> {
        return storeProjectService.uninstall(storeType, storeCode, projectCode)
    }

    override fun getSensitiveConf(storeType: StoreTypeEnum, storeCode: String): Result<List<SensitiveConfResp>?> {
        return sensitiveConfService.list("", storeType, storeCode, true)
    }

    override fun handleStoreBuildResult(
        pipelineId: String,
        buildId: String,
        storeBuildResultRequest: StoreBuildResultRequest
    ): Result<Boolean> {
        return storeBuildService.handleStoreBuildResult(pipelineId, buildId, storeBuildResultRequest)
    }

    override fun isStoreMember(storeCode: String, storeType: StoreTypeEnum, userId: String): Result<Boolean> {
        return Result(
            storeMemberService.isStoreMember(
                userId, storeCode, storeType.type.toByte()
            )
        )
    }

    override fun isPublicProject(projectCode: String): Result<Boolean> {
        return Result(
            projectCode == storeInnerPipelineConfig.innerPipelineProject
        )
    }

    override fun validatePipelineUserStorePermission(
        storeCode: String,
        storeType: StoreTypeEnum,
        userId: String
    ): Result<Boolean> {
        return Result(
            storeInnerPipelineConfig.innerPipelineUser == userId || storeMemberService.isStoreMember(
                userId, storeCode, storeType.type.toByte()
            )
        )
    }

    override fun isComplianceErrorCode(
        storeCode: String,
        storeType: StoreTypeEnum,
        errorCode: Int,
        errorCodeType: ErrorCodeTypeEnum
    ): Result<Boolean> {
        return Result(
            storeErrorCodeService.isComplianceErrorCode(
                storeCode = storeCode,
                storeType = storeType,
                errorCode = errorCode,
                errorCodeType = errorCodeType
            )
        )
    }

    override fun validateComponentDownloadPermission(
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        projectCode: String,
        userId: String,
        instanceId: String?
    ): Result<StoreBaseInfo?> {
        return storeComponentManageService.validateComponentDownloadPermission(
            storeCode = storeCode,
            storeType = storeType,
            version = version,
            projectCode = projectCode,
            userId = userId,
            instanceId = instanceId
        )
    }

    override fun getClassifyList(storeType: StoreTypeEnum): Result<List<Classify>> {
        return classifyService.getAllClassify(storeType.type.toByte())
    }
}
