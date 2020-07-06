package com.tencent.devops.sign.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import java.io.File

interface SignService {

    fun resignIpaPackage(
            ipaPackage: File,
            ipaSignInfo: IpaSignInfo
    ): File?

    fun resignApp(
            appPath: File,
            bundleId: String?,
            mobileprovision: String?,
            entitlement: String?
    ): Result<Boolean>
}