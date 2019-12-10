package com.tencent.devops.plugin.worker.task.gitComment

import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class GitCommentCheckTask : ITask() {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCommentCheckTask::class.java)
    }

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParam = buildTask.params ?: mapOf()
        val path = taskParam["path"]
        val sourceBranch = taskParam["sourceBranch"] ?: throw TaskExecuteException(
            errorMsg = "Git Comment检查没有源分支",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val targetBranch = taskParam["targetBranch"] ?: throw TaskExecuteException(
            errorMsg = "Git Comment检查没有目标分支",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val commentPattern = taskParam["commentPattern"] ?: throw TaskExecuteException(
            errorMsg = "Git Comment检查没有检查的正则匹配",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val failOnMismatch = (taskParam["failOnMismatch"] ?: false.toString()).toBoolean()

        val codeDir = if (path.isNullOrBlank()) {
            workspace
        } else {
            File(workspace, path)
        }

        if (!codeDir.exists()) {
            throw TaskExecuteException(
                errorMsg = "Git Comment检查的代码库（${codeDir.absolutePath}）不存在",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }
        if (!File(codeDir, ".git").exists()) {
            throw TaskExecuteException(
                errorMsg = "Git Comment检查路径不是一个git代码库",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }
        val git = Git(FileRepositoryBuilder().setWorkTree(codeDir).readEnvironment().build())
        LoggerService.addNormalLine("开始检查git代码库的comment信息 - ${git.repository.config.getString(ConfigConstants.CONFIG_KEY_REMOTE, "origin", ConfigConstants.CONFIG_KEY_URL)}")
        checkGitComment(git, sourceBranch, targetBranch, commentPattern, failOnMismatch)
    }

    // Helper gets the diff as a string.
    fun checkGitComment(git: Git, sourceBranch: String, targetBranch: String, commentPattern: String, failOnMismatch: Boolean) {

        val sourceObjectId = resolve(git, sourceBranch) ?: throw TaskExecuteException(
            errorMsg = "Git源分支${sourceBranch}没有找到对应的commitID",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
        )
        val targetObjectId = resolve(git, targetBranch) ?: throw TaskExecuteException(
            errorMsg = "Git目标分支${targetBranch}没有找到对应的commitID",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
        )
        val commits = try {
            git.log().addRange(targetObjectId, sourceObjectId).call()
        } catch (t: Throwable) {
            logger.warn("Git Comment检查获取分支信息失败", t)
            throw TaskExecuteException(
                errorMsg = "Git Comment检查获取分支信息失败 - ${t.message}",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL
            )
        }

        var isEmpty = true

        val pattern = Pattern.compile(commentPattern)

        commits.forEach { it ->
            isEmpty = false
            LoggerService.addNormalLine("Git Comment检查: 提交消息-${it.fullMessage}")
            logger.info("Git Comment检查: 提交消息-${it.fullMessage}")
            if (!pattern.matcher(it.fullMessage).matches()) {
                if (failOnMismatch) {
                    throw TaskExecuteException(
                        errorMsg = "Git Comment检查: ${it.fullMessage} 与匹配规则($commentPattern)不匹配",
                        errorType = ErrorType.USER,
                        errorCode = AtomErrorCode.USER_INPUT_INVAILD
                    )
                }
                LoggerService.addYellowLine("Git Comment检查: ${it.fullMessage} 与匹配规则($commentPattern)不匹配")
            }
        }

        if (isEmpty) {
            LoggerService.addNormalLine("The commit is the same between $sourceBranch and $targetBranch")
        }
    }

    private fun resolve(git: Git, branch: String): ObjectId? {
        git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().forEach {
            if (git.repository.shortenRemoteBranchName(it.name) == branch) {
                return it.objectId
            }
        }
        return null
    }
}