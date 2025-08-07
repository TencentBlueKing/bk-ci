package com.tencent.devops.store.pojo.template

import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "市场模板信息")
data class MarketTemplateInfo(
    @get:Schema(title = "ID")
    val id: String,
    @get:Schema(title = "市场模板名称")
    val templateName: String,
    @get:Schema(title = "模板code")
    val templateCode: String,
    @get:Schema(title = "所属分类ID")
    val classifyId: String,
    @get:Schema(title = "版本号")
    val version: String,
    @get:Schema(title = "模板类型，0：自由模式 1：约束模式")
    val templateType: Int = 1,
    @get:Schema(title = "模板研发类型，0：自研 1：第三方开发")
    val templateRdType: Int = 1,
    @get:Schema(title = "模板状态，0：初始化|1：审核中|2：审核驳回|3：已发布|4：上架中止|5：已下架")
    val templateStatus: TemplateStatusEnum,
    @get:Schema(title = "状态对应的描述，如上架失败原因")
    val templateStatusMsg: String? = null,
    @get:Schema(title = "logo地址")
    val logoUrl: String? = null,
    @get:Schema(title = "模板简介")
    val summary: String? = null,
    @get:Schema(title = "模板描述")
    val description: String? = "",
    @get:Schema(title = "发布描述")
    val pubDescription: String? = "",
    @get:Schema(title = "是否为公共模板， 1：是 0：不是")
    val publicFlag: Boolean = false,
    @get:Schema(title = "是否为最新版本原子， TRUE：最新 FALSE：非最新")
    val latestFlag: Boolean,
    @get:Schema(title = "模板发布者")
    val publisher: String,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "修改人")
    val modifier: String,
    @get:Schema(title = "发布时间")
    val pubTime: LocalDateTime,
)
