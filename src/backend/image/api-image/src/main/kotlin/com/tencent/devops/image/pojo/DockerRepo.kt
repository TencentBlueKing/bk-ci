package com.tencent.devops.image.pojo

data class DockerRepo(
    var repo: String? = null,
    var type: String? = null,
    var repoType: String? = "",
    var name: String? = null,
    var createdBy: String? = null,
    var created: String? = null,
    var modified: String? = null,
    var modifiedBy: String? = null,
    var imagePath: String? = null,
    var desc: String? = "",
    var tags: List<DockerTag>? = null,
    var tagCount: Int? = null,
    var tagStart: Int? = null,
    var tagLimit: Int? = null,
    var downloadCount: Int? = null
)
