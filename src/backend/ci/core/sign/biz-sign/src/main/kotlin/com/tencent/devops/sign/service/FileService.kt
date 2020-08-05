package com.tencent.devops.sign.service

import com.tencent.devops.sign.api.pojo.IpaSignInfo
import java.io.File
import java.io.InputStream

interface FileService {

    fun copyToTargetFile(
        ipaInputStream: InputStream,
        ipaSignInfo: IpaSignInfo,
        md5Check: Boolean = true
    ): File
}