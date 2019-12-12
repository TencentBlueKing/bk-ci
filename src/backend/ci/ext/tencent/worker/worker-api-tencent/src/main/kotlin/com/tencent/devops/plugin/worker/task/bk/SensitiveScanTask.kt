package com.tencent.devops.plugin.worker.task.bk

import com.tencent.devops.common.pipeline.element.SensitiveScanElement
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.report.ReportSDKApi
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.task.script.CommandFactory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import org.slf4j.LoggerFactory
import java.io.File
import java.io.Writer
import java.nio.file.Paths

/**
 * 构建脚本任务
 */
@TaskClassType(classTypes = [SensitiveScanElement.classType])
class SensitiveScanTask : ITask() {

    private val reportArchiveResourceApi = ApiFactory.create(ReportSDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val excludePath = taskParams["excludePath"] ?: ""
        val elementId = taskParams["id"] ?: taskParams["elementId"] ?: "T-2-1-1" // 给个默认值，而不是抛异常，兼容旧数据

        val command = CommandFactory.create("SHELL")
        val buildId = buildVariables.buildId
        val runtimeVariables = buildVariables.variables
        val projectId = buildVariables.projectId
        val deleteDir = "rm -rf public_script"
        command.execute(
            buildId = buildId,
            script = deleteDir,
            taskParam = taskParams,
            runtimeVariables = runtimeVariables,
            projectId = projectId,
            dir = workspace,
            buildEnvs = buildVariables.buildEnvs
        )

        val writer = object : Writer() {
            override fun write(cbuf: CharArray?, off: Int, len: Int) {
            }

            override fun flush() {
            }

            override fun close() {
            }
        }

        val sensitiveWorkspace = File(workspace, "public_script")
        Git.cloneRepository()
            .setProgressMonitor(TextProgressMonitor(writer))
            .setBare(false)
            .setCloneAllBranches(true)
            .setCloneSubmodules(false)
            .setDirectory(sensitiveWorkspace)
            .setURI("http://gitlab-paas.open.oa.com/bkpublic/public_script.git")
            .call()

        val script = """
                cd public_script && /bin/bash check.sh "$excludePath"
            """
        logger.info("Start to scan the sensitive information.excludePath: $excludePath")
        LoggerService.addNormalLine("开始敏感信息扫描，待排除目录：$excludePath")

        command.execute(buildId, script, taskParams, runtimeVariables, projectId, workspace, buildVariables.buildEnvs)

        val fileDirParam = "public_script/report"
        val indexFileParam = "detect_ssd.html"
        val reportNameParam = "敏感信息扫描报告"

        val fileDir = getFile(workspace, fileDirParam)
        if (!fileDir.isDirectory) {
            throw TaskExecuteException(
                errorMsg = "文件夹($fileDirParam)不存在",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        val indexFile = getFile(fileDir, indexFileParam)
        if (!indexFile.exists()) {
            LoggerService.addNormalLine("无敏感信息，无需生成报告")
            return
        }

        val fileDirPath = Paths.get(fileDir.canonicalPath)
        val allFileList = recursiveGetFiles(fileDir)
        allFileList.forEach {
            val relativePath = fileDirPath.relativize(Paths.get(it.canonicalPath)).toString()
            reportArchiveResourceApi.uploadReport(
                file = it,
                taskId = elementId,
                relativePath = relativePath,
                buildVariables = buildVariables
            )
        }
        reportArchiveResourceApi.createReportRecord(
            taskId = elementId,
            indexFile = indexFileParam,
            name = reportNameParam
        )
    }

    private fun recursiveGetFiles(file: File): List<File> {
        val fileList = mutableListOf<File>()
        file.listFiles()?.forEach {
            if (it.isDirectory) {
                val subFileList = recursiveGetFiles(it)
                fileList.addAll(subFileList)
            } else {
                fileList.add(it)
            }
        }
        return fileList
    }

    private fun getFile(workspace: File, filePath: String): File {
        val absPath = filePath.startsWith("/") || (filePath[0].isLetter() && filePath[1] == ':')
        return if (absPath) {
            File(filePath)
        } else {
            File(workspace, filePath)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SensitiveScanTask::class.java)
    }
}