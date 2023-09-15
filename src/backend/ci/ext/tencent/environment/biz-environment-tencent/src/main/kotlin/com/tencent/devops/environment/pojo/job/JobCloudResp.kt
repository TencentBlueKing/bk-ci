package com.tencent.devops.environment.pojo.job

data class JobCloudResp(
    var code : Int,
    var result : Boolean,
    var jobRequestId: String,
    var message: String?,
    var data: ScriptExecuteResult?
)