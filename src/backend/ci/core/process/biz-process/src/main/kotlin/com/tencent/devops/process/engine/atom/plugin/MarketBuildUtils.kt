package com.tencent.devops.process.engine.atom.plugin

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import javax.ws.rs.HttpMethod

object MarketBuildUtils {
    private const val BK_ATOM_HOOK_URL = "bk_atom_del_hook_url"
    private const val BK_ATOM_HOOK_URL_METHOD = "bk_atom_del_hook_url_method"
    private const val BK_ATOM_HOOK_URL_BODY = "bk_atom_del_hook_url_body"

    private val PROJECT_ID = "project_id"
    private val PIPELINE_ID = "pipeline_id"
    private val USER_ID = "user_id"

    private val logger = LoggerFactory.getLogger(MarketBuildUtils::class.java)

    private val marketBuildExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())!!

    fun beforeDelete(inputMap: Map<*, *>, atomCode: String, param: BeforeDeleteParam) {
        marketBuildExecutorService.execute {
            val bkAtomHookUrl = inputMap.getOrDefault(BK_ATOM_HOOK_URL, "") as String
            val bkAtomHookUrlMethod = inputMap.getOrDefault(BK_ATOM_HOOK_URL_METHOD, "") as String
            logger.info("start to execute atom delete hook url: $atomCode, $bkAtomHookUrlMethod, $bkAtomHookUrl, $param")

            if (bkAtomHookUrl.isBlank()) return@execute


            val url = resolveParam(bkAtomHookUrl, param)
            var request = Request.Builder()
                .url(url)

            when (bkAtomHookUrlMethod) {
                HttpMethod.GET -> {
                    request = request.get()
                }
                HttpMethod.POST -> {
                    val requestBody = resolveParam(inputMap.getOrDefault(BK_ATOM_HOOK_URL_BODY, "") as String, param)
                    request = request.post(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), requestBody))
                }
                HttpMethod.PUT -> {
                    val requestBody = resolveParam(inputMap.getOrDefault(BK_ATOM_HOOK_URL_BODY, "") as String, param)
                    request = request.put(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), requestBody))
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
    }

    private fun resolveParam(str: String, param: BeforeDeleteParam): String {
        return str.replace("{$PROJECT_ID}", param.projectId)
            .replace("{$PIPELINE_ID}", param.pipelineId)
            .replace("{$USER_ID}", param.userId)
    }
}
