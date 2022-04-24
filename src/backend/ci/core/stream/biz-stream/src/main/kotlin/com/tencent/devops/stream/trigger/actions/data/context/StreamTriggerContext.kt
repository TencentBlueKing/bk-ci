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

import com.tencent.devops.stream.pojo.StreamRepoHookEvent
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline

/**
 * Stream触发过程中需要用到的上下文数据
 * @param requestEventId 保存用来展示的gitRequestEvent的id方便关联一些展示项
 * @param pipeline stream当前触发的流水线信息
 * @param repoTrigger 是否是远程仓库触发以及相关参数
 * @param changeSet 当前git event涉及的文件变更列表
 * @param originYaml 当前流水线对应的yaml原文
 * @param parsedYaml 替换完模板之后的yaml
 * @param normalizedYaml 填充完成的yaml，可以用来生成流水线model
 * @param finishData stream在构建结束后的相关逻辑需要的数据
 */
data class StreamTriggerContext(
    var requestEventId: Long? = null,
    var pipeline: StreamTriggerPipeline? = null,
    var repoTrigger: RepoTrigger? = null,
    var changeSet: List<String>? = null,
    var defaultBranch: String? = null,
    var originYaml: String? = null,
    var parsedYaml: String? = null,
    var normalizedYaml: String? = null,
    var finishData: BuildFinishData? = null
)

/**
 * 跨Git库触发相关数据
 * @param branch 跨库的触发分支，一般为默认分支
 */
data class RepoTrigger(
    val branch: String,
    val repoTriggerPipelineList: List<StreamRepoHookEvent>
)
