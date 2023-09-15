package com.tencent.devops.environment.pojo.job

@Suppress("ALL")
class FileSourceJobCloudReq (
    val file_list: List<String>,
    val server: ExecuteTargetJobCloudReq,
    val account: AccountJobCloudReq
)