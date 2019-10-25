package com.tencent.devops.plugin.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.common.api.constant.APP_CODE
import com.tencent.devops.common.api.constant.APP_SECRET
import com.tencent.devops.common.api.util.UnicodeUtil
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.plugin.pojo.ParametersInfo
import com.tencent.devops.plugin.pojo.tcm.TcmApp
import com.tencent.devops.plugin.pojo.tcm.TcmException
import com.tencent.devops.plugin.pojo.tcm.TcmReqParam
import com.tencent.devops.plugin.pojo.tcm.TcmTemplate
import com.tencent.devops.plugin.pojo.tcm.TcmTemplateParam
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TcmService @Autowired constructor(
    private val objectMapper: ObjectMapper
) {

    @Value("\${app.code}")
    private val appCode = ""

    @Value("\${app.secret}")
    private val appSecret = ""

    @Value("\${tcm.apps.url}")
    private val getAppsUrl = ""

    @Value("\${tcm.templates.url}")
    private val getTemplatesUrl = ""

    @Value("\${tcm.templateInfo.url}")
    private val getTemplateInfoUrl = ""

    @Value("\${tcm.startTask.url}")
    private val startTaskUrl = ""

    companion object {
        private val logger = LoggerFactory.getLogger(TcmService::class.java)
        private val parser = JsonParser()
    }

    fun getApps(userId: String): List<TcmApp> {
        val params = mapOf("app_code" to appCode,
                "app_secret" to appSecret,
                "username" to userId,
                "operator" to userId)
        val requestBody = objectMapper.writeValueAsString(params)
        logger.info("tcm get apps request body for userId($userId): $requestBody")
        val request = Request.Builder()
                .url(getAppsUrl)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()
            logger.info("tcm get apps response body for userId($userId): $body")
            val json = parser.parse(body).asJsonObject
            if (!response.isSuccessful) {
                AlertUtils.doAlert(AlertLevel.CRITICAL, "fail to get apps info for userId($userId)", body)
                throw RuntimeException("fail to get apps info:$body")
            }
            val data = json["data"]
            if (data == null || data.toString() == "null") return listOf()
            return data.asJsonArray.map {
                val obj = it.asJsonObject
                TcmApp(
                        obj["buid"].asString,
                        obj["buname"].asString
                )
            }
        }
    }

    fun getTemplates(userId: String, ccid: String, tcmAppId: String): List<TcmTemplate> {
        val params = mapOf("app_code" to appCode,
                "app_secret" to appSecret,
                "username" to userId,
                "operator" to userId,
                "app_id" to ccid,
                "tcm_app_id" to tcmAppId)
        val requestBody = objectMapper.writeValueAsString(params)
        logger.info("tcm get apps templates request body for userId($userId): $requestBody")
        val request = Request.Builder()
                .url(getTemplatesUrl)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()
            logger.info("tcm get apps templates response body for userId($userId): $body")
            val json = parser.parse(body).asJsonObject
            if (!response.isSuccessful) {
                AlertUtils.doAlert(AlertLevel.CRITICAL, "fail to get templates for userId($userId), ccid($ccid)", body)
                throw RuntimeException("fail to get templates info:$body")
            }
            val data = json["data"]
            if (data == null || data.toString() == "null") return listOf()
            return data.asJsonArray.map {
                val obj = it.asJsonObject
                TcmTemplate(
                        obj["template_category"].asString,
                        obj["template_name"].asString,
                        obj["template_id"].asString
                )
            }
        }
    }

    fun getTemplateInfo(userId: String, ccid: String, tcmAppId: String, templateId: String): List<TcmTemplateParam> {
        val params = mapOf("app_code" to appCode,
                "app_secret" to appSecret,
                "username" to userId,
                "operator" to userId,
                "app_id" to ccid,
                "tcm_app_id" to tcmAppId,
                "template_id" to templateId)
        val requestBody = objectMapper.writeValueAsString(params)
        logger.info("tcm get apps template info request body for userId($userId): $requestBody")
        val request = Request.Builder()
                .url(getTemplateInfoUrl)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()
            logger.info("tcm get apps template info response body for userId($userId): $body")
            val json = parser.parse(body).asJsonObject
            if (!response.isSuccessful) {
                AlertUtils.doAlert(AlertLevel.CRITICAL, "fail to get template info for userId($userId), ccid($ccid)", body)
                throw RuntimeException("fail to get templates info:$body")
            }
            val data = json["data"]
            if (data == null || data.toString() == "null") return listOf()
            return data.asJsonArray.map {
                val obj = it.asJsonObject
                TcmTemplateParam(
                        obj["seq"].asString,
                        obj["ft_rename"].asString
                )
            }
        }
    }

    fun startTask(tcmReqParam: TcmReqParam, buildId: String, userId: String): String {
        val params = TcmRequestParam(
                tcmReqParam.operator,
                tcmReqParam.appId,
                tcmReqParam.tcmAppId,
                tcmReqParam.templateId,
                tcmReqParam.name,
                tcmReqParam.workJson,
                APP_CODE,
                APP_SECRET,
                userId
        )

        val json = objectMapper.writeValueAsString(params)
        logger.info("tcm exec request for buildId($buildId): $json")

        val request = Request.Builder()
                .url(startTaskUrl)
                .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), json))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val body = UnicodeUtil.unicodeToString(response.body()!!.string())
            logger.info("tcm exec response for app(${tcmReqParam.appId}): $body")
            try {
                val resultMap = objectMapper.readValue<Map<String, Any>>(body)
                if (resultMap["result"] == true) return body
                throw RuntimeException(resultMap["message"] as? String ?: "")
            } catch (e: Exception) {
                // 失败，告警
                AlertUtils.doAlert(AlertLevel.CRITICAL, "tcm operation fail for build($buildId)", e.message ?: "")
                throw TcmException(e.message ?: "")
            }
        }
    }

    fun getParamsList(userId: String, appId: String, tcmAppId: String, templateId: String): List<ParametersInfo> {
        val templateInfo = getTemplateInfo(userId, appId, tcmAppId, templateId)
        val params = ArrayList<ParametersInfo>()
        templateInfo.forEach { p ->
            val info = ParametersInfo(
                key = p.paramName,
                keyDisable = true,
                keyType = "input",
                keyListType = "list",
                keyUrl = "",
                keyUrlQuery = ArrayList(),
                keyList = ArrayList(),
                keyMultiple = false,
                value = "",
                valueDisable = false,
                valueType = "input",
                valueListType = "list",
                valueUrl = "",
                valueUrlQuery = ArrayList(),
                valueList = ArrayList(),
                valueMultiple = false
            )
            params.add(info)
        }
        return params
    }

    data class TcmRequestParam(
        val operator: String,
        @field:JsonProperty("app_id")
        val appId: String,
        @field:JsonProperty("tcm_app_id")
        val tcmAppId: String,
        @field:JsonProperty("template_id")
        val templateId: String,
        val name: String,
        @field:JsonProperty("workjson")
        val workJson: List<Map<String, String>>,
        @field:JsonProperty("app_code")
        val appCode: String,
        @field:JsonProperty("app_secret")
        val appSecert: String,
        val username: String
    )
}
