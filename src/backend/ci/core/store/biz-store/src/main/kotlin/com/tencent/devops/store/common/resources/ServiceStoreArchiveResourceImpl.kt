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
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.ServiceStoreArchiveResource
import com.tencent.devops.store.common.service.StoreArchiveService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.common.QueryComponentPkgEnvInfoParam
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StorePkgEnvInfo
import com.tencent.devops.store.pojo.common.publication.StorePkgInfoUpdateRequest
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStoreArchiveResourceImpl @Autowired constructor(
    private val storeArchiveService: StoreArchiveService
) : ServiceStoreArchiveResource {

    override fun verifyComponentPackage(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        releaseType: ReleaseTypeEnum?
    ): Result<Boolean> {
        return Result(
            storeArchiveService.verifyComponentPackage(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType,
                version = version,
                releaseType = releaseType
            )
        )
    }

    override fun updateComponentPkgInfo(
        userId: String,
        storePkgInfoUpdateRequest: StorePkgInfoUpdateRequest
    ): Result<Boolean> {
        return Result(
            storeArchiveService.updateComponentPkgInfo(userId, storePkgInfoUpdateRequest)
        )
    }

    override fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        osName: String?,
        osArch: String?
    ): Result<List<StorePkgEnvInfo>> {
        val storeReleaseSpecBusService = SpringContextUtil.getBean(
            StoreReleaseSpecBusService::class.java,
            StoreUtils.getReleaseSpecBusServiceBeanName(storeType)
        )
        return Result(
            storeReleaseSpecBusService.getComponentPkgEnvInfo(
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                version = version,
                osName = osName,
                osArch = osArch
            )
        )
    }

    override fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        queryComponentPkgEnvInfoParam: QueryComponentPkgEnvInfoParam
    ): Result<List<StorePkgEnvInfo>> {
        val storeReleaseSpecBusService = SpringContextUtil.getBean(
            StoreReleaseSpecBusService::class.java,
            StoreUtils.getReleaseSpecBusServiceBeanName(storeType)
        )
        return Result(
            storeReleaseSpecBusService.getComponentPkgEnvInfo(
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                version = version,
                queryComponentPkgEnvInfoParam = queryComponentPkgEnvInfoParam
            )
        )
    }
}
