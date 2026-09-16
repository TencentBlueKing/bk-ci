package com.tencent.devops.openapi.pojo.auth

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "按多个操作查询用户有权限的资源实例结果")
data class UserResourcesByPermissionsVO(
    @get:Schema(title = "待查询用户ID", required = true, example = "zhangsan")
    val userId: String,
    @get:Schema(title = "项目ID(项目英文名)", required = true, example = "demo")
    val projectId: String,
    @get:Schema(title = "资源类型", required = true, example = "pipeline")
    val resourceType: String,
    @get:Schema(title = "按操作分组的资源实例列表", required = true)
    val resources: List<UserResourceByActionVO>
)

@Schema(title = "单个操作对应的有权限资源")
data class UserResourceByActionVO(
    @get:Schema(
        title = "操作标识",
        description = "与请求中的 action 对应",
        required = true,
        example = "pipeline_view"
    )
    val action: String,
    @get:Schema(
        title = "权限动作",
        description = "action 的权限后缀，如 view、edit、execute、delete、list",
        required = true,
        example = "view"
    )
    val permission: String,
    @get:Schema(
        title = "资源实例Code列表",
        description = "用户对该操作有权限的资源Code。无权限时为空数组。" +
            "若用户拥有项目级或管理员权限，将返回该项目下该类型的全部资源Code。",
        required = true,
        example = "[\"p-xxxxx\", \"p-yyyyy\"]"
    )
    val resourceCodes: List<String>
)
