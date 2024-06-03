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

package com.tencent.devops.scm.code

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.scm.IScm
import com.tencent.devops.scm.code.p4.api.P4Api
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.RevisionInfo
import org.slf4j.LoggerFactory

@SuppressWarnings("TooManyFunctions")
class CodeP4ScmImpl(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private val username: String,
    private val password: String,
    private val event: String?
) : IScm {

    companion object {
        private val logger = LoggerFactory.getLogger(CodeP4ScmImpl::class.java)
    }

    override fun getLatestRevision(): RevisionInfo {
        throw UnsupportedOperationException("p4 unsupported getLatestRevision")
    }

    override fun getBranches(search: String?, page: Int, pageSize: Int): List<String> {
        throw UnsupportedOperationException("p4 unsupported getBranches")
    }

    override fun getTags(search: String?): List<String> {
        throw UnsupportedOperationException("p4 unsupported getTags")
    }

    override fun checkTokenAndPrivateKey() = Unit

    override fun checkTokenAndUsername() {
        try {
            P4Api(
                p4port = url,
                username = username,
                password = password
            ).connection()
        } catch (ignored: Throwable) {
            logger.warn("Fail to login p4", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    CommonMessageCode.P4_USERNAME_PASSWORD_FAIL
                ),
                ScmType.CODE_P4.name
            )
        }
    }

    override fun addWebHook(hookUrl: String) {
        logger.info("add p4 webhook|$url|$hookUrl")
        try {
            P4Api(
                p4port = url,
                username = username,
                password = password
            ).addWebHook(
                hookUrl = hookUrl,
                event = event
            )
        } catch (ignored: Throwable) {
            logger.warn("Fail to add p4 triggers", ignored)
            throw ScmException(
                ignored.message ?: I18nUtil.getCodeLanMessage(
                    CommonMessageCode.P4_USERNAME_PASSWORD_FAIL
                ),
                ScmType.CODE_P4.name
            )
        }
    }

    override fun addCommitCheck(
        commitId: String,
        state: String,
        targetUrl: String,
        context: String,
        description: String,
        block: Boolean,
        targetBranch: List<String>?
    ) = Unit

    override fun addMRComment(mrId: Long, comment: String) = Unit

    override fun lock(repoName: String, applicant: String, subpath: String) = Unit

    override fun unlock(repoName: String, applicant: String, subpath: String) = Unit
}
