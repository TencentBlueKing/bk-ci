package com.tencent.devops.sign.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import java.io.File
import java.io.InputStream

interface SignService {
    fun singIpa(
            userId: String,
            ipaSignInfoHeader: String,
            ipaInputStream: InputStream
    ): String?

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