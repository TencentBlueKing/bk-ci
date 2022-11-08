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

package com.tencent.devops.plugin.worker.api.sgin

import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

/**
 * deng
 * 2019-02-15
 */
class IOSSignApi : AbstractBuildResourceApi() {

    fun uploadIpa(file: File, props: String, repoType: Int, customPath: String?, certId: String?, p12Id: Int) {
        // p12Id: 1为深圳科技，2为世纪天游
        val gatewayDomain = AgentEnv.getGateway()
        val path = "/ios/sign/upload?size=${file.length()}&md5=${FileUtil.getMD5(file)}&" +
            "properties=$props&repoType=$repoType&customPath=$customPath&certId=$certId&p12Id=$p12Id"
        val fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, fileBody)
            .build()
        val request = buildPost(path, requestBody)
        val timeout = (1 + file.length() / 1024 / 1024 / 1024) * 14 // 每G给14分钟，再增加14分钟做签名。
        val response = request(request, "企业签名失败", 100, timeout * 60, timeout * 60)
        LoggerService.addErrorLine("response:$response")
        if (response.trim() != "success") {
            LoggerService.addErrorLine("Enterprise sign ($file) fail in domain:$gatewayDomain")
            throw RuntimeException("Enterprise sign ($file) fail in domain:$gatewayDomain")
        } else {
            LoggerService.addNormalLine("enterprise sign successfully ($file)  in  domain:$gatewayDomain")
        }
    }
}
