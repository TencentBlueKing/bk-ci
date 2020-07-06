package com.tencent.devops.sign.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.resources.UserSignResourceImpl
import com.tencent.devops.sign.service.FileService
import com.tencent.devops.sign.utils.FileSignUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream

@Service
class FileServiceImpl: FileService {
    @Value("\${bkci.sign.tmpDir:/data/enterprise_sign_tmp/}")
    val tmpDir: String = "/data/enterprise_sign_tmp/"

    companion object {
        val logger = LoggerFactory.getLogger(FileServiceImpl::class.java)
    }

    override fun copyToTargetFile(
            ipaInputStream: InputStream,
            ipaSignInfo: IpaSignInfo
    ):File {
        val ipaTmpDir = "$tmpDir/${ipaSignInfo.projectId}/${ipaSignInfo.pipelineId}/${ipaSignInfo.buildId}/"
        val ipaTmpDirFile = File(ipaTmpDir)
        val ipaFile = File("$ipaTmpDir/${ipaSignInfo.fileName}")
        FileUtil.mkdirs(ipaTmpDirFile)
        val md5 = FileSignUtil.copyInputStreamToFile(ipaInputStream, ipaFile)
        when {
            md5 == null -> {
                logger.error("copy file and calculate file md5 is failed.")
                throw ErrorCodeException(errorCode = SignMessageCode.ERROR_COPY_FILE, defaultMessage = "复制并计算文件md5失败。")
            }
            md5 != ipaSignInfo.md5 -> {
                logger.error("copy file success,but md5 is diff.")
                throw ErrorCodeException(errorCode = SignMessageCode.ERROR_COPY_FILE, defaultMessage = "复制文件成功但md5不一致。")
            }
            else -> {
                return ipaFile
            }
        }
    }
}