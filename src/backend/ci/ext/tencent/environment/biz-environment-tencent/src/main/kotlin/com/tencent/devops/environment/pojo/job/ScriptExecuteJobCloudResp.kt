package com.tencent.devops.environment.pojo.job

data class ScriptExecuteJobCloudResp(
    var code : Int,
    var result : Boolean,
    var jobRequestId: String,
    var message: String?,
    var data: ScriptExecuteResult?
)