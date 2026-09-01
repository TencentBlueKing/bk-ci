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

package com.tencent.devops.process.engine.pojo.builds

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 构建历史查询条件参数
 */
@Suppress("LongParameterList")
@Schema(title = "构建历史查询条件参数")
data class BuildHistoryQueryParam(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "代码库别名列表")
    val materialAlias: List<String>? = null,
    @get:Schema(title = "代码库URL")
    val materialUrl: String? = null,
    @get:Schema(title = "代码库分支列表")
    val materialBranch: List<String>? = null,
    @get:Schema(title = "代码库提交ID")
    val materialCommitId: String? = null,
    @get:Schema(title = "代码库提交信息")
    val materialCommitMessage: String? = null,
    @get:Schema(title = "构建状态列表")
    val status: List<BuildStatus>? = null,
    @get:Schema(title = "触发方式列表")
    val trigger: List<StartType>? = null,
    @get:Schema(title = "排队开始时间")
    val queueTimeStartTime: Long? = null,
    @get:Schema(title = "排队结束时间")
    val queueTimeEndTime: Long? = null,
    @get:Schema(title = "开始时间起始")
    val startTimeStartTime: Long? = null,
    @get:Schema(title = "开始时间截止")
    val startTimeEndTime: Long? = null,
    @get:Schema(title = "结束时间起始")
    val endTimeStartTime: Long? = null,
    @get:Schema(title = "结束时间截止")
    val endTimeEndTime: Long? = null,
    @get:Schema(title = "最小总耗时")
    val totalTimeMin: Long? = null,
    @get:Schema(title = "最大总耗时")
    val totalTimeMax: Long? = null,
    @get:Schema(title = "备注")
    val remark: String? = null,
    @get:Schema(title = "构建号起始")
    val buildNoStart: Int? = null,
    @get:Schema(title = "构建号截止")
    val buildNoEnd: Int? = null,
    @get:Schema(title = "构建信息")
    val buildMsg: String? = null,
    @get:Schema(title = "启动用户列表")
    val startUser: List<String>? = null,
    @get:Schema(title = "是否调试")
    val debug: Boolean? = null,
    @get:Schema(title = "触发器别名列表")
    val triggerAlias: List<String>? = null,
    @get:Schema(title = "触发分支列表")
    val triggerBranch: List<String>? = null,
    @get:Schema(title = "触发用户列表")
    val triggerUser: List<String>? = null,
    @get:Schema(title = "触发事件类型列表")
    val triggerEventTypes: List<String>? = null,
    @get:Schema(title = "触发节点HashId列表")
    val triggerNodeHashIds: List<String>? = null
)