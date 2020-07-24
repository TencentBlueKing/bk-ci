package com.tencent.devops.sign.service

import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.sign.api.constant.SignMessageCode
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

interface DownloadService {
    /*
    * 获取下载连接
    * */
    fun getDownloadUrl(
        userId: String,
        resignId: String
    ): String

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadService::class.java)
    }
}