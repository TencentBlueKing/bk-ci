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
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.ServiceStoreComponentBaseResource
import com.tencent.devops.store.common.service.StoreComponentBaseInfoQueryService
import com.tencent.devops.store.common.service.StoreComponentDetailQueryService
import com.tencent.devops.store.common.service.StoreMediaService
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.media.StoreMediaInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStoreComponentBaseResourceImpl @Autowired constructor(
    private val storeComponentDetailQueryService: StoreComponentDetailQueryService,
    private val storeComponentBaseInfoQueryService: StoreComponentBaseInfoQueryService,
    private val storeMediaService: StoreMediaService
) : ServiceStoreComponentBaseResource {

    override fun getComponentDetailInfoById(
        userId: String,
        storeType: String,
        storeId: String
    ): Result<StoreDetailInfo?> {
        return Result(
            storeComponentDetailQueryService.getComponentDetailInfoById(
                userId = userId,
                storeId = storeId,
                storeType = StoreTypeEnum.valueOf(storeType)
            )
        )
    }

    override fun getComponentBaseInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        version: String?
    ): Result<StoreBaseInfo?> {
        return Result(
            storeComponentBaseInfoQueryService.getComponentBaseInfo(
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                version = version
            )
        )
    }

    override fun getComponentDataInfoByCode(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String?,
        status: StoreStatusEnum?
    ): Result<StoreDetailInfo?> {
        return Result(
            storeComponentDetailQueryService.getComponentDataInfoByCode(
                storeType = storeType.name,
                storeCode = storeCode,
                version = version,
                status = status
            )
        )
    }

    override fun getComponentBaseInfoByCodes(
        storeType: StoreTypeEnum,
        storeCodes: String?
    ): Result<List<StoreBaseInfo>> {
        return Result(
            storeComponentBaseInfoQueryService.getComponentBaseInfoList(
                storeType = storeType,
                storeCodes = storeCodes?.split(",")?.toSet()
            )
        )
    }

    override fun getStoreMediaInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<List<StoreMediaInfo>?> {
        return storeMediaService.getByCode(storeCode, storeType)
    }
}
