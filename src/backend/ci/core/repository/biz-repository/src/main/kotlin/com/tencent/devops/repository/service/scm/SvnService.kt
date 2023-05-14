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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.scm.code.svn.ISvnService
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.jmx.JMX
import com.tencent.devops.scm.pojo.SvnFileInfo
import com.tencent.devops.scm.pojo.SvnRevisionInfo
import com.tencent.devops.scm.pojo.enums.SvnFileType
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.tmatesoft.svn.core.SVNAuthenticationException
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNLogEntry
import org.tmatesoft.svn.core.SVNProperties
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.io.SVNRepositoryFactory

@Service
@Suppress("ALL")
class SvnService : ISvnService {

    companion object {
        private val logger = LoggerFactory.getLogger(SvnService::class.java)
    }

    override fun getFileContent(
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
                else -> throw IllegalArgumentException("unknown svn repo type: ${svnType.toUpperCase()}")
            }
            val basicAuthenticationManager = BasicAuthenticationManager(arrayOf(auth))
            repository.authenticationManager = basicAuthenticationManager
            repository.getFile(filePath.removePrefix("/"), reversion, SVNProperties(), bos)
            return bos.toString()
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get svn file content")
        }
    }

    override fun getDirectories(
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
                else -> throw IllegalArgumentException("unknown svn repo type: ${svnType.toUpperCase()}")
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
        } catch (e: Exception) {
            logger.error("getDirectories error, msg:$e")
            throw e
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the directories")
        }
    }

    override fun getSvnRevisionList(
        url: String,
        username: String,
        privateKey: String,
        passphrase: String?,
        branchName: String?,
        currentVersion: String?
    ): Pair<Long, List<SvnRevisionInfo>> {
        val branch = branchName ?: "trunk"
        var success = false
        val svnBean = JMX.getSvnBean()
        try {
            svnBean.latestRevision()
            val repository = getRepository(
                url = url,
                username = username,
                privateKey = privateKey,
                passphrase = passphrase
            )
            val revision = getLatestRevision(
                branchName = branch,
                repository = repository
            )
            val result = getRevisionInfoList(repository, getCurrentRevision(currentVersion, revision), revision, branch)
            success = true
            return Pair(revision, result)
        } catch (e: SVNAuthenticationException) {
            if ((!e.message.isNullOrBlank()) && e.message!!.contains("timeout")) {
                svnBean.latestRevisionTimeout()
            }
            throw e
        } catch (e: SVNException) {
            if ((!e.message.isNullOrBlank()) && e.message!!.contains("There was a problem while connecting")) {
                svnBean.latestRevisionTimeout()
            }
            throw e
        } finally {
            if (!success) {
                svnBean.latestRevisionFail()
            }
        }
    }

    private fun getRepository(
        url: String,
        username: String,
        privateKey: String,
        passphrase: String?
    ): SVNRepository {

        try {
            return SvnUtils.getRepository(url, username, privateKey, passphrase)
        } catch (e: SVNException) {
            logger.error("project($url)Failed to create local warehouse", e)
            throw ScmException(
                message = I18nUtil.getCodeLanMessage(CommonMessageCode.CALL_REPO_ERROR),
                scmType = ScmType.CODE_SVN.name
            )
        }
    }

    private fun getLatestRevision(
        branchName: String,
        repository: SVNRepository
    ): Long {
        try {
            val info: SVNDirEntry? = repository.info(branchName, -1)
            return info?.revision ?: repository.latestRevision // 如果因为路径错误导致找不到的话，使用整个仓库的最新版本号
        } catch (ignored: Throwable) {
            logger.warn("Fail to check the svn latest revision", ignored)
            throw ScmException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.SVN_SECRET_OR_PATH_ERROR
                ),
                scmType = ScmType.CODE_SVN.name
            )
        }
    }

    /**
     * 如果currentRevision为null，则使用最新的revision
     */
    private fun getCurrentRevision(
        svnRevision: String?,
        revision: Long
    ): Long {
        if (svnRevision == null) {
            return revision
        }
        return svnRevision.toLong()
    }

    private fun getRevisionInfoList(
        svnRepository: SVNRepository,
        currentVersion: Long,
        revision: Long,
        branchName: String
    ): List<SvnRevisionInfo> {
        try {
            val collection = svnRepository.log(arrayOf(""), null, currentVersion, revision, true, true)
            val result = mutableListOf<SvnRevisionInfo>()
            if (!collection.isEmpty()) {
                for (aCollection in collection) {
                    val logEntry = aCollection as SVNLogEntry
                    if (currentVersion != revision && currentVersion == logEntry.revision) {
                        logger.info("this revision is builded, ignoer this one")
                        continue
                    }
                    val revisionInfo = SvnRevisionInfo(
                        revision = logEntry.revision.toString(),
                        branchName = branchName,
                        authorName = logEntry.author,
                        commitTime = logEntry.date.time,
                        paths = logEntry.changedPaths.values.map { it.path }
                    )
                    result.add(revisionInfo)
                }
            }
            return result
        } catch (e: SVNException) {
            throw ScmException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.CALL_REPO_ERROR
                ),
                scmType = ScmType.CODE_SVN.name
            )
        }
    }
}
