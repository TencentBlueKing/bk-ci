package com.tencent.devops.remotedev.pojo.gitproxy

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "重新绑定工蜂项目信息")
data class ReBindingLinkData(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "需要重新绑定的工蜂项目ID和项目类型如 GIT/SVN")
    val idMap: Map<Long, String>,
    @get:Schema(title = "凭据ID,为空时使用,调用用户的OAUTH")
    val credId: String?
)

@Schema(title = "重新绑定工蜂项目信息返回")
data class ReBindingLinkResp(
    @get:Schema(title = "工蜂项目ID")
    val errorIds: Set<Long>,
    @get:Schema(title = "未成功时错误信息")
    val errMsg: String?
)