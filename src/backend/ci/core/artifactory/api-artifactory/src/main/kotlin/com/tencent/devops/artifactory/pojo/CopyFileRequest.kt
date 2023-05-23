package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("拷贝文件请求")
data class CopyFileRequest(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("源制品类型")
    val srcArtifactoryType: ArtifactoryType,
    @ApiModelProperty("源文件完整路径")
    val srcFileFullPaths: List<String>,
    @ApiModelProperty("目标制品类型")
    val dstArtifactoryType: ArtifactoryType = ArtifactoryType.CUSTOM_DIR,
    @ApiModelProperty("目标目录完整路径")
    val dstDirFullPath: String
)
