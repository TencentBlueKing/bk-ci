package com.tencent.devops.environment.pojo.envOperate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

enum class EnvOperateOrigin {
    WEB,
    API,
    INTERNAL,
    ;
}

enum class EnvOperateName {
    UPDATE_ENV_VAR,
    UPDATE_SHARE_SETTING,
    UPDATE_USER,
    UPDATE_OWNER,
    UPDATE_INFO,
    UPDATE_DISPATCH_STRATEGY,
    ENABLE_NODE,
    DISABLE_NODE,
    ADD_NODE,
    REMOVE_NODE,
    ;
}


@Schema(title = "操作环境内容")
@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvOperateContent(
    @get:Schema(title = "具体操作原因")
    val content: String?,
    @get:Schema(title = "操作资源数")
    val resourceCount: Int?,
)