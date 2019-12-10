package com.tencent.devops.plugin.worker.task.ktlint

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.build.ktlint.KtlintReporter
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.archive.ReportArchiveResourceApi
import com.tencent.devops.worker.common.api.process.ReportResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

class KtlintCheckTask : ITask() {

    private val reportArchiveResourceApi = ReportArchiveResourceApi()
    private val reportResourceApi = ReportResourceApi()

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val flags = taskParams["flags"]
        val patterns = taskParams["patterns"]
        val path = taskParams["path"]
        val reporters = taskParams["reporters"]
        LoggerService.addNormalLine("Start to execute ktlint check with flags($flags) and patterns($patterns) & reporters($reporters)")
        val tmpArgs = if (flags.isNullOrBlank()) {
            emptyList()
        } else {
            flags!!.split("//s+")
        }

        val ktlintReporters: List<KtlintReporter> = if (reporters.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val tmp: List<KtlintReporter> = JsonUtil.getObjectMapper().readValue(reporters!!)
                tmp
            } catch (t: Throwable) {
                logger.warn("Fail to read the reporters($reporters) to pojo", t)
                throw RuntimeException("Fail to parse the reporters($reporters)")
            }
        }

        val args = if (!patterns.isNullOrBlank()) {
            tmpArgs.plus(patterns!!)
        } else {
            tmpArgs
        }.plus(ktlintReporters.map {
            if (it.reportOutput.isNullOrBlank()) {
                "--reporter=${it.reporter.name}"
            } else {
                "--reporter=${it.reporter.name},output=${it.reportOutput}"
            }
        }.toList())

        val codeDir = if (path.isNullOrBlank()) {
            workspace
        } else {
            File(workspace, path)
        }

        logger.info("Start to do the ktlint check with args($args) of path $codeDir")
        try {
            KtlintChecker(codeDir).check(args.toTypedArray())
        } finally {
            ktlintReporters.filter { !it.reportOutput.isNullOrBlank() }.forEach {
                val file = File(codeDir, it.reportOutput)
                if (!file.exists()) {
                    logger.warn("The file (${file.absolutePath}) is not exist")
                    LoggerService.addNormalLine("The reporter file ${it.reportOutput} is not exist")
                    return@forEach
                }
                LoggerService.addNormalLine("Adding the ${it.reporter}'s reporter with file ${it.reportOutput}")
                val relativePath = Paths.get(codeDir.canonicalPath).relativize(Paths.get(file.canonicalPath)).toString()
                reportArchiveResourceApi.upload(file, buildTask.elementId!!, relativePath, buildVariables)
                reportResourceApi.create(buildTask.elementId!!, it.reportName ?: it.reporter.name, it.reporter.name)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KtlintCheckTask::class.java)
    }
}