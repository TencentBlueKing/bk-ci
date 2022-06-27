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

package com.tencent.devops.stream.trigger.actions.data

import com.tencent.devops.common.api.enums.ScmType

/**
 * 需要根据各事件源的event去拿的通用数据，随event改变可能会不同
 * @param gitProjectId Git平台项目唯一标识
 * @param scmType 当前事件 git平台唯一标识
 * @param branch 当前event的触发branch
 * @param commit 当前event的commitId
 * @param userId 当前event的触发人
 * @param gitProjectName Git平台项目全称: namespace/name
 * @param eventType 当前事件类型 仅在github需要
 * @param sourceGitProjectId mr触发时的源Git库
 */
data class EventCommonData(
    val gitProjectId: String,
    val scmType: ScmType?,
    val branch: String,
    val commit: EventCommonDataCommit,
    val userId: String,
    val gitProjectName: String?,
    val eventType: String? = null,
    val sourceGitProjectId: String? = null
)

/**
 * 公共数据的commit数据
 * @param commitId commit唯一标识
 * @param commitMsg commit提交信息
 * @param commitAuthorName commit提交作者
 * @param commitTimeStamp commit提交时间点
 */
data class EventCommonDataCommit(
    val commitId: String,
    val commitMsg: String?,
    val commitAuthorName: String?,
    val commitTimeStamp: String?
)
