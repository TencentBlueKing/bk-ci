package com.tencent.devops.process.pojo.ipt

import com.tencent.devops.common.pipeline.enums.artifactory.SourceType

data class IptBuildArtifactoryInfo (
    val buildId: String,
    val pkgList: List<PkgInfo>
) {
    data class PkgInfo(
        val name: String,
        val sourceType: SourceType
    )
}