package com.tencent.devops.common.environment.agent.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.environment.agent.pojo.agent.BcsVmNode
import com.tencent.devops.common.environment.agent.pojo.agent.NodeStatusAndOS
import com.tencent.devops.common.environment.agent.utils.BcsVmStatusUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response

class BcsClient constructor(
    private val bkAuthTokenApi: AuthTokenApi,
    private val serviceCode: EnvironmentAuthServiceCode
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BcsClient::class.java)
        private val apiVersion = "v1"
        private val kind = "vmsets"
        private val JSON = MediaType.parse("application/json;charset=utf-8")

        private val APIGW_SERVER = "http://api.apigw-biz.o.oa.com"
    }

    fun createVM(clusterId: String, namespace: String, instanceCount: Int, image: String, resCpu: String, resMemory: String): List<BcsVmNode> {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = "$APIGW_SERVER/api/bcss_api/prod/v1/containerware/vmsets?access_token=$token"

        val requestData = mapOf("apiVersion" to apiVersion,
                "kind" to kind,
                "metadata" to mapOf("cluster" to clusterId, "namespace" to namespace),
                "spec" to mapOf("instance" to instanceCount,
                        "resources" to mapOf("limits" to mapOf("cpu" to resCpu, "memory" to resMemory)),
                        "image" to mapOf("image" to image)))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody)).build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//            val response = call.execute()
                val responseBody = response.body()?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["code"] != 0) {
                    val message = responseData["message"]
                    logger.error("create VM failed: $message")
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "创建 BCSVM 节点失败")
                }

                val dataList = responseData["data"] as List<Map<String, Any>>
                logger.info("create vm dataList: $dataList")
                return dataList.map {
                    BcsVmNode(
                        it["name"] as String,
                        clusterId,
                        namespace,
                        (it["image"] as Map<String, Any>)["osName"] as String,
                        it["ip"] as String,
                        BcsVmStatusUtils.parseBcsVmStatus(it["status"] as String).name
                    )
                }
            } catch (e: Exception) {
                logger.error("create VM error", e)
                throw OperationException("创建BCS虚拟机失败, 请联系【蓝盾助手】")
            }
        }
    }

    fun deleteVm(vmList: List<BcsVmNode>) {
        for (bcsVmNode in vmList) {
            val token = bkAuthTokenApi.getAccessToken(serviceCode)
            val url = "$APIGW_SERVER/api/bcss_api/prod/v1/containerware/clusters/${bcsVmNode.clusterId}/namespaces/${bcsVmNode.namespace}/vmsets/${bcsVmNode.name}?enforce=0&access_token=$token"

            logger.info("DELETE url: $url")

            val request = Request.Builder().url(url).delete().build()
            OkhttpUtils.doHttp(request).use { response ->
                try {
//                val response = call.execute()
                    val responseBody = response.body()?.string()
                    logger.info("responseBody: $responseBody")

                    val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                    if (responseData["code"] != 0) {
                        val message = responseData["message"]
                        logger.error("delete VM failed: $message")
                        throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "删除 BCSVM 节点失败")
                    }
                } catch (e: Exception) {
                    logger.error("delete VM error", e)
                    throw OperationException("删除BCS虚拟机失败")
                }
            }
        }
    }

    fun getVmList(cluster: String, namespace: String): List<BcsVmNode> {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = "$APIGW_SERVER/api/bcss_api/prod/v1/containerware/clusters/$cluster/namespaces/$namespace/vmsets?access_token=$token"

        logger.info("Get url: $url")
        val request = Request.Builder().url(url).get().build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//            val response = call.execute()
                val responseBody = response.body()?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["code"] != 0) {
                    val message = responseData["message"]
                    logger.error("Get vm list failed: $message")
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "查询 BCSVM 列表失败")
                }
                val dataList = responseData["data"] as List<Map<String, Any>>
                logger.info("Get vm list: $dataList")
                return dataList.map {
                    BcsVmNode(
                        it["name"] as String,
                        cluster,
                        namespace,
                        (it["image"] as Map<String, Any>)["osName"] as String,
                        it["ip"] as String,
                        BcsVmStatusUtils.parseBcsVmStatus(it["status"] as String).name
                    )
                }
            } catch (e: Exception) {
                logger.error("Get vm list error", e)
                throw OperationException("获取BCS虚拟机列表失败")
            }
        }
    }

    fun resetVm(bcsVmNode: BcsVmNode) {
        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = "$APIGW_SERVER/api/bcss_api/prod/v1/containerware/clusters/${bcsVmNode.clusterId}/namespaces/${bcsVmNode.namespace}/vmsets/${bcsVmNode.name}/reset?access_token=$token"
        logger.info("Put url: $url")
        val request = Request.Builder().url(url).put(RequestBody.create(JSON, "")).build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//            val response = call.execute()
                val responseBody = response.body()?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["code"] != 0) {
                    val message = responseData["message"]
                    logger.error("Reset vm failed: $message")
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "重置 BCSVM 节点失败")
                }
            } catch (e: Exception) {
                logger.error("Reset vm error", e)
                throw OperationException("重置BCS虚拟机失败")
            }
        }
    }

    fun inspectVmList(clusterId: String?, namespace: String?, nodeNames: String): NodeStatusAndOS? {

        val token = bkAuthTokenApi.getAccessToken(serviceCode)
        val url = "$APIGW_SERVER/api/bcss_api/prod/v1/containerware/clusters/$clusterId/namespaces/$namespace/vmsets/$nodeNames?access_token=$token"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get().build()
        OkhttpUtils.doHttp(request).use { response ->
            //        val call = okHttpClient.newCall(request)
            try {
//            val response = call.execute()
                val responseBody = response.body()?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["code"] != 0) {
                    val message = responseData["message"]
                    logger.error("Get vm list failed: $message")
                    // throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "查询BCSVM状态失败")
                    return null
                }
                val dataList = responseData["data"] as List<Map<String, *>>
                val nameToNodeMap = dataList.associate { it["name"] as String to it }

                if (nameToNodeMap.containsKey(nodeNames)) {
                    val node = nameToNodeMap.getValue(nodeNames)
                    val status = BcsVmStatusUtils.parseBcsVmStatus(node["status"] as String).name
                    val osName = (node["image"] as Map<String, *>)["osName"] as String
                    return NodeStatusAndOS(status, osName)
                }
            } catch (e: Exception) {
                logger.error("Get vm list error", e)
                throw OperationException("获取BCS虚拟机状态失败")
            }
        }
        return null
    }
}