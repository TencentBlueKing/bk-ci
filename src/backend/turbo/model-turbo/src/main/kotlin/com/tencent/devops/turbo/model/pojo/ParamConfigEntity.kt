package com.tencent.devops.turbo.model.pojo

import com.tencent.devops.common.util.enums.ConfigParamType
import org.springframework.data.mongodb.core.mapping.Field

data class ParamConfigEntity(
    @Field("param_key")
    var paramKey: String,
    @Field("param_name")
    var paramName: String,
    @Field("param_type")
    var paramType: ConfigParamType,
    @Field("param_props")
    var paramProps: Map<String, Any?>?,
    @Field("param_enum")
    var paramEnum: List<ParamEnumEntity>?,
    @Field("displayed")
    var displayed: Boolean = true,
    @Field("default_value")
    var defaultValue: Any? = null,
    @Field("required")
    var required: Boolean? = false,
    @Field("tips")
    var tips: String? = null,
    @Field("data_type")
    var dataType: String? = null,
    @Field("param_url")
    var paramUrl: String? = null
)
