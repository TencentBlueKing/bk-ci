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

package com.tencent.devops.stream.trigger.parsers

import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import org.slf4j.LoggerFactory

object CheckStreamSetting {

    private val logger = LoggerFactory.getLogger(CheckStreamSetting::class.java)

    @Throws(TriggerException::class)
    fun checkGitProjectConf(
        gitRequestEventForHandel: GitRequestEventForHandle,
        event: GitEvent,
        gitProjectSetting: GitCIBasicSetting
    ): Boolean {
        if (!gitProjectSetting.enableCi) {
            logger.warn(
                "git ci is disabled, git project id: ${gitRequestEventForHandel.gitProjectId}, " +
                        "name: ${gitProjectSetting.name}"
            )
            TriggerException.triggerError(
                request = gitRequestEventForHandel,
                reason = TriggerReason.CI_DISABLED
            )
        }
        when (event) {
            is GitPushEvent -> {
                if (!gitProjectSetting.buildPushedBranches) {
                    logger.warn(
                        "git ci conf buildPushedBranches is false, git project id: " +
                                "${gitRequestEventForHandel.gitProjectId}, name: ${gitProjectSetting.name}"
                    )
                    TriggerException.triggerError(
                        request = gitRequestEventForHandel,
                        reason = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED
                    )
                }
            }
            is GitTagPushEvent -> {
                if (!gitProjectSetting.buildPushedBranches) {
                    logger.warn(
                        "git ci conf buildPushedBranches is false, git project id: " +
                                "${gitRequestEventForHandel.gitProjectId}, name: ${gitProjectSetting.name}"
                    )
                    TriggerException.triggerError(
                        request = gitRequestEventForHandel,
                        reason = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED
                    )
                }
            }
            is GitMergeRequestEvent -> {
                if (!gitProjectSetting.buildPushedPullRequest) {
                    logger.warn(
                        "git ci conf buildMergePullRequest is false, git project id: " +
                                "${gitRequestEventForHandel.gitProjectId}, name: ${gitProjectSetting.name}"
                    )
                    TriggerException.triggerError(
                        request = gitRequestEventForHandel,
                        reason = TriggerReason.BUILD_MERGE_REQUEST_DISABLED
                    )
                }
            }
        }
        return true
    }
}
