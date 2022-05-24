/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.executor.arrowhead

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.tencent.bkrepo.common.api.constant.StringPool.SLASH
import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.scanner.pojo.scanner.ScanExecutorResult
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ApplicationItem
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ArrowheadScanExecutorResult
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ArrowheadScanExecutorResult.Companion.overviewKeyOfCve
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ArrowheadScanExecutorResult.Companion.overviewKeyOfLicenseRisk
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ArrowheadScanExecutorResult.Companion.overviewKeyOfSensitive
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ArrowheadScanner
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.CheckSecItem
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.CveSecItem
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.SensitiveItem
import com.tencent.bkrepo.scanner.executor.ScanExecutor
import com.tencent.bkrepo.scanner.executor.configuration.DockerProperties.Companion.SCANNER_EXECUTOR_DOCKER_ENABLED
import com.tencent.bkrepo.scanner.executor.configuration.ScannerExecutorProperties
import com.tencent.bkrepo.scanner.executor.pojo.ScanExecutorTask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.Resource
import org.springframework.expression.common.TemplateParserContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

@Component(ArrowheadScanner.TYPE)
@ConditionalOnProperty(SCANNER_EXECUTOR_DOCKER_ENABLED, matchIfMissing = true)
class ArrowheadScanExecutor @Autowired constructor(
    private val dockerClient: DockerClient,
    private val scannerExecutorProperties: ScannerExecutorProperties
) : ScanExecutor {

    @Value(CONFIG_FILE_TEMPLATE_CLASS_PATH)
    private lateinit var arrowheadConfigTemplate: Resource

    override fun scan(task: ScanExecutorTask): ScanExecutorResult {
        require(task.scanner is ArrowheadScanner)
        val scanner = task.scanner
        // 创建工作目录
        val workDir = createWorkDir(scanner.rootPath, task.taskId)
        logger.info(logMsg(task, "create work dir success, $workDir"))
        try {
            // 加载待扫描文件
            val scannerInputFile = File(File(workDir, scanner.container.inputDir), task.sha256)
            scannerInputFile.parentFile.mkdirs()
            task.inputStream.use { taskInputStream ->
                scannerInputFile.outputStream().use { taskInputStream.copyTo(it) }
            }
            logger.info(logMsg(task, "read file success"))

            // 加载扫描配置文件
            loadConfigFile(task, workDir, scannerInputFile)
            logger.info(logMsg(task, "load config success"))

            // 执行扫描
            val scanStatus = doScan(workDir, task)
            return result(
                File(workDir, scanner.container.outputDir),
                scanStatus
            )
        } finally {
            // 清理工作目录
            if (task.scanner.cleanWorkDir) {
                workDir.deleteRecursively()
            }
        }
    }

    /**
     * 创建工作目录
     *
     * @param rootPath 扫描器根目录
     * @param taskId 任务id
     *
     * @return 工作目录
     */
    private fun createWorkDir(rootPath: String, taskId: String): File {
        // 创建工作目录
        val workDir = File(File(scannerExecutorProperties.workDir, rootPath), taskId)
        if (!workDir.deleteRecursively() || !workDir.mkdirs()) {
            throw SystemErrorException(CommonMessageCode.SYSTEM_ERROR, workDir.absolutePath)
        }
        return workDir
    }

    /**
     * 加载扫描器配置文件
     *
     * @param scanTask 扫描任务
     * @param workDir 工作目录
     * @param scannerInputFile 待扫描文件
     *
     * @return 扫描器配置文件
     */
    private fun loadConfigFile(
        scanTask: ScanExecutorTask,
        workDir: File,
        scannerInputFile: File
    ): File {
        require(scanTask.scanner is ArrowheadScanner)
        val scanner = scanTask.scanner
        val knowledgeBase = scanner.knowledgeBase
        val dockerImage = scanner.container
        val template = arrowheadConfigTemplate.inputStream.use { it.reader().readText() }
        val inputFilePath = "${dockerImage.inputDir.removePrefix(SLASH)}$SLASH${scannerInputFile.name}"
        val outputDir = dockerImage.outputDir.removePrefix(SLASH)
        val params = mapOf(
            TEMPLATE_KEY_INPUT_FILE to inputFilePath,
            TEMPLATE_KEY_OUTPUT_DIR to outputDir,
            TEMPLATE_KEY_LOG_FILE to RESULT_FILE_NAME_LOG,
            TEMPLATE_KEY_KNOWLEDGE_BASE_SECRET_ID to knowledgeBase.secretId,
            TEMPLATE_KEY_KNOWLEDGE_BASE_SECRET_KEY to knowledgeBase.secretKey,
            TEMPLATE_KEY_KNOWLEDGE_BASE_ENDPOINT to knowledgeBase.endpoint
        )

        val content = SpelExpressionParser()
            .parseExpression(template, TemplateParserContext())
            .getValue(params, String::class.java)!!

        val configFile = File(workDir, scanner.configFilePath)
        configFile.writeText(content)
        return configFile
    }

    /**
     * 拉取镜像
     */
    private fun pullImage(tag: String) {
        val images = dockerClient.listImagesCmd().exec()
        val exists = images.any { image ->
            image.repoTags.any { it == tag }
        }
        if (exists) {
            return
        }
        logger.info("pulling image: $tag")
        val elapsedTime = measureTimeMillis {
            val result = dockerClient
                .pullImageCmd(tag)
                .exec(PullImageResultCallback())
                .awaitCompletion(DEFAULT_PULL_IMAGE_DURATION, TimeUnit.MILLISECONDS)
            if (!result) {
                throw SystemErrorException(CommonMessageCode.SYSTEM_ERROR, "image $tag pull failed")
            }
        }
        logger.info("image $tag pulled, elapse: $elapsedTime")
    }

    /**
     * 创建容器执行扫描
     * @param workDir 工作目录,将挂载到容器中
     * @param task 扫描任务
     *
     * @return true 扫描成功， false 扫描失败
     */
    private fun doScan(workDir: File, task: ScanExecutorTask): SubScanTaskStatus {
        require(task.scanner is ArrowheadScanner)
        val containerConfig = task.scanner.container
        pullImage(containerConfig.image)

        val bind = Volume(containerConfig.workDir)
        val binds = Binds(Bind(workDir.absolutePath, bind))
        val containerId = dockerClient.createContainerCmd(containerConfig.image)
            .withHostConfig(HostConfig().withBinds(binds))
            .withCmd(containerConfig.args)
            .withTty(true)
            .withStdinOpen(true)
            .exec().id
        logger.info(logMsg(task, "run container instance Id [$workDir, $containerId]"))
        try {
            dockerClient.startContainerCmd(containerId).exec()
            val resultCallback = WaitContainerResultCallback()
            dockerClient.waitContainerCmd(containerId).exec(resultCallback)
            val result = resultCallback.awaitCompletion(task.scanner.maxScanDuration, TimeUnit.MILLISECONDS)
            logger.info(logMsg(task, "task docker run result[$result], [$workDir, $containerId]"))
            if (!result) {
                return SubScanTaskStatus.TIMEOUT
            }
            return scanStatus(task, workDir)
        } finally {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec()
        }
    }

    /**
     * 解析arrowhead输出日志，判断扫描结果
     */
    private fun scanStatus(task: ScanExecutorTask, workDir: File): SubScanTaskStatus {
        val logFile = File(workDir, RESULT_FILE_NAME_LOG)
        if (!logFile.exists()) {
            logger.info(logMsg(task, "arrowhead log file not exists"))
            return SubScanTaskStatus.FAILED
        }

        val lastLineLog = logFile.readLines().lastOrNull() ?: return SubScanTaskStatus.FAILED
        if (lastLineLog.trimEnd().endsWith("Done")) {
            return SubScanTaskStatus.SUCCESS
        }
        logger.info(logMsg(task, "scan failed: $lastLineLog"))

        return SubScanTaskStatus.FAILED
    }

    /**
     * 解析扫描结果
     */
    private fun result(
        outputDir: File,
        scanStatus: SubScanTaskStatus
    ): ArrowheadScanExecutorResult {

        val cveMap = HashMap<String, CveSecItem>()
        readJsonString<List<CveSecItem>>(File(outputDir, RESULT_FILE_NAME_CVE_SEC_ITEMS))
            ?.forEach {
                // 按（组件-POC_ID）对漏洞去重
                // POC_ID为arrowhead使用的漏洞库内部漏洞编号，与CVE_ID、CNNVD_ID、CNVD_ID一一对应
                val cveSecItem = cveMap.getOrPut("${it.component}-${it.pocId}") { CveSecItem.normalize(it) }
                cveSecItem.versions.add(cveSecItem.version)
            }
        val cveSecItems = cveMap.values.toList()

        val applicationItems =
            readJsonString<List<ApplicationItem>>(File(outputDir, RESULT_FILE_NAME_APPLICATION_ITEMS))
                ?.map { ApplicationItem.normalize(it) }
                ?: emptyList()

        val checkSecItems = emptyList<CheckSecItem>()
        val sensitiveItems = emptyList<SensitiveItem>()

        return ArrowheadScanExecutorResult(
            scanStatus = scanStatus.name,
            overview = overview(applicationItems, sensitiveItems, cveSecItems),
            checkSecItems = checkSecItems,
            applicationItems = applicationItems,
            sensitiveItems = sensitiveItems,
            cveSecItems = cveSecItems
        )
    }

    private fun overview(
        applicationItems: List<ApplicationItem>,
        sensitiveItems: List<SensitiveItem>,
        cveSecItems: List<CveSecItem>
    ): Map<String, Any?> {
        val overview = HashMap<String, Long>()

        // license risk
        applicationItems.forEach {
            it.license?.let { license ->
                val overviewKey = overviewKeyOfLicenseRisk(license.risk)
                overview[overviewKey] = overview.getOrDefault(overviewKey, 0L) + 1L
            }
        }

        // sensitive count
        sensitiveItems.forEach {
            val overviewKey = overviewKeyOfSensitive(it.type)
            overview[overviewKey] = overview.getOrDefault(overviewKey, 0L) + 1L
        }

        // cve count
        cveSecItems.forEach {
            val overviewKey = overviewKeyOfCve(it.cvssRank)
            overview[overviewKey] = overview.getOrDefault(overviewKey, 0L) + 1L
        }

        return overview
    }

    private inline fun <reified T> readJsonString(file: File): T? {
        return if (file.exists()) {
            file.inputStream().use { it.readJsonString<T>() }
        } else {
            null
        }
    }

    private fun logMsg(task: ScanExecutorTask, msg: String) = with(task) {
        "$msg, parentTaskId[$parentTaskId], subTaskId[$taskId], sha256[$sha256], scanner[${scanner.name}]"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArrowheadScanExecutor::class.java)

        /**
         * 扫描器配置文件路径
         */
        private const val CONFIG_FILE_TEMPLATE_CLASS_PATH = "classpath:standalone.toml"

        // arrowhead配置文件模板key
        private const val TEMPLATE_KEY_INPUT_FILE = "inputFile"
        private const val TEMPLATE_KEY_OUTPUT_DIR = "outputDir"
        private const val TEMPLATE_KEY_LOG_FILE = "logFile"
        private const val TEMPLATE_KEY_KNOWLEDGE_BASE_SECRET_ID = "knowledgeBaseSecretId"
        private const val TEMPLATE_KEY_KNOWLEDGE_BASE_SECRET_KEY = "knowledgeBaseSecretKey"
        private const val TEMPLATE_KEY_KNOWLEDGE_BASE_ENDPOINT = "knowledgeBaseEndpoint"

        // arrowhead输出日志路径
        private const val RESULT_FILE_NAME_LOG = "sysauditor.log"

        // arrowhead扫描结果文件名
        /**
         * 证书扫描结果文件名
         */
        private const val RESULT_FILE_NAME_APPLICATION_ITEMS = "application_items.json"

        /**
         * CVE扫描结果文件名
         */
        private const val RESULT_FILE_NAME_CVE_SEC_ITEMS = "cvesec_items.json"

        /**
         * 拉取镜像最大时间
         */
        private const val DEFAULT_PULL_IMAGE_DURATION = 15 * 60 * 1000L
    }
}
