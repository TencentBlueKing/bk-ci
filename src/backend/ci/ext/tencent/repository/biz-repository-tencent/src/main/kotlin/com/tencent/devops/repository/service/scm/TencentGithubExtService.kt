package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.trace.TraceTag.BIZID
import com.tencent.devops.repository.service.github.IGithubExtService
import com.tencent.devops.scm.api.ServiceGithubExtResource
import org.apache.lucene.util.NamedThreadFactory
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
@Primary
class TencentGithubExtService @Autowired constructor(
    private val client: Client
) : IGithubExtService {
    override fun webhookCommit(
        event: String,
        guid: String,
        signature: String,
        body: String
    ) {
        val bizId = MDC.get(BIZID)
        threadPool.execute {
            try {
                MDC.put(BIZID, bizId)
                logger.info("sync github webhook to bkcode")
                client.getScm(ServiceGithubExtResource::class).webhookCommit(
                    event = event,
                    guid = guid,
                    signature = signature,
                    body = body
                )
            } catch (ignored: Exception) {
                logger.warn("Failed to sync github webhook", ignored)
            } finally {
                MDC.remove(BIZID)
            }
        }
    }

    companion object {
        private val threadPool = Executors.newFixedThreadPool(5, NamedThreadFactory("github-webhook-sync"))
        private val logger = LoggerFactory.getLogger(TencentGithubExtService::class.java)
    }
}
