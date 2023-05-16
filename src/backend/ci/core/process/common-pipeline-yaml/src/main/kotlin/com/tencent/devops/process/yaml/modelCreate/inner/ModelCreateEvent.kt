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

package com.tencent.devops.process.yaml.modelCreate.inner

import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind

/**
 * 将yaml转为蓝盾model需要的参数
 * @param userId 用户ID
 * @param projectCode 蓝盾项目ID
 * @param pipelineInfo 蓝盾流水线相关信息，目前用于创建红线
 * @param gitData git相关数据
 * @param streamData stream微服务特有的函数需要的数据
 * @param changeSet 文件变更列表，主要用于实现 if-modify
 * @param jobTemplateAcrossInfo job级别的跨模板凭证需要的信息
 * @param checkIfModify 是否检查if-modify
 * @param elementInstallUserId 插件安装用户
 */
data class ModelCreateEvent(
    val userId: String,
    val projectCode: String,
    val elementInstallUserId: String,
    val pipelineInfo: PipelineInfo? = null,
    val gitData: GitData? = null,
    val streamData: StreamData? = null,
    val changeSet: Set<String>? = null,
    val jobTemplateAcrossInfo: Map<String, BuildTemplateAcrossInfo>? = null,
    val preCIData: PreCIData? = null,
    val checkIfModify: Boolean = false
)

/**
 * 蓝盾流水线相关数据
 * @param pipelineId 流水线id
 */
data class PipelineInfo(
    val pipelineId: String
)

/**
 * 来自git相关的数据
 * @param repositoryUrl git仓库的clone地址
 * @param gitProjectId git项目id
 * @param commitId 当前触发的最新提交
 * @param branch 当前触发的分支
 */
data class GitData(
    val repositoryUrl: String,
    val gitProjectId: Long,
    val commitId: String,
    val branch: String
)

/**
 * 来自stream服务的相关数据
 * 注：其他微服务需要可以将其抽象为接口根据泛型使用
 * @param gitProjectId git项目id，stream绑定git
 * @param enableUserId 开启stream项目的用户
 * @param requestEventId stream request事件id
 * @param objectKind stream 中git的触发方式
 */
data class StreamData(
    val gitProjectId: Long,
    val enableUserId: String,
    val requestEventId: Long,
    val objectKind: StreamObjectKind
)

/**
 * preci
 */
data class PreCIData(
    val agentId: String,
    val workspace: String,
    val userId: String,
    val projectId: String = "_$userId",
    val extraParam: ExtraParam?
)

data class ExtraParam(
    val codeccScanPath: String? = null,
    val incrementFileList: List<String>? = null,
    val ideVersion: String? = null,
    val pluginVersion: String? = null
)
