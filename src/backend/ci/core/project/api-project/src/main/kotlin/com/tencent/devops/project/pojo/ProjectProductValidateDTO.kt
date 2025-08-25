package com.tencent.devops.project.pojo

import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectOperation
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目关联产品检验类")
data class ProjectProductValidateDTO(
    @Schema(title = "项目ID")
    val englishName: String,
    @Schema(title = "用户ID")
    val userId: String,
    @Schema(title = "项目操作")
    val projectOperation: ProjectOperation,
    @Schema(title = "渠道")
    val channelCode: ProjectChannelCode? = null,
    @Schema(title = "产品ID")
    val productId: Int? = null,
    @Schema(title = "产品名称")
    val productName: String? = null,
    @Schema(title = "bgId")
    val bgId: Long,
    @Schema(title = "bg名称")
    val bgName: String
)
