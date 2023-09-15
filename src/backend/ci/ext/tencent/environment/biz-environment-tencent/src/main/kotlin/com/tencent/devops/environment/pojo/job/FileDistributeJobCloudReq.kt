package com.tencent.devops.environment.pojo.job

@Suppress("ALL")
data class FileDistributeJobCloudReq (
    val bk_scope_type: String,
    val bk_scope_id: String,
    val file_source_list: List<FileSourceJobCloudReq>,
    val file_target_path: String,
    val target_server: ExecuteTargetJobCloudReq,
    val account_alias: String,
    val timeout: Long,
    override var bk_app_code: String,
    override var bk_app_secret: String,
    override var bk_username: String
) : BaseJobCloudReq(bk_app_code, bk_app_secret, bk_username)

