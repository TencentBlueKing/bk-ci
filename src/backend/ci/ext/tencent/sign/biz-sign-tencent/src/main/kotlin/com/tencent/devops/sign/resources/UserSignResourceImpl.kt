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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.sign.api.user.UserSignResource
import com.tencent.devops.sign.pojo.IosProfile
import com.tencent.devops.sign.pojo.IpaCustomizedSignRequest
import com.tencent.devops.sign.service.impl.IpaSignServiceImpl
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.utils.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest


@RestResource
class UserSignResourceImpl @Autowired constructor(
        private val ipaSignService: IpaSignServiceImpl
) : UserSignResource {

    override fun ipaCustomizedSign(
            userId: String,
            ipaSignRequest: String?,
            ipaInputStream: InputStream
//        ipaDisposition: FormDataContentDisposition
    ): Result<String?> {
//        return Result(data = null)
        var outputFile = File("/data/frey/test.ipa")
        var outputStream = outputFile.outputStream()
        ipaInputStream.copyTo(outputStream)
//        return ipaSignService.resignCustomizedIpaPackage(
//            userId = userId,
//            ipaCustomizedSignRequest = ipaSignRequest,
//            inputStream = ipaInputStream,
//            disposition = ipaDisposition
//        )
        return Result(data = null)
    }

    override fun ipaCustomizedSign2(
            userId: String,
            ipaSignRequest: String,
            ipaInputStream: InputStream
//        ipaDisposition: FormDataContentDisposition
    ): Result<String?> {
//        return Result(data = null)
        var outputFile = File("/data/frey/test.ipa")
        val mad5 = copyInputStreamToFileAndGetMd5Hex(ipaInputStream, outputFile)
//        var outputStream = outputFile.outputStream()
//        ipaInputStream.copyTo(outputStream)
//        return ipaSignService.resignCustomizedIpaPackage(
//            userId = userId,
//            ipaCustomizedSignRequest = ipaSignRequest,
//            inputStream = ipaInputStream,
//            disposition = ipaDisposition
//        )
        return Result(data = null)
    }

    override fun getKeystoreCerts(userId: String, appId: String): Result<List<IosProfile>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun copyInputStreamToFileAndGetMd5Hex(inputStream: InputStream, file: File?): String? {
        val digest: MessageDigest = DigestUtils.getMd5Digest()
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(file)
            val buffer = ByteArray(8 * 1024)
            var read = inputStream.read(buffer)
            while (read > -1) {
                // 计算MD5,顺便写到文件
                digest.update(buffer, 0, read)
                outputStream.write(buffer, 0, read)
                read = inputStream.read(buffer)
            }
        } finally {
            IOUtils.closeQuietly(outputStream)
        }
//        return ""
        return Hex.encodeHexString(digest.digest())
    }
}