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

package com.tencent.devops.stream.trigger.actions.data

import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.TriggerReviewSetting

/**
 * Stream触发时需要的配置信息
 * @param enableCi 是否开启Ci
 * @param buildPushedBranches 是否开启push触发
 * @param buildPushedPullRequest 是否开启合并请求触发
 * @param enableUser stream的开启人，一般用其oauth作权限代持
 * @param gitHttpUrl 项目的http/https链接：http://xxxx.git
 * @param projectCode 蓝盾项目id
 * @param enableCommitCheck 当前项目是否发送commit check
 * @param enableMrBlock 合并请求的commit check是否锁定不让合并
 * @param name 项目名称
 * @param homepage Git项目主页
 * @param triggerReviewSetting pr、mr触发时的权限校验
 */
data class StreamTriggerSetting(
    val enableCi: Boolean,
    val buildPushedBranches: Boolean,
    val buildPushedPullRequest: Boolean,
    val enableUser: String,
    var gitHttpUrl: String,
    val projectCode: String?,
    val enableCommitCheck: Boolean,
    val enableMrBlock: Boolean,
    val name: String,
    val enableMrComment: Boolean,
    val homepage: String,
    val triggerReviewSetting: TriggerReviewSetting
) {
    constructor(projectSetting: StreamBasicSetting) : this(
        enableCi = projectSetting.enableCi,
        buildPushedBranches = projectSetting.buildPushedBranches,
        buildPushedPullRequest = projectSetting.buildPushedPullRequest,
        enableUser = projectSetting.enableUserId,
        gitHttpUrl = projectSetting.gitHttpUrl,
        projectCode = projectSetting.projectCode,
        enableCommitCheck = projectSetting.enableCommitCheck,
        enableMrBlock = projectSetting.enableMrBlock,
        name = projectSetting.name,
        enableMrComment = projectSetting.enableMrComment,
        homepage = projectSetting.homepage,
        triggerReviewSetting = projectSetting.triggerReviewSetting
    )
}
