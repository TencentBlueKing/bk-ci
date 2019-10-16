package com.tencent.devops.project.pojo

import java.util.Date

data class PaasProject(
    val approval_status: Int,
    val approval_time: String,
    val approver: String,
    val bg_id: Int,
    val bg_name: String,
    val cc_app_id: Int,
    val center_id: Int,
    val center_name: String,
    val created_at: Date,
    val creator: String,
    val data_id: Int,
    val deploy_type: String,
    val dept_id: Int,
    val dept_name: String,
    val description: String,
    val english_name: String,
    val extra: Any,
    val is_offlined: Boolean,
    val is_secrecy: Boolean,
    val kind: Int,
    val logo_addr: String,
    val project_id: String,
    val project_name: String,
    val project_type: Int,
    val remark: String,
    val updated_at: Date?,
    val use_bk: Boolean
)