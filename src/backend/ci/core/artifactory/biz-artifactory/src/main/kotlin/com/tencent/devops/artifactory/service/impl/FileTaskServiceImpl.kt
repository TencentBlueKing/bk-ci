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

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.constant.FileTaskStatusEnum
import com.tencent.devops.artifactory.dao.FileTaskDao
import com.tencent.devops.artifactory.pojo.CreateFileTaskReq
import com.tencent.devops.artifactory.pojo.FileTaskInfo
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.artifactory.service.FileTaskService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.event.util.IPUtils
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Paths
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response

@Service
class FileTaskServiceImpl : FileTaskService {

    val threadPoolExecutor = ThreadPoolExecutor(50, 100, 60, TimeUnit.SECONDS, LinkedBlockingQueue(50))

    @Value("artifactory.file.task.savedir:/tmp/bkee/ci/artifactory/filetask/")
    val basePath: String? = null

    protected val fileSeparator: String = System.getProperty("file.separator")!!

    protected val machineIp = IPUtils.getInnerIP()

    @Autowired
    lateinit var archiveFileService: ArchiveFileService

    @Autowired
    lateinit var fileTaskDao: FileTaskDao

    @Autowired
    lateinit var dslContext: DSLContext

    override fun createFileTask(userId: String, projectId: String, pipelineId: String, buildId: String, createFileTaskReq: CreateFileTaskReq): String {
        // 1.生成taskId
        val taskId = UUIDUtil.generate()
        val path = createFileTaskReq.path
        val fileType = createFileTaskReq.fileType
        logger.info("Input=($userId,$projectId,$pipelineId,$buildId,$fileType,$path)")
        // 获取文件名
        var fileName = path
        if (path.contains(fileSeparator)) {
            val index = path.lastIndexOf(fileSeparator)
            fileName = path.substring(index + 1)
        }
        logger.info("fileName=$fileName")
        val tmpDir = Paths.get(basePath, taskId).toFile()
        if (!tmpDir.exists()) {
            tmpDir.mkdirs()
        }
        val tmpFile = Paths.get(basePath, taskId, fileName).toFile()
        val localPath = tmpFile.absolutePath
        logger.info("localPath=$localPath")
        // 2.关联入库
        fileTaskDao.addFileTaskInfo(dslContext, taskId, createFileTaskReq.fileType.name, createFileTaskReq.path, machineIp, localPath, FileTaskStatusEnum.WAITING.status, userId, projectId, pipelineId, buildId)

        // 3.下载文件到本地
        val destPath = archiveFileService.generateDestPath(
            fileType = fileType,
            projectId = projectId,
            customFilePath = path,
            pipelineId = pipelineId,
            buildId = buildId
        )
        logger.info("destPath=$destPath")
        // 下载文件到本地临时目录
        fileTaskDao.updateFileTaskStatus(dslContext, taskId, FileTaskStatusEnum.DOWNLOADING.status)
        archiveFileService.downloadFile(destPath.data!!, FileOutputStream(tmpFile))
        fileTaskDao.updateFileTaskStatus(dslContext, taskId, FileTaskStatusEnum.DONE.status)
        logger.info("taskId=$taskId")
        return taskId
    }

    override fun getStatus(userId: String, projectId: String, pipelineId: String, buildId: String, taskId: String): FileTaskInfo? {
        val fileTaskRecord = fileTaskDao.getFileTaskInfo(dslContext, taskId)
        if (fileTaskRecord != null) {
            return FileTaskInfo(
                id = taskId,
                status = fileTaskRecord.status,
                ip = fileTaskRecord.machineIp,
                path = fileTaskRecord.localPath
            )
        } else {
            return null
        }
    }

    override fun clearFileTask(userId: String, projectId: String, pipelineId: String, buildId: String, taskId: String): Boolean {
        val fileTaskRecord = fileTaskDao.getFileTaskInfo(dslContext, taskId)
        if (fileTaskRecord != null) {
            return File(fileTaskRecord.localPath).delete()
        } else {
            return false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileTaskServiceImpl::class.java)
    }
}
