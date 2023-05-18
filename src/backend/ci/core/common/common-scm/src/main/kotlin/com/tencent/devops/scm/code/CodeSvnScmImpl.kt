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
import com.tencent.devops.scm.code.svn.api.SVNApi
import com.tencent.devops.scm.config.SVNConfig
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.jmx.JMX
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import org.slf4j.LoggerFactory
import org.tmatesoft.svn.core.SVNAuthenticationException
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNLogEntry
import org.tmatesoft.svn.core.SVNNodeKind
import org.tmatesoft.svn.core.io.SVNRepository

@Suppress("ALL")
class CodeSvnScmImpl constructor(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private val username: String,
    private val privateKey: String,
    private val passphrase: String?,
    private val svnConfig: SVNConfig
) : IScm {

    override fun getLatestRevision(): RevisionInfo {
        val branch = branchName ?: "trunk"
        var success = false
        val svnBean = JMX.getSvnBean()
        try {
            svnBean.latestRevision()
            val repository = getRepository()
            val revision = getLatestRevision(repository)
            val updatedMessage = getCommitMessage(repository, revision)
            success = true
            return RevisionInfo(revision.toString(), updatedMessage, branch, "")
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

    private fun getLatestRevision(repository: SVNRepository): Long {
        try {
            return if (!branchName.isNullOrBlank()) {
                val info: SVNDirEntry? = repository.info(branchName, -1)
                info?.revision ?: repository.latestRevision // 如果因为路径错误导致找不到的话，使用整个仓库的最新版本号
            } else {
                repository.latestRevision
            }
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

    override fun getBranches(search: String?, page: Int, pageSize: Int): List<String> {
        val repository = getRepository()
        val branchNames = ArrayList<String>()
        branchNames.add("trunk")
        branchNames.addAll(getBranchNames(repository))
        return branchNames
    }

    override fun getTags(search: String?): List<String> {
        throw ScmException("SVN not support get tags", ScmType.CODE_SVN.name)
    }

    override fun checkTokenAndPrivateKey() {
        try {
            getLatestRevision()
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

    override fun checkTokenAndUsername() {
        try {
            getLatestRevision()
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

    override fun addWebHook(hookUrl: String) {
        logger.info(
            "[$hookUrl|${svnConfig.apiUrl}|${svnConfig.webhookApiUrl}|${svnConfig.svnHookUrl}] " +
                    "|AddWebHookSVN|repo=$projectName"
        )
        try {
            val hooks = SVNApi.getWebhooks(svnConfig, url)
            val addHooks = if (hooks.isEmpty()) {
                hookUrl
            } else {
                if (hooks.contains(hookUrl)) {
                    logger.info("The hook url is already exist, ignore")
                    return
                }
                logger.info("Get the exist hooks - ($hooks)")

                val result = StringBuilder()
                hooks.forEach {
                    result.append(it).append(",")
                }
                result.append(hookUrl)
                result.toString()
            }
            logger.info("Adding the svn webhooks($addHooks)")
            SVNApi.addWebhooks(svnConfig, username, url, addHooks)
        } catch (ignored: Exception) {
            logger.warn("Fail to add the webhook", ignored)
            throw ScmException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.SVN_CREATE_HOOK_FAIL
                ),
                scmType = ScmType.CODE_SVN.name
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

    override fun lock(repoName: String, applicant: String, subpath: String) {
        logger.info("Start to lock the repo $repoName")
        try {
            SVNApi.lock(repname = repoName, applicant = applicant, subpath = subpath, svnConfig = svnConfig)
        } catch (e: Exception) {
            logger.warn("Fail to lock the repo:$repoName", e)
            throw ScmException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.LOCK_FAIL
                ),
                scmType = ScmType.CODE_SVN.name
            )
        }
    }

    override fun unlock(repoName: String, applicant: String, subpath: String) {
        logger.info("Start to unlock the repo $repoName")
        try {
            SVNApi.unlock(repname = repoName, applicant = applicant, subpath = subpath, svnConfig = svnConfig)
        } catch (e: Exception) {
            logger.warn("Fail to unlock the repo:$repoName", e)
            throw ScmException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.UNLOCK_FAIL
                ),
                scmType = ScmType.CODE_SVN.name
            )
        }
    }

    private fun getBranchNames(repository: SVNRepository): Set<String> {
        try {
            val nodeKind = repository.checkPath("branches", repository.latestRevision)
            return if (nodeKind === SVNNodeKind.DIR) {
                val dirEntries = HashSet<SVNDirEntry>()
                repository.getDir("branches", repository.latestRevision, false, dirEntries)
                dirEntries.filter {
                    it.kind == SVNNodeKind.DIR
                }.map {
                    it.name
                }.toSet()
            } else {
                setOf()
            }
        } catch (e: SVNException) {
            if (e.errorMessage.errorCode.isAuthentication) {
                throw ScmException(
                    message = I18nUtil.getCodeLanMessage(
                        CommonMessageCode.GIT_REPO_PEM_FAIL
                    ),
                    scmType = ScmType.CODE_SVN.name
                )
            } else {
                logger.error("engineering($projectName)failed to get branch", e)
                throw ScmException(
                    message = I18nUtil.getCodeLanMessage(
                        CommonMessageCode.CALL_REPO_ERROR
                    ),
                    scmType = ScmType.CODE_SVN.name
                )
            }
        }
    }

    private fun getRepository(): SVNRepository {

        try {
            return SvnUtils.getRepository(url, username, privateKey, passphrase)
        } catch (e: SVNException) {
            logger.error("engineering($projectName)local repository creation failed", e)
            throw ScmException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.CALL_REPO_ERROR
                ),
                scmType = ScmType.CODE_SVN.name
            )
        }
    }

    private fun getCommitMessage(svnRepository: SVNRepository, revision: Long): String {
        try {
            val collection = svnRepository.log(arrayOf(""), null, revision, revision, true, true)
            val sb = StringBuilder()
            if (!collection.isEmpty()) {
                for (aCollection in collection) {
                    val logEntry = aCollection as SVNLogEntry
                    sb.append("Revision:")
                    sb.append(logEntry.revision)
                    sb.append("\r\n")
                    sb.append(logEntry.message)
                    sb.append("\r\n")
                }
            }
            return sb.toString()
        } catch (e: SVNException) {
            logger.warn("Get the project($projectName})version changelog failed", e)
            throw ScmException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.CALL_REPO_ERROR
                ),
                scmType = ScmType.CODE_SVN.name
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeSvnScmImpl::class.java)
    }
}
