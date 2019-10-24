package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.pojo.pipeline.StartUpInfo
import com.tencent.devops.process.pojo.pipeline.SubPipelineStartUpInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.ServiceOperationResource
import org.asynchttpclient.Dsl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 标准运维服务，负责拉取对应模板的运行参数返回至前端参数渲染界面
 */
@Service
class OperationService @Autowired constructor(
    private val client: Client
) {
    private val appCode: String = "ci_gcloud"
    private val appSecret: String = "r.6E\$J+xKQ,kjz+k.4Ae<b<~ol0W6.ry!%,1fx05>R&JGQWqqL"
    private val logger = LoggerFactory.getLogger(OperationService::class.java)

    /**
     * 通过process服务中的PipelineUserServiceu获取最后一次修改流水线的用户名
     * @param pipelineId 当前运行的流水线ID
     */
    fun getUserName(pipelineId: String): String {
        val res = client.get(ServiceOperationResource::class).getUpdateUser(pipelineId)
        return res.data ?: ""
    }

    /**
     * 获取对应模板的参数
     * @param id 标准运维模板ID
     * @param pipelineId 当前运行的流水线ID
     */
    fun getParam(id: String, pipelineId: String): Result<ArrayList<SubPipelineStartUpInfo>> {
        val username = getUserName(pipelineId)
        val requestData = mutableMapOf<String, String>()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["username"] = username
        val requestStr = ObjectMapper().writeValueAsString(requestData)
        val url = "http://gcloud.apigw.o.oa.com/prod/api/get_template_info/$id/"
        // 使用带请求体的GET方法请求参数
        val async = Dsl.asyncHttpClient()
        var responseStr = ""
        try {
            val res = async
                    .prepareGet(url)
                    .setBody(requestStr)
                    .execute()
                    .get()

            responseStr = res.responseBody
            logger.info("get template's params request: $responseStr")
            if (res.statusCode < 200 || res.statusCode >= 300) {
                logger.warn("get template's params request: url = $url \n request message = $requestStr \n response message = " +
                        "$responseStr \n status code = ${res.statusCode}")

                return Result(ArrayList())
            }
        } catch (e: Exception) {
            logger.error("The operation service request attitudes failed: ${e.message} ${e.stackTrace}")
        } finally {
            async.close()
        }

        val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
        val params = response["data"] as Map<*, *>
        val param = params["template_params"] as List<*>

        val parameter = ArrayList<SubPipelineStartUpInfo>()
        param.forEach { p ->
            val px = p as Map<*, *>
            // 只支持标准运维参数的文本框和输入框输入类型
            if (px["type"] == "1" || px["type"] == "2") {
                val keyList = ArrayList<StartUpInfo>()
                val valueList = ArrayList<StartUpInfo>()
                keyList.add(StartUpInfo(px["key"] as String, px[px["key"]] as String))
                valueList.add(StartUpInfo(px["key"] as String, px[px["key"]] as String))
                val info = SubPipelineStartUpInfo(
                        key = px["key"] as String,
                        keyDisable = true,
                        keyType = "input",
                        keyListType = "list",
                        keyUrl = "",
                        keyUrlQuery = ArrayList(),
                        keyList = keyList,
                        keyMultiple = false,
                        value = "",
                        valueDisable = false,
                        valueType = "input",
                        valueListType = "list",
                        valueUrl = "",
                        valueUrlQuery = ArrayList(),
                        valueList = valueList,
                        valueMultiple = false)

                parameter.add(info)
            }
        }
        return Result(parameter)
    }

    fun getList(ccId: String, pipelineId: String): Result<Page<Map<String, String>>> {
        if (ccId.isEmpty() || pipelineId.isEmpty())
            return Result(Page(0, -1, 0, mutableListOf()))
        val username = getUserName(pipelineId)
        val requestData = mutableMapOf<String, String>()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["username"] = username
        val requestStr = JsonUtil.toJson(requestData)
        val client = Dsl.asyncHttpClient()
        var response = ""
        try {
            val res = client.prepareGet("http://gcloud.apigw.o.oa.com/prod/api/get_template_list/$ccId/custom/")
                    .setBody(requestStr)
                    .execute()
                    .get()
            response = res.responseBody
        } catch (e: Exception) {
            logger.error("The operation service request attitudes failed: ${e.message} ${e.stackTrace}")
        } finally {
            client.close()
        }
        val list = jacksonObjectMapper().readValue<List<Map<String, Any>>>(response)
        val records = mutableListOf<Map<String, String>>()
        list.forEach {
            if (it["creator"] == username) {
                val map = mutableMapOf<String, String>()
                map["template_name"] = it["template_name"] as String
                map["template_id"] = it["template_id"] as String
                records.add(map)
            }
        }
        return Result(Page(0, records.size, records.size.toLong(), records))
    }
}
