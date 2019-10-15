package com.tencent.devops.image.pojo

data class ImagePageData(
    val imageList: List<DockerRepo>,
    val start: Int,
    val limit: Int,
    val total: Int
)