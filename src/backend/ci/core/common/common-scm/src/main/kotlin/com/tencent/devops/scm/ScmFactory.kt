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

package com.tencent.devops.scm

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.scm.code.CodeGitScmImpl
import com.tencent.devops.scm.code.CodeGitlabScmImpl
import com.tencent.devops.scm.code.CodeP4ScmImpl
import com.tencent.devops.scm.code.CodeSvnScmImpl
import com.tencent.devops.scm.code.CodeTGitScmImpl
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.config.SVNConfig
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.exception.ScmException

object ScmFactory {

    @Suppress("ALL")
    fun getScm(
        projectName: String,
        url: String,
        type: ScmType,
        branchName: String?,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?,
        event: String? = null
    ): IScm {
        return when (type) {
            ScmType.CODE_SVN -> {

                if (userName == null) {
                    throw TaskExecuteException(
                        errorCode = ErrorCode.USER_INPUT_INVAILD,
                        errorType = ErrorType.USER,
                        errorMsg = "The svn username is null"
                    )
                }

                if (privateKey == null) {
                    throw TaskExecuteException(
                        errorCode = ErrorCode.USER_INPUT_INVAILD,
                        errorType = ErrorType.USER,
                        errorMsg = "The svn private key is null"
                    )
                }
                val svnConfig = SpringContextUtil.getBean(SVNConfig::class.java)
                CodeSvnScmImpl(
                    projectName = projectName,
                    branchName = branchName,
                    url = url,
                    username = userName,
                    privateKey = privateKey,
                    passphrase = passPhrase,
                    svnConfig = svnConfig
                )
            }
            ScmType.CODE_GIT -> {
                if (token == null) {
                    throw TaskExecuteException(
                        errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                        errorType = ErrorType.USER,
                        errorMsg = "The git token is null"
                    )
                }
                if (event != null && CodeGitWebhookEvent.find(event) == null) {
                    throw TaskExecuteException(
                        errorCode = ErrorCode.USER_INPUT_INVAILD,
                        errorType = ErrorType.USER,
                        errorMsg = "The git event is invalid"
                    )
                }
                val gitConfig = SpringContextUtil.getBean(GitConfig::class.java)
                CodeGitScmImpl(
                    projectName = projectName,
                    branchName = branchName,
                    url = url,
                    privateKey = privateKey,
                    passPhrase = passPhrase,
                    token = token,
                    gitConfig = gitConfig,
                    event = event
                )
            }
            ScmType.CODE_TGIT -> {
                if (token == null) {
                    throw TaskExecuteException(
                        errorCode = ErrorCode.USER_INPUT_INVAILD,
                        errorType = ErrorType.USER,
                        errorMsg = "The git token is null"
                    )
                }
                if (event != null && CodeGitWebhookEvent.find(event) == null) {
                    throw TaskExecuteException(
                        errorCode = ErrorCode.USER_INPUT_INVAILD,
                        errorType = ErrorType.USER,
                        errorMsg = "The git event is invalid"
                    )
                }
                val gitConfig = SpringContextUtil.getBean(GitConfig::class.java)
                CodeTGitScmImpl(
                    projectName = projectName,
                    branchName = branchName,
                    url = url,
                    privateKey = privateKey,
                    passPhrase = passPhrase,
                    token = token,
                    gitConfig = gitConfig,
                    event = event
                )
            }
            ScmType.CODE_GITLAB -> {
                if (token == null) {
                    throw TaskExecuteException(
                        errorCode = ErrorCode.USER_INPUT_INVAILD,
                        errorType = ErrorType.USER,
                        errorMsg = "The gitlab access token is null"
                    )
                }
                val gitConfig = SpringContextUtil.getBean(GitConfig::class.java)
                CodeGitlabScmImpl(
                    projectName = projectName,
                    branchName = branchName,
                    url = url,
                    privateKey = privateKey,
                    passPhrase = passPhrase,
                    token = token,
                    gitConfig = gitConfig,
                    event = event
                )
            }
            ScmType.CODE_P4 -> {
                if (passPhrase == null) {
                    throw ScmException(
                        MessageUtil.getMessageByLocale(
                            messageCode = CommonMessageCode.PWD_EMPTY,
                            language = DEFAULT_LOCALE_LANGUAGE
                        ),
                        ScmType.CODE_P4.name
                    )
                }
                if (userName == null) {
                    throw ScmException(
                        MessageUtil.getMessageByLocale(
                            messageCode = CommonMessageCode.USER_NAME_EMPTY,
                            language = DEFAULT_LOCALE_LANGUAGE
                        ),
                        ScmType.CODE_P4.name
                    )
                }
                CodeP4ScmImpl(
                    projectName = projectName,
                    branchName = branchName,
                    url = url,
                    username = userName,
                    password = passPhrase,
                    event = event
                )
            }
            else -> throw TaskExecuteException(
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorType = ErrorType.USER,
                errorMsg = "Unknown repo($type)"
            )
        }
    }
}
