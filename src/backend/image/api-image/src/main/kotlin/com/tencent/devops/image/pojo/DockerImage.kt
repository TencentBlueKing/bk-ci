package com.tencent.devops.image.pojo

data class DockerImage(
    val imageName: String,
    val imageTag: String,
    val imageShortName: String
)