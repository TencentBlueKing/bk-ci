package com.tencent.devops.dockerhost.services.image

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.exception.DockerClientException
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.PushResponseItem
import com.github.dockerjava.api.model.ResponseItem
import com.tencent.devops.common.api.constant.BK_PUSH_IMAGE
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.services.Handler
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ImagePushHandler(
    dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
) : Handler<ImageHandlerContext>(dockerHostConfig, dockerHostBuildApi) {
    override fun handlerRequest(handlerContext: ImageHandlerContext) {
        with(handlerContext) {
            imageTagSet.parallelStream().forEach {
                logger.info("[$buildId]|[$vmSeqId] Build image success, now push to repo, image name and tag: $it")
                val authConfig = AuthConfig()
                    .withUsername(dockerBuildParam.userName)
                    .withPassword(dockerBuildParam.password)
                    .withRegistryAddress(dockerBuildParam.repoAddr)

                dockerClient.pushImageCmd(it)
                    .withAuthConfig(authConfig)
                    .exec(MyPushImageResultCallback(buildId, pipelineTaskId, dockerHostBuildApi))
                    .awaitCompletion(20, TimeUnit.MINUTES)
            }

            nextHandler.get()?.handlerRequest(this)
        }
    }

    inner class MyPushImageResultCallback internal constructor(
        private val buildId: String,
        private val elementId: String?,
        private val dockerHostBuildApi: DockerHostBuildResourceApi
    ) : ResultCallback.Adapter<PushResponseItem>() {
        private var latestItem: PushResponseItem? = null

        private val totalList = mutableListOf<Long>()
        private val step = mutableMapOf<Int, Long>()
        override fun onNext(item: PushResponseItem?) {
            val text = item?.progressDetail
            if (canPrintLog(text)) {
                val lays = if (!totalList.contains(text!!.total!!)) {
                    totalList.add(text.total!!)
                    totalList.size + 1
                } else {
                    totalList.indexOf(text.total!!) + 1
                }
                var currentProgress = text.current!! * 100 / text.total!!
                if (currentProgress > 100) {
                    currentProgress = 100
                }
                if (currentProgress >= (step[lays]?.plus(25) ?: 5)) {
                    dockerHostBuildApi.postLog(
                        buildId,
                        false,
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_PUSH_IMAGE,
                            params = arrayOf("$lays", "$currentProgress"),
                            language = I18nUtil.getDefaultLocaleLanguage()
                        ),
                        elementId
                    )
                    step[lays] = currentProgress
                }
            }

            if (item != null && item.errorDetail == null) {
                dockerHostBuildApi.postLog(
                    buildId,
                    false,
                    item.status ?: "",
                    elementId
                )
            }

            this.latestItem = item
            super.onNext(item)
        }

        override fun throwFirstError() {
            super.throwFirstError()
            if (latestItem == null) {
                throw DockerClientException("Could not push image")
            } else if (latestItem?.isErrorIndicated == true) {
                throw DockerClientException("Could not push image: " + latestItem!!.errorDetail?.message)
            }
        }

        private fun canPrintLog(text: ResponseItem.ProgressDetail?): Boolean {
            if (text == null || text.current == null) {
                return false
            }

            if (text.total == null || text.total == 0L) {
                return false
            }

            return true
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImagePushHandler::class.java)
    }
}
