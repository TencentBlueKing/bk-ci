package com.tencent.devops.dockerhost.docker.impl

import com.github.dockerjava.api.model.Volume
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.docker.DockerVolumeGenerator
import com.tencent.devops.dockerhost.docker.annotation.BindGenerator
import com.tencent.devops.dockerhost.docker.annotation.VolumeGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@VolumeGenerator(description = "默认Docker Volume生成器")
@Component
class SystemDockerVolumeGenerator @Autowired constructor(private val dockerHostConfig: DockerHostConfig) :
    DockerVolumeGenerator {

    override fun generateVolumes(dockerHostBuildInfo: DockerHostBuildInfo): List<Volume> {
        val volumeWs = Volume(dockerHostConfig.volumeWorkspace)
        val volumeApps = Volume(dockerHostConfig.volumeApps)
        val volumeInit = Volume(dockerHostConfig.volumeInit)
        return listOf(volumeWs, volumeApps, volumeInit)
    }
}