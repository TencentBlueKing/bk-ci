package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "通过条件对项目进行邮件通知")
data class SendEmailForProjectByConditionDTO(
    val bgId: Long? = null,
    val deptId: Long? = null,
    val centerId: Long? = null
)
