/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.image.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.ArtifactoryAuthServiceCode
import com.tencent.devops.image.config.DockerConfig
import com.tencent.devops.image.dao.UploadImageTaskDao
import com.tencent.devops.image.pojo.UploadImageTask
import com.tencent.devops.image.pojo.enums.TaskStatus
import com.tencent.devops.model.image.tables.records.TUploadImageTaskRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.Executors

@Service
class ImportImageService @Autowired constructor(
    private val bkAuthProjectApi: AuthProjectApi,
    private val dslContext: DSLContext,
    private val uploadImageTaskDao: UploadImageTaskDao,
    private val dockerConfig: DockerConfig,
    private val artifactoryAuthServiceCode: ArtifactoryAuthServiceCode
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ImportImageService::class.java)
        private val executorService = Executors.newFixedThreadPool(10)
    }

    private val dockerClientconfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(dockerConfig.dockerHost)
        .withDockerConfig(dockerConfig.dockerConfig)
        .withApiVersion(dockerConfig.apiVersion)
        .withRegistryUrl(dockerConfig.registryUrl)
        .withRegistryUsername(dockerConfig.registryUsername)
        .withRegistryPassword(
            try {
                SecurityUtil.decrypt(dockerConfig.registryPassword!!)
            } catch (ignored: Throwable) {
                dockerConfig.registryPassword!!
            }
        )
        .build()

    fun importImage(projectId: String, user: String, taskId: String, isBuildImage: Boolean): UploadImageTask {
        logger.info("import image, projectId $projectId, user $user, taskId $taskId, isBuildImage $isBuildImage")
        val now = LocalDateTime.now()
        val record = TUploadImageTaskRecord().apply {
            this.taskId = taskId
            this.projectId = projectId
            operator = user
            createdTime = now
            updatedTime = now
            taskStatus = TaskStatus.RUNNING.name
            imageData = "[]"
        }

        uploadImageTaskDao.add(dslContext, record)
        executorService.execute(ImportImageRunner(taskId = taskId,
            projectId = projectId,
            dslContext = dslContext,
            uploadImageTaskDao = uploadImageTaskDao,
            imagePrefix = dockerConfig.imagePrefix!!,
            dockerClientConfig = dockerClientconfig,
            isBuildImage = isBuildImage))
        return UploadImageTask(
            taskId = taskId,
            projectId = projectId,
            operator = user,
            createdTime = now.timestamp(),
            updatedTime = now.timestamp(),
            taskStatus = TaskStatus.RUNNING.name,
            taskMessage = "",
            imageData = listOf()
        )
    }

    fun queryTask(taskId: String, projectId: String): UploadImageTask? {
        val task = uploadImageTaskDao.get(dslContext, taskId, projectId) ?: return null
        return UploadImageTask(
            taskId = task.taskId,
            projectId = task.projectId,
            operator = task.operator,
            createdTime = task.createdTime.timestamp(),
            updatedTime = task.updatedTime.timestamp(),
            taskStatus = task.taskStatus,
            taskMessage = task.taskMessage ?: "",
            imageData = jacksonObjectMapper().readValue(task.imageData)
        )
    }

    fun checkDeployPermission(projectId: String, user: String): Boolean {
        return bkAuthProjectApi.checkProjectUser(user, artifactoryAuthServiceCode, projectId)
    }
}
