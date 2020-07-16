package com.tencent.devops.sign.service

import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import java.io.File
import java.io.InputStream

interface SignService {
    /*
    * 对ipa文件进行签名，并归档
    * */
    fun signIpaAndArchive(
        userId: String,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): String?

    /*
    * 对解压后的ipa目录进行签名
    * 对主App，扩展App和框架文件进行签名
    * */
    fun resignIpaPackage(
        ipaPackage: File,
        ipaSignInfo: IpaSignInfo,
        mobileProvisionInfoList: Map<String, MobileProvisionInfo>?
    ): File

    /*
    * 下载描述文件
    * 返回描述文件所在目录
    * */
    fun downloadMobileProvision(
        mobileProvisionDir: File,
        ipaSignInfo: IpaSignInfo
    ): Map<String, MobileProvisionInfo>

    /*
    * 解析描述文件的内容
    * */
    fun parseMobileProvision(
        mobileProvisionFile:File
    ): MobileProvisionInfo
}