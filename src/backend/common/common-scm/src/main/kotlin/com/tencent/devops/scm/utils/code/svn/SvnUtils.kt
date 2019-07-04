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

package com.tencent.devops.scm.utils.code.svn

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.exception.ScmException
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc.DefaultSVNRepositoryPool
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNWCUtil

object SvnUtils {

    fun getClientManager(svnURL: SVNURL, userName: String, privateKey: String, passphrase: String?): SVNClientManager {
        val options = SVNWCUtil.createDefaultOptions(true)
        val auth = if (isSSHProtocol(svnURL.protocol)) {
            SVNSSHAuthentication.newInstance(
                userName,
                privateKey.toCharArray(),
                passphrase?.toCharArray(),
                22,
                false,
                svnURL,
                false
            )
        } else {
            SVNPasswordAuthentication.newInstance(userName, privateKey.toCharArray(), false, svnURL, false)
        }
        return SVNClientManager.newInstance(options, BasicAuthenticationManager(arrayOf(auth)))
    }

    fun getRepository(url: String, userName: String, privateKey: String, passphrase: String?): SVNRepository {
        val svnURL = SVNURL.parseURIEncoded(url)

        val auth = if (isSSHProtocol(svnURL.protocol)) {
            SVNSSHAuthentication.newInstance(
                userName,
                privateKey.toCharArray(),
                passphrase?.toCharArray(),
                22,
                false,
                svnURL,
                false
            )
        } else {
            SVNPasswordAuthentication.newInstance(userName, privateKey.toCharArray(), false, svnURL, false)
        }

        val basicAuthenticationManager = BasicAuthenticationManager(arrayOf(auth))
        val options =
            DefaultSVNRepositoryPool(basicAuthenticationManager, SVNWCUtil.createDefaultOptions(true), 30 * 1000L, true)
        val repository = SVNRepositoryFactory.create(svnURL, options)

        repository.authenticationManager = basicAuthenticationManager
        return repository
    }

    fun isSSHProtocol(protocol: String?): Boolean {
        if (protocol == "http" ||
            protocol == "https"
        ) {
            return false
        }
        return true
    }

    fun getSVNProjectUrl(url: String): String {
        // 第一个以_proj结尾的
        // 比如
        // svn+ssh://user@svn.xx.com/sodash/maven_hello_world_proj/trunk
        // 返回svn+ssh://user@svn.xx.com/sodash/maven_hello_world_proj
        val split = url.split("/")
        val builder = StringBuilder()
        run lit@{
            split.forEach {
                builder.append(it)
                if (it.endsWith("_proj")) {
                    return@lit
                }
                builder.append("/")
            }
        }
        return builder.toString()
    }

    fun getSvnProjectName(svnUrl: String): String {
        val urlSplitArray = svnUrl.split("tencent.com/")
        if (urlSplitArray.size < 2) {
            throw ScmException("Invalid svn url($svnUrl)", ScmType.CODE_SVN.name)
        }

        val path = urlSplitArray[1]
        val pathArray = path.split("/")
        if (pathArray.size < 2) {
            throw ScmException("Invalid svn url($svnUrl)", ScmType.CODE_SVN.name)
        }

        return if (pathArray.size >= 3 && pathArray[2].endsWith("_proj")) {
            // 如果项目名是三层的，比如ied/ied_kihan_rep/server_proj
            "${pathArray[0]}/${pathArray[1]}/${pathArray[2]}"
        } else {
            "${pathArray[0]}/${pathArray[1]}"
        }
    }

    fun getSvnRepName(url: String): String {
        val split = url.split("/")
        run lit@{
            split.forEachIndexed { index, s ->
                if (s.endsWith("_proj")) {
                    return split[index - 1]
                }
            }
        }
        return ""
    }

    fun getSvnSubPath(url: String): String {
        val split = url.split("/")
        val builder = StringBuilder()
        run lit@{
            split.forEach {
                if (it.endsWith("_proj")) {
                    return@lit
                }
                builder.append(it)
                builder.append("/")
            }
        }

        return url.substring(builder.toString().length)
    }
}