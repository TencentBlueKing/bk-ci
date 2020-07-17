package com.tencent.devops.sign.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream

interface FileService {

    fun copyToTargetFile(
            ipaInputStream: InputStream,
            ipaSignInfo: IpaSignInfo
    ): File

    fun zipDirToFile(
        srcDir: File,
        destFile: String
    ): File
}