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

package com.tencent.devops.store.template.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.template.UserTemplateCommentResource
import com.tencent.devops.store.pojo.common.comment.StoreCommentInfo
import com.tencent.devops.store.pojo.common.comment.StoreCommentRequest
import com.tencent.devops.store.pojo.common.comment.StoreCommentScoreInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.common.service.StoreCommentService
import com.tencent.devops.store.common.service.StoreStatisticService
import com.tencent.devops.store.template.service.MarketTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTemplateCommentResourceImpl @Autowired constructor(
    private val marketTemplateService: MarketTemplateService,
    private val storeCommentService: StoreCommentService,
    private val storeStatisticService: StoreStatisticService
) :
    UserTemplateCommentResource {

    override fun getTemplateCommentScoreInfo(templateCode: String): Result<StoreCommentScoreInfo> {
        return storeStatisticService.getStoreCommentScoreInfo(templateCode, StoreTypeEnum.TEMPLATE)
    }

    override fun getStoreComment(userId: String, commentId: String): Result<StoreCommentInfo?> {
        return storeCommentService.getStoreComment(userId, commentId)
    }

    override fun getStoreComments(
        userId: String,
        templateCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?> {
        return storeCommentService.getStoreComments(userId, templateCode, StoreTypeEnum.TEMPLATE, page, pageSize)
    }

    override fun addTemplateComment(
        userId: String,
        templateId: String,
        templateCode: String,
        storeCommentRequest: StoreCommentRequest
    ): Result<StoreCommentInfo?> {
        // 判断templateId和templateCode是否真实有效
        val result = marketTemplateService.judgeTemplateExistByIdAndCode(templateId, templateCode)
        if (result.isNotOk()) {
            return Result(status = result.status, message = result.message ?: "")
        }
        return storeCommentService.addStoreComment(
            userId,
            templateId,
            templateCode,
            storeCommentRequest,
            StoreTypeEnum.TEMPLATE
        )
    }

    override fun updateStoreComment(
        userId: String,
        commentId: String,
        storeCommentRequest: StoreCommentRequest
    ): Result<Boolean> {
        return storeCommentService.updateStoreComment(userId, commentId, storeCommentRequest)
    }

    override fun updateStoreCommentPraiseCount(userId: String, commentId: String): Result<Int> {
        return storeCommentService.updateStoreCommentPraiseCount(userId, commentId)
    }
}
