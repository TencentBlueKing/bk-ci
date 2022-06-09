package com.tencent.devops.dispatch.kubernetes.utils

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.kubernetes.KubernetesDispatchType
import com.tencent.devops.dispatch.kubernetes.config.DefaultImageConfig
import com.tencent.devops.dispatch.kubernetes.pojo.Credential
import com.tencent.devops.dispatch.kubernetes.pojo.Pool
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DispatchUtils @Autowired constructor(
    private val client: Client,
    private val defaultImageConfig: DefaultImageConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DispatchUtils::class.java)
    }

    /**
     * 获取容器池
     */
    fun getPool(dispatchMessage: DispatchMessage): Pool {
        val dispatchType = dispatchMessage.dispatchType as KubernetesDispatchType
        val dockerImage = if (dispatchType.imageType == ImageType.THIRD) {
            dispatchType.dockerBuildVersion
        } else {
            when (dispatchType.dockerBuildVersion) {
                DockerVersion.TLINUX1_2.value -> {
                    defaultImageConfig.getTLinux1_2CompleteUri()
                }
                DockerVersion.TLINUX2_2.value -> {
                    defaultImageConfig.getTLinux2_2CompleteUri()
                }
                else -> {
                    defaultImageConfig.getCompleteUriByImageName(dispatchType.dockerBuildVersion)
                }
            }
        }
        logger.info(
            "${dispatchMessage.buildId}|startBuild|${dispatchMessage.id}|$dockerImage|${dispatchType.imageType}" +
                    "|${dispatchType.dockerBuildVersion}|${dispatchType.imageCode}|${dispatchType.imageVersion}" +
                    "|${dispatchType.credentialId}" +
                    "|${dispatchType.credentialProject}"
        )

        return if (dispatchType.imageType == ImageType.THIRD && !dispatchType.credentialId.isNullOrBlank()) {

            val projectId = if (dispatchType.credentialProject.isNullOrBlank()) {
                dispatchMessage.projectId
            } else {
                dispatchType.credentialProject!!
            }
            val ticketsMap = CommonUtils.getCredential(
                client = client,
                projectId = projectId,
                credentialId = dispatchType.credentialId!!,
                type = CredentialType.USERNAME_PASSWORD
            )
            val userName = ticketsMap["v1"] as String
            val password = ticketsMap["v2"] as String
            Pool(
                container = dockerImage,
                credential = Credential(
                    user = userName,
                    password = password
                )
            )
        } else {
            Pool(
                container = dockerImage,
                credential = null
            )
        }
    }
}
