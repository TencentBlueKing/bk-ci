package com.tencent.devops.image.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PushImageResultCallback
import com.tencent.devops.image.dao.UploadImageTaskDao
import com.tencent.devops.image.pojo.DockerImage
import com.tencent.devops.image.pojo.enums.TaskStatus
import com.tencent.devops.image.utils.FileStoreUtils
import com.tencent.devops.image.utils.ImageFileUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.io.File

class ImportImageRunner constructor(
    private val taskId: String,
    private val projectId: String,
    private val dslContext: DSLContext,
    private val uploadImageTaskDao: UploadImageTaskDao,
    private val imagePrefix: String,
    private val dockerClientConfig: DefaultDockerClientConfig,
    private val isBuildImage: Boolean
) : Runnable {
    companion object {
        private val logger = LoggerFactory.getLogger(ImportImageRunner::class.java)
    }

    override fun run() {
        val allImages = mutableListOf<DockerImage>()
        val imageFilePath = FileStoreUtils.getFullFileName(taskId)
        try {
            logger.info("parse image info from file: $imageFilePath")
            val images = ImageFileUtils.parseImageMeta(imageFilePath)
            logger.info("parse image info done, images: $images")

            val reTagedImages = images.map {
                val newImageName = buildRetagedImageName(it.imageShortName, isBuildImage)
                DockerImage(newImageName, it.imageTag, it.imageShortName)
            }

            logger.info("load image from file: $imageFilePath")
            loadImages(imageFilePath)
            logger.info("load image done: $imageFilePath")

            logger.info("retag image(s)")
            reTagImages(images, isBuildImage)
            logger.info("retag image(s) done")

            logger.info("push image(s)")
            pushImages(reTagedImages)
            logger.info("push image(s) done")

            uploadImageTaskDao.update(dslContext,
                taskId,
                TaskStatus.SUCCESS.name,
                "",
                ObjectMapper().writeValueAsString(reTagedImages)
            )

            allImages.addAll(images)
            allImages.addAll(reTagedImages)
            // deleteImages(allImages)
            // FileStoreUtils.deleteFile(imageFilePath)
        } catch (ex: Exception) {
            uploadImageTaskDao.update(dslContext,
                taskId,
                TaskStatus.FAILED.name,
                ex.message ?: "",
                "[]"
            )
            logger.error("run upload image task failed", ex)
        } finally {
            deleteImages(allImages)
            FileStoreUtils.deleteFile(imageFilePath)
        }
    }

    private fun loadImages(imageFilePath: String) {
        try {
            getClient().loadImageCmd(File(imageFilePath).inputStream()).exec()
        } catch (e: Throwable) {
            logger.error("load image error", e)
            throw RuntimeException("load image error")
        }
    }

    private fun getClient(): DockerClient {
        return DockerClientBuilder.getInstance(dockerClientConfig).build()
    }

    private fun reTagImages(images: List<DockerImage>, isBuildImage: Boolean) {
        images.forEach {
            reTagImage(it, isBuildImage)
        }
    }

    private fun buildRetagedImageName(imageShortName: String, isBuildImage: Boolean): String {
        return if (isBuildImage) {
            "$imagePrefix/paas/bkdevops/$projectId/$imageShortName"
        } else {
            "$imagePrefix/paas/$projectId/$imageShortName"
        }
    }

    private fun reTagImage(image: DockerImage, isBuildImage: Boolean) {
        val newImageRepo = buildRetagedImageName(image.imageShortName, isBuildImage)
        try {
            val fromImageFullName = "${image.imageName}:${image.imageTag}"
            logger.info("tag image from $fromImageFullName to $newImageRepo:${image.imageTag}")
            getClient().tagImageCmd(fromImageFullName, newImageRepo, image.imageTag).withForce().exec()
        } catch (e: Throwable) {
            logger.error("retag image error", e)
            throw RuntimeException("retag image error")
        }
    }

    private fun pushImages(images: List<DockerImage>) {
        images.forEach {
            pushImage(it)
        }
    }

    private fun pushImage(image: DockerImage) {
        try {
            val imageFullName = "${image.imageName}:${image.imageTag}"
            logger.info("push image: $imageFullName")
            val authConfig = AuthConfig()
                    .withUsername(dockerClientConfig.registryUsername)
                    .withPassword(dockerClientConfig.registryPassword)
                    .withRegistryAddress(dockerClientConfig.registryUrl)
            getClient().pushImageCmd(imageFullName).withAuthConfig(authConfig).exec(PushImageResultCallback()).awaitSuccess()
            logger.info("push image done")
        } catch (e: Throwable) {
            logger.error("push image error", e)
            throw RuntimeException("push image error")
        }
    }

    private fun deleteImages(images: List<DockerImage>) {
        images.forEach {
            deleteImage(it)
        }
    }

    private fun deleteImage(image: DockerImage) {
        try {
            val imageFullName = "$imagePrefix/${image.imageName}/${image.imageTag}"
            logger.info("delete image: $imageFullName")
            getClient().removeImageCmd(imageFullName).exec()
        } catch (e: NotFoundException) {
            logger.warn("delete not exist")
        } catch (e: Throwable) {
            logger.error("remove image error", e)
//            throw RuntimeException("remove image error")
        }
    }
}
