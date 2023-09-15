package com.tencent.devops.environment.pojo.job

@Suppress("ALL")
data class ScriptExecuteJobCloudReq(
    val bk_scope_type: String,
    val bk_scope_id: String,
    val script_content: String,
    val script_param: String,
    val timeout: Long,
    val account_alias: String,
    val is_param_sensitive: Int,
    val script_language: Int,
    val target_server: ExecuteTargetJobCloudReq,
    override var bk_app_code: String,
    override var bk_app_secret: String,
    override var bk_username: String
) : BaseJobCloudReq(bk_app_code, bk_app_secret, bk_username)