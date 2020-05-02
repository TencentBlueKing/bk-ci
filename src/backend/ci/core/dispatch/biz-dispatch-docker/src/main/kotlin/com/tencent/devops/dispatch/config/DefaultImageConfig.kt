package com.tencent.devops.dispatch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DefaultImageConfig {
    // 编译环境，末尾不含bkdevops
    @Value("\${dispatch.dockerBuildImagePrefix:#{null}}")
    val dockerBuildImagePrefix: String? = null

    @Value("\${dispatch.imageTLinux1_2:/bkdevops/docker-builder1.2:v1}")
    val imageTLinux1_2: String? = null

    @Value("\${dispatch.imageTLinux2_2:/bkdevops/docker-builder2.2:v1}")
    val imageTLinux2_2: String? = null

    // 无编译环境，末尾需含bkdevops
    @Value("\${dispatch.dockerBuildLessImagePrefix:#{null}}")
    var dockerBuildLessImagePrefix: String? = null

    @Value("\${dispatch.imageBuildLessTLinux1_2:/docker-build-less1.2:v1}")
    val imageBuildLessTLinux1_2: String? = null

    @Value("\${dispatch.imageBuildLessTLinux2_2:/docker-build-less2.2:v1}")
    val imageBuildLessTLinux2_2: String? = null

}