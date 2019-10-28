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
        executorService.execute(ImportImageRunner(taskId, projectId, dslContext, uploadImageTaskDao, dockerConfig.imagePrefix!!, dockerClientconfig, isBuildImage))
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
        return bkAuthProjectApi.getProjectUsers(artifactoryAuthServiceCode, projectId).contains(user)
    }
}

// fun main(args: Array<String>) {
//    println(SecurityUtil.decrypt("7Rq3q4+3wRSkYX78nrcWNw=="))
//    println(SecurityUtil.encrypt("!@#098Bcs"))
// }