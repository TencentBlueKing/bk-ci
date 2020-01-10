package com.tencent.devops.worker.common.api.ticket

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.ticket.pojo.CertAndroid
import com.tencent.devops.ticket.pojo.CertIOS

class CertResourceApi : AbstractBuildResourceApi() {

    fun queryIos(certId: String, publicKey: String): Result<CertIOS> {
        val path = "/ticket/api/build/certs/ios/$certId?publicKey=${encode(publicKey)}"
        val request = buildGet(path)
        val responseContent = request(request, "获取IOS证书失败")
        return objectMapper.readValue(responseContent)
    }

    fun queryAndroid(certId: String, publicKey: String): Result<CertAndroid> {
        val path = "/ticket/api/build/certs/android/$certId?publicKey=${encode(publicKey)}"
        val request = buildGet(path)
        val responseContent = request(request, "获取Android证书失败")
        return objectMapper.readValue(responseContent)
    }
}