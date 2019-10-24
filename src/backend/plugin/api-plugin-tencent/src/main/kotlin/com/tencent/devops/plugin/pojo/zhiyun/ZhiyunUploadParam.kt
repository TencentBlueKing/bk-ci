package com.tencent.devops.plugin.pojo.zhiyun

import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam

data class ZhiyunUploadParam(
    val operator: String,
    val para: CommonParam,
    val fileParams: ArtifactorySearchParam
) {
    data class CommonParam(
        val product: String,
        val name: String,
        val author: String,
        val description: String,
        val clean: String,
        val buildId: String,
        val codeUrl: String?
    )
}
