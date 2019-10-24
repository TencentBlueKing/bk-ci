package com.tencent.devops.plugin.pojo.migcdn

import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam

data class MigCDNUploadParam(
    val operator: String,
    val para: CommonParam,
    val fileParams: ArtifactorySearchParam
) {
    data class CommonParam(
        val appName: String,
        val appSecret: String,
        val destFileDir: String, // 上传到CDN的目录, 不能包含中文
        val needUnzip: Int // 对于zip包上传后是否解压，0：不解；1：解压
    )
}
