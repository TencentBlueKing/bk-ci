package com.tencent.devops.store.pojo.image.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "镜像信息模型")
data class DockerRepoList(
    @get:Schema(title = "仓库url")
    var repoUrl: String? = null,
    @get:Schema(title = "仓库")
    var repo: String? = null,
    @get:Schema(title = "类型")
    var type: String? = null,
    @get:Schema(title = "仓库类型")
    var repoType: String? = "",
    @get:Schema(title = "名称")
    var name: String? = null,
    @get:Schema(title = "创建者")
    var createdBy: String? = null,
    @get:Schema(title = "创建时间")
    var created: String? = null,
    @get:Schema(title = "修改时间")
    var modified: String? = null,
    @get:Schema(title = "修改者")
    var modifiedBy: String? = null,
    @get:Schema(title = "镜像路径")
    var imagePath: String? = null,
    @get:Schema(title = "描述")
    var desc: String? = "",
    @get:Schema(title = "标签")
    var tags: List<DockerTag>? = null,
    @get:Schema(title = "标签数量")
    var tagCount: Int? = null,
    @get:Schema(title = "开始索引")
    var tagStart: Int? = null,
    @get:Schema(title = "页大小")
    var tagLimit: Int? = null,
    @get:Schema(title = "下载次数")
    var downloadCount: Int? = null
)