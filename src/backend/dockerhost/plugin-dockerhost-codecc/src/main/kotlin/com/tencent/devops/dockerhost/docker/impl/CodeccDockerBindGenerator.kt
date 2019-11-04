package com.tencent.devops.dockerhost.docker.impl

import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.docker.DockerBindGenerator
import com.tencent.devops.dockerhost.docker.annotation.BindGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@BindGenerator(description = "Docker上Codecc用到的Bind生成器")
@Component
class CodeccDockerBindGenerator @Autowired constructor(private val dockerHostConfig: DockerHostConfig) :
    DockerBindGenerator {

    override fun generateBinds(dockerHostBuildInfo: DockerHostBuildInfo): List<Bind> {
        return listOf(Bind(dockerHostConfig.hostPathCodecc, Volume(dockerHostConfig.volumeCodecc), AccessMode.ro))
    }
}