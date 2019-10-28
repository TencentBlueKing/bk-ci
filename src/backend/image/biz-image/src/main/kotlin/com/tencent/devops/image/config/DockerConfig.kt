package com.tencent.devops.image.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DockerConfig {
    @Value("\${image.dockerCli.dockerHost}")
    val dockerHost: String? = null

    @Value("\${image.dockerCli.dockerConfig}")
    var dockerConfig: String? = null

    @Value("\${image.dockerCli.apiVersion:1.23}")
    var apiVersion: String? = null

    @Value("\${image.dockerCli.registryUrl}")
    var registryUrl: String? = null

    @Value("\${image.dockerCli.registryUsername}")
    var registryUsername: String? = null

    @Value("\${image.dockerCli.registryPassword}")
    var registryPassword: String? = null

    @Value("\${image.dockerCli.imagePrefix}")
    var imagePrefix: String? = null
}