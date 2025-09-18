package com.tencent.devops.scm.services

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ScmGithubExtService {
    @Value("\${scm.bkCode.callbackUrl:}")
    private val bkCodeCallbackUrl: String = ""

    fun webhookCommit(event: String, guid: String, signature: String, body: String) {
        try {
            val headers = mapOf(
                "X-GitHub-Event" to event,
                "X-Github-Delivery" to guid,
                "X-Hub-Signature" to signature
            )
            OkhttpUtils.doShortPost(
                url = bkCodeCallbackUrl,
                headers = headers,
                jsonParam = JsonUtil.toJson(body)
            )
        } catch (ignore: Exception) {
            logger.warn("Failed to sync github webhook", ignore)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmGithubExtService::class.java)
    }
}
