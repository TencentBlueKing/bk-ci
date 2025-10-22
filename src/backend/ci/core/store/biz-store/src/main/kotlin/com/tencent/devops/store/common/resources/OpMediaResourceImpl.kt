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
import com.tencent.devops.store.api.common.OpMediaResource
import com.tencent.devops.store.pojo.common.media.MediaInfoReq
import com.tencent.devops.store.pojo.common.media.StoreMediaInfo
import com.tencent.devops.store.pojo.common.media.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.common.service.StoreMediaService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpMediaResourceImpl @Autowired constructor(
    val storeMediaService: StoreMediaService
) : OpMediaResource {

    override fun createStoreMedia(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        mediaInfoList: List<MediaInfoReq>
    ): Result<Boolean> {
        storeMediaService.deleteByStoreCode(userId, storeCode, storeType)
        mediaInfoList.forEach {
            val storeMediaInfoRequest = StoreMediaInfoRequest(
                storeCode = storeCode,
                mediaUrl = it.mediaUrl,
                mediaType = it.mediaType.toString(),
                modifier = userId
            )
            storeMediaService.add(
                userId = userId,
                type = storeType,
                storeMediaInfo = storeMediaInfoRequest
            )
        }
        return Result(true)
    }

    override fun getStoreMedia(userId: String, mediaId: String): Result<StoreMediaInfo?> {
        return storeMediaService.get(
            userId = userId,
            id = mediaId
        )
    }

    override fun getStoreMediaByStoreCode(
        userId: String,
        storeCode: String,
        labelType: StoreTypeEnum
    ): Result<List<StoreMediaInfo>?> {
        return storeMediaService.getByCode(
            storeCode = storeCode,
            storeType = labelType
        )
    }
}
