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
        if (response.trim() != "success") {
            LoggerService.addRedLine("Enterprise sign ($file) fail in domain:$gatewayDomain")
            throw RuntimeException("Enterprise sign ($file) fail in domain:$gatewayDomain")
        } else {
            LoggerService.addNormalLine("enterprise sign successfully ($file)  in  domain:$gatewayDomain")
        }
    }
}