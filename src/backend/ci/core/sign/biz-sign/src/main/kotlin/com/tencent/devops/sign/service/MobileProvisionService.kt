package com.tencent.devops.sign.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import java.io.File

interface MobileProvisionService {

    fun downloadMobileProvision(
        mobileProvisionDir: File,
        projectId: String,
        mobileProvisionId: String
    ): File
}