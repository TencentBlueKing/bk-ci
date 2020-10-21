package com.tencent.devops.process.engine.atom.plugin

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.plugin.codecc.CodeccUtils
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.ws.rs.HttpMethod

object MarketBuildUtils {
    private const val BK_ATOM_HOOK_URL = "bk_atom_del_hook_url"
    private const val BK_ATOM_HOOK_URL_METHOD = "bk_atom_del_hook_url_method"
    private const val BK_ATOM_HOOK_URL_BODY = "bk_atom_del_hook_url_body"

    private const val PROJECT_ID = "projectId"
    private const val PIPELINE_ID = "pipelineId"
    private const val USER_ID = "userId"

    private val logger = LoggerFactory.getLogger(MarketBuildUtils::class.java)

    private val marketBuildExecutorService = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors(),
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(16000)
    )

    fun beforeDelete(inputMap: Map<*, *>, atomCode: String, param: BeforeDeleteParam, codeccApi: CodeccApi) {
        marketBuildExecutorService.execute {
            val bkAtomHookUrl = inputMap.getOrDefault(BK_ATOM_HOOK_URL, getDefaultHookUrl(atomCode, codeccApi, param.channelCode)) as String
            val bkAtomHookUrlMethod = inputMap.getOrDefault(BK_ATOM_HOOK_URL_METHOD, getDefaultHookMethod(atomCode)) as String
            val bkAtomHookBody = inputMap.getOrDefault(BK_ATOM_HOOK_URL_BODY, "") as String
            logger.info("start to execute atom delete hook url: $atomCode, $bkAtomHookUrlMethod, $bkAtomHookUrl, $param")

            if (bkAtomHookUrl.isBlank()) return@execute

            doHttp(bkAtomHookUrl, bkAtomHookUrlMethod, bkAtomHookBody, param)
        }
    }

    private fun doHttp(bkAtomHookUrl: String, bkAtomHookUrlMethod: String, bkAtomHookBody: String, param: BeforeDeleteParam) {
        val url = resolveParam(bkAtomHookUrl, param)
        var request = Request.Builder()
            .url(url)

        when (bkAtomHookUrlMethod) {
            HttpMethod.GET -> {
                request = request.get()
            }
            HttpMethod.POST -> {
                val requestBody = resolveParam(bkAtomHookBody, param)
                request = request.post(RequestBody.create(OkhttpUtils.jsonMediaType, requestBody))
            }
            HttpMethod.PUT -> {
                val requestBody = resolveParam(bkAtomHookBody, param)
                request = request.put(RequestBody.create(OkhttpUtils.jsonMediaType, requestBody))
            }
            HttpMethod.DELETE -> {
                request = request.delete()
            }
        }

        OkhttpUtils.doHttp(request.build()).use { response ->
            val body = response.body()!!.string()
            logger.info("before delete execute result: $url, $body")
        }
    }

    private fun getDefaultHookUrl(atomCode: String, codeccApi: CodeccApi, channelCode: ChannelCode): String {
        if (!CodeccUtils.isCodeccNewAtom(atomCode)) return ""
        if (channelCode != ChannelCode.BS) return ""
        return codeccApi.getExecUrl("/ms/task/api/service/task/pipeline/stop?userName={userId}&pipelineId={$PIPELINE_ID}")
    }

    private fun getDefaultHookMethod(atomCode: String): String {
        if (!CodeccUtils.isCodeccNewAtom(atomCode)) return "GET"
        return "DELETE"
    }

    private fun resolveParam(str: String, param: BeforeDeleteParam): String {
        return str.replace("{$PROJECT_ID}", param.projectId)
            .replace("{$PIPELINE_ID}", param.pipelineId)
            .replace("{$USER_ID}", param.userId)
    }
}
