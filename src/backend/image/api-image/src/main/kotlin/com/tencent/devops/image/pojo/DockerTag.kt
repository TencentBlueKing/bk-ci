package com.tencent.devops.image.pojo

data class DockerTag(
    var tag: String? = null,
    var repo: String? = null,
    var image: String? = null,
    var createdBy: String? = null,
    var created: String? = null,
    var modified: String? = null,
    var modifiedBy: String? = null,
    var desc: String? = "",
    var size: String? = null,
    var artifactorys: List<String>? = null
)
