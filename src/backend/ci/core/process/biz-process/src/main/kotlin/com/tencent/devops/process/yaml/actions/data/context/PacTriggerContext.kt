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

package com.tencent.devops.process.yaml.actions.data.context

import com.tencent.devops.process.yaml.actions.data.YamlTriggerPipeline
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo

/**
 * pac触发过程中需要用到的上下文数据
 * 注：上下文对象涉及消息传递时，需要确保不是确定对象
 *
 * @param hookRequestId webhook触发的requestId,对应代码库T_REPOSITORY_WEBHOOK_REQUEST的requestId
 * @param pipeline 触发流水线
 * @param yamlFile yaml文件信息
 * @param eventId 触发事件ID,对应T_PIPELINE_TRIGGER_EVENT的eventId
 */
data class PacTriggerContext(
    var hookRequestId: Long? = null,
    var eventId: Long? = null,
    var pipeline: YamlTriggerPipeline? = null,
    var yamlFile: YamlPathListEntry? = null,
    // 仓库homePage
    var homePage: String? = null,
    // 默认分支
    var defaultBranch: String? = null,
    // 缓存
    var changeSet: Set<String>? = null,
    // 删除ci文件列表
    var deleteCiSet: Set<String>? = null,
    var gitMrReviewInfo: GitMrReviewInfo? = null,
    var gitMrInfo: GitMrInfo? = null
)
