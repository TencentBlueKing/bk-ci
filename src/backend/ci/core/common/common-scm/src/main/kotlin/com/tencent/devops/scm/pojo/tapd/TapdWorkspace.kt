package com.tencent.devops.scm.pojo.tapd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * TAPD 项目(Workspace)实体
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 项目实体")
data class TapdWorkspace(
    @get:Schema(description = "项目 ID")
    val id: String,
    @get:Schema(description = "项目名称")
    val name: String,
    @get:Schema(description = "项目简称/别名")
    @JsonProperty("pretty_name")
    val prettyName: String? = null,
    @get:Schema(description = "项目分类")
    val category: String? = null,
    @get:Schema(description = "项目状态")
    val status: String? = null,
    @get:Schema(description = "项目描述")
    val description: String? = null,
    @get:Schema(description = "开始日期")
    @JsonProperty("begin_date")
    val beginDate: String? = null,
    @get:Schema(description = "结束日期")
    @JsonProperty("end_date")
    val endDate: String? = null,
    @get:Schema(description = "外部访问开关")
    @JsonProperty("external_on")
    val externalOn: String? = null,
    @get:Schema(description = "创建人")
    val creator: String? = null,
    @get:Schema(description = "创建时间")
    val created: String? = null
)
