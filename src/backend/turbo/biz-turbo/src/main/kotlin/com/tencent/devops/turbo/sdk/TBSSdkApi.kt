package com.tencent.devops.turbo.sdk

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_THIRDPARTY_SYSTEM_FAIL
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.turbo.config.TBSProperties
import com.tencent.devops.turbo.dto.DistccRequestBody
import com.tencent.devops.turbo.dto.DistccResponse
import com.tencent.devops.turbo.dto.ParamEnumDto
import com.tencent.devops.turbo.dto.TBSTurboStatDto
import com.tencent.devops.turbo.dto.WhiteListDto
import com.tencent.devops.web.util.SpringContextHolder
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod

@Suppress("MaxLineLength")
object TBSSdkApi {

    private val logger = LoggerFactory.getLogger(TBSSdkApi::class.java)

    private fun buildGet(path: String, headers: Map<String, String>): Request {
        return Request.Builder().url(path).headers(Headers.of(headers)).get().build()
    }

    private fun buildPost(path: String, requestBody: RequestBody, headers: Map<String, String>): Request {
        return Request.Builder().url(path).headers(Headers.of(headers)).post(requestBody).build()
    }

    private fun buildPut(path: String, requestBody: RequestBody, headers: Map<String, String>): Request {
        return Request.Builder().url(path).headers(Headers.of(headers)).put(requestBody).build()
    }

    private fun buildDelete(path: String, headers: Map<String, String>): Request {
        return Request.Builder().url(path).headers(Headers.of(headers)).delete().build()
    }

    /**
     * 查询编译加速记录统计信息
     */
    fun queryTurboStatInfo(engineCode: String, taskId: String): List<TBSTurboStatDto> {
        val responseStr = try {
            tbsCommonRequest(
                engineCode = engineCode,
                resourceName = "stats",
                queryParam = mapOf(
                    "task_id" to taskId
                )
            )
        } catch (e: Exception) {
            logger.info("no record detail found, engine code: $engineCode")
            return listOf()
        }
        val response: DistccResponse<List<TBSTurboStatDto>> = JsonUtil.to(responseStr, object : TypeReference<DistccResponse<List<TBSTurboStatDto>>>() {})
        if (response.code != 0 || !response.result) {
            logger.info("response not success!")
            throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "fail to invoke request")
        }
        return response.data
    }

    /**
     * 查询编译加速记录信息
     */
    fun queryTurboRecordInfo(engineCode: String, queryParam: Map<String, Any>): List<Map<String, Any?>> {
        val responseStr = tbsCommonRequest(
            engineCode = engineCode,
            resourceName = "task",
            queryParam = queryParam
        )
        val response: DistccResponse<List<Map<String, Any?>>> = JsonUtil.to(responseStr, object : TypeReference<DistccResponse<List<Map<String, Any?>>>>() {})
        if (response.code != 0 || !response.result) {
            logger.info("response not success!")
            throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "fail to invoke request")
        }
        return response.data
    }

    /**
     * 插入更新项目信息
     */
    fun updateTurboProjectInfo(
        engineCode: String,
        projectId: String,
        jsonBody: String
    ) {
        tbsCommonRequest(
            engineCode = engineCode,
            resourceName = "project",
            pathParam = listOf(projectId),
            jsonBody = jsonBody,
            method = "PUT"
        )
    }

    /**
     * 插入更新白名单信息
     */
    fun upsertTurboWhiteList(
        engineCode: String,
        whiteListInfo: List<WhiteListDto>,
        user: String
    ) {
        tbsCommonRequest(
            engineCode = engineCode,
            resourceName = "whitelist",
            jsonBody = JsonUtil.toJson(DistccRequestBody(user, whiteListInfo)),
            method = "PUT"
        )
    }

    private fun tbsCommonRequest(
        engineCode: String,
        resourceName: String,
        pathParam: List<String> = listOf(),
        queryParam: Map<String, Any> = mutableMapOf(),
        jsonBody: String = "",
        headers: MutableMap<String, String> = mutableMapOf(),
        method: String = "GET"
    ): String {
        val requestBody = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"), jsonBody
        )
        val properties = SpringContextHolder.getBean<TBSProperties>()
        val customPath =
            properties.urlTemplate!!.replace("{engine}", engineCode).replace("{resource_type}", resourceName)
        var url = "${properties.rootPath}/$customPath"
        if (!pathParam.isNullOrEmpty()) {
            pathParam.forEach {
                url = url.plus("/$it")
            }
        }
        if (!queryParam.isNullOrEmpty()) {
            url = url.plus(
                queryParam.entries.fold("?") { acc, entry ->
                    acc.plus("${entry.key}=${entry.value}&")
                }
            )
            url = url.trimEnd('&')
        }
        val request = when (method) {
            HttpMethod.GET.name -> {
                buildGet(url, headers)
            }
            HttpMethod.POST.name -> {
                buildPost(url, requestBody, headers)
            }
            HttpMethod.PUT.name -> {
                buildPut(url, requestBody, headers)
            }
            HttpMethod.DELETE.name -> {
                buildDelete(url, headers)
            }
            else -> {
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "http method not correct!")
            }
        }

        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.info(
                    "Fail to execute ($url) task($jsonBody) because of ${response.message()} with" +
                        " response: $responseBody"
                )
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "fail to invoke request")
            }
            return responseBody
        }
    }

    /**
     * 查询编译加速地区关联的编译环境
     */
    fun queryTurboCompileList(engineCode: String, queryParam: Map<String, Any>): List<ParamEnumDto>{
        val responseStr = tbsCommonRequest(
            engineCode = engineCode,
            resourceName = "images",
            queryParam = queryParam
        )
        val response: DistccResponse<List<ParamEnumDto>> =
            JsonUtil.to(responseStr, object : TypeReference<DistccResponse<List<ParamEnumDto>>>() {})
        if (response.code != 0 || !response.result) {
            logger.warn("response not success!")
            throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "fail to invoke request")
        }
        return response.data ?: listOf()
    }
}
