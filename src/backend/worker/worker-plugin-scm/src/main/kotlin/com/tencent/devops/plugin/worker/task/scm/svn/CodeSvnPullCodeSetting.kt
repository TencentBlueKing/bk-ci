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

package com.tencent.devops.plugin.worker.task.scm.svn

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.SVNVersion
import com.tencent.devops.common.pipeline.enums.SvnDepth
import com.tencent.devops.plugin.worker.task.scm.IPullCodeSetting
import com.tencent.devops.plugin.worker.task.scm.util.SvnUtil
import com.tencent.devops.plugin.worker.task.scm.util.SvnUtil.SvnCredential
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_ALIASNAME
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_BRANCHNAME
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_URL
import com.tencent.devops.repository.pojo.CodeSvnRepository
import org.slf4j.LoggerFactory
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNRevision
import java.io.File

open class CodeSvnPullCodeSetting constructor(
    override val pipelineId: String,
    override val buildId: String,
    override val repositoryConfig: RepositoryConfig,
    override val branchName: String?,
    override val revision: String?,
    override val strategy: CodePullStrategy?,
    override val workspace: File,
    override val path: String?,
    override val enableSubmodule: Boolean,
    override val taskParams: Map<String, String>,
    private val svnDepth: SvnDepth,
    private val svnPath: String?,
    private val specifyRevision: String?,
    private val svnVersion: SVNVersion?
) : IPullCodeSetting {

    override fun getRemoteBranch(): String {
        return ""
    }

    override fun pullCode(): Map<String, String>? {

        logger.info("Start to pull the svn code")

        val repo = getRepository() as CodeSvnRepository

        val projectName = repo.projectName
        val url = getUrl(repo)
        val svnUrl = SVNURL.parseURIEncoded(url)

        val credentialsWithType = getCredentialWithType(repo.credentialId)

        val credentials = credentialsWithType.first

        val credentialType = credentialsWithType.second

        val svnCredential = SvnUtil.genSvnCredential(repo, credentials, credentialType)

        val updateStrategy = strategy ?: CodePullStrategy.INCREMENT_UPDATE

        val targetRevision = svnRevision()

        val task = when (updateStrategy) {
            CodePullStrategy.INCREMENT_UPDATE -> svnUpdateTask(
                svnUrl = svnUrl,
                projectName = projectName,
                svnCredential = svnCredential,
                targetRevision = targetRevision,
                updateStrategy = updateStrategy
            )
            CodePullStrategy.REVERT_UPDATE -> revertUpdateTask(
                svnUrl = svnUrl,
                projectName = projectName,
                svnCredential = svnCredential,
                targetRevision = targetRevision,
                updateStrategy = updateStrategy
            )
            CodePullStrategy.FRESH_CHECKOUT -> freshCheckoutTask(
                svnUrl = svnUrl,
                projectName = projectName,
                svnCredential = svnCredential,
                targetRevision = targetRevision,
                updateStrategy = updateStrategy
            )
        }
        val env = mutableMapOf<String, String>()

        env["$PIPELINE_MATERIAL_URL.${repositoryConfig.getRepositoryId()}"] = url
        env["$PIPELINE_MATERIAL_BRANCHNAME.${repositoryConfig.getRepositoryId()}"] = getRemoteBranch()
        env["$PIPELINE_MATERIAL_ALIASNAME.${repositoryConfig.getRepositoryId()}"] = repo.aliasName
        val performEnv = task.perform()
        if (null != performEnv) {
            env.putAll(performEnv)
        }
        return env
    }

    private fun freshCheckoutTask(
        svnUrl: SVNURL,
        projectName: String,
        svnCredential: SvnCredential,
        targetRevision: SVNRevision,
        updateStrategy: CodePullStrategy
    ): FreshCheckoutTask {
        return FreshCheckoutTask(
            svnUrl = svnUrl,
            projectName = projectName,
            username = svnCredential.username,
            privateKey = svnCredential.password,
            passPhrase = svnCredential.passphrase,
            revision = targetRevision,
            workspace = getCodeSourceDir(path),
            strategy = updateStrategy,
            update = true,
            enableSubmodule = enableSubmodule,
            svnDepth = SVNDepth.fromString(svnDepth.name),
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig,
            svnVersion = svnVersion
        )
    }

    private fun revertUpdateTask(
        svnUrl: SVNURL,
        projectName: String,
        svnCredential: SvnCredential,
        targetRevision: SVNRevision,
        updateStrategy: CodePullStrategy
    ): RevertUpdateTask {
        return RevertUpdateTask(
            svnUrl = svnUrl,
            projectName = projectName,
            username = svnCredential.username,
            privateKey = svnCredential.password,
            passPhrase = svnCredential.passphrase,
            revision = targetRevision,
            workspace = getCodeSourceDir(path),
            strategy = updateStrategy,
            update = true,
            enableSubmodule = enableSubmodule,
            svnDepth = SVNDepth.fromString(svnDepth.name),
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig,
            svnVersion = svnVersion
        )
    }

    private fun svnUpdateTask(
        svnUrl: SVNURL,
        projectName: String,
        svnCredential: SvnCredential,
        targetRevision: SVNRevision,
        updateStrategy: CodePullStrategy
    ): SvnUpdateTask {
        return SvnUpdateTask(
            svnUrl = svnUrl,
            projectName = projectName,
            username = svnCredential.username,
            privateKey = svnCredential.password,
            passPhrase = svnCredential.passphrase,
            revision = targetRevision,
            workspace = getCodeSourceDir(path),
            strategy = updateStrategy,
            update = true,
            enableSubmodule = enableSubmodule,
            svnDepth = SVNDepth.fromString(svnDepth.name),
            pipelineId = pipelineId,
            buildId = buildId,
            repositoryConfig = repositoryConfig,
            svnVersion = svnVersion
        )
    }

    private fun getUrl(repo: CodeSvnRepository): String {
        return if (svnPath.isNullOrBlank()) {
            repo.url
        } else {
            if (repo.url.endsWith("/")) {
                if (svnPath!!.startsWith("/")) {
                    repo.url + svnPath
                } else {
                    repo.url + "/" + svnPath
                }
            } else {
                if (svnPath!!.startsWith("/")) {
                    repo.url + svnPath
                } else {
                    repo.url + "/" + svnPath
                }
            }
        }
    }

    private fun svnRevision(): SVNRevision {

        val v = if (revision.isNullOrBlank()) {
            if (specifyRevision.isNullOrBlank())
                SVNRevision.HEAD.name
            else {
                specifyRevision
            }
        } else {
            revision
        }

        return if (v.isNullOrBlank())
            SVNRevision.HEAD
        else {
            REVISIONS[v] ?: try {
                SVNRevision.create(v!!.toLong())
            } catch (e: NumberFormatException) {
                SVNRevision.HEAD
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeSvnPullCodeSetting::class.java)
        private val REVISIONS = mapOf(
            SVNRevision.HEAD.name to SVNRevision.HEAD,
            SVNRevision.BASE.name to SVNRevision.BASE,
            SVNRevision.COMMITTED.name to SVNRevision.COMMITTED,
            SVNRevision.PREVIOUS.name to SVNRevision.PREVIOUS,
            SVNRevision.WORKING.name to SVNRevision.WORKING,
            SVNRevision.UNDEFINED to SVNRevision.UNDEFINED
        )
    }
}
