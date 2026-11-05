package com.tencent.devops.process.trigger.api.external

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.trigger.event.GitWebhookEvent
import com.tencent.devops.process.trigger.event.GitlabWebhookEvent
import com.tencent.devops.process.trigger.event.P4WebhookEvent
import com.tencent.devops.process.trigger.event.ScmWebhookRequestEvent
import com.tencent.devops.process.trigger.event.SvnWebhookEvent
import com.tencent.devops.process.trigger.event.TGitWebhookEvent
import com.tencent.devops.process.trigger.scm.CodeWebhookEventDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.stream.function.StreamBridge

/**
 * 路径与 process 的 ExternalScmResourceImpl 相同，同进程加载会导致 Jersey 资源冲突，
 * 只能在 trigger 独立部署（trigger.standalone.enabled=true）时加载
 */
@RestResource
@ConditionalOnProperty(prefix = "trigger.standalone", name = ["enabled"], havingValue = "true")
class ExternalTriggerScmResourceImpl @Autowired constructor(
    private val streamBridge: StreamBridge,
    private val simpleDispatcher: SampleEventDispatcher
) : ExternalTriggerScmResource {

    @Value("\${scm.external.tGit.hookSecret:}")
    private val tGitWebhookSecret: String = ""

    @Value("\${scm.external.tGit.enableHookSecret}")
    private val enableTGitWebhookSecret: Boolean = false

    override fun webHookCodeSvnCommit(event: String) =
        Result(CodeWebhookEventDispatcher.dispatchEvent(streamBridge, SvnWebhookEvent(requestContent = event)))

    override fun webHookCodeGitCommit(
        event: String,
        secret: String?,
        sourceType: String?,
        traceId: String,
        body: String
    ): Result<Boolean> {
        // 工蜂的测试请求,应该忽略
        if (sourceType == "Test") {
            return Result(true)
        }
        CodeWebhookEventDispatcher.dispatchEvent(
            streamBridge = streamBridge,
            event = GitWebhookEvent(
                requestContent = body,
                event = event,
                secret = secret,
                traceId = traceId
            )
        )
        return Result(true)
    }

    override fun webHookGitlabCommit(event: String) =
        Result(CodeWebhookEventDispatcher.dispatchEvent(streamBridge, GitlabWebhookEvent(requestContent = event)))

    override fun webHookCodeTGitCommit(
        event: String,
        secret: String?,
        traceId: String,
        body: String
    ): Result<Boolean> {
        logger.info("tgit webhook secret|$secret")
        if (enableTGitWebhookSecret) {
            if (secret.isNullOrBlank()) {
                logger.warn("the secret of tgit webhook can not be empty")
                throw ParamBlankException("secret empty")
            }
            if (secret != tGitWebhookSecret) {
                logger.warn("the secret of tgit webhook is illegal")
                throw InvalidParamException(message = "secret illegal", params = arrayOf("secret"))
            }
        }
        return Result(
            CodeWebhookEventDispatcher.dispatchEvent(
                streamBridge = streamBridge,
                event = TGitWebhookEvent(
                    requestContent = body,
                    event = event,
                    secret = secret
                )
            )
        )
    }

    override fun webHookCodeP4Commit(body: String): Result<Boolean> {
        logger.info("p4 webhook|$body")
        return Result(
            CodeWebhookEventDispatcher.dispatchEvent(
                streamBridge = streamBridge,
                event = P4WebhookEvent(
                    requestContent = body
                )
            )
        )
    }

    override fun webHookScmCommit(scmCode: String, request: HttpServletRequest, body: String): Result<Boolean> {
        logger.info("scm webhook|$body")
        simpleDispatcher.dispatch(
            ScmWebhookRequestEvent(
                scmCode = scmCode,
                request = WebhookRequest(
                    headers = scmWebhookHeader(request),
                    queryParams = parseQueryString(request.queryString ?: ""),
                    body = body
                )
            )
        )
        return Result(true)
    }

    private fun scmWebhookHeader(request: HttpServletRequest) = request.headerNames.toList()
            .associateWith { request.getHeader(it) }

    private fun parseQueryString(queryString: String): Map<String, String> {
        return queryString.split("&")
                .associate {
                    val pair = it.split("=", limit = 2)
                    val key = pair[0]
                    val value = if (pair.size > 1) pair[1] else ""
                    key to value
                }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExternalTriggerScmResourceImpl::class.java)
    }
}
