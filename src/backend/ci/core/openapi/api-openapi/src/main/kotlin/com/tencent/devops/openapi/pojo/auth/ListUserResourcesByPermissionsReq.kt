package com.tencent.devops.openapi.pojo.auth

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "查询用户有权限的资源实例请求")
data class ListUserResourcesByPermissionsReq(
    @get:Schema(
        title = "待查询用户ID",
        description = "不传则默认查询请求头 X-DEVOPS-UID 对应用户",
        required = false,
        example = "zhangsan"
    )
    val userId: String? = null,
    @get:Schema(
        title = "资源类型",
        description = "权限模型中的资源类型标识。常见值：pipeline(流水线)、pipeline_group(流水线组)、" +
            "repertory(代码库)、credential(凭证)、environment(环境)、env_node(环境节点)、project(项目)。" +
            "完整列表可通过 GET /v4/auth/metadata/list_resource_types 获取。",
        required = true,
        example = "pipeline"
    )
    val resourceType: String,
    @get:Schema(
        title = "操作列表",
        description = "至少一个。推荐传完整 action，格式为 {resourceType}_{permission}，例如 " +
            "pipeline_view、pipeline_edit、pipeline_execute。也支持只传权限后缀：view、edit、execute。" +
            "可用操作可通过 GET /v4/auth/metadata/list_actions?resourceType={resourceType} 获取。",
        required = true,
        example = "[\"pipeline_view\", \"pipeline_edit\"]"
    )
    val actions: List<String>
)
