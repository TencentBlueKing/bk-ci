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

package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceCommentResource
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreStatisticService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceCommentResourceImpl @Autowired constructor(
    private val storeStatisticService: StoreStatisticService,
    private val storeCommentService: StoreCommentService
) : UserExtServiceCommentResource {
    override fun createServiceComment(
        userId: String,
        serviceId: String,
        serviceCodes: String,
        storeCommentRequest: StoreCommentRequest
    ): Result<StoreCommentInfo?> {
        return storeCommentService.addStoreComment(
            userId = userId,
            storeId = serviceId,
            storeCode = serviceCodes,
            storeCommentRequest = storeCommentRequest,
            storeType = StoreTypeEnum.SERVICE
        )
    }

    override fun updateServiceComment(
        userId: String,
        commentId: String,
        storeCommentRequest: StoreCommentRequest
    ): Result<Boolean> {
        return storeCommentService.updateStoreComment(
            userId = userId,
            commentId = commentId,
            storeCommentRequest = storeCommentRequest
        )
    }

    override fun getServiceComment(userId: String, commentId: String): Result<StoreCommentInfo?> {
        return storeCommentService.getStoreComment(
            userId = userId,
            commentId = commentId
        )
    }

    override fun getServiceCommentByServiceCode(
        userId: String,
        serviceCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?> {
        return storeCommentService.getStoreComments(
            userId = userId,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getServiceCommentScoreInfo(serviceCode: String): Result<StoreCommentScoreInfo> {
        return storeStatisticService.getStoreCommentScoreInfo(
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
    }

    override fun updateStoreCommentPraiseCount(userId: String, commentId: String): Result<Int> {
        return storeCommentService.updateStoreCommentPraiseCount(userId, commentId)
    }
}
