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
import com.tencent.devops.store.api.common.UserStoreComponentVersionResource
import com.tencent.devops.store.common.service.StoreComponentVersionQueryService
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreComponentVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.StoreVersionLogInfo
import com.tencent.devops.store.pojo.common.version.StoreVersionSizeInfo
import com.tencent.devops.store.pojo.common.version.VersionInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStoreComponentVersionResourceImpl @Autowired constructor(
    private val storeComponentVersionQueryService: StoreComponentVersionQueryService
) : UserStoreComponentVersionResource {

    override fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int,
        availableFlag: Boolean?
    ): Result<Page<StoreComponentVersionItem>> {
        // availableFlag=true  → 仅 RELEASED，不校验成员权限（首页等公开场景）
        // availableFlag=null/false → 全部版本，校验成员权限（工作台，兼容老逻辑）
        val checkPermission = availableFlag != true
        val statusList = if (availableFlag == true) listOf(StoreStatusEnum.RELEASED.name) else null
        return Result(
            storeComponentVersionQueryService.getComponentVersionsByCode(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType,
                page = page,
                pageSize = pageSize,
                checkPermissionFlag = checkPermission,
                storeStatusList = statusList
            )
        )
    }

    override fun getComponentShowVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String
    ): Result<StoreShowVersionInfo> {
        return Result(
            storeComponentVersionQueryService.getComponentShowVersionInfo(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType
            )
        )
    }

    override fun getStoreUpgradeVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        projectCode: String,
        instanceId: String?,
        osName: String?,
        osArch: String?
    ): Result<VersionInfo?> {
        return Result(
            storeComponentVersionQueryService.getComponentUpgradeVersionInfo(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType,
                projectCode = projectCode,
                instanceId = instanceId,
                osName = osName,
                osArch = osArch
            )
        )
    }

    override fun getStoreVersionLogs(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreVersionLogInfo>> {
        return storeComponentVersionQueryService.getStoreVersionLogs(
            storeCode = storeCode,
            storeType = storeType,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getStoreVersionSize(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        osName: String?,
        osArch: String?
    ): Result<StoreVersionSizeInfo> {
        return Result(
            storeComponentVersionQueryService.getStoreVersionSize(
                storeCode = storeCode,
                storeType = storeType,
                version = version,
                osName = osName,
                osArch = osArch
            )
        )
    }
}
