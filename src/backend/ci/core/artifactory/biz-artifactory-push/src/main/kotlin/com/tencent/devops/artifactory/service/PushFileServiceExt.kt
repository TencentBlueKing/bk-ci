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

package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.FUSH_FILE_VALIDATE_FAIL
import com.tencent.devops.artifactory.pojo.EnvSet
import com.tencent.devops.artifactory.pojo.FastPushFileRequest
import com.tencent.devops.artifactory.pojo.FileResourceInfo
import com.tencent.devops.artifactory.pojo.PushStatus
import com.tencent.devops.artifactory.pojo.RemoteResourceInfo
import com.tencent.devops.artifactory.pojo.vo.PushResultVO
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.web.utils.I18nUtil
import java.io.File
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service@Suppress("ALL")
class PushFileServiceExt @Autowired constructor(
    private val authPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val envService: EnvServiceExt,
    private val jobService: JobServiceExt,
    private val fileService: FileService
) : PushFileService {

    val jobInstanceMap = mutableMapOf<String, List<String>>()

    override fun pushFileByJob(
        userId: String,
        pushResourceInfo: RemoteResourceInfo,
        fileResourceInfo: FileResourceInfo
    ): Result<Long> {
        logger.info("pushFileByJob|$userId|pushResourceInfo[$pushResourceInfo]|fileResourceInfo[$fileResourceInfo]")
        var jobInstanceId = 0L
        val projectId = fileResourceInfo.projectId
        val pipelineId = fileResourceInfo.pipelineId
        val buildId = fileResourceInfo.buildId
        // 校验用户是否有权限下载文件
        val validatePermission = authPermissionApi.validateUserResourcePermission(
            userId,
            pipelineAuthServiceCode,
            AuthResourceType.PIPELINE_DEFAULT,
            projectId,
            pipelineId,
            AuthPermission.DOWNLOAD
        )

        if (!validatePermission) {
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    messageCode = FUSH_FILE_VALIDATE_FAIL,
                    params = null,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        // 解析目标机器，需校验用户是否有权限操作目标机
        val envSet = envService.parsingAndValidateEnv(pushResourceInfo, userId, projectId)
        try {
            // 下载目标文件到本地
            val downloadFiles = fileService.downloadFileTolocal(
                userId, projectId, pipelineId, buildId, fileResourceInfo.fileName,
                fileResourceInfo.isCustom!!
            ).toMutableList()
            val filePath = mutableListOf<String>()
            downloadFiles.forEach {
                filePath.add(it.absolutePath)
            }

            val localEnvSet = EnvSet(emptyList(), emptyList(), envService.buildIpDto())

            val fileSource = FastPushFileRequest.FileSource(
                files = filePath,
                envSet = localEnvSet,
                account = pushResourceInfo.account
            )

            val fastPushFileRequest = FastPushFileRequest(
                userId = userId,
                envSet = envSet,
                fileSources = listOf(fileSource),
                fileTargetPath = pushResourceInfo.targetPath,
                account = pushResourceInfo.account,
                timeout = pushResourceInfo.timeout ?: 600
            )
            // 执行分发动作
            jobInstanceId = jobService.fastPushFileDevops(fastPushFileRequest, projectId)
            cachePushMsg(jobInstanceId, downloadFiles)
        } catch (e: Exception) {
            logger.warn("push file by job fail: $e")
            clearTmpFile(jobInstanceId)
            throw e
        }

        return Result(jobInstanceId)
    }

    fun checkStatus(
        userId: String,
        projectId: String,
        taskInstanceId: Long
    ): Result<PushResultVO> {
        val result = jobService.getTaskResult(projectId, taskInstanceId, userId)
        if (result.isFinish) {
            return if (result.success) {
                Result(
                    PushResultVO(
                        status = PushStatus.SUCCESS,
                        msg = result.msg
                    )
                )
            } else {
                Result(
                    PushResultVO(
                        status = PushStatus.FAIL, msg = result.msg
                    )
                )
            }
        } else {
            return Result(
                PushResultVO(
                    status = PushStatus.RUNNING,
                    msg = result.msg
                )
            )
        }
    }

    fun clearTmpFile(jobId: Long) {
        val fileList = jobInstanceMap[jobId.toString()]
        if (fileList != null) {
            if (fileList.isNotEmpty()) {
                clearTmpFile(fileList)
            }
        }
    }

    private fun clearTmpFile(files: List<String>) {
        logger.info("start clear TmpFile, tmpFile count:${files.size}")
        val startTime = System.currentTimeMillis()
        files.forEach { file ->
            val tmpFile = File(file)
            if (tmpFile.exists()) {
                if (tmpFile.isDirectory) {
                    logger.info("[ delete temp dir $file : ${tmpFile.deleteRecursively()}")
                } else {
                    logger.info("[delete temp file $file : ${tmpFile.delete()}")
                }
            }
        }
        logger.info("delete temp file time：${System.currentTimeMillis() - startTime}")
    }

    private fun cachePushMsg(jobId: Long, files: List<File>) {
        val pathList = mutableListOf<String>()
        files.forEach {
            pathList.add(it.absolutePath)
        }
        jobInstanceMap[jobId.toString()] = pathList
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PushFileServiceExt::class.java)
    }
}
