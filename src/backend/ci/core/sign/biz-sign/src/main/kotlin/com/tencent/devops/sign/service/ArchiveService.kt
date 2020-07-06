package com.tencent.devops.sign.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import java.io.File

interface ArchiveService {

    fun archive(
            signedIpaFile: File,
            ipaSignInfo: IpaSignInfo
    ): String?
}