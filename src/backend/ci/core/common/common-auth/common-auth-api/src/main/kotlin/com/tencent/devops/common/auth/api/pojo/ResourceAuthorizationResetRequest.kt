package com.tencent.devops.common.auth.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "业务方重置资源授权请求体")
data class ResourceAuthorizationResetRequest(
    @get:Schema(description = "资源授权重置条件实体", required = true)
    val condition: ResourceAuthorizationHandoverConditionRequest,
    @get:Schema(
        description = "重置资源授权操作的渠道有两种，一种是管理员界面视角，一种是在具体资源列表界面对单个资源进行操作;" +
            "对于在管理员界面重置授权时，统一校验是否有管理员权限，不存在差异;" +
            "但对于在具体资源列表界面单条操作时，它们的鉴权方式存在差异，需要业务方自己自行进行处理，抛出异常。",
        required = true
    )
    val validateSingleResourcePermission: ((
        operator: String,
        projectCode: String,
        resourceCode: String
    ) -> Unit)?,
    @get:Schema(description = "资源授权重置函数", required = true)
    val handoverResourceAuthorization: (ResourceAuthorizationHandoverDTO) -> ResourceAuthorizationHandoverResult
)
