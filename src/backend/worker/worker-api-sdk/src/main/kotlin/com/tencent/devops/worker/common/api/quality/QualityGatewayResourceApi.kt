package com.tencent.devops.worker.common.api.quality

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MediaType
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

class QualityGatewayResourceApi : QualityGatewaySDKApi, AbstractBuildResourceApi() {
    companion object {
        private val logger = LoggerFactory.getLogger(QualityGatewayResourceApi::class.java)
    }

    override fun saveScriptHisMetadata(elementType: String, data: Map<String, String>): Result<String> {
        try {
            val path = "/ms/quality/api/build/metadata/saveHisMetadata?elementType=$elementType"
            val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), objectMapper.writeValueAsString(data))
            val request = buildPost(path, requestBody)
            val responseContent = request(request, "保存脚本元数据失败")
            return Result(responseContent)
        } catch (e: Exception) {
            LoggerService.addRedLine("保存脚本元数据失败: ${e.message}")
            logger.error(e.message, e)
        }
        return Result("")
    }
}