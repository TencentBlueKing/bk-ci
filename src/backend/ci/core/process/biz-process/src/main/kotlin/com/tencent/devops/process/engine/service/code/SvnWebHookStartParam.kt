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

package com.tencent.devops.process.engine.service.code

import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.process.pojo.code.ScmWebhookStartParams
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_COMMIT_TIME
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_EXCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_EXCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_INCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_RELATIVE_PATH
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_REVERSION
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_USERNAME

class SvnWebHookStartParam(
    private val matcher: SvnWebHookMatcher
) : ScmWebhookStartParams<CodeSVNWebHookTriggerElement> {

    override fun getStartParams(element: CodeSVNWebHookTriggerElement): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        val svnEvent = matcher.event
        startParams[BK_REPO_SVN_WEBHOOK_REVERSION] = matcher.getRevision()
        startParams[BK_REPO_SVN_WEBHOOK_USERNAME] = matcher.getUsername()
        startParams[BK_REPO_SVN_WEBHOOK_COMMIT_TIME] = svnEvent.commitTime ?: 0L
        startParams[BK_REPO_SVN_WEBHOOK_RELATIVE_PATH] = element.relativePath ?: ""
        startParams[BK_REPO_SVN_WEBHOOK_EXCLUDE_PATHS] = element.excludePaths ?: ""
        startParams[BK_REPO_SVN_WEBHOOK_INCLUDE_USERS] = element.includeUsers?.joinToString(",") ?: ""
        startParams[BK_REPO_SVN_WEBHOOK_EXCLUDE_USERS] = element.excludeUsers?.joinToString(",") ?: ""
        return startParams
    }
}