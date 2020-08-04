package com.tencent.devops.sign.service

import com.tencent.devops.sign.api.pojo.IpaSignInfo
import java.io.File
import java.io.InputStream

interface SignService {

    /*
    * 对ipa文件进行签名，并归档
    * */
    fun uploadIpaAndDecodeInfo(
        resignId: String,
        ipaSignInfo: IpaSignInfo,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): Pair<File, Int>

    /*
    * 对ipa文件进行签名，并归档
    * */
    fun signIpaAndArchive(
        resignId: String,
        ipaSignInfo: IpaSignInfo,
        ipaFile: File,
        taskExecuteCount: Int
    )

    /*
    * 查询某次签名任务是否完成
    * */
    fun getSignResult(resignId: String): Boolean
}