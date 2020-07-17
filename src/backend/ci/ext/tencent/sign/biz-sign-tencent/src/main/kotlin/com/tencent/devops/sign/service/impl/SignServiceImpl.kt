package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.pojo.IpaCustomizedSignRequest
import com.tencent.devops.sign.resources.UserIpaResourceImpl
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.service.FileService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import com.tencent.devops.sign.utils.SignUtils
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files

@Service
class SignServiceImpl @Autowired constructor(
    private val fileService: FileService,
    private val signInfoService: SignInfoService,
    private val archiveService: ArchiveService,
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao
): SignService {

    @Value("\${bkci.sign.tmpDir:/data/enterprise_sign_tmp/}")
    private val tmpDir = "/data/enterprise_sign_tmp/"

    private lateinit var ipaFile: File
    private lateinit var ipaUnzipDir: File
    private lateinit var mobileProvisionDir: File

    override fun signIpaAndArchive(userId: String, ipaSignInfoHeader: String, ipaInputStream: InputStream): String? {
        val resignId = UUIDUtil.generate()
        var ipaSignInfo = decodeIpaSignInfo(ipaSignInfoHeader)

        if (ipaSignInfo == null) {
            UserIpaResourceImpl.logger.error("Fail to parse ipaSignInfoHeaderDecode:$ipaSignInfo")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_PARSE_SIGN_INFO_HEADER, defaultMessage = "解析签名信息失败")
        }


        ipaSignInfo = signInfoService.check(ipaSignInfo)
        signIpaInfoDao.saveSignInfo(dslContext, resignId, ipaSignInfo, ipaSignInfoHeader)

        // 检查ipaSignInfo的合法性
        if (ipaSignInfo == null) {
            UserIpaResourceImpl.logger.error("Check ipaSignInfo is invalided,  ipaSignInfoHeaderDecode:$ipaSignInfo")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER, defaultMessage = "验证签名信息为非法信息")
        }

        // 复制文件到临时目录
        ipaFile = fileService.copyToTargetFile(ipaInputStream, ipaSignInfo)
        // ipa解压后的目录
        ipaUnzipDir = File("${ipaFile.canonicalPath}.unzipDir")
        FileUtil.mkdirs(ipaUnzipDir)
        // 描述文件的目录
        mobileProvisionDir = File("${ipaFile.canonicalPath}.mobileProvisionDir")
        FileUtil.mkdirs(mobileProvisionDir)

        // 解压ipa包
        SignUtils.unzipIpa(ipaFile, ipaUnzipDir)
        // 下载并返回描述文件信息
        val mobileProvisionInfoMap = downloadMobileProvision(mobileProvisionDir, ipaSignInfo)

        val signedIpaFile = resignIpaPackage(ipaUnzipDir, ipaSignInfo, mobileProvisionInfoMap)

        if (signedIpaFile == null) {
            UserIpaResourceImpl.logger.error("sign ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA包签名失败")
        }

        // 归档ipa包
        val fileDownloadUrl = archiveService.archive(signedIpaFile, ipaSignInfo)
        if (fileDownloadUrl == null) {
            UserIpaResourceImpl.logger.error("archive signed ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ARCHIVE_SIGNED_IPA, defaultMessage = "归档IPA包失败")
        }
        return fileDownloadUrl
    }

    override fun downloadMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo): Map<String, MobileProvisionInfo> {
        TODO("Not yet implemented")
    }

    @Value("\${sign.workspace:#{null}}")
    val workspace: String = ""

    fun resignIpaPackage(
        userId: String,
        ipaSignInfo: String?,
        inputStream: InputStream
    ): Result<String?> {
        logger.info("the upload file info is:$ipaSignInfo")
//        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO8859-1")), Charset.forName("UTF-8"))
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
//
//        file.copyTo(
//            target = File(workspace + File.separator + fileName),
//            overwrite = true
//        )

        return Result("")
    }

    fun resignCustomizedIpaPackage(
        userId: String,
        ipaCustomizedSignRequest: IpaCustomizedSignRequest,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        logger.info("the upload file info is:$disposition")
        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO8859-1")), Charset.forName("UTF-8"))
        val index = fileName.lastIndexOf(".")
        val fileSuffix = fileName.substring(index + 1)

        if (!fileSuffix.contains("ipa") && !fileSuffix.contains("IPA")) {
            throw InvalidParamException(
                message = "该文件不是正确的IPA包",
                params = arrayOf(fileName)
            )
        }

        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }

        file.copyTo(
            target = File(workspace + File.separator + fileName),
            overwrite = true
        )

        return Result(fileName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }
}