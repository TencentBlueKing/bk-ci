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

package com.tencent.devops.plugin.worker.task.scm.svn

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.SVNVersion
import com.tencent.devops.log.meta.Ansi
import com.tencent.devops.plugin.worker.task.scm.util.DirectoryUtil
import com.tencent.devops.plugin.worker.task.scm.util.RepoCommitUtil
import com.tencent.devops.plugin.worker.task.scm.util.SvnUtil
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_COMMENT
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_ID
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_TIMES
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.scm.CommitSDKApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File
import java.nio.file.Files
import java.util.LinkedList
import java.util.Queue
import org.slf4j.LoggerFactory
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNErrorCode
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec
import org.tmatesoft.svn.core.wc.ISVNEventHandler
import org.tmatesoft.svn.core.wc.ISVNExternalsHandler
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNEvent
import org.tmatesoft.svn.core.wc.SVNEventAction
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNStatusType
import org.tmatesoft.svn.core.wc.SVNUpdateClient
import org.tmatesoft.svn.core.wc2.SvnTarget

@Suppress("ALL")
open class SvnUpdateTask constructor(
    protected open val svnUrl: SVNURL,
    protected open val projectName: String,
    protected open val username: String,
    protected open val privateKey: String,
    protected open val passPhrase: String?,
    protected open val revision: SVNRevision,
    protected open val workspace: File,
    protected open val strategy: CodePullStrategy,
    protected open val update: Boolean,
    protected open val enableSubmodule: Boolean,
    protected open val svnDepth: SVNDepth,
    protected open val pipelineId: String,
    protected open val buildId: String,
    protected open val repositoryConfig: RepositoryConfig,
    protected open val svnVersion: SVNVersion?,
    protected open val svnRepo: CodeSvnRepository
) {
    companion object {
        private val SVN_ERROR_CODES_SHOULD_RETRY = HashSet<Int>()
        private val SVN_ERROR_CATEGORIES_SHOULD_RETRY = HashSet<Int>()

        init {
            SVN_ERROR_CODES_SHOULD_RETRY.add(SVNErrorCode.UNKNOWN.code)
            SVN_ERROR_CODES_SHOULD_RETRY.add(SVNErrorCode.IO_ERROR.code)
            SVN_ERROR_CODES_SHOULD_RETRY.add(SVNErrorCode.RA_SVN_CONNECTION_CLOSED.code)
            SVN_ERROR_CODES_SHOULD_RETRY.add(SVNErrorCode.RA_SVN_IO_ERROR.code)
            SVN_ERROR_CODES_SHOULD_RETRY.add(SVNErrorCode.RA_SVN_MALFORMED_DATA.code)
            SVN_ERROR_CODES_SHOULD_RETRY.add(SVNErrorCode.APMOD_CONNECTION_ABORTED.code)
            SVN_ERROR_CODES_SHOULD_RETRY.add(SVNErrorCode.BASE.code)
            SVN_ERROR_CODES_SHOULD_RETRY.add(SVNErrorCode.PLUGIN_LOAD_FAILURE.code)

            SVN_ERROR_CATEGORIES_SHOULD_RETRY.add(SVNErrorCode.IO_CATEGORY)
            SVN_ERROR_CATEGORIES_SHOULD_RETRY.add(SVNErrorCode.FS_CATEGORY)
            SVN_ERROR_CATEGORIES_SHOULD_RETRY.add(SVNErrorCode.RA_DAV_CATEGORY)
        }

        private val logger = LoggerFactory.getLogger(SvnUpdateTask::class.java)
    }

    private val commitResourceApi = ApiFactory.create(CommitSDKApi::class)

    protected open fun preUpdate() {}

    protected open fun cleanupWorkspace(workspace: File) {
        if (workspace.exists()) {
            LoggerService.addNormalLine("Clean up the workspace(${workspace.path})")
            Files.list(workspace.toPath()).forEach(DirectoryUtil::deleteRecursively)
            val deleteSuccess = true
            LoggerService.addNormalLine("delete the file: ${workspace.canonicalPath} ($deleteSuccess)")
        }
    }

    fun perform(): Map<String, String>? {
        val manager = SvnUtils.getClientManager(svnUrl, username, privateKey, passPhrase)
        val client = manager.updateClient
        client.isIgnoreExternals = !enableSubmodule

        val externalQueue = LinkedList<External>()
        registerEvent(client)
        registerExternalHandler(client, externalQueue)
        preUpdate()

        checkLocalSvnRepo()
        LoggerService.addNormalLine("Update the svn code from $svnUrl")
        val lastRevision = pullCode(manager, client, workspace, svnUrl, SVNRevision.HEAD, revision)

        if (enableSubmodule) {
            while (externalQueue.isNotEmpty()) {
                val external = externalQueue.poll() ?: break
                LoggerService.addNormalLine("Trying to pull the external svn code from ${external.externalURL}")

                // 如果都是HTTP协议，那么就不用转化， http:// https://
                val url = formatUrl(
                    if (!SvnUtils.isSSHProtocol(svnUrl.protocol) &&
                        !SvnUtils.isSSHProtocol(external.externalURL.protocol)) {
                        external.externalURL
                    } else if (svnUrl.protocol != external.externalURL.protocol) {
                        convertUrl(external.externalURL)
                    } else {
                        external.externalURL
                    }
                )
                val m = SvnUtils.getClientManager(url, username, privateKey, passPhrase)
                val c = m.updateClient
                registerEvent(c)
                registerExternalHandler(c, externalQueue)
                try {
                    pullCode(
                        m,
                        c,
                        external.externalPath,
                        url,
                        external.externalPegRevision,
                        external.externalRevision,
                        false
                    )
                } catch (ignored: Throwable) {
                    logger.warn("Fail to checkout the checkout external code, use export strategy", ignored)
                    LoggerService.addNormalLine("Export the svn external $url")
                    client.doExport(
                        url,
                        external.externalPath,
                        external.externalPegRevision,
                        external.externalRevision,
                        null,
                        true,
                        svnDepth
                    )
                    update(manager = m,
                        client = c,
                        workspace = external.externalPath,
                        revision = external.externalRevision,
                        shouldRetry = true,
                        printLog = true,
                        cleanup = false)
                }
            }
        } else {
            LoggerService.addNormalLine("Svn is not enable submodule")
        }

        return saveRevision(manager, svnUrl, pipelineId, buildId, repositoryConfig, lastRevision)
    }

    private fun saveRevision(
        manager: SVNClientManager,
        svnUrl: SVNURL,
        pipelineId: String,
        buildId: String,
        repositoryConfig: RepositoryConfig,
        lastRevision: Long
    ): Map<String, String> {
        val commitMaterial =
            RepoCommitUtil.saveSvnCommit(manager, svnUrl, pipelineId, buildId, repositoryConfig, lastRevision)

        val envProjectName = projectName.replace("/", ".")
        val env = mutableMapOf<String, String>()
        if (commitMaterial.lastCommitId != null) {
            env["svn.$envProjectName.last.revision"] = commitMaterial.lastCommitId!!
        }
        if (commitMaterial.newCommitId != null) {
            env["svn.$envProjectName.new.revision"] = commitMaterial.newCommitId!!
            env["$PIPELINE_MATERIAL_NEW_COMMIT_ID.${repositoryConfig.getRepositoryId()}"] = commitMaterial.newCommitId!!
        }
        if (commitMaterial.newCommitComment != null) {
            env["$PIPELINE_MATERIAL_NEW_COMMIT_COMMENT.${repositoryConfig.getRepositoryId()}"] =
                commitMaterial.newCommitComment!!
        }
        if (commitMaterial.commitTimes != 0) {
            env["$PIPELINE_MATERIAL_NEW_COMMIT_TIMES.${repositoryConfig.getRepositoryId()}"] =
                commitMaterial.commitTimes.toString()
        }

        commitResourceApi.saveBuildMaterial(listOf(
            PipelineBuildMaterial(
                aliasName = svnRepo.aliasName,
                url = svnRepo.url,
                branchName = "",
                newCommitId = commitMaterial.newCommitId ?: commitMaterial.lastCommitId,
                newCommitComment = commitMaterial.newCommitComment,
                commitTimes = commitMaterial.commitTimes,
                scmType = ScmType.CODE_SVN.name
            )
        ))
        return env
    }

    private fun pullCode(
        manager: SVNClientManager,
        client: SVNUpdateClient,
        workspace: File,
        svnUrl: SVNURL,
        pegRevision: SVNRevision,
        targetRevision: SVNRevision,
        printLog: Boolean = true
    ): Long {
        try {
            return if (!update || !workspace.exists()) {
                checkout(client = client,
                    workspace = workspace,
                    svnUrl = svnUrl,
                    pegRevision = pegRevision,
                    revision = targetRevision,
                    shouldRetry = true,
                    printLog = printLog)
            } else {
                if (!File(workspace, ".svn").exists()) {
                    LoggerService.addNormalLine("Clean the workspace($workspace) as there is no .svn file")
                    cleanupWorkspace(workspace)
                    checkout(client = client,
                        workspace = workspace,
                        svnUrl = svnUrl,
                        pegRevision = pegRevision,
                        revision = targetRevision,
                        shouldRetry = true,
                        printLog = printLog)
                } else {
                    update(
                        manager = manager,
                        client = client,
                        workspace = workspace,
                        revision = targetRevision,
                        shouldRetry = true,
                        printLog = printLog,
                        cleanup = strategy != CodePullStrategy.INCREMENT_UPDATE
                    ) // Increment update don't clean up the workspace
                }
            }
        } catch (t: Throwable) {
            logger.warn("Fail to pull the svn($svnUrl) update task", t)
            if (printLog) {
                LoggerService.addErrorLine("Fail to pull svn($svnUrl) code because of ${t.message}")
            }
            throw t
        }
    }

    private fun switchCode(
        manager: SVNClientManager,
        client: SVNUpdateClient,
        workspace: File,
        svnUrl: SVNURL,
        pegRevision: SVNRevision,
        targetRevision: SVNRevision,
        printLog: Boolean = true
    ): Long {
        try {
            return switch(
                manager = manager,
                client = client,
                workspace = workspace,
                svnUrl = svnUrl,
                pegRevision = pegRevision,
                revision = targetRevision,
                shouldRetry = true,
                printLog = printLog,
                cleanup = strategy != CodePullStrategy.INCREMENT_UPDATE
            )
        } catch (t: Throwable) {
            logger.warn("Fail to switch the svn($svnUrl) update task", t)
            if (printLog) {
                LoggerService.addErrorLine("Fail to switch svn($svnUrl) code because of ${t.message}")
            }
            throw t
        }
    }

    private fun checkout(
        client: SVNUpdateClient,
        workspace: File,
        svnUrl: SVNURL,
        pegRevision: SVNRevision,
        revision: SVNRevision,
        shouldRetry: Boolean,
        printLog: Boolean
    ): Long {
        val svnOperationFactory = client.operationsFactory
        try {
            if (svnVersion != null) {
                LoggerService.addNormalLine(
                    "Start to checkout the code($svnUrl) to workspace(${workspace.path}) with svn version $svnVersion")
            } else {
                LoggerService.addNormalLine("Start to checkout the code($svnUrl) to workspace(${workspace.path})")
            }

            val co = svnOperationFactory.createCheckout()
            co.isIgnoreExternals = client.isIgnoreExternals
            co.isUpdateLocksOnDemand = client.isUpdateLocksOnDemand
            co.source = SvnTarget.fromURL(svnUrl, pegRevision)
            co.depth = svnDepth
            co.revision = revision
            co.isAllowUnversionedObstructions = false
            co.setSingleTarget(SvnTarget.fromFile(workspace))
            co.externalsHandler = SvnCodec.externalsHandler(client.externalsHandler)
            if (svnVersion != null) {
                co.targetWorkingCopyFormat = svnVersion!!.version
            }

            return co.run() ?: 0
        } catch (e: SVNException) {
            if (shouldRetry) {
                if (printLog) {
                    LoggerService.addWarnLine("Warning: fail to checkout code because of ${e.message}, retry")
                }
                return checkout(client, workspace, svnUrl, pegRevision, revision, false, printLog)
            } else {
                if (printLog) {
                    LoggerService.addErrorLine("Error: fail to checkout the code because of ${e.message}")
                }
                throw e
            }
        } finally {
            svnOperationFactory.dispose()
        }
    }

    private fun switch(
        manager: SVNClientManager,
        client: SVNUpdateClient,
        workspace: File,
        svnUrl: SVNURL,
        pegRevision: SVNRevision,
        revision: SVNRevision,
        shouldRetry: Boolean,
        printLog: Boolean,
        cleanup: Boolean = true
    ): Long {
        val svnOperationFactory = client.operationsFactory
        try {
            if (svnVersion != null) {
                LoggerService.addNormalLine(
                    "Start to switch the code($svnUrl) to workspace(${workspace.path}) with svn version $svnVersion")
            } else {
                LoggerService.addNormalLine("Start to switch the code($svnUrl) to workspace(${workspace.path})")
            }

            val switch = svnOperationFactory.createSwitch()
            switch.isIgnoreExternals = client.isIgnoreExternals
            switch.isIgnoreAncestry = true
            switch.isUpdateLocksOnDemand = client.isUpdateLocksOnDemand
            switch.switchTarget = SvnTarget.fromURL(svnUrl, pegRevision)
            switch.depth = svnDepth
            switch.revision = revision
            switch.isAllowUnversionedObstructions = false
            switch.setSingleTarget(SvnTarget.fromFile(workspace))
            switch.externalsHandler = SvnCodec.externalsHandler(client.externalsHandler)

            return switch.run() ?: 0
        } catch (e: SVNException) {
            if (shouldRetry) {
                if (printLog) {
                    LoggerService.addWarnLine("Warning: fail to switch code because of ${e.message}, retry")
                }
                if (cleanup) {
                    LoggerService.addNormalLine("Clean up the workspace")
                    SvnUtil.deleteWcLockAndCleanup(manager.wcClient, workspace)
                }
                return switch(manager = manager,
                    client = client,
                    workspace = workspace,
                    svnUrl = svnUrl,
                    pegRevision = pegRevision,
                    revision = revision,
                    shouldRetry = false,
                    printLog = printLog)
            } else {
                if (printLog) {
                    LoggerService.addErrorLine("Error: fail to switch the code because of ${e.message}")
                }
                throw e
            }
        } finally {
            svnOperationFactory.dispose()
        }
    }

    private fun update(
        manager: SVNClientManager,
        client: SVNUpdateClient,
        workspace: File,
        revision: SVNRevision,
        shouldRetry: Boolean,
        printLog: Boolean,
        cleanup: Boolean = true
    ): Long {
        try {
            if (cleanup) {
                LoggerService.addNormalLine("Clean up the workspace")
                SvnUtil.deleteWcLockAndCleanup(manager.wcClient, workspace)
            }
            LoggerService.addNormalLine("Start to update the workspace($workspace)")
            return client.doUpdate(workspace, revision, svnDepth, false, false)
        } catch (e: SVNException) {
            if (shouldRetry && isSVNErrorShouldRetry(e.errorMessage.errorCode)) {
                if (printLog) {
                    LoggerService.addWarnLine("Warning: fail to update code because of ${e.message}, retry")
                }
                if (cleanup) {
                    LoggerService.addNormalLine("Clean up the workspace")
                    SvnUtil.deleteWcLockAndCleanup(manager.wcClient, workspace)
                }
                return update(manager, client, workspace, revision, false, printLog, cleanup)
            } else {
                if (printLog) {
                    LoggerService.addErrorLine("Error: fail to update the code")
                }
                throw e
            }
        }
    }

    private fun formatUrl(url: SVNURL): SVNURL {
        return if (url.protocol == "http" || url.protocol == "https") {
            url
        } else {
            SVNURL.parseURIEncoded(String.format("svn+ssh://%s/%s", url.host, url.path))
        }
    }

    /**
     * Convert the http url to ssh url of svn repo
     * @param from http url
     * @return convert the http url to svn+ssh url
     */
    private fun convertUrl(from: SVNURL): SVNURL {
        try {
            val url = if (from.protocol == "http" || from.protocol == "https") {
                String.format("svn+ssh://%s/%s", from.host, from.path)
            } else {
                String.format("http://%s/%s", from.host, from.path)
            }
            val svnUrl = SVNURL.parseURIEncoded(url)
            LoggerService.addNormalLine("Covert the svn url from <$from> to <$svnUrl>")
            return svnUrl
        } catch (e: SVNException) {
            LoggerService.addNormalLine("Fail to convert the url from - $from because of ${e.message}")
            throw e
        }
    }

    private fun registerEvent(client: SVNUpdateClient) {
        client.setEventHandler(object : ISVNEventHandler {
            override fun handleEvent(event: SVNEvent, progress: Double) {
                val action = event.action
                if (action == SVNEventAction.UPDATE_ADD || action == SVNEventAction.ADD) {
                    LoggerService.addNormalLine(
                        Ansi().fgGreen().a("A").reset()
                        .a("\t").a(event.file.path).reset().toString())
                } else if (action == SVNEventAction.UPDATE_DELETE || action == SVNEventAction.DELETE) {
                    LoggerService.addNormalLine(
                        Ansi().fgRed().a("D").reset()
                        .a("\t").a(event.file.path).reset().toString())
                } else if (action == SVNEventAction.UPDATE_UPDATE) {
                    when (event.contentsStatus) {
                        SVNStatusType.CHANGED -> {
                            LoggerService.addNormalLine(
                                Ansi().fgBrightCyan().a("U").reset().a("\t").a(event.file.path)
                                    .reset().toString())
                        }
                        SVNStatusType.CONFLICTED -> {
                            LoggerService.addNormalLine(
                                Ansi().fgBrightRed().a("C").reset().a("\t").a(event.file.path)
                                    .reset().toString())
                        }
                        SVNStatusType.MERGED -> {
                            LoggerService.addNormalLine(
                                Ansi().fgBrightYellow().a("G").reset().a("\t").a(event.file.path)
                                    .reset().toString())
                        }
                    }
                } else if (action == SVNEventAction.UPDATE_EXTERNAL) {
                    LoggerService.addNormalLine("Fetching external item into '${event.file.path}'")
                    LoggerService.addNormalLine(
                        Ansi().a("External at revision ").fgBrightCyan().a(event.revision).reset().toString())
                } else if (action == SVNEventAction.UPDATE_COMPLETED) {
                    LoggerService.addNormalLine(
                        Ansi().a("At revision ").fgCyan().a(event.revision).reset().toString())
                } else if (action == SVNEventAction.LOCKED) {
                    LoggerService.addNormalLine(
                        Ansi().fgMagenta().a("L").reset().a("\t").a(event.file.path).reset().toString())
                } else if (action == SVNEventAction.LOCK_FAILED) {
                    LoggerService.addWarnLine("Failed to lock ${event.file.path}")
                }
            }

            override fun checkCancelled() {
            }
        })
    }

    private fun registerExternalHandler(client: SVNUpdateClient, externalQueue: Queue<External>) =
        client.setExternalsHandler { externalPath: File,
                                     externalURL: SVNURL,
                                     externalRevision: SVNRevision,
                                     externalPegRevision: SVNRevision,
                                     externalsDefinition: String,
                                     externalsWorkingRevision: SVNRevision ->
            if ("http" == externalURL.protocol || "https" == externalURL.protocol) {
                LoggerService.addNormalLine("Need to convert the http svn url to ssh - $externalURL")
            } else {
                LoggerService.addNormalLine(("Add external ssh url - $externalURL"))
            }
            externalQueue.add(
                External(
                    externalPath = externalPath,
                    externalURL = externalURL,
                    externalRevision = externalRevision,
                    externalPegRevision = externalPegRevision,
                    externalsDefinition = externalsDefinition,
                    externalsWorkingRevision = externalsWorkingRevision
                )
            )
            ISVNExternalsHandler.DEFAULT.handleExternal(
                externalPath,
                externalURL,
                externalRevision,
                externalPegRevision,
                externalsDefinition,
                externalsWorkingRevision
            )
        }

    private fun isSVNErrorShouldRetry(svnErrorCode: SVNErrorCode?) =
        svnErrorCode != null &&
            (SVN_ERROR_CATEGORIES_SHOULD_RETRY.contains(svnErrorCode.category) ||
                SVN_ERROR_CODES_SHOULD_RETRY.contains(svnErrorCode.code))

    private fun checkLocalSvnRepo() {
        if (!workspace.exists()) {
            return
        }
        if (!File(workspace, ".svn").exists()) {
            return
        }
        val info = SVNClientManager.newInstance().wcClient.doInfo(workspace, SVNRevision.WORKING) ?: return
        if (info.url == null) {
            return
        }
        if (info.url != svnUrl) {
            LoggerService.addWarnLine(
                "SVN repo url " + MessageUtil.getMessageByLocale(
                            messageCode = WorkerMessageCode.PULL_THE_REPOSITORY_IN_FULL,
                            language = AgentEnv.getLocaleLanguage(),
                            params = arrayOf("${info.url}", "$svnUrl")
                        )
            )
            cleanupWorkspace(workspace)
        }
    }

    private fun isSwitched(): Boolean {
        if (!workspace.exists()) {
            return false
        }
        if (!File(workspace, ".svn").exists()) {
            return false
        }
        val info = SVNClientManager.newInstance().wcClient.doInfo(workspace, SVNRevision.WORKING) ?: return false
        if (info.url == null) {
            return false
        }
        if (info.url != svnUrl) {
            val localProjectName = SvnUtils.getSvnProjectName(info.url.toString())
            val newProjectName = SvnUtils.getSvnProjectName(svnUrl.toString())

            if (localProjectName == newProjectName) {
                LoggerService.addWarnLine(
                    "SVN repo url " + MessageUtil.getMessageByLocale(
                        messageCode = WorkerMessageCode.PULL_THE_REPOSITORY_IN_SWITCH,
                        language = AgentEnv.getLocaleLanguage(),
                        params = arrayOf("${info.url}", "$svnUrl")
                    )
                )
                return true
            }
        }
        return false
    }

    private class External internal constructor(
        internal val externalPath: File,
        internal val externalURL: SVNURL,
        internal val externalRevision: SVNRevision,
        internal val externalPegRevision: SVNRevision,
        internal val externalsDefinition: String,
        internal val externalsWorkingRevision: SVNRevision
    )
}
