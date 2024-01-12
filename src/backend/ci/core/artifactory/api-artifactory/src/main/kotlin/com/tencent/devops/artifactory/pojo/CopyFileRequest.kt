package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "拷贝文件请求")
data class CopyFileRequest(
    @Schema(name = "项目ID")
    val projectId: String,
    @Schema(name = "源制品类型")
    val srcArtifactoryType: ArtifactoryType,
    @Schema(name = "源文件完整路径")
    val srcFileFullPaths: List<String>,
    @Schema(name = "目标制品类型")
    val dstArtifactoryType: ArtifactoryType = ArtifactoryType.CUSTOM_DIR,
    @Schema(name = "目标目录完整路径")
    val dstDirFullPath: String
)
