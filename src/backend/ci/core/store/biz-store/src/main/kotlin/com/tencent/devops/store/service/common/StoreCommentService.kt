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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreUserCommentInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

/**
 * store评论业务逻辑类
 *
 * since: 2019-03-26
 */
interface StoreCommentService {

    /**
     * 获取评论信息
     */
    fun getStoreComment(userId: String, commentId: String): Result<StoreCommentInfo?>

    /**
     * 获取评论信息列表
     */
    fun getStoreComments(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?>

    /**
     * 添加评论
     */
    fun addStoreComment(
        userId: String,
        storeId: String,
        storeCode: String,
        storeCommentRequest: StoreCommentRequest,
        storeType: StoreTypeEnum
    ): Result<StoreCommentInfo?>

    /**
     * 更新评论信息
     */
    fun updateStoreComment(userId: String, commentId: String, storeCommentRequest: StoreCommentRequest): Result<Boolean>

    /**
     * 评论点赞/取消点赞
     */
    fun updateStoreCommentPraiseCount(userId: String, commentId: String): Result<Int>

    /**
     * 获取用户评论信息
     */
    fun getStoreUserCommentInfo(userId: String, storeCode: String, storeType: StoreTypeEnum): StoreUserCommentInfo
}
