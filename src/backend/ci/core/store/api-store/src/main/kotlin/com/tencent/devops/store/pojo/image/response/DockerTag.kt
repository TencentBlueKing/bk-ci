package com.tencent.devops.store.pojo.image.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "Docker标签模型")
data class DockerTag(
    @get:Schema(title = "标签")
    var tag: String? = null,
    @get:Schema(title = "所属项目")
    var projectId: String? = null,
    @get:Schema(title = "仓库")
    var repoName: String? = null,
    @get:Schema(title = "镜像名称")
    var imageName: String? = null,
    @get:Schema(title = "创建者")
    var createdBy: String? = null,
    @get:Schema(title = "创建时间")
    var created: LocalDateTime? = null,
    @get:Schema(title = "修改时间")
    var modified: LocalDateTime? = null,
    @get:Schema(title = "修改者")
    var modifiedBy: String? = null,
    @get:Schema(title = "描述")
    var desc: String? = "",
    @get:Schema(title = "大小")
    var size: Long? = null,
    @get:Schema(title = "是否已关联到store")
    var storeFlag: Boolean? = null
)