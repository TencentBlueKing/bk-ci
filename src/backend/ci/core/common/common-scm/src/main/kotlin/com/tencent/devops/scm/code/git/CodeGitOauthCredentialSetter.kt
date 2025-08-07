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

package com.tencent.devops.scm.code.git

import com.tencent.devops.scm.code.git.api.GitCredentialSetter
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.net.URL

class CodeGitOauthCredentialSetter constructor(private val token: String) : GitCredentialSetter {
    override fun setGitCredential(command: TransportCommand<*, *>) {
        command.setCredentialsProvider(UsernamePasswordCredentialsProvider("oauth2", token))
    }

    override fun getCredentialUrl(url: String): String {
        try {
            val u = URL(url)
            val host = if (u.host.endsWith("/")) {
                u.host.removeSuffix("/")
            } else {
                u.host
            }

            val path = if (u.path.startsWith("/")) {
                u.path.removePrefix("/")
            } else {
                u.path
            }
            return "http://oauth2:$token@$host/$path"
        } catch (t: Throwable) {
            logger.warn("Fail to get the oauth credential url for $url", t)
        }
        return url
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitOauthCredentialSetter::class.java)
    }
}
