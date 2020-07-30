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
    fun signIpaAndArchive(
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): String

    /*
    * 查询某次签名任务是否完成
    * */
    fun getSignResult(resignId: String): Boolean

    /*
    * 下载描述文件
    * 返回描述文件所在目录
    * */
    fun downloadMobileProvision(
        mobileProvisionDir: File,
        ipaSignInfo: IpaSignInfo
    ): Map<String, MobileProvisionInfo>

    /*
    * 通用逻辑-解析描述文件的内容
    * */
    fun parseMobileProvision(mobileProvisionFile: File): MobileProvisionInfo

    /*
    * 通用逻辑-对解压后的ipa目录进行签名
    * 对主App，扩展App和框架文件进行签名
    * */
    fun resignIpaPackage(
        unzipDir: File,
        ipaSignInfo: IpaSignInfo,
        mobileProvisionInfoList: Map<String, MobileProvisionInfo>
    ): Boolean

    /*
    * 通用逻辑-对解压后的ipa目录进行通配符签名
    * 对主App，扩展App和框架文件进行通配符签名
    * */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun resignIpaPackageWildcard(
        unzipDir: File,
        ipaSignInfo: IpaSignInfo,
        wildcardInfo: MobileProvisionInfo
    ): Boolean
}