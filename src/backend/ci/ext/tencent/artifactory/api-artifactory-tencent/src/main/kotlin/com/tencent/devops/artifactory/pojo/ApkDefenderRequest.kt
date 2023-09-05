package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("APK加固请求")
data class ApkDefenderRequest(
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @ApiModelProperty("仓库路径", required = true)
    val fullPath: String,
    @ApiModelProperty("用户列表", required = true)
    val userIds: Collection<String>,
    @ApiModelProperty("每批多少个", required = true)
    val batchSize: Int
)
