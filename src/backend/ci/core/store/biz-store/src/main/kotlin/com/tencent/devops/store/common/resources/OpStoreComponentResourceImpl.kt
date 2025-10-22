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
package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.OpStoreComponentResource
import com.tencent.devops.store.common.service.OpStoreComponentService
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.common.service.StoreComponentQueryService
import com.tencent.devops.store.common.service.StoreReleaseService
import com.tencent.devops.store.pojo.common.InstalledPkgFileShaContentRequest
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.QueryComponentsParam
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreApproveReleaseRequest
import com.tencent.devops.store.pojo.common.publication.StoreDeleteRequest
import com.tencent.devops.store.pojo.common.publication.StoreOfflineRequest
import com.tencent.devops.store.pojo.common.version.StoreDeskVersionItem
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpStoreComponentResourceImpl @Autowired constructor(
    private val opStoreComponentService: OpStoreComponentService,
    private val storeComponentQueryService: StoreComponentQueryService,
    private val storeComponentManageService: StoreComponentManageService,
    private val storeReleaseService: StoreReleaseService
) : OpStoreComponentResource {

    override fun approveComponentRelease(
        userId: String,
        storeId: String,
        storeApproveReleaseRequest: StoreApproveReleaseRequest
    ): Result<Boolean> {
        return Result(opStoreComponentService.approveComponentRelease(userId, storeId, storeApproveReleaseRequest))
    }

    override fun listComponents(
        userId: String,
        storeType: String,
        name: String?,
        type: String?,
        processFlag: Boolean?,
        classifyCode: String?,
        categoryCodes: String?,
        labelCodes: String?,
        sortType: StoreSortTypeEnum?,
        page: Int,
        pageSize: Int
    ): Result<Page<MyStoreComponent>?> {
        return Result(
            storeComponentQueryService.listComponents(
                userId = userId,
                queryComponentsParam = QueryComponentsParam(
                    storeType = storeType,
                    type = type,
                    name = name,
                    processFlag = processFlag,
                    classifyCode = classifyCode,
                    categoryCodes = categoryCodes,
                    labelCodes = labelCodes,
                    sortType = sortType,
                    page = page,
                    pageSize = pageSize
                )
            )
        )
    }

    override fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreDeskVersionItem>> {
        return Result(
            storeComponentQueryService.getComponentVersionsByCode(
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                page = page,
                pageSize = pageSize,
                checkPermissionFlag = false
            )
        )
    }

    override fun getComponentDetailInfoById(
        userId: String,
        storeType: String,
        storeId: String
    ): Result<StoreDetailInfo?> {
        return Result(
            storeComponentQueryService.getComponentDetailInfoById(
                userId = userId,
                storeType = StoreTypeEnum.valueOf(storeType),
                storeId = storeId
            )
        )
    }

    override fun updateComponentBaseInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    ): Result<Boolean> {
        return storeComponentManageService.updateComponentBaseInfo(
            userId = userId,
            storeType = storeType,
            storeCode = storeCode,
            storeBaseInfoUpdateRequest = storeBaseInfoUpdateRequest,
            checkPermissionFlag = false
        )
    }

    override fun deleteComponent(userId: String, storeType: String, storeCode: String): Result<Boolean> {
        return storeComponentManageService.deleteComponent(
            userId = userId,
            handlerRequest = StoreDeleteRequest(
                storeCode = storeCode,
                storeType = storeType,
                checkPermissionFlag = false
            )
        )
    }

    override fun offlineComponent(userId: String, storeOfflineRequest: StoreOfflineRequest): Result<Boolean> {
        return Result(storeReleaseService.offlineComponent(
            userId = userId,
            storeOfflineRequest = storeOfflineRequest,
            checkPermissionFlag = false
            )
        )
    }

    override fun updateComponentInstalledPkgFileShaContent(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        installedPkgFileShaContentRequest: InstalledPkgFileShaContentRequest
    ): Result<Boolean> {
        return storeComponentManageService.updateComponentInstalledPkgShaContent(
            userId = userId,
            storeType = storeType,
            storeCode = storeCode,
            version = version,
            installedPkgFileShaContentRequest = installedPkgFileShaContentRequest
        )
    }
}
