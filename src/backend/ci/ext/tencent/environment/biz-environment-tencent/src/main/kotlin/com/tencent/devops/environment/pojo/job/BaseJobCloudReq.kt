package com.tencent.devops.environment.pojo.job

@Suppress("ALL")
abstract class BaseJobCloudReq(
    open var bk_app_code: String,
    open var bk_app_secret: String,
    open var bk_username: String
)