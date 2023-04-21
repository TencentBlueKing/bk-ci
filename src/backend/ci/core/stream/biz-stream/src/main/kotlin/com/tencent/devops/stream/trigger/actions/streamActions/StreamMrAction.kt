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

package com.tencent.devops.stream.trigger.actions.streamActions

import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.git.pojo.StreamGitMrInfo
import com.tencent.devops.stream.trigger.pojo.MrCommentBody
import org.slf4j.LoggerFactory

/**
 * 需要各Git端统一提供的一些mr参数
 */
interface StreamMrAction : BaseAction {

    companion object {
        val logger = LoggerFactory.getLogger(StreamMrAction::class.java)
    }

    // 负责跳转的某个git下的mr的一个Id
    val mrIId: String

    /**
     * 判断是否是fork仓库触发的
     */
    fun checkMrForkAction(): Boolean

    /**
     * 为合并请求添加评论
     */
    fun addMrComment(
        body: MrCommentBody
    )

    /**
     * 检查mr触发时的权限校验(检查白名单)
     */
    fun checkMrForkReview(): Boolean

    /**
     * 获取 mr/pr 页面上的id
     */
    fun getMrId(): Long

    /**
     * 获取当前 mr/pr 上所配置的 reviewers
     */
    fun getMrReviewers(): List<String>

    /**
     * 尝试从已有缓存中拿数据，拿不到再调接口
     */
    fun tryGetMrInfoFromCache(): StreamGitMrInfo? = null

    override fun forkMrNeedReviewers(): List<String> {
        return if (!checkMrForkReview()) {
            var page = 1
            val reviewers = mutableListOf<String>()
            // 首先添加reviewer为审核人
            reviewers.addAll(getMrReviewers())
            // 再添加master、owner权限的为审核人
            while (true) {
                val res = api.getProjectMember(
                    cred = this.data.context.repoTrigger?.repoTriggerCred ?: getGitCred(),
                    gitProjectId = this.data.eventCommon.gitProjectId,
                    page = page,
                    pageSize = 100
                )
                reviewers.addAll(res.filter { it.accessLevel >= 40 }.map { it.userId })
                if (res.size != 100) {
                    break
                }
                page += 1
            }
            logger.warn(
                "check mr fork review false, need review, gitProjectId: ${this.data.getGitProjectId()}|" +
                    "eventId: ${this.data.context.requestEventId}| " +
                    "reviewers: ${reviewers.ifEmpty { data.setting.enableUser }}"
            )
            if (reviewers.isEmpty()) {
                // 兜底用ci开启人做审核人
                listOf(data.setting.enableUser)
            } else reviewers
        } else emptyList()
    }

    override fun checkIfModify() = true
}
