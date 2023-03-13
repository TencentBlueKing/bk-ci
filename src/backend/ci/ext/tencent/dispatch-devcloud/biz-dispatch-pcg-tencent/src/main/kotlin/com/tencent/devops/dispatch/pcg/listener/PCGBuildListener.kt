package com.tencent.devops.dispatch.pcg.listener

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.pcg.common.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * deng
 * 2019-03-26
 */
@Component
class PCGBuildListener @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter
) : BuildListener {


    @Value("\${pcg.docker.url:#{null}}")
    private val pcgDockerStartURL: String = "http://ciserver.wsd.com/interface"

    @Value("\${pcg.docker.init:#{null}}")
    val dockerInitShell: String? = null

    private var alertUserLastUpdate = 0L

    private val keepWorkspaceProjectCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(30000)
        .build(
            object : CacheLoader<String, Boolean>() {
                override fun load(projectId: String): Boolean {
                    return try {
                        redisOperation.isMember(PCG_DISPATCHER_WORKSPACE_PROJECT_CACHE, projectId)
                    } catch (t: Throwable) {
                        logger.warn("Fail to get the redis workspace keep projects", t)
                        false
                    }
                }
            }
        )

    override fun getShutdownQueue(): String {
        return ".pcg.sumeru"
    }

    override fun getStartupDemoteQueue(): String {
        return ".pcg.sumeru.demote"
    }

    override fun getStartupQueue(): String {
        return ".pcg.sumeru"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.OTHER
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("On shutdown - ($event)")
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("On start up - ($dispatchMessage)")

        val split = dispatchMessage.dispatchMessage.split(":")
        if (split.size != 5 && split.size != 4) {
            logger.warn("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}] " +
                            "The dispatch message is illegal - (${dispatchMessage.dispatchMessage})")
            onFailure(
                ErrorType.USER,
                ErrorCodeEnum.IMAGE_ILLEGAL_ERROR.errorCode,
                ErrorCodeEnum.IMAGE_ILLEGAL_ERROR.formatErrorMessage,
                "The pcg dispatch image is illegal - (${dispatchMessage.dispatchMessage})"
            )
        }

        val imgName = split[0]
        val imgVer = split[1]
        val os = split[2]
        val language = split[3]
        var useRoot = true
        if (split.size == 5) {
            useRoot = split[4].toBoolean()
        }
        logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}] " +
                        "get the pcg image ($imgName|$imgVer|$os|$language)")
        startPCGAgent(imgName, imgVer, os, language, useRoot, dispatchMessage)
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        onStartup(dispatchMessage)
    }

    private fun startPCGAgent(
        imgName: String,
        imgVer: String,
        os: String,
        language: String,
        useRoot: Boolean,
        dispatchMessage: DispatchMessage
    ) {

        val script = dockerInitShell
        val cmd = with(dispatchMessage) {
            "cd /data/inotify-tools; sh $script $id $secretKey $gateway $projectId $pipelineId $buildId $vmSeqId ${
                !keepWorkspaceProjectCache.get(
                    projectId
                )
            }"
        }

        val param = if (useRoot) {
            PCGInterfaceParam(
                cn = "",
                cmd = cmd,
                img_name = imgName,
                img_ver = imgVer,
                os = os,
                language = language,
                project_id = dispatchMessage.projectId,
                pipeline_id = dispatchMessage.pipelineId,
                build_id = dispatchMessage.buildId,
                id = dispatchMessage.id,
                user = "root"
            )
        } else {
            PCGInterfaceParam(
                cn = "",
                cmd = cmd,
                img_name = imgName,
                img_ver = imgVer,
                os = os,
                language = language,
                project_id = dispatchMessage.projectId,
                pipeline_id = dispatchMessage.pipelineId,
                build_id = dispatchMessage.buildId,
                id = dispatchMessage.id,
                user = null
            )
        }

        val requestMap = mapOf(
            "skey" to "8b116e40-5a06-4d17-a1a8-8a131f1b732d",
            "operator" to "",
            "interface_name" to "get_tc_container_info_by_cn",
            "interface_params" to JsonUtil.getObjectMapper().writeValueAsString(param)
        )
        logger.info("Start the pcg agent with url - ($pcgDockerStartURL) with body - ($requestMap)")
        val requestBody = FormBody.Builder()
        requestMap.forEach { (key, value) ->
            requestBody.add(key, value)
        }

        val request = Request.Builder()
            .url(pcgDockerStartURL)
            .post(requestBody.build())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val response = request(dispatchMessage, request, true)
        if (response.isNullOrBlank()) {
            onFailure(
                ErrorType.THIRD_PARTY,
                ErrorCodeEnum.START_UP_ERROR.errorCode,
                ErrorCodeEnum.START_UP_ERROR.formatErrorMessage,
                "Fail to start up pcg docker, response is null"
            )
        }
        //{"data":"","err_msg":"get container fail","errMsg":"没有空闲编译容器","ret_code":"500"}
        //{"data":{"password":"HI71a4173d993635","host":"9.77.29.50"},"errMsg":"","ret_code":200}
        val jsonResponse = try {
            JSONObject(response)
        } catch (e: JSONException) {
            logger.warn("The pcg response is not a json", e)
            onFailure(
                ErrorType.THIRD_PARTY,
                ErrorCodeEnum.START_UP_RESPONSE_JSON_ERROR.errorCode,
                ErrorCodeEnum.START_UP_RESPONSE_JSON_ERROR.formatErrorMessage,
                "Fail to start up pcg docker, parse responseJson error"
            )
        } as JSONObject

        val code = jsonResponse.optInt("ret_code")
        if (code != 200) {
            logger.warn("The pcg response is not ok with code $code")
            onFailure(
                ErrorType.THIRD_PARTY,
                ErrorCodeEnum.START_UP_FAIL.errorCode,
                ErrorCodeEnum.START_UP_FAIL.formatErrorMessage,
                "Fail to start up pcg docker - (${jsonResponse.optString("errMsg")})"
            )
        }

        log(
            buildLogPrinter,
            dispatchMessage.buildId,
            dispatchMessage.containerHashId,
            dispatchMessage.vmSeqId,
            "Success to start up pcg docker on ${getHost(dispatchMessage, jsonResponse)}",
            dispatchMessage.executeCount
        )
    }

    override fun parseMessageTemplate(content: String, data: Map<String, String>): String {
        if (content.isBlank()) {
            return content
        }
        val pattern = Pattern.compile("#\\{([^}]+)}")
        val newValue = StringBuffer(content.length)
        val matcher = pattern.matcher(content)
        while (matcher.find()) {
            val key = matcher.group(1)
            val variable = data[key] ?: ""
            matcher.appendReplacement(newValue, variable)
        }
        matcher.appendTail(newValue)
        return newValue.toString()
    }

    private fun expire(): Boolean {
        return System.currentTimeMillis() - alertUserLastUpdate >= (ALERT_CACHE_EXPIRE * 1000)
    }

    private fun getHost(dispatchMessage: DispatchMessage, jsonResponse: JSONObject): String {
        return try {
            jsonResponse.optJSONObject("data").optString("host")
        } catch (t: Throwable) {
            logger.warn("[${dispatchMessage.buildId}] Fail to get the pcg docker host - $jsonResponse", t)
            logRed(
                buildLogPrinter,
                dispatchMessage.buildId,
                dispatchMessage.containerHashId,
                dispatchMessage.vmSeqId,
                "Fail to get the pcg docker host",
                dispatchMessage.executeCount
            )
            ""
        }
    }

    private fun request(dispatchMessage: DispatchMessage, request: Request, retry: Boolean = false): String? {
        okHttpClient.newBuilder().build().newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to start pcg image docker - ($dispatchMessage) " +
                                "with response(${response.code}|${response.message}|${response.body}) ")
                if (retry) {
                    return request(dispatchMessage, request, false)
                }
                return null
            }
            logger.info("Get the response - ($body) with message($dispatchMessage)")
            return body
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(NetInterceptor())
        .connectTimeout(5L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.MINUTES) // Set to 30 minutes
        .writeTimeout(60L, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private data class PCGInterfaceParam(
        val cn: String,
        val cmd: String,
        val img_name: String,
        val img_ver: String,
        val os: String,
        val language: String,
        val project_id: String,
        val pipeline_id: String,
        val build_id: String,
        val id: String,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        val user: String?
    )

    companion object {
        private val logger = LoggerFactory.getLogger(PCGBuildListener::class.java)
        private const val ALERT_CACHE_EXPIRE = 10 // expire in 10 seconds
        private const val PCG_DISPATCHER_WORKSPACE_PROJECT_CACHE = "dispatcher:pcg:workspace:project:cache"
    }

    class NetInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .addHeader("Connection", "close").build()
            return chain.proceed(request)
        }
    }
}
