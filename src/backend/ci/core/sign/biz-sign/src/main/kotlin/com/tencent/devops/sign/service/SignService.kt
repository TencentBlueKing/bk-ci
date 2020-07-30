package com.tencent.devops.sign.service

import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaInfoPlist
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import com.tencent.devops.sign.api.pojo.SignResult
import com.tencent.devops.sign.impl.SignServiceImpl
import com.tencent.devops.sign.utils.SignUtils
import org.jolokia.util.Base64Util
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.lang.RuntimeException

interface SignService {

    /*
    * 对ipa文件进行签名，并归档
    * */
    fun asyncSignIpaAndArchive(
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): String

    /*
    * 对ipa文件进行签名，并归档
    * */
    fun signIpaAndArchive(
        resignId: String,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    )

    /*
    * 查询某次签名任务是否完成
    * */
    fun getSignResult(resignId: String): Boolean

}