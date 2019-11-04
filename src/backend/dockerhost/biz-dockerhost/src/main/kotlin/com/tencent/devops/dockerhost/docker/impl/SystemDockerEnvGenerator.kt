package com.tencent.devops.dockerhost.docker.impl

import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.docker.DockerEnvGenerator
import com.tencent.devops.dockerhost.docker.annotation.EnvGenerator
import com.tencent.devops.dockerhost.pojo.Env
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_PORT
import com.tencent.devops.dockerhost.utils.ENV_KEY_AGENT_ID
import com.tencent.devops.dockerhost.utils.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dockerhost.utils.ENV_KEY_GATEWAY
import com.tencent.devops.dockerhost.utils.ENV_KEY_PROJECT_ID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@EnvGenerator(description = "默认Docker环境变量生成器")
@Component
class SystemDockerEnvGenerator @Autowired constructor(val commonConfig: CommonConfig) : DockerEnvGenerator {

    override fun generateEnv(dockerHostBuildInfo: DockerHostBuildInfo): List<Env> {

        /*  refactor from DockerHostBuildService
            "$ENV_KEY_PROJECT_ID=${dockerBuildInfo.projectId}",
            "$ENV_KEY_AGENT_ID=${dockerBuildInfo.agentId}",
            "$ENV_KEY_AGENT_SECRET_KEY=${dockerBuildInfo.secretKey}",
            "$ENV_KEY_GATEWAY=$gateway",
            "TERM=xterm-256color",
            "$ENV_DOCKER_HOST_IP=${CommonUtils.getInnerIP()}",
            "$ENV_DOCKER_HOST_PORT=${commonConfig.serverPort}",
            "$BK_DISTCC_LOCAL_IP=${CommonUtils.getInnerIP()}", move to ----> DistccDockerEnvGenerator
         */

        val hostIp = CommonUtils.getInnerIP()
        val gateway = System.getProperty("devops.gateway", commonConfig.devopsBuildGateway!!)
        return listOf(
            Env(key = ENV_KEY_PROJECT_ID, value = dockerHostBuildInfo.projectId),
            Env(key = ENV_KEY_AGENT_ID, value = dockerHostBuildInfo.agentId),
            Env(key = ENV_KEY_AGENT_SECRET_KEY, value = dockerHostBuildInfo.secretKey),
            Env(key = "TERM", value = "xterm-256color"),
            Env(key = ENV_KEY_GATEWAY, value = gateway),
            Env(key = ENV_DOCKER_HOST_IP, value = hostIp),
            Env(key = ENV_DOCKER_HOST_PORT, value = commonConfig.serverPort.toString())
        )
    }
}