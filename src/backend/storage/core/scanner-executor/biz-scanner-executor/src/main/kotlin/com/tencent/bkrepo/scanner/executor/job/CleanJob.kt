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

package com.tencent.bkrepo.scanner.executor.job

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.PruneType
import com.tencent.bkrepo.scanner.executor.configuration.ScannerExecutorProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files

/**
 * 清理删除失败的文件和容器任务
 */
@Component
class CleanJob(
    private val dockerClient: DockerClient,
    private val scannerExecutorProperties: ScannerExecutorProperties
) {
    @Scheduled(fixedDelay = FIXED_DELAY, initialDelay = FIXED_DELAY)
    fun clean() {
        cleanExpireFile()
        cleanContainer()
    }

    /**
     * 清理工作目录
     */
    private fun cleanExpireFile() {
        val workDir = File(scannerExecutorProperties.workDir)
        val expiredMinutes = scannerExecutorProperties.fileExpiredMinutes.toMinutes()
        val now = System.currentTimeMillis()
        workDir.walkBottomUp().forEach {
            try {
                val notModifiedMinutes = (now - it.lastModified()) / 1000L / 60L
                if (it.exists() && notModifiedMinutes > expiredMinutes) {
                    Files.deleteIfExists(it.toPath())
                }
            } catch (e: DirectoryNotEmptyException) {
                logger.warn(
                    "directory [${it.absolutePath}] is not empty[${it.listFiles()?.joinToString(",")}]"
                )
            } catch (e: Exception) {
                logger.error("delete file[${it.absolutePath}] failed", e)
            }
        }
    }

    /**
     * 清理docker相关资源
     */
    private fun cleanContainer() {
        dockerClient.pruneCmd(PruneType.CONTAINERS).exec()
        dockerClient.pruneCmd(PruneType.VOLUMES).exec()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CleanJob::class.java)
        private const val FIXED_DELAY = 15 * 60 * 1000L
    }
}
