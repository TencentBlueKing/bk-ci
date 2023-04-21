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

package com.tencent.devops.common.webhook.pojo.code

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType

data class WebHookParams(
    val repositoryConfig: RepositoryConfig,
    var branchName: String? = null,
    var excludeBranchName: String? = null,
    var tagName: String? = null,
    var excludeTagName: String? = null,
    var fromBranches: String? = null,
    var pathFilterType: PathFilterType? = PathFilterType.NamePrefixFilter,
    var includePaths: String? = null,
    var excludePaths: String? = null,
    var eventType: CodeEventType? = null,
    var block: Boolean = false,
    var relativePath: String? = null,
    var excludeUsers: String? = "",
    var includeUsers: String? = null,
    var codeType: CodeType = CodeType.GIT,
    var excludeSourceBranchName: String? = null,
    var includeSourceBranchName: String? = null,
    var includeCommitMsg: String? = null,
    var excludeCommitMsg: String? = null,
    var webhookQueue: Boolean = false,
    /**
     * 当代码库有多个域名时,代码库配置url为git.a.com，事件触发的url为git.b.com，但是git.a.com和git.b.com指向同一个仓库
     * **/
    var includeHost: String? = null,
    // code review状态
    var includeCrState: String? = null,
    var includeCrTypes: String? = null,
    // issue事件action
    var includeIssueAction: String? = null,
    // mr事件action
    var includeMrAction: String? = null,
    // note事件action
    var includeNoteComment: String? = null,
    var includeNoteTypes: String? = null,
    // push事件action
    var includePushAction: String? = null,
    var enableThirdFilter: Boolean? = false,
    var thirdUrl: String? = null,
    var thirdSecretToken: String? = null,
    // 插件版本
    var version: String? = null
)
