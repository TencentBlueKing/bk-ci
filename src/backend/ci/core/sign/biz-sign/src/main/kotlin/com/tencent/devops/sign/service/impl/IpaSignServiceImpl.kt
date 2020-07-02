package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.pojo.IpaSignInfo
import com.tencent.devops.sign.service.IpaSignService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream

@Service
class IpaSignServiceImpl : IpaSignService {

    override fun resignIpaPackage(
            userId: String,
            ipaSignInfo: IpaSignInfo?,
            ipaPackage: File
    ): Result<String?> {
        logger.info("the upload file info is:$ipaSignInfo")
//        val index = fileName.lastIndexOf(".")
//        val fileSuffix = fileName.substring(index + 1)
//
//        if (!fileSuffix.contains("ipa") && !fileSuffix.contains("IPA")) {
//            throw InvalidParamException(
//                message = "该文件不是正确的IPA包",
//                params = arrayOf(fileName)
//            )
//        }
//
//        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
//        file.outputStream().use {
//            inputStream.copyTo(it)
//        }

        return Result("")
    }

    override fun resignApp(appPath: File, bundleId: String?, mobileprovision: String?, entitlement: String?): Result<Boolean> {
        TODO("Not yet implemented")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IpaSignServiceImpl::class.java)
    }
}