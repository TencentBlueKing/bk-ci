package com.tencent.devops.sign.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.pojo.IpaSignInfo
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import java.io.File
import java.io.InputStream

interface IpaSignService {
    fun resignIpaPackage(
            userId: String,
            ipaSignInfo: IpaSignInfo?,
            ipaPackage: File
    ): Result<String?>

    fun resignApp(
            appPath: File,
            bundleId: String?,
            mobileprovision: String?,
            entitlement: String?
    ): Result<Boolean>
}