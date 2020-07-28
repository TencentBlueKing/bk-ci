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

package com.tencent.devops.plugin.worker.task.ktlint

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.element.KtlintStyleElement
import com.tencent.devops.common.pipeline.element.ktlint.KtlintReporter
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.report.ReportSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

@TaskClassType(classTypes = [KtlintStyleElement.classType])
class KtlintCheckTask : ITask() {

    private val reportArchiveResourceApi = ApiFactory.create(ReportSDKApi::class)

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
                reportArchiveResourceApi.uploadReport(file, buildTask.elementId!!, relativePath, buildVariables)
                reportArchiveResourceApi.createReportRecord(buildTask.elementId!!, it.reportName ?: it.reporter.name, it.reporter.name)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KtlintCheckTask::class.java)
    }
}