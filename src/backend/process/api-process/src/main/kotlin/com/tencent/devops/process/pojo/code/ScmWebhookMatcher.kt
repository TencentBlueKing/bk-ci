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

package com.tencent.devops.process.pojo.code

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.repository.pojo.Repository

interface ScmWebhookMatcher {

    fun isMatch(
        pipelineId: String,
        repository: Repository,
        branchName: String?,
        excludeBranchName: String?,
        includePaths: String?,
        excludePaths: String?,
        includeUsers: String?,
        excludeUsers: String?,
        relativePath: String?,
        eventType: CodeEventType?
    ): Boolean

    fun getUsername(): String

    fun getRevision(): String

    fun getRepoName(): String

    fun getBranchName(): String?

    fun getEventType(): CodeEventType

    fun getCodeType(): CodeType

    fun getHookSourceBranch(): String? = null

    fun getHookTargetBranch(): String? = null

    fun getHookSourceUrl(): String? = null

    fun getHookTargetUrl(): String? = null

    fun getEnv() = emptyMap<String, Any>()

    /**
     * Check if the branch match
     * example:
     * branchName: origin/master
     * ref: refs/heads/origin/master
     */
    fun isBranchMatch(branchName: String, ref: String): Boolean {
        val eventBranch = ref.removePrefix("refs/heads/")
        return branchName == eventBranch
    }

    /**
     * Check if the path match
     * example:
     * fullPath: a/1.txt
     * prefixPath: a/
     */
    fun isPathMatch(fullPath: String, prefixPath: String): Boolean {
        return fullPath.removePrefix("/").startsWith(prefixPath.removePrefix("/"))
    }

    fun getBranch(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    fun getTag(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    fun getMergeRequestId(): Long?
}