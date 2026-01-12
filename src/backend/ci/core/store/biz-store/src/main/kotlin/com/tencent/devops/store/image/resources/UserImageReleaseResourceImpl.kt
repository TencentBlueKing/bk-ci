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
package com.tencent.devops.store.image.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.UserImageReleaseResource
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.OfflineMarketImageReq
import com.tencent.devops.store.pojo.image.response.ImageAgentTypeInfo
import com.tencent.devops.store.image.service.ImageReleaseService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageReleaseResourceImpl @Autowired constructor(
    private val imageReleaseService: ImageReleaseService
) : UserImageReleaseResource {

    override fun offlineMarketImage(
        userId: String,
        imageCode: String,
        offlineMarketImageReq: OfflineMarketImageReq
    ): Result<Boolean> {
        return imageReleaseService.offlineMarketImage(
            userId = userId,
            imageCode = imageCode,
            version = offlineMarketImageReq.version,
            reason = offlineMarketImageReq.reason,
            interfaceName = "/user/market/desk/image/offline/imageCodes/{imageCode}/versions"
        )
    }

    override fun addMarketImage(
        userId: String,
        imageCode: String,
        marketImageRelRequest: MarketImageRelRequest
    ): Result<String> {
        return imageReleaseService.addMarketImage(userId, imageCode, marketImageRelRequest)
    }

    override fun updateMarketImage(
        userId: String,
        marketImageUpdateRequest: MarketImageUpdateRequest
    ): Result<String?> {
        return imageReleaseService.updateMarketImage(userId, marketImageUpdateRequest)
    }

    override fun getProcessInfo(userId: String, imageId: String): Result<StoreProcessInfo> {
        return imageReleaseService.getProcessInfo(userId, imageId)
    }

    override fun cancelRelease(userId: String, imageId: String): Result<Boolean> {
        return imageReleaseService.cancelRelease(userId, imageId)
    }

    override fun recheck(userId: String, imageId: String): Result<Boolean> {
        return imageReleaseService.recheck(userId, imageId)
    }

    override fun passTest(userId: String, imageId: String): Result<Boolean> {
        return imageReleaseService.passTest(userId, imageId)
    }

    override fun getImageAgentTypes(userId: String): Result<List<ImageAgentTypeInfo>> {
        return Result(imageReleaseService.getImageAgentTypes(userId))
    }
}
