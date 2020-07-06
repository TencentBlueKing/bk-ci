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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.sign.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.user.UserSignResource
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.service.FileService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import com.tencent.devops.sign.utils.FileSignUtil
import org.jolokia.util.Base64Util
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.InputStream
import javax.servlet.http.HttpServletResponse

@RestResource
class UserSignResourceImpl @Autowired constructor(
        private val ipaSignService: SignService,
        private val fileService: FileService,
        private val signInfoService: SignInfoService,
        private val archiveService: ArchiveService,
        private val objectMapper: ObjectMapper
) : UserSignResource {


    override fun ipaSign(
            userId: String,
            ipaSignInfoHeader: String,
            ipaInputStream: InputStream
    ): Result<String?> {
        var ipaSignInfo: IpaSignInfo? = null
        var ipaSignInfoHeaderDecode = Base64Util.decode(ipaSignInfoHeader)
        try {
            ipaSignInfo = objectMapper.readValue(ipaSignInfoHeaderDecode, IpaSignInfo::class.java)
        } catch (e: Exception) {
            logger.error("Fail to parse ipaSignInfoHeaderDecode:$ipaSignInfoHeaderDecode; Exception:", e)
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_PARSE_SIGN_INFO_HEADER, defaultMessage = "解析签名信息失败。")
        }
        // 检查ipaSignInfo的合法性
        ipaSignInfo = signInfoService.check(ipaSignInfo)
        if (ipaSignInfo == null) {
            logger.error("Check ipaSignInfo is invalided,  ipaSignInfoHeaderDecode:$ipaSignInfoHeaderDecode")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER, defaultMessage = "验证签名信息为非法信息。")
        }
        // 复制文件到临时目录
        val ipaFile = fileService.copyToTargetFile(ipaInputStream, ipaSignInfo)

        // 签名ipa包
        val signedIpaFile = ipaSignService.resignIpaPackage(ipaFile, ipaSignInfo)
        if(signedIpaFile == null) {
            logger.error("sign ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA包签名失败。")
        }

        // 归档ipa包
        val fileDownloadUrl = archiveService.archive(signedIpaFile, ipaSignInfo)
        if(fileDownloadUrl == null) {
            logger.error("archive signed ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ARCHIVE_SIGNED_IPA, defaultMessage = "归档IPA包失败。")
        }
        return Result(fileDownloadUrl)
    }

    override fun downloadIpa(userId: String, filePath: String, response: HttpServletResponse) {
        TODO("Not yet implemented")
    }
//
//    private fun
//
//    override fun downloadIpa(userId: String, filePath: String, response: HttpServletResponse) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }


    companion object {
        val logger = LoggerFactory.getLogger(UserSignResourceImpl::class.java)
    }
}