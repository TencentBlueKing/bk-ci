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

package com.tencent.devops.stream.trigger.actions.data.context

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.stream.pojo.StreamRepoHookEvent
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred

/**
 * Stream触发过程中需要用到的上下文数据
 * 注：上下文对象涉及消息传递时，需要确保不是确定对象
 * @param requestEventId 保存用来展示的gitRequestEvent的id方便关联一些展示项
 * @param pipeline stream当前触发的流水线信息
 * @param triggerCache 触发器缓存相关
 * @param repoTrigger 是否是远程仓库触发以及相关参数
 * @param changeSet 当前git event涉及的文件变更列表
 * @param mrInfo 当前MR触发的mr信息
 * @param originYaml 当前流水线对应的yaml原文
 * @param parsedYaml 替换完模板之后的yaml
 * @param normalizedYaml 填充完成的yaml，可以用来生成流水线model
 * @param finishData stream在构建结束后的相关逻辑需要的数据
 * @param repoCreatedTime 触发仓库创建时间字符串 如:2017-08-13T07:37:14+0000
 * @param repoCreatorId 触发仓库创建人id， 工蜂侧是数字 id 需要使用时转换为 name
 */
data class StreamTriggerContext(
    var requestEventId: Long? = null,
    var pipeline: StreamTriggerPipeline? = null,
    var triggerCache: TriggerCache? = null,
    var repoTrigger: RepoTrigger? = null,
    var changeSet: Set<String>? = null,
    var mrInfo: StreamMrInfo? = null,
    var defaultBranch: String? = null,
    var repoCreatedTime: String? = null,
    var repoCreatorId: String? = null,
    var originYaml: String? = null,
    var parsedYaml: String? = null,
    var normalizedYaml: String? = null,
    var finishData: BuildFinishData? = null,
    var pipelineAsCodeSettings: PipelineAsCodeSettings? = null,
    // 缓存
    var gitMrReviewInfo: GitMrReviewInfo? = null,
    var gitMrInfo: GitMrInfo? = null,
    var gitDefaultBranchLatestCommitInfo: Pair<String?, GitCommit?>? = null
)

/**
 * 跨Git库触发相关数据
 * @param branch 跨库的触发分支，一般为默认分支
 * @param repoTriggerPipelineList 存储所有配置了该远程仓库的触发流水线信息
 * @param repoTriggerCred 存储访问远程仓库的凭证信息，来自于yaml文件中的配置
 * @param buildUserID repoTriggerCred对应的userID
 */
data class RepoTrigger(
    val branch: String,
    val repoTriggerPipelineList: List<StreamRepoHookEvent>,
    var repoTriggerCred: StreamGitCred? = null,
    var buildUserID: String? = null,
    var triggerGitHttpUrl: String? = null
)

/**
 * Stream mr触发拉取文件时需要的mr信息
 */
data class StreamMrInfo(
    val baseCommit: String?
)

/**
 * 缓存触发器相关
 * @param pipelineFileBranch 本次流水线使用的ci文件的分支
 * @param blobId 文件的blobId
 */
data class TriggerCache(
    val pipelineFileBranch: String,
    val blobId: String
)
