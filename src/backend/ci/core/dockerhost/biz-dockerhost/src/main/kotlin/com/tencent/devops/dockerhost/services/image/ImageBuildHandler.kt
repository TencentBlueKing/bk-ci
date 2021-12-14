package com.tencent.devops.dockerhost.services.image

import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.AuthConfigurations
import com.github.dockerjava.api.model.BuildResponseItem
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.services.Handler
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class ImageBuildHandler(
    private val dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
) : Handler<ImageHandlerContext>(dockerHostConfig, dockerHostBuildApi) {
    override fun handlerRequest(handlerContext: ImageHandlerContext) {
        with(handlerContext) {
            val authConfigurations = AuthConfigurations()
            val ticket = dockerBuildParam.ticket
            val args = dockerBuildParam.args
            ticket.forEach {
                val baseConfig = AuthConfig()
                    .withUsername(it.second)
                    .withPassword(it.third)
                    .withRegistryAddress(it.first)
                authConfigurations.addConfig(baseConfig)
            }

            val workspace = getWorkspace(pipelineId, vmSeqId, dockerBuildParam.poolNo ?: "0")
            val buildDir = Paths.get(workspace + dockerBuildParam.buildDir).normalize().toString()
            val dockerfilePath = Paths.get(workspace + dockerBuildParam.dockerFile).normalize().toString()
            var baseDirectory = File(buildDir)
            var dockerfile = File(dockerfilePath)

            if (!baseDirectory.exists()) {
                baseDirectory = File(Paths.get(workspace + "upper/" + dockerBuildParam.buildDir)
                    .normalize().toString())
                dockerfile = File(Paths.get(workspace + "upper/" + dockerBuildParam.dockerFile)
                    .normalize().toString())
            }

            val imageNameTagSet = mutableSetOf<String>()
            if (dockerBuildParam.imageTagList.isNotEmpty()) {
                dockerBuildParam.imageTagList.forEach {
                    imageNameTagSet.add(getImageNameWithTag(
                        repoAddr = dockerBuildParam.repoAddr,
                        projectId = projectId,
                        imageName = dockerBuildParam.imageName,
                        imageTag = it,
                        outer = outer
                    ))
                }
            } else {
                imageNameTagSet.add(getImageNameWithTag(
                    repoAddr = dockerBuildParam.repoAddr,
                    projectId = projectId,
                    imageName = dockerBuildParam.imageName,
                    imageTag = dockerBuildParam.imageTag,
                    outer = outer
                ))
            }

            this.imageTagSet = imageNameTagSet

            logger.info("[$buildId]|[$vmSeqId] Build docker image, workspace: $workspace, " +
                    "buildDir:$buildDir, dockerfile: $dockerfilePath")
            logger.info("[$buildId]|[$vmSeqId] Build docker image, imageNameTag: $imageNameTagSet")
            val step = dockerClient.buildImageCmd().withNoCache(true)
                .withPull(true)
                .withBuildAuthConfigs(authConfigurations)
                .withBaseDirectory(baseDirectory)
                .withDockerfile(dockerfile)
                .withTags(imageNameTagSet)
            args.map { it.trim().split("=") }.forEach {
                step.withBuildArg(it.first(), it.last())
            }
            val imageId = step.exec(MyBuildImageResultCallback(buildId, pipelineTaskId, dockerHostBuildApi))
                .awaitImageId(60, TimeUnit.MINUTES)
            this.imageId = imageId
            logger.info("[$buildId]|[$vmSeqId] Build docker image mageId: $imageId")

            nextHandler.get()?.handlerRequest(this)
        }
    }

    private fun getWorkspace(pipelineId: String, vmSeqId: Int, poolNo: String): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/${getTailPath(vmSeqId, poolNo.toInt())}/"
    }

    private fun getTailPath(vmSeqId: Int, poolNo: Int): String {
        return if (poolNo > 1) {
            "$vmSeqId" + "_$poolNo"
        } else {
            vmSeqId.toString()
        }
    }

    private fun getImageNameWithTag(
        repoAddr: String,
        projectId: String,
        imageName: String,
        imageTag: String,
        outer: Boolean = false
    ): String {
        return if (outer) {
            "$repoAddr/$imageName:$imageTag"
        } else {
            "$repoAddr/paas/$projectId/$imageName:$imageTag"
        }
    }

    inner class MyBuildImageResultCallback internal constructor(
        private val buildId: String,
        private val elementId: String?,
        private val dockerHostBuildApi: DockerHostBuildResourceApi
    ) : BuildImageResultCallback() {
        override fun onNext(item: BuildResponseItem?) {
            val text = item?.stream
            if (null != text) {
                dockerHostBuildApi.postLog(
                    buildId,
                    false,
                    StringUtils.removeEnd(text, "\n"),
                    elementId
                )
            }

            super.onNext(item)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImageHandlerContext::class.java)
    }
}
