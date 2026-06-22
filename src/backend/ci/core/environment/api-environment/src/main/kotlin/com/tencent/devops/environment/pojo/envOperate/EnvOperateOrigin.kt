package com.tencent.devops.environment.pojo.envOperate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "环境操作来源")
enum class EnvOperateOrigin {
    @Schema(title = "用户网页操作")
    WEB,

    @Schema(title = "OPENAPI操作")
    API,

    @Schema(title = "内部操作")
    INTERNAL,
    ;
}

@Schema(title = "环境操作类型")
enum class EnvOperateName {
    @Schema(title = "更新环境变量")
    UPDATE_ENV_VAR,

    @Schema(title = "更新环境共享设置")
    UPDATE_SHARE_SETTING,

    @Schema(title = "删除环境共享设置")
    DELETE_SHARE_SETTING,

    @Schema(title = "更新使用者")
    UPDATE_USER,

    @Schema(title = "更新拥有者")
    UPDATE_OWNER,

    @Schema(title = "更新环境基础信息")
    UPDATE_INFO,

    @Schema(title = "新增环境调度策略")
    ADD_DISPATCH_STRATEGY,

    @Schema(title = "更新环境调度策略基础信息")
    UPDATE_DISPATCH_STRATEGY,

    @Schema(title = "删除环境调度策略")
    DELETE_DISPATCH_STRATEGY,

    @Schema(title = "更新环境调度策略排序")
    UPDATE_DISPATCH_STRATEGY_ORDER,

    @Schema(title = "启用环境节点")
    ENABLE_NODE,

    @Schema(title = "停用环境节点")
    DISABLE_NODE,

    @Schema(title = "修改环境关联标签")
    UPDATE_ENV_LINK_TAG,

    @Schema(title = "关联环境节点")
    ADD_NODE,

    @Schema(title = "移除环境节点")
    REMOVE_NODE,
    ;
}


@Schema(title = "操作环境内容")
@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvOperateContent(
    @get:Schema(title = "具体操作内容")
    val content: String?,
    @get:Schema(title = "操作资源数")
    val resourceCount: Int?,
)