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

package com.tencent.devops.plugin.worker.task.scm.util

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ScmException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.worker.common.CommonEnv
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_SVN_DIRECTORY_ERROR
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.utils.CredentialUtils
import org.slf4j.LoggerFactory
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNWCClient
import org.tmatesoft.svn.core.wc2.SvnTarget
import java.io.File
import java.sql.DriverManager

@Suppress("ALL")
object SvnUtil {
    private val logger = LoggerFactory.getLogger(SvnUtil::class.java)

    /**
     * 删除svn锁并执行clean up
     */
    fun deleteWcLockAndCleanup(client: SVNWCClient, workspace: File) {
        deleteWcLock(workspace)
        client.doCleanup(workspace)
    }

    /**
     * 获取代码库下目录列表
     */
    fun getDirectories(repo: CodeSvnRepository): List<String> {
        val rootDirName = "/tmp/${ShaUtils.sha1(repo.url.toByteArray())}/"
        val rootDir = File(rootDirName)

        try {
            val svnUrl = SVNURL.parseURIEncoded(repo.url)
            val pair = CredentialUtils.getCredentialWithType(repo.credentialId)
            val svnCredential = genSvnCredential(repo, pair.first, pair.second)

            val manager = SvnUtils.getClientManager(
                svnUrl,
                svnCredential.username,
                svnCredential.password,
                svnCredential.passphrase
            )
            val client = manager.updateClient

            val checkout = client.operationsFactory.createCheckout()
            checkout.isUpdateLocksOnDemand = client.isUpdateLocksOnDemand
            checkout.source = SvnTarget.fromURL(svnUrl, SVNRevision.HEAD)
            checkout.depth = SVNDepth.IMMEDIATES
            checkout.revision = SVNRevision.HEAD
            checkout.isAllowUnversionedObstructions = false
            checkout.setSingleTarget(SvnTarget.fromFile(rootDir))
            checkout.externalsHandler = SvnCodec.externalsHandler(client.externalsHandler)
            checkout.run()

            val directories = mutableListOf<String>()
            rootDir.listFiles()?.forEach {
                if (it.isDirectory) {
                    directories.add(it.name)
                }
            }
            return directories
        } catch (t: Throwable) {
            logger.error("SvnUtil get directories error.", t)
            throw ScmException(
                MessageUtil.getMessageByLocale(
                    messageCode = GET_SVN_DIRECTORY_ERROR,
                    language = AgentEnv.getLocaleLanguage()
                ),
                ScmType.CODE_SVN.name
            )
        } finally {
            rootDir.deleteRecursively()
        }
    }

    fun genSvnCredential(
        repository: CodeSvnRepository,
        credentials: List<String>,
        credentialType: CredentialType
    ): SvnCredential {
        return if (repository.svnType == CodeSvnRepository.SVN_TYPE_HTTP) {
            val svnCredential = getSvnHttpCredential(credentialType, credentials, repository)
            CommonEnv.addSvnHttpCredential(svnCredential.username, svnCredential.password)
            svnCredential
        } else {
            val svnCredential = getSshCredential(repository, credentials)
            SSHAgentUtils(svnCredential.password, svnCredential.passphrase).addIdentity()
            svnCredential
        }
    }

    private fun getSshCredential(repository: CodeSvnRepository, credentials: List<String>): SvnCredential {
        val privateKey = credentials[0]
        val passPhrase = if (credentials.size > 1) {
            val p = credentials[1]
            if (p.isEmpty()) {
                null
            } else {
                p
            }
        } else {
            null
        }
        return SvnCredential(repository.userName, privateKey, passPhrase)
    }

    private fun getSvnHttpCredential(
        credentialType: CredentialType,
        credentials: List<String>,
        repository: CodeSvnRepository
    ): SvnCredential {
        return if (credentialType == CredentialType.USERNAME_PASSWORD) {
            getSvnCredential(repository, credentials, credentialType)
        } else {
            SvnCredential(repository.userName, credentials[0], null)
        }
    }

    data class SvnCredential(
        val username: String,
        val password: String, // password or private key
        val passphrase: String? // passphrase for ssh private key
    )

    /**
     * svn锁住后需要删除LOCK, WC_LOCK, WORK_QUEUE中的记录
     */
    private fun deleteWcLock(workspace: File) {
        val wcDbFile = File(workspace, ".svn/wc.db")
        if (!wcDbFile.exists()) {
            return
        }

        val wcDbFilePath = wcDbFile.absolutePath
        val connection = DriverManager.getConnection("jdbc:sqlite:$wcDbFilePath")
        connection.use {
            connection.autoCommit = true
            val stmt = connection.createStatement()
            stmt.use {
                stmt.execute("delete from LOCK")
                stmt.execute("delete from WC_LOCK")
                stmt.execute("delete from WORK_QUEUE")
            }
        }
    }

    fun getSvnCredential(
        repository: CodeSvnRepository,
        credentials: List<String>,
        credentialType: CredentialType
    ): SvnCredential {
        if (repository.svnType == CodeSvnRepository.SVN_TYPE_HTTP) {
            // 兼容老的数据，老的数据是用的是password, 新的是username_password
            return if (credentialType == CredentialType.USERNAME_PASSWORD) {
                if (credentials.size <= 1) {
                    logger.warn("Fail to get the username($credentials) of the svn repo $repository")
                    SvnCredential(username = repository.userName, password = credentials[0], passphrase = null)
                } else {
                    SvnCredential(username = credentials[0], password = credentials[1], passphrase = null)
                }
            } else {
                SvnCredential(username = repository.userName, password = credentials[0], passphrase = null)
            }
        } else {
            val privateKey = credentials[0]
            val passPhrase = if (credentials.size > 1) {
                val p = credentials[1]
                if (p.isEmpty()) {
                    null
                } else {
                    p
                }
            } else {
                null
            }
            return SvnCredential(username = repository.userName, password = privateKey, passphrase = passPhrase)
        }
    }
}
