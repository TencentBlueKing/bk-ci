package com.tencent.devops.dockerhost.docker.impl

import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.docker.DockerEnvGenerator
import com.tencent.devops.dockerhost.pojo.Env
import com.tencent.devops.dockerhost.docker.annotation.EnvGenerator
import com.tencent.devops.dockerhost.utils.BK_DISTCC_LOCAL_IP
import org.springframework.stereotype.Component

@EnvGenerator(description = "Docker用到的Distcc环境变量生成器")
@Component
class DistccDockerEnvGenerator : DockerEnvGenerator {
    override fun generateEnv(dockerHostBuildInfo: DockerHostBuildInfo): List<Env> {
        return listOf(
            Env(
                key = BK_DISTCC_LOCAL_IP,
                value = CommonUtils.getInnerIP()
            )
        )
    }
}