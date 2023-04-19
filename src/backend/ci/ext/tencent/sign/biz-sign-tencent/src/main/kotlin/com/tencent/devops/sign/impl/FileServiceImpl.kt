/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.sign.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.FileService
import com.tencent.devops.sign.utils.IpaFileUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream

@Service
class FileServiceImpl : FileService {

    @Value("\${bkci.sign.tmpDir:/data/enterprise_sign_tmp}")
    val tmpDir: String = "/data/enterprise_sign_tmp"

    companion object {
        private val logger = LoggerFactory.getLogger(FileServiceImpl::class.java)
    }

    override fun copyToTargetFile(
        ipaInputStream: InputStream,
        ipaSignInfo: IpaSignInfo,
        md5Check: Boolean,
        resignId: String?
    ): File {
        val ipaTmpDirFile = getIpaTmpDir(ipaSignInfo, resignId)
        val ipaFile = getIpaFile(ipaSignInfo, resignId)
        FileUtil.mkdirs(ipaTmpDirFile, false)
        val md5 = IpaFileUtil.copyInputStreamToFile(ipaInputStream, ipaFile)
        logger.info("copy file md5 check:$md5Check")
        if (md5Check) {
            when {
                md5 == null -> {
                    logger.warn("copy file and calculate file md5 is failed.")
                    throw ErrorCodeException(
                        errorCode = SignMessageCode.ERROR_COPY_FILE,
                        defaultMessage = "复制并计算文件md5失败。"
                    )
                }
                md5 != ipaSignInfo.md5 -> {
                    logger.warn("copy file success, but md5 is diff.")
                    throw ErrorCodeException(
                        errorCode = SignMessageCode.ERROR_COPY_FILE,
                        defaultMessage = "复制文件成功但md5不一致。"
                    )
                }
                else -> {
                    return ipaFile
                }
            }
        } else {
            return ipaFile
        }
    }

    override fun getIpaFile(
        ipaSignInfo: IpaSignInfo,
        resignId: String?
    ): File {
        return File("${getIpaTmpDir(ipaSignInfo, resignId).canonicalPath}/${ipaSignInfo.fileName}")
    }

    override fun getIpaUnzipDir(
        ipaSignInfo: IpaSignInfo,
        resignId: String?
    ): File {
        return File("${getIpaFile(ipaSignInfo, resignId).canonicalPath}.unzipDir")
    }

    override fun getMobileProvisionDir(
        ipaSignInfo: IpaSignInfo,
        resignId: String?
    ): File {
        return File("${getIpaFile(ipaSignInfo, resignId).canonicalPath}.mobileProvisionDir")
    }

    override fun getIpaTmpDir(
        ipaSignInfo: IpaSignInfo,
        resignId: String?
    ): File {
        return File("$tmpDir/${ipaSignInfo.projectId}/${ipaSignInfo.pipelineId}" +
            "/${ipaSignInfo.buildId}/$resignId/")
    }
}
