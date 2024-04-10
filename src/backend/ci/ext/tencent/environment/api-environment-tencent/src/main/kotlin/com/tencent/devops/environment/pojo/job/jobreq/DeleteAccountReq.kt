package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

data class DeleteAccountReq(
    @get:Schema(title = "帐号ID", required = true)
    val id: Long
)