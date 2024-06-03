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

package com.tencent.devops.plugin.worker.task.scm.git

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_URL
import com.tencent.devops.plugin.worker.task.scm.util.DirectoryUtil
import com.tencent.devops.plugin.worker.task.scm.util.GitUtil
import com.tencent.devops.plugin.worker.task.scm.util.RepoCommitUtil
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.utils.GITHUB_PR_NUMBER
import com.tencent.devops.process.utils.GIT_MR_NUMBER
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_COMMENT
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_ID
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_TIMES
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.scm.code.git.api.GitCredentialSetter
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.scm.utils.code.github.GithubUtils
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.scm.CommitSDKApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_WRONG_GIT_SPECIFIES_THE_PULL_METHOD
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_GIT_HOST_INFO_FAIL
import com.tencent.devops.worker.common.constants.WorkerMessageCode.PULL_THE_REPOSITORY_IN_FULL
import com.tencent.devops.worker.common.constants.WorkerMessageCode.URL_INCORRECT
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File
import java.io.Writer
import java.net.URL
import java.nio.file.Files
import org.apache.commons.lang3.exception.ExceptionUtils
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_REMOTE
import org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_URL
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.submodule.SubmoduleWalk
import org.eclipse.jgit.transport.URIish
import org.slf4j.LoggerFactory

@Suppress("ALL")
open class GitUpdateTask constructor(
    protected open val projectName: String,
    protected open val userName: String,
    protected open val url: String,
    protected open val branchName: String?,
    protected open val revision: String?,
    protected open val workspace: File,
    protected open val credentialSetter: GitCredentialSetter,
    protected open val convertSubmoduleUrl: Boolean = true,
    protected open val enableSubmodule: Boolean,
    protected open val enableVirtualMergeBranch: Boolean,
    protected open val modeType: String?,
    protected open val modeValue: String?,
    protected open val pipelineId: String,
    protected open val buildId: String,
    protected open val repositoryConfig: RepositoryConfig,
    protected open val gitType: ScmType,
    protected open val variables: Map<String, String>,
    protected open val aliasName: String
) {

    private val commitResourceApi = ApiFactory.create(CommitSDKApi::class)

    private val writer = object : Writer() {
        override fun write(cbuf: CharArray?, off: Int, len: Int) {
            if (cbuf == null) {
                return
            }
            LoggerService.addNormalLine(String(cbuf, off, len))
        }

        override fun flush() {
        }

        override fun close() {
        }
    }

    open fun beforeActions() {
    }

    open fun preUpdate(git: Git) {
    }

    open fun cleanupWorkspace() {
        if (workspace.exists()) {
            LoggerService.addNormalLine("Clean up the workspace(${workspace.path})")
            Files.list(workspace.toPath())
                .forEach(DirectoryUtil::deleteRecursively)
            val deleteSuccess = true
            LoggerService.addNormalLine("delete the file: ${workspace.canonicalPath} ($deleteSuccess)")
        }
    }

    open fun command(git: Git, workspace: File, url: String, writer: Writer): TransportCommand<*, *> {
        return if (workspace.exists() && File(workspace.path, ".git").exists()) {
            LoggerService.addNormalLine("Fetch the code from $url")
            git.fetch()
                .setProgressMonitor(TextProgressMonitor(writer))
        } else {
            LoggerService.addNormalLine("Clone the repo from $url")
            val file = File(workspace.path)
            val initGit = Git.init().setDirectory(file).call()

            val remoteCommand = git.remoteAdd()
            remoteCommand.setUri(URIish(credentialSetter.getCredentialUrl(url)))
            remoteCommand.setName(Constants.DEFAULT_REMOTE_NAME)
            remoteCommand.call()

            val pullCommand = initGit.pull()
            pullCommand.setProgressMonitor(TextProgressMonitor(writer))
                .remoteBranchName = Constants.HEAD
            pullCommand
        }
    }

    protected fun cloneGit(writer: Writer): TransportCommand<*, *> {
        LoggerService.addNormalLine("Clone the repo from $url")

        return Git.cloneRepository()
            .setProgressMonitor(TextProgressMonitor(writer))
            .setBare(false)
            .setCloneAllBranches(true)
            .setCloneSubmodules(false)
            .setDirectory(workspace)
            .setURI(credentialSetter.getCredentialUrl(url))
    }

    fun perform(): Map<String, String>? {
        try {
            deleteLock()
            beforeActions()
            val builder = FileRepositoryBuilder()
            val repo = builder.setWorkTree(workspace).readEnvironment().build()
            val git = Git(repo)

            checkLocalGitRepo(git, url)
            preUpdate(git)
            val command = command(git, workspace, url, writer)
            credentialSetter.setGitCredential(command)
            command.call()

            if (url.startsWith("http")) {
                val config = git.repository.config
                config.setString("user", null, "name", userName)
                config.setString("user", null, "email", "$userName@tencent.com")
                config.save()
            }

            val startType =
                if (variables[PIPELINE_START_TYPE] != null) {
                    StartType.valueOf(variables[PIPELINE_START_TYPE]!!)
                } else null
            val hookType =
                if (variables[PIPELINE_WEBHOOK_EVENT_TYPE] != null) {
                    CodeEventType.valueOf(variables[PIPELINE_WEBHOOK_EVENT_TYPE]!!)
                } else null
            val sourceBranch = variables[PIPELINE_WEBHOOK_SOURCE_BRANCH]
            val targetBranch = variables[PIPELINE_WEBHOOK_TARGET_BRANCH]
            val sourceUrl = variables[PIPELINE_WEBHOOK_SOURCE_URL]
            val targetUrl = variables[PIPELINE_WEBHOOK_TARGET_URL]

            if (enableVirtualMergeBranch && isSameProject(gitType, url, targetUrl) && startType == StartType.WEB_HOOK &&
                (hookType == CodeEventType.PULL_REQUEST || hookType == CodeEventType.MERGE_REQUEST)
            ) {
                LoggerService.addWarnLine("The mode enable virtual merge branch")
                checkoutVirtualBranch(gitType, git, sourceUrl!!, sourceBranch!!, targetUrl!!, targetBranch!!, variables)
            } else {
                LoggerService.addNormalLine("The mode type($modeType) and mode value($modeValue) - ${revision ?: ""}")
                checkout(git)
            }

            if (enableSubmodule) {
                LoggerService.addNormalLine("Enable pull git submodule")
                updateSubmodule(workspace, git.repository)
            } else {
                LoggerService.addNormalLine("Disable pull git submodule")
            }

            return saveCommit(git, repo, pipelineId, buildId, repositoryConfig, gitType)
        } catch (t: Throwable) {
            logger.warn("Fail to perform git code($url)", t)
            LoggerService.addErrorLine("Fail to pull git($url) code because of " +
                "(${ExceptionUtils.getStackTrace(t)})")
            throw t
        }
    }

    private fun saveCommit(
        git: Git,
        repo: Repository,
        pipelineId: String,
        buildId: String,
        repositoryConfig: RepositoryConfig,
        gitType: ScmType
    ): Map<String, String> {
        LoggerService.addNormalLine("save commit($gitType)")
        val commitMaterial =
            RepoCommitUtil.saveGitCommit(git, repo, pipelineId, buildId, repositoryConfig, gitType)

        val envProjectName = projectName.replace("/", "")
        val env = mutableMapOf<String, String>()
        if (commitMaterial.lastCommitId != null) {
            env["git.$envProjectName.last.commit"] = commitMaterial.lastCommitId!!
            env["$PIPELINE_MATERIAL_NEW_COMMIT_ID.${repositoryConfig.getRepositoryId()}"] =
                commitMaterial.lastCommitId!!
        }
        if (commitMaterial.newCommitId != null) {
            env["git.$envProjectName.new.commit"] = commitMaterial.newCommitId!!
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
                aliasName = aliasName,
                url = url,
                branchName = modeValue,
                newCommitId = commitMaterial.newCommitId ?: commitMaterial.lastCommitId,
                newCommitComment = commitMaterial.newCommitComment,
                commitTimes = commitMaterial.commitTimes,
                scmType = gitType.name
            )
        ))
        return env
    }

    private fun deleteLock() {
        GitUtil.deleteLock(workspace)
    }

    private fun isSameProject(scmType: ScmType, url: String, targetUrl: String?): Boolean {
        targetUrl ?: return false

        return when (scmType) {
            ScmType.GITHUB -> {
                GithubUtils.getProjectName(url) == GithubUtils.getProjectName(targetUrl)
            }
            ScmType.CODE_GIT -> {
                GitUtils.getProjectName(url) == GitUtils.getProjectName(targetUrl)
            }
            else -> {
                false
            }
        }
    }

    private fun checkoutVirtualBranch(
        gitType: ScmType,
        git: Git,
        sourceUrl: String,
        sourceBranch: String,
        targetUrl: String,
        targetBranch: String,
        variables: Map<String, String>
    ) {
        logger.info("Check out virtual branch source($sourceUrl, $sourceBranch) and target($targetUrl, $targetBranch)")

        // checkout target branch
        val targetRemoteBranch = getRemoteBranch(git, targetBranch)
        checkoutBranch(git, targetRemoteBranch!!)

        // delete last virtual branch
        clearVirtualBranch(gitType, git, variables)

        // create virtual branch
        createVirtualBranch(gitType, git, variables)

        // pull source branch and merge to virtual branch
        pullBranchToMerge(git, sourceUrl, sourceBranch)
    }

    private fun clearVirtualBranch(gitType: ScmType, git: Git, variables: Map<String, String>) {
        val branchPrefix = when (gitType) {
            ScmType.CODE_GIT -> "mr-"
            ScmType.GITHUB -> "pr-"
            else -> throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = "Invalid scm type $gitType"
            )
        }

        val refList = git.branchList().call()
        refList.forEach { ref ->
            val branch = Repository.shortenRefName(ref.name)
            if (branch.startsWith(branchPrefix)) {
                deleteBranch(git, branch)
            }
        }
    }

    private fun createVirtualBranch(gitType: ScmType, git: Git, variables: Map<String, String>) {
        val branch = when (gitType) {
            ScmType.CODE_GIT -> {
                val mrNumber = variables[GIT_MR_NUMBER]!!
                "mr-$mrNumber"
            }
            ScmType.GITHUB -> {
                val prNumber = variables[GITHUB_PR_NUMBER]!!
                "pr-$prNumber"
            }
            else -> throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = "Invalid scm type $gitType"
            )
        }

        createBranch(git, branch)
    }

    private fun checkout(git: Git) {
        // old pipeline logic, it has not mode type and mode value
        if (modeType.isNullOrBlank() || modeValue.isNullOrBlank()) {
            // Checkout the code
            var checkoutName = (revision ?: branchName ?: "master").trim()
            if (checkoutName.trim().isEmpty()) {
                checkoutName = "master"
            }

            val remoteBranch = getRemoteBranch(git, checkoutName)
            if (remoteBranch == null) {
                // Checkout the revision
                val tag = getRemoteTag(git, checkoutName)
                if (!tag.isNullOrEmpty()) {
                    checkoutTag(git, tag!!)
                } else {
                    checkoutCommitId(git, checkoutName)
                }
            } else {
                checkoutBranch(git, remoteBranch)
            }
        } else {
            when (modeType) {
                GitPullModeType.BRANCH.name -> {
                    if (revision.isNullOrBlank()) {
                        val remoteBranch = getRemoteBranch(git, modeValue!!)
                            ?: throw TaskExecuteException(
                                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                                errorType = ErrorType.USER,
                                errorMsg = "Can't find the branch($modeValue) of git repository($url)"
                            )
                        checkoutBranch(git, remoteBranch)
                    } else {
                        checkoutCommitId(git, revision!!)
                    }
                }

                GitPullModeType.TAG.name -> {
                    if (revision.isNullOrBlank()) {
                        val tag = getRemoteTag(git, modeValue!!)
                            ?: throw TaskExecuteException(
                                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                                errorType = ErrorType.USER,
                                errorMsg = "Can't find the tag($modeValue) of git repository($url)"
                            )
                        checkoutTag(git, tag)
                    } else {
                        checkoutCommitId(git, revision!!)
                    }
                }

                GitPullModeType.COMMIT_ID.name -> {
                    checkoutCommitId(git, modeValue!!)
                }

                else -> {
                    logger.warn("Unknown checkout mode type($modeType)")
                    throw TaskExecuteException(
                        errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                        errorType = ErrorType.USER,
                        errorMsg = MessageUtil.getMessageByLocale(
                            messageCode = BK_WRONG_GIT_SPECIFIES_THE_PULL_METHOD,
                            params = arrayOf(modeType.toString()),
                            language = AgentEnv.getLocaleLanguage()
                        )
                    )
                }
            }
        }
    }

    private fun deleteBranch(git: Git, remoteBranch: String) {
        val branch = Repository.shortenRefName(remoteBranch)

        LoggerService.addNormalLine("Delete branch $branch")
        git.branchDelete().setForce(true).setBranchNames(branch).call()
    }

    private fun createBranch(git: Git, branchName: String) {
        val branch = Repository.shortenRefName(branchName)

        LoggerService.addNormalLine("Create branch $branch")
        git.checkout()
            .setCreateBranch(true)
            .setName(normalBranch(branch))
            .setForce(true)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
            .call()
    }

    private fun checkoutBranch(git: Git, remoteBranch: String) {
        val branch = Repository.shortenRefName(remoteBranch)

        LoggerService.addNormalLine("Checkout to branch $branch")
        if (isBranchExist(git, branch)) {
            git.checkout()
                .setName(normalBranch(branch))
                .setForce(true)
                .setStartPoint(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .call()
        } else if (Repository.shortenRefName(git.repository.branch) != branch) {
            git.checkout()
                .setCreateBranch(true)
                .setName(normalBranch(branch))
                .setForce(true)
                .setStartPoint(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .call()
        }

        LoggerService.addNormalLine("Pull the code ...")
        val pullCommand = git.pull()
        credentialSetter.setGitCredential(pullCommand)
        pullCommand.call()
    }

    private fun checkoutTag(git: Git, tag: String) {
        LoggerService.addNormalLine("Checkout to tag $tag")
        git.checkout()
            .setName(tag)
            .setForce(true)
            .call()
    }

    private fun checkoutCommitId(git: Git, commitId: String) {
        LoggerService.addNormalLine("Checkout to revision $commitId")
        git.checkout()
            .setName(commitId)
            .setForce(true)
            .call()
    }

    private fun pullBranchToMerge(git: Git, url: String, branchName: String) {
        val branch = Repository.shortenRefName(branchName)

        val remoteAddCommand = git.remoteAdd()
        remoteAddCommand.setName(DEVOPS_VIRTUAL)
        remoteAddCommand.setUri(URIish(credentialSetter.getCredentialUrl(url)))
        remoteAddCommand.call()

        LoggerService.addNormalLine("Pull the code remote($url) and branch($branch)")
        val pullCommand = git.pull()
        pullCommand.remote = DEVOPS_VIRTUAL
        pullCommand.remoteBranchName = branch
        pullCommand.setProgressMonitor(TextProgressMonitor(writer))
        val pullResult = pullCommand.call()

        val remoteRemoveCommand = git.remoteRemove()
        remoteRemoveCommand.setName(DEVOPS_VIRTUAL)
        remoteRemoveCommand.call()

        if (pullResult.mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING) {
            LoggerService.addErrorLine("Merge branch $branchName conflict")
            pullResult.mergeResult.conflicts.forEach { file ->
                LoggerService.addErrorLine("Conflict file $file")
            }
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "Merge branch $branchName conflict"
            )
        } else {
            LoggerService.addNormalLine("Merge branch $branchName succeed")
        }
    }

    private fun isBranchExist(git: Git, branch: String): Boolean {
        git.branchList().call().forEach {
            if (Repository.shortenRefName(it.name) == branch) {
                return true
            }
        }
        return false
    }

    private fun getRemoteBranch(git: Git, checkoutName: String): String? {
        val branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
        return checkBranchOrTag(branches, checkoutName)
    }

    private fun getRemoteTag(git: Git, checkoutName: String): String? {
        val tags = git.tagList().call()
        return checkBranchOrTag(tags, checkoutName)
    }

    private fun checkBranchOrTag(refs: List<Ref>, checkoutName: String): String? {
        // Check if it match the checkout name
        refs.forEach { branchRef ->
            if (Repository.shortenRefName(branchRef.name) == checkoutName) {
                return branchRef.name
            }
        }

        val matchRef = mutableListOf<String>()

        refs.forEach loop@{ branchRef ->
            val names = branchRef.name.split("/")
            if (checkoutName.contains("/")) {
                val checkoutNames = checkoutName.split("/").reversed()
                if (checkoutNames.size > names.size) {
                    return@loop
                }

                val reverseNames = names.reversed()
                checkoutNames.forEachIndexed { index, value ->
                    if (value != reverseNames[index]) {
                        return@loop
                    }
                }
                matchRef.add(branchRef.name)
            } else {
                if (names.last() == checkoutName) {
                    matchRef.add(branchRef.name)
                }
            }
        }
        if (matchRef.isEmpty()) {
            return null
        }

        if (matchRef.size == 1) {
            return matchRef[0]
        }
        return matchRef.minWithOrNull(Comparator { o1, o2 ->
            o1.split("/").size - o2.split("/").size
        })
    }

    private fun normalBranch(branch: String?): String? {
        if (branch == null) {
            return null
        }
        if (branch.startsWith(Constants.DEFAULT_REMOTE_NAME + "/")) {
            return branch.removePrefix(Constants.DEFAULT_REMOTE_NAME + "/")
        }
        return branch
    }

    private fun updateSubmodule(workspace: File, repository: Repository) {
        val submoduleConfigFile = File(workspace, ".gitmodules")

        if (submoduleConfigFile.exists()) {
            /**
             * The following logic is to replace the url in the .gitmodules which start with "http" with "ssh"
             * This is because we only support the ssh submodule
             */
            val modules = listSubmodules(repository)
            var submoduleConfig = submoduleConfigFile.readText()

            modules.forEach { m ->
                submoduleConfig = submoduleConfig.replace(m.url, m.credentialUrl)
            }
            submoduleConfigFile.writeText(submoduleConfig)
            updateSubmodule(repository)

            // update submodule' submodule iteratively
            modules.forEach { m ->
                val submoduleWorkspace = File(workspace, m.path)
                val submoduleRepository =
                    FileRepositoryBuilder().setWorkTree(submoduleWorkspace).readEnvironment().build()
                updateSubmodule(submoduleWorkspace, submoduleRepository)
            }
        }
    }

    private fun listSubmodules(repository: Repository): List<Submodule> {
        val walk = SubmoduleWalk.forIndex(repository)
        val result = mutableListOf<Submodule>()

        val rootUrlIsSsh = !url.startsWith("http")
        val rootHost = getUrlHost(this.url)

        while (walk.next()) {
            val path = walk.path
            val url = walk.modulesUrl
            if (url.isNullOrEmpty()) {
                logger.warn("The url is empty of submodule(${walk.modulesPath})")
            } else {
                /**
                 * http://git.com/example.git & git@git.com/example.git
                 *
                 * 1. convert http url to ssh url
                 * 2. convert ssh url to http url
                 */
                val subHost = getUrlHost(url)
                // 相对路径的 Submodule, 直接使用相对路径
                if (subHost == "") {
                    result.add(
                        Submodule(
                            path,
                            url,
                            url
                        )
                    )
                    continue
                }
                if (rootHost != subHost) continue

                if (convertSubmoduleUrl) {
                    if (rootUrlIsSsh) {
                        if (url.startsWith("http")) {
                            try {
                                val u = URL(url)
                                val convert = "git@${u.host}:${u.path.removePrefix("/")}"
                                LoggerService.addWarnLine("Convert the git submodule url from ($url) to ($convert)")
                                result.add(
                                    Submodule(
                                        path,
                                        url,
                                        convert
                                    )
                                )
                            } catch (e: Exception) {
                                LoggerService.addErrorLine(
                                    MessageUtil.getMessageByLocale(
                                        messageCode = URL_INCORRECT,
                                        language = AgentEnv.getLocaleLanguage(),
                                        params = arrayOf(url)
                                    )
                                )
                                throw e
                            }
                        } else {
                            result.add(
                                Submodule(
                                    path,
                                    url,
                                    credentialSetter.getCredentialUrl(url)
                                )
                            )
                        }
                    } else if (!rootUrlIsSsh) {
                        if (url.startsWith("git@")) {
                            try {
                                val (domain, repoName) = GitUtils.getDomainAndRepoName(url)
                                val convert = "http://$domain/$repoName.git"
                                LoggerService.addWarnLine("Convert the git submodule url from ($url) to ($convert)")
                                result.add(
                                    Submodule(
                                        path,
                                        url,
                                        credentialSetter.getCredentialUrl(convert)
                                    )
                                )
                            } catch (e: Exception) {
                                LoggerService.addErrorLine(
                                    MessageUtil.getMessageByLocale(
                                        messageCode = URL_INCORRECT,
                                        language = AgentEnv.getLocaleLanguage(),
                                        params = arrayOf(url)
                                    )
                                )
                                throw e
                            }
                        } else {
                            result.add(
                                Submodule(
                                    path,
                                    url,
                                    credentialSetter.getCredentialUrl(url)
                                )
                            )
                        }
                    }
                }
            }
        }
        return result
    }

    private fun getUrlHost(url: String): String {
        try {
            val actualUrl = url.trim()
            return when {
                actualUrl.startsWith("http") -> return URL(actualUrl).host
                actualUrl.startsWith("git@") -> actualUrl.substring("git@".length, actualUrl.indexOf(":"))
                else -> ""
            }
        } catch (e: Exception) {
            logger.warn("Fail to get the url host - ($url)", e)
            throw TaskExecuteException(
                errorType = ErrorType.THIRD_PARTY,
                errorCode = ErrorCode.THIRD_PARTY_INTERFACE_ERROR,
                errorMsg = MessageUtil.getMessageByLocale(
                    messageCode = GET_GIT_HOST_INFO_FAIL,
                    language = AgentEnv.getLocaleLanguage(),
                    params = arrayOf(url)
                )
            )
        }
    }

    private fun updateSubmodule(repository: Repository) {
        val git = Git.wrap(repository)
        val walk = SubmoduleWalk.forIndex(repository)

        while (walk.next()) {
            try {
                logger.info("Get the submodule ${walk.modulesUrl} ${walk.modulesPath} ${walk.objectId}")
                if (walk.repository != null) {
                    continue
                }
                if (walk.modulesPath.isNullOrEmpty()) {
                    continue
                }

                LoggerService.addNormalLine("Init the submodule ${walk.modulesPath}")
                git.submoduleInit().addPath(walk.modulesPath).call()

                LoggerService.addNormalLine("Checkout the submodule to revision ${walk.objectId.name}")
                val clone = Git.cloneRepository()
                clone.setURI(walk.remoteUrl)
                    .setDirectory(walk.directory)
                    .setGitDir(File(File(repository.directory, Constants.MODULES), walk.path))
                    .setBranch(walk.objectId.name)
                credentialSetter.setGitCredential(clone)
                clone.setProgressMonitor(TextProgressMonitor(writer))
                clone.call()
            } catch (e: Exception) {
                LoggerService.addErrorLine(
                    "Fail to checkout the submodule(${walk.modulesPath}) " +
                        "with url(${walk.modulesUrl}) to revision(${walk.objectId}) because of ${e.message}")
                throw e
            }
        }

        LoggerService.addNormalLine("Updating the submodule")
        val command = git.submoduleUpdate().setFetch(true)
        command.setProgressMonitor(TextProgressMonitor(writer))
        credentialSetter.setGitCredential(command)
        command.call()
    }

    private fun checkLocalGitRepo(git: Git, url: String) {
        val localUrl = git.repository.config.getString(CONFIG_KEY_REMOTE, "origin", CONFIG_KEY_URL)
        if (localUrl.isNullOrBlank()) {
            return
        }
        val localDecodedUrl = GitUtils.urlDecode(localUrl)

        val remoteUrl = credentialSetter.getCredentialUrl(url)
        if (localUrl != remoteUrl && localDecodedUrl != remoteUrl) {
            LoggerService.addWarnLine(
                "Git repo url " + MessageUtil.getMessageByLocale(
                    messageCode = PULL_THE_REPOSITORY_IN_FULL,
                    language = AgentEnv.getLocaleLanguage(),
                    params = arrayOf(localUrl, url)
                )
            )
            cleanupWorkspace()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitUpdateTask::class.java)
        private const val DEVOPS_VIRTUAL = "devops-virtual"
    }

    data class Submodule(
        val path: String,
        val url: String,
        val credentialUrl: String
    )
}
