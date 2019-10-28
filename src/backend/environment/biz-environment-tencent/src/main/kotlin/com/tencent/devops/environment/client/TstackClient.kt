package com.tencent.devops.environment.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.RawFloatingIp
import com.tencent.devops.environment.pojo.RawTstackVm
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TstackClient {
    companion object {
        private val logger = LoggerFactory.getLogger(TstackClient::class.java)
        private val GET_TOKEN_REQUEST = "{\"auth\":{\"scope\":{\"project\":{\"domain\":{\"name\":\"Default\"},\"name\":\"ALL_DEFAULT\"}},\"identity\":{\"password\":{\"user\":{\"domain\":{\"name\":\"Default\"},\"password\":\"###password\",\"name\":\"###username\"}},\"methods\":[\"password\"]}}}"
        private val USERNAME_PLACE_HOLDER = "###username"
        private val PASSWORD_PLACE_HOLDER = "###password"

        private val VOLUME_STATUS_AVAILABLE = "available"
        private val VOLUME_STATUS_INUSE = "in-use"
        private val VOLUME_STATUS_CREATING = "creating"

        private val TSTACK_PROJECT_ID = "2a90c3297342408f876f824d93ac0630"

        private val VOLUME_SNAPSHOT_ID = "914f545f-ba98-4f7b-a6fc-d92a7608d70d"
        private val NETWORK_ID = "d42a5a12-66b9-4700-b7ad-5609f2873d82"
    }

    @Value("\${env.tstack.username:#{null}}")
    private val tstackUsername: String? = null

    @Value("\${env.tstack.password:#{null}}")
    private val tstackPassword: String? = null

    @Value("\${env.tstack.server:#{null}}")
    private val server: String? = null

    @Value("\${env.tstack.tokenPort:#{null}}")
    private val tokenPort: String? = null

    @Value("\${env.tstack.computePort:#{null}}")
    private val computePort: String? = null

    @Value("\${env.tstack.volumePort:#{null}}")
    private val volumePort: String? = null

    @Value("\${env.tstack.networkPort:#{null}}")
    private val networkPort: String? = null

    @Value("\${env.tstack.defaultVolumeSize:#{null}}")
    private val defaultVolumeSize: Int? = null

//    private val okHttpClient = HttpUtil.getHttpClient()

    fun getToken(): String {
        val url = "$server:$tokenPort/v3/auth/tokens"
        val requestBody = GET_TOKEN_REQUEST
                .replace(USERNAME_PLACE_HOLDER, tstackUsername!!)
                .replace(PASSWORD_PLACE_HOLDER, tstackPassword!!)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")
        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseBody = response.body()!!.string()
                logger.error("failed to get tstack token, responseBody: $responseBody")
                throw RuntimeException("failed to get tstack token")
            }
            val token = response.header("X-Subject-Token")!!
            logger.info("token: $token")
            return token
        }
    }

    fun syncCreateVolume(pipelineId: String, volumeName: String): String {
        val token = getToken()

        val volumeId = createVolume(token, pipelineId, volumeName)
        val pollingStart = System.currentTimeMillis()
        loop@ while (true) {
            Thread.sleep(3 * 1000)
            if ((System.currentTimeMillis() - pollingStart) > 60 * 1000) {
                throw RuntimeException("polling create volume timeout(60s)")
            }
            val status = getVolumeStatus(token, volumeId)
            when (status) {
                VOLUME_STATUS_CREATING -> continue@loop
                VOLUME_STATUS_AVAILABLE -> return volumeId
                else -> throw RuntimeException("unexpected volume status")
            }
        }
    }

    fun createVolume(token: String, pipelineId: String, volumeName: String): String {
        val url = "$server:$volumePort/v2/$TSTACK_PROJECT_ID/volumes"
        val requestData = mapOf("volume" to mapOf(
                "name" to volumeName,
                "size" to defaultVolumeSize,
                "snapshot_id" to VOLUME_SNAPSHOT_ID,
                "description" to "volume for pipeline $pipelineId",
                "metadata" to emptyMap<String, Any>()
        ))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("failed to create tstack volume, responseBody: $responseBody")
                throw RuntimeException("failed to create tstack volume")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val volume = responseData["volume"] as Map<String, Any>
            return volume["id"] as String
        }
    }

    fun getVolumeStatus(token: String, volumeId: String): String {
        val url = "$server:$volumePort/v2/$TSTACK_PROJECT_ID/volumes/$volumeId"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("failed to get tstack volume status, responseBody: $responseBody")
                throw RuntimeException("failed to get tstack volume status")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val volume = responseData["volume"] as Map<String, Any>
            return volume["status"] as String
        }
    }

    fun attachVolume(tstackVmId: String, volumeId: String) {
        val token = getToken()
        val url = "$server:$computePort/v2/$TSTACK_PROJECT_ID/servers/$tstackVmId/os-volume_attachments"
        val requestData = mapOf(
                "volumeAttachment" to mapOf("volumeId" to volumeId))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $requestBody")
            if (!response.isSuccessful) {
                logger.error("failed to attach volume, responseBody: $responseBody")
                throw RuntimeException("failed to attach volume")
            }
        }
    }

    fun detachVolume(tstackVmId: String, volumeId: String) {
        val token = getToken()
        val url = "$server:$computePort/v2/$TSTACK_PROJECT_ID/servers/$tstackVmId/os-volume_attachments/$volumeId"
        logger.info("DELETE url: $url")

        val request = Request.Builder().url(url).delete()
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful && response.code() != 404) {
                val responseBody = response.body()!!.string()
                logger.error("failed to detach volume, responseBody: $responseBody")
                throw RuntimeException("failed to detach volume")
            }
        }
    }

    fun createVncToken(tstackVmId: String): String {
        val token = getToken()
        val url = "$server:$computePort/v2/$TSTACK_PROJECT_ID/servers/$tstackVmId/createEnv"

        val requestData = mapOf("os-getVNCConsole" to mapOf("type" to "novnc"))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("failed to get VNC token, responseBody: $responseBody")
                throw RuntimeException("failed to get VNC token, responseBody")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)

            val console = responseData["console"] as Map<String, Any>
            val vncUrl = console["url"] as String
            return parseVncToken(vncUrl)
        }
    }

    private fun parseVncToken(vncUrl: String): String {
        // "http://9.30.3.86:6080/vnc_auto.html?token=7dc6f20d-a4e5-4aeb-b40f-a8e1113d7e4d"
        val start = vncUrl.indexOf("=")
        return if (start == -1) {
            throw RuntimeException("parse vnc token failed, vncUrl: $vncUrl")
        } else {
            return vncUrl.substring(start + 1)
        }
    }

    fun createVm(token: String, serverName: String, imageId: String, flavorId: String): RawTstackVm {
        /** {
            "server": {
              "name": "test20180827",
              "imageRef": "551c3b1b-0e79-4427-a170-09737102fb7c",
              "flavorRef": "5",
              "networks": [
                {
                  "uuid": "d42a5a12-66b9-4700-b7ad-5609f2873d82"
                }
              ]
           }
        } */
        val url = "$server:$computePort/v2/$TSTACK_PROJECT_ID/servers"

        val requestData = mapOf("server" to mapOf(
                "name" to serverName,
                "imageRef" to imageId,
                "flavorRef" to flavorId,
                "networks" to listOf(mapOf("uuid" to NETWORK_ID))
        ))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("failed to create tstack vm, responseBody: $responseBody")
                throw RuntimeException("failed to create tstack vm")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val server = responseData["server"] as Map<String, Any>
            val id = server["id"] as String
            val adminPassword = server["adminPass"] as String
            return RawTstackVm(id, adminPassword)
        }
    }

    fun getVmStatus(vmId: String): String {
        val token = getToken()
        val url = "$server:$computePort/v2/$TSTACK_PROJECT_ID/servers/$vmId"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("failed to get tstack vm info, responseBody: $responseBody")
                throw RuntimeException("failed to get tstack vm info")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val server = responseData["server"] as Map<String, Any>
            return server["status"] as String
        }
    }

    fun deleteVm(vmId: String) {
        val token = getToken()
        val url = "$server:$computePort/v2/$TSTACK_PROJECT_ID/servers/$vmId"
        logger.info("DELETE url: $url")

        val request = Request.Builder().url(url).delete()
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val success = response.isSuccessful || response.code() == 404
            if (!success) {
                val responseBody = response.body()!!.string()
                logger.error("failed to delete vm, responseBody: $responseBody")
                throw RuntimeException("failed to delete vm")
            }
        }
    }

    fun listFloatingIp(): List<RawFloatingIp> {
        val token = getToken()
        val url = "$server:$networkPort/v2.0/floatingips"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("failed to list floating ips, responseBody: $responseBody")
                throw RuntimeException("failed to list floating ips")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val floatingIpData = responseData["floatingips"] as List<Map<String, Any>>

            val floatingIps = mutableListOf<RawFloatingIp>()
            floatingIpData.forEach {
                val floatingIp = RawFloatingIp().apply {
                    floatingNetworkId = (it["floating_network_id"] ?: "") as String
                    fixedIp = (it["fixed_ip_address"] ?: "") as String
                    floatingIp = (it["floating_ip_address"] ?: "") as String
                    tenantId = (it["tenant_id"] ?: "") as String
                    status = (it["status"] ?: "") as String
                    portId = (it["port_id"] ?: "") as String
                    id = (it["id"] ?: "") as String
                }
                floatingIps.add(floatingIp)
            }
            return floatingIps
        }
    }

    fun createFloatingIp(): RawFloatingIp {
        val token = getToken()
        val url = "$server:$networkPort/v2.0/floatingips"
        /*{
            "floatingip": {
            "floating_network_id": "6723629e-196a-4f70-884e-0b2724daa188"
            }
        }*/
        val requestData = mapOf("floatingip" to mapOf(
                "floating_network_id" to NETWORK_ID
        ))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            /*{
                "floatingip": {
                    "floating_network_id": "c6d058f4-5de3-4429-b119-cb8173e3ea5b",
                    "router_id": null,
                    "fixed_ip_address": null,
                    "floating_ip_address": "9.77.5.61",
                    "tenant_id": "2a90c3297342408f876f824d93ac0630",
                    "status": "DOWN",
                    "port_id": null,
                    "id": "71859851-fd47-4704-bd4e-214698e581bd"
                }
            }*/
            if (!response.isSuccessful) {
                logger.error("failed to create floating ip, responseBody: $responseBody")
                throw RuntimeException("failed to create floating ip")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val floatingIpData = responseData["floatingip"] as Map<String, Any>
            return RawFloatingIp().apply {
                    floatingNetworkId = (floatingIpData["floating_network_id"] ?: "") as String
                    fixedIp = (floatingIpData["fixed_ip_address"] ?: "") as String
                    floatingIp = (floatingIpData["floating_ip_address"] ?: "") as String
                    tenantId = (floatingIpData["tenant_id"] ?: "") as String
                    status = (floatingIpData["status"] ?: "") as String
                    portId = (floatingIpData["port_id"] ?: "") as String
                    id = (floatingIpData["id"] ?: "") as String
            }
        }
    }

    fun getFloatingIpInfo(floatingIpId: String): RawFloatingIp {
        val token = getToken()
        val url = "$server:$networkPort/v2.0/floatingips/$floatingIpId"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("failed to get floating ip info, responseBody: $responseBody")
                throw RuntimeException("failed to get floating ip info")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val floatingIpData = responseData["floatingip"] as Map<String, Any>
            return RawFloatingIp().apply {
                floatingNetworkId = (floatingIpData["floating_network_id"] ?: "") as String
                fixedIp = (floatingIpData["fixed_ip_address"] ?: "") as String
                floatingIp = (floatingIpData["floating_ip_address"] ?: "") as String
                tenantId = (floatingIpData["tenant_id"] ?: "") as String
                status = (floatingIpData["status"] ?: "") as String
                portId = (floatingIpData["port_id"] ?: "") as String
                id = (floatingIpData["id"] ?: "") as String
            }
        }
    }

    fun deleteFloatingIp(floatingIpId: String) {
        val token = getToken()
        val url = "$server:$networkPort/v2.0/floatingips/$floatingIpId"
        logger.info("DELETE url: $url")

        val request = Request.Builder().url(url).delete()
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val success = response.isSuccessful || response.code() == 404
            if (!success) {
                val responseBody = response.body()!!.string()
                logger.error("failed to delete floating ip, responseBody: $responseBody")
                throw RuntimeException("failed to delete floating ip")
            }
        }
    }

    fun getVmPortId(vmId: String): String {
        val token = getToken()
        val url = "$server:$computePort/v2/$TSTACK_PROJECT_ID/servers/$vmId/os-interface"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("failed to get tstack vm info, responseBody: $responseBody")
                throw RuntimeException("failed to get tstack vm info")
            }
            /*{
                "interfaceAttachments": [
                {
                    "port_state": "ACTIVE",
                    "fixed_ips": [
                    {
                        "subnet_id": "e9e81d43-8fab-4a27-a7c9-c912a78516c6",
                        "ip_address": "192.168.100.16"
                    }
                    ],
                    "port_id": "b2ea6245-6733-4d81-bc5d-21acc19f7aa9",
                    "net_id": "d42a5a12-66b9-4700-b7ad-5609f2873d82",
                    "mac_addr": "fa:16:3e:54:ea:0a"
                }
                ]
            }*/
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val interfaceAttachments = responseData["interfaceAttachments"] as List<Map<String, Any>>
            if (interfaceAttachments.isEmpty()) {
                throw RuntimeException("failed to get stack vm interface, interfaceAttachments jis empty")
            }
            return (interfaceAttachments[0]["port_id"] ?: throw RuntimeException("portId not found")) as String
        }
    }

    fun bindFloatingIpToVm(floatingIpId: String, statskVmId: String?): RawFloatingIp {
        val token = getToken()
        val url = "$server:$networkPort/v2.0/floatingips/$floatingIpId"
        /*{
            "floatingip": {
                "port_id": "305b84bd-897e-49c5-bb19-b41c0ca59c2a"
                }
        }*/
        val requestData: Map<String, Any>? =
            if (statskVmId.isNullOrBlank()) {
                mapOf(
                    "floatingip" to mapOf(
                        "port_id" to null
                    )
                )
            } else {
                val vmPortId = getVmPortId(statskVmId!!)
                mapOf(
                    "floatingip" to mapOf(
                        "port_id" to vmPortId
                    )
                )
            }
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("PUT url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .put(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("failed to band floating IP to tstack vm, responseBody: $responseBody")
                throw RuntimeException("failed to band floating IP to tstack vm")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val floatingIpData = responseData["floatingip"] as Map<String, Any>
            return RawFloatingIp().apply {
                floatingNetworkId = (floatingIpData["floating_network_id"] ?: "") as String
                fixedIp = (floatingIpData["fixed_ip_address"] ?: "") as String
                floatingIp = (floatingIpData["floating_ip_address"] ?: "") as String
                tenantId = (floatingIpData["tenant_id"] ?: "") as String
                status = (floatingIpData["status"] ?: "") as String
                portId = (floatingIpData["port_id"] ?: "") as String
                id = (floatingIpData["id"] ?: "") as String
            }
        }
    }

    fun unBindFloatingIpToVm(floatingIpId: String): RawFloatingIp {
        return bindFloatingIpToVm(floatingIpId, null)
    }
}

// fun main(args: Array<String>) {
//    ObjectMapper().writeValueAsString(mapOf("floatingip" to mapOf(
//            "port_id" to null
//    )))
// }
