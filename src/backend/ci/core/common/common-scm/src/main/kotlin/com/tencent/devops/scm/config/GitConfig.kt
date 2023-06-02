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

package com.tencent.devops.scm.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * Git通用配置
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class GitConfig {

    @Value("\${scm.git.gitlabUrl:}")
    val gitlabUrl: String = ""

    @Value("\${scm.external.gitlab.apiUrl:}")
    val gitlabApiUrl: String = ""

    @Value("\${scm.external.gitlab.gitlabHookUrl:}")
    val gitlabHookUrl: String = ""

    /* git config*/
    @Value("\${scm.git.url:}")
    val gitUrl: String = ""

    @Value("\${scm.git.apiUrl:}")
    val gitApiUrl: String = ""

    @Value("\${scm.git.clientId:}")
    val clientId: String = ""

    @Value("\${scm.git.clientSecret:}")
    val clientSecret: String = ""

    /* oauth认证成功重定向路径 */
    @Value("\${scm.git.redirectUrl:}")
    val redirectUrl: String = ""

    @Value("\${scm.git.gitHookUrl:}")
    val gitHookUrl: String = ""

    @Value("\${scm.git.callbackUrl:}")
    val callbackUrl: String = ""

    @Value("\${scm.git.hookLockToken:}")
    val hookLockToken: String = ""

    /* github config */
    @Value("\${scm.external.github.signSecret:}")
    val signSecret: String = ""

    @Value("\${scm.external.github.clientId:}")
    val githubClientId: String = ""

    @Value("\${scm.external.github.oauthAppClientId:}")
    val oauthAppClientId: String = ""

    @Value("\${scm.external.github.clientSecret:}")
    val githubClientSecret: String = ""

    @Value("\${scm.external.github.oauthAppClientSecret:}")
    val oauthAppClientSecret: String = ""

    @Value("\${scm.external.github.webhookUrl:}")
    val githubWebhookUrl: String = ""

    @Value("\${scm.external.github.callbackUrl:}")
    val githubCallbackUrl: String = ""

    @Value("\${scm.external.github.redirectUrl:}")
    val githubRedirectUrl: String = ""

    @Value("\${scm.external.github.appUrl:}")
    val githubAppUrl: String = ""

    /* tGit config */
    @Value("\${scm.git.tGitUrl:}")
    val tGitUrl: String = ""

    @Value("\${scm.external.tGit.apiUrl:}")
    val tGitApiUrl: String = ""

    @Value("\${scm.git.frontendSampleProjectUrl:}")
    val frontendSampleProjectUrl: String = ""

    @Value("\${scm.external.tGit.tGitHookUrl:}")
    val tGitHookUrl: String = ""

    @Value("\${scm.external.tGit.hookSecret:}")
    val tGitHookSecret: String = ""
}
