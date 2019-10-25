package com.tencent.devops.plugin.pojo.luna

import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam

data class LunaUploadParam(
    val operator: String,
    val para: CommonParam,
    val fileParams: ArtifactorySearchParam
) {
    data class CommonParam(
        val appName: String,
        val appSecret: String,
        val destFileDir: String? // 上传到LUNA的目录

    )
}
