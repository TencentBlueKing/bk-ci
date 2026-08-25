package com.tencent.devops.store.pojo.template

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板版本发布记录实体")
data class TemplatePublishedVersionInfo(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "模板代码（对应process数据库的ID）")
    val templateCode: String,
    @get:Schema(title = "发布版本号")
    val version: Long,
    @get:Schema(title = "版本排序号")
    val number: Int,
    @get:Schema(title = "发布版本名称")
    val versionName: String,
    @get:Schema(title = "是否发布")
    val published: Boolean,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "更新人")
    val updater: String,
    @get:Schema(title = "创建时间")
    val createTime: Long? = null,
    @get:Schema(title = "更新时间")
    val updateTime: Long? = null
)
