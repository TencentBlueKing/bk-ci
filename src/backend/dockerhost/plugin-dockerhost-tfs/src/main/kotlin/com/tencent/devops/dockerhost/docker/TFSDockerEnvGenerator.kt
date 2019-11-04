package com.tencent.devops.dockerhost.docker

import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.ENV_TSF_HOST_IP
import com.tencent.devops.dockerhost.docker.annotation.EnvGenerator
import com.tencent.devops.dockerhost.pojo.Env
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@EnvGenerator(description = "TFS相关环境变量生成")
@Component
class TFSDockerEnvGenerator : DockerEnvGenerator {

    @Value("\${tsf.ip}")
    private var tsfIp: String = ""

    override fun generateEnv(dockerHostBuildInfo: DockerHostBuildInfo): List<Env> {
        return listOf(
            Env(key = ENV_TSF_HOST_IP, value = tsfIp)
        )
    }
}