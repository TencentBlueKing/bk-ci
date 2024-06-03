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

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.constant.FileTaskStatusEnum
import com.tencent.devops.artifactory.dao.FileTaskDao
import com.tencent.devops.artifactory.pojo.CreateFileTaskReq
import com.tencent.devops.artifactory.pojo.FileTaskInfo
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.artifactory.service.FileTaskService
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.event.util.IPUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
@Suppress("ALL")
class FileTaskServiceImpl : FileTaskService {

    val threadPoolExecutor = ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS, LinkedBlockingQueue(50))

    @Value("\${artifactory.fileTask.savedir:/tmp/bkee/ci/artifactory/filetask/}")
    val basePath: String? = null

    @Value("\${artifactory.fileTask.file.expireTimeMinutes:720}")
    val fileExpireTimeMinutes: Long = 720L

    @Value("\${artifactory.fileTask.record.clear.enable:false}")
    val clearRecordEnable: Boolean = false

    @Value("\${artifactory.fileTask.record.clear.expireTimeDays:7}")
    val recordExpireTimeDays: Long = 7L

    protected val fileSeparator: String = System.getProperty("file.separator")!!

    protected val machineIp = IPUtils.getInnerIP()

    @Autowired
    lateinit var archiveFileService: ArchiveFileService

    @Autowired
    lateinit var fileTaskDao: FileTaskDao

    @Autowired
    lateinit var dslContext: DSLContext

    fun normalizeSeparator(path: String): String {
        var finalPath = path
        finalPath = finalPath.replace(Regex("/+"), fileSeparator.replace("\\", "\\\\"))
        finalPath = finalPath.replace(Regex("\\+"), fileSeparator.replace("\\", "\\\\"))
        return finalPath
    }

    fun getFileBasePath(): String {
        return normalizeSeparator(basePath!!)
    }

    override fun createFileTask(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        createFileTaskReq: CreateFileTaskReq
    ): String {
        // 1.生成taskId
        val taskId = UUIDUtil.generate()
        var path = createFileTaskReq.path
        val fileType = createFileTaskReq.fileType
        logger.info("Input=($userId,$projectId,$pipelineId,$buildId,$fileType,$path)")
        path = normalizeSeparator(path)
        // 获取文件名
        var fileName = path
        if (path.contains(fileSeparator)) {
            val index = path.lastIndexOf(fileSeparator)
            fileName = path.substring(index + 1)
        }
        logger.info("fileName=$fileName")
        val tmpDir = Paths.get(getFileBasePath(), taskId).toFile()
        if (!tmpDir.exists()) {
            tmpDir.mkdirs()
        }
        val tmpFile = Paths.get(getFileBasePath(), taskId, fileName).toFile()
        val localPath = tmpFile.absolutePath
        logger.info("localPath=$localPath")
        // 2.关联入库
        fileTaskDao.addFileTaskInfo(
            dslContext = dslContext,
            taskId = taskId,
            fileType = createFileTaskReq.fileType.name,
            filePath = createFileTaskReq.path,
            machineIp = machineIp,
            localPath = localPath,
            status = FileTaskStatusEnum.WAITING.status,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )

        // 3.下载文件到本地
        var destPath = archiveFileService.generateDestPath(
            fileType = fileType,
            projectId = projectId,
            customFilePath = path,
            pipelineId = pipelineId,
            buildId = buildId
        )
        if (archiveFileService is DiskArchiveFileServiceImpl) {
            destPath = destPath.removePrefix((archiveFileService as DiskArchiveFileServiceImpl).getBasePath())
        }
        logger.info("destPath=$destPath")
        threadPoolExecutor.submit {
            // 下载文件到本地临时目录
            fileTaskDao.updateFileTaskStatus(dslContext, taskId, FileTaskStatusEnum.DOWNLOADING.status)
            try {
                archiveFileService.downloadFile(userId, destPath, FileOutputStream(tmpFile))
                fileTaskDao.updateFileTaskStatus(dslContext, taskId, FileTaskStatusEnum.DONE.status)
            } catch (e: Exception) {
                logger.error("BKSystemErrorMonitor|downloadFile|$taskId|error=${e.message}", e)
                // 清理文件
                tmpFile.delete()
                fileTaskDao.updateFileTaskStatus(dslContext, taskId, FileTaskStatusEnum.ERROR.status)
            }
        }
        logger.info("taskId=$taskId")
        return taskId
    }

    override fun getStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): FileTaskInfo? {
        val fileTaskRecord = fileTaskDao.getFileTaskInfo(dslContext, taskId)
        return if (fileTaskRecord != null) {
            FileTaskInfo(
                id = taskId,
                status = fileTaskRecord.status,
                ip = fileTaskRecord.machineIp,
                path = fileTaskRecord.localPath
            )
        } else {
            null
        }
    }

    override fun clearFileTask(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Boolean {
        val fileTaskRecord = fileTaskDao.getFileTaskInfo(dslContext, taskId)
        return if (fileTaskRecord != null) {
            val filePath = normalizeSeparator(fileTaskRecord.localPath)
            val dirPath = filePath.substring(0, filePath.lastIndexOf(fileSeparator))
            if (File(dirPath).deleteRecursively()) {
                true
            } else {
                logger.warn("Fail to delete file dir on disk, taskId=$taskId, path=$dirPath")
                false
            }
        } else {
            logger.warn("fileTask not exist, taskId=$taskId")
            false
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")
    fun clearRecordTask() {
        logger.info("clearRecordTask start")
        if (!clearRecordEnable) {
            logger.info("clearRecordEnable=false, skip")
            return
        }
        // 多实例并发控制
        Thread.sleep((Math.random() * 10000).toLong())
        // 清理一段时间前的已完成记录
        val limit = 100
        var allCount = 0
        var successCount = 0
        var records = fileTaskDao.listHistoryFileTaskInfo(
            dslContext = dslContext,
            status = FileTaskStatusEnum.DONE.status,
            updateTime = LocalDateTime.now().minusDays(recordExpireTimeDays),
            limit = limit
        )
        while (records != null && records.size > 0) {
            val taskIds = records.map { it.taskId }.toList()
            allCount += taskIds.size
            val affectedRows = fileTaskDao.deleteFileTaskInfo(dslContext, taskIds)
            successCount += affectedRows
            if (records.size != affectedRows) {
                logger.warn("affectedRows=$affectedRows when delete fileTasks(taskIds=$taskIds)")
            }
            records = fileTaskDao.listHistoryFileTaskInfo(
                dslContext = dslContext,
                status = FileTaskStatusEnum.DONE.status,
                updateTime = LocalDateTime.now().minusDays(recordExpireTimeDays),
                limit = limit
            )
        }
        logger.info("clearRecordTask end, $successCount records deleted, ${allCount - successCount} fail")
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    fun clearFileTask() {
        logger.info("clearFileTask start")
        var successCount = 0
        var failCount = 0
        val rootDir = File(getFileBasePath())
        val taskDirs = rootDir.listFiles(FileFilter { it.isDirectory })
        // 清理机器上的文件目录
        taskDirs?.forEach {
            if (System.currentTimeMillis() - it.lastModified() > fileExpireTimeMinutes * 60 * 1000) {
                val result = it.deleteRecursively()
                if (result) {
                    successCount += 1
                } else {
                    failCount += 1
                    logger.warn("fail to clear taskDir:${it.path}")
                }
            }
        }
        logger.info("clearFileTask end, $successCount taskDirs deleted, $failCount fail")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileTaskServiceImpl::class.java)
    }
}
