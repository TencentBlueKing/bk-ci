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

package com.tencent.devops.scm.services

import com.tencent.devops.scm.pojo.SvnFileInfo
import com.tencent.devops.scm.pojo.enums.SvnFileType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.SVNProperties
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.io.ByteArrayOutputStream

@Service
class SvnService {

    companion object {
        private val logger = LoggerFactory.getLogger(SvnService::class.java)
    }

    fun getFileContent(
        url: String,
        userId: String,
        svnType: String,
        filePath: String,
        reversion: Long,
        credential1: String,
        credential2: String?
    ): String {
        logger.info("get svn file content: $url, $userId, $svnType, $filePath, $reversion")
        val startEpoch = System.currentTimeMillis()
        try {
            val bos = ByteArrayOutputStream()
            val svnUrl = SVNURL.parseURIEncoded(url)
            val repository = SVNRepositoryFactory.create(svnUrl)
            val auth = when (svnType.toUpperCase()) {
                "HTTP" -> SVNPasswordAuthentication.newInstance(
                    credential1,
                    credential2?.toCharArray(),
                    false,
                    svnUrl,
                    false
                )
                "SSH" -> SVNSSHAuthentication.newInstance(
                    userId,
                    credential1.toCharArray(),
                    credential2?.toCharArray(),
                    22,
                    false,
                    svnUrl,
                    false
                )
                else -> throw RuntimeException("unknown svn repo type: ${svnType.toUpperCase()}")
            }
            val basicAuthenticationManager = BasicAuthenticationManager(arrayOf(auth))
            repository.authenticationManager = basicAuthenticationManager
            repository.getFile(filePath.removePrefix("/"), reversion, SVNProperties(), bos)
            return bos.toString()
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get svn file content")
        }
    }

    fun getDirectories(
        url: String,
        userId: String,
        svnType: String,
        svnPath: String?,
        revision: Long,
        credential1: String,
        credential2: String,
        credential3: String?
    ): List<SvnFileInfo> {
        logger.info("get svn dir: [url=$url, userId=$userId, svnType=$svnType]")
        val startEpoch = System.currentTimeMillis()
        try {
            val svnUrl = SVNURL.parseURIEncoded(url)
            val repository = SVNRepositoryFactory.create(svnUrl)
            val auth = when (svnType.toUpperCase()) {
                "HTTP" -> SVNPasswordAuthentication.newInstance(
                    credential1,
                    credential2?.toCharArray(),
                    false,
                    svnUrl,
                    false
                )
                "SSH" -> SVNSSHAuthentication.newInstance(
                    credential1,
                    credential2.toCharArray(),
                    credential3?.toCharArray(),
                    22,
                    false,
                    svnUrl,
                    false
                )
                else -> throw RuntimeException("unknown svn repo type: ${svnType.toUpperCase()}")
            }
            val entries = mutableListOf<SVNDirEntry>()
            val basicAuthenticationManager = BasicAuthenticationManager(arrayOf(auth))
            repository.authenticationManager = basicAuthenticationManager
            repository.getDir(svnPath ?: "", revision, SVNProperties(), entries)

            return entries.map {
                val type = SvnFileType.valueOf(it.kind.toString().toUpperCase())
                val name = it.name
                SvnFileInfo(type, name)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the directories")
        }
    }
}
