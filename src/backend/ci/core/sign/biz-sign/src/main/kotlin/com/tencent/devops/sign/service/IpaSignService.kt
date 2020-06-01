package com.tencent.devops.sign.service

import com.tencent.devops.common.api.pojo.Result
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import java.io.File
import java.io.InputStream

interface IpaSignService {
    fun resignIpaPackage(
            userId: String,
            ipaSignInfo: String?,
            inputStream: InputStream
    ): Result<String?>

    fun resignApp(
            appPath: File,
            bundleId: String?,
            mobileprovision: String?,
            entitlement: String?
    ): Result<Boolean>
}