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

package com.tencent.devops.store.pojo.common

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("评论信息")
data class StoreCommentInfo(
    @ApiModelProperty("评论ID", required = true)
    val commentId: String,
    @ApiModelProperty("评论者", required = true)
    val commenter: String,
    @ApiModelProperty("评论内容", required = true)
    val commentContent: String,
    @ApiModelProperty("评论者组织架构信息", required = true)
    val commenterDept: String,
    @ApiModelProperty("评论者头像url地址", required = false)
    val profileUrl: String,
    @ApiModelProperty("点赞个数", required = true)
    val praiseCount: Int,
    @ApiModelProperty("点赞用户列表", required = false)
    val praiseUsers: List<String>?,
    @ApiModelProperty("是否已点赞 true:是，false:否", required = true)
    val praiseFlag: Boolean,
    @ApiModelProperty("评分", required = true)
    val score: Int,
    @ApiModelProperty("评论回复个数", required = true)
    val replyCount: Int,
    @ApiModelProperty("评论创建时间", required = true)
    val commentTime: Long,
    @ApiModelProperty("评论更新时间", required = true)
    val updateTime: Long
)