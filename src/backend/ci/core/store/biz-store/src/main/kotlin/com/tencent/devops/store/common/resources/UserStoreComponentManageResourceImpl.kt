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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.UserStoreComponentManageResource
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreDeleteRequest
import com.tencent.devops.store.pojo.common.test.StoreTestRequest

@RestResource
class UserStoreComponentManageResourceImpl(
    private val storeComponentManageService: StoreComponentManageService,
    private val storeProjectService: StoreProjectService
) : UserStoreComponentManageResource {

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
            storeBaseInfoUpdateRequest = storeBaseInfoUpdateRequest
        )
    }

    override fun installComponent(userId: String, installStoreReq: InstallStoreReq): Result<Boolean> {
        return storeComponentManageService.installComponent(
            userId = userId,
            channelCode = ChannelCode.BS,
            installStoreReq = installStoreReq
        )
    }

    override fun uninstallComponent(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
        return storeComponentManageService.uninstallComponent(
            userId = userId,
            projectCode = projectCode,
            storeType = storeType,
            storeCode = storeCode,
            unInstallReq = unInstallReq
        )
    }

    override fun deleteComponent(userId: String, storeType: String, storeCode: String): Result<Boolean> {
        return storeComponentManageService.deleteComponent(
            userId = userId,
            handlerRequest = StoreDeleteRequest(storeCode, storeType)
        )
    }

    override fun updateStoreRepositoryAuthorizer(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<Boolean> {
        return storeComponentManageService.updateStoreRepositoryAuthorizer(userId, storeType, storeCode)
    }

    override fun saveStoreTestInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        storeTestRequest: StoreTestRequest
    ): Result<Boolean> {
        return Result(
            storeProjectService.saveStoreTestInfo(
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                storeTestRequest = storeTestRequest
            )
        )
    }
}
