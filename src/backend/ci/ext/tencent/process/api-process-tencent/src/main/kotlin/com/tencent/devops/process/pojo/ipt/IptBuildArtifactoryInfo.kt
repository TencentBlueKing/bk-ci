package com.tencent.devops.process.pojo.ipt

import com.tencent.devops.artifactory.pojo.FileInfo

data class IptBuildArtifactoryInfo (
    val buildId: String = "",
    val pkgList: List<FileInfo> = listOf()
)