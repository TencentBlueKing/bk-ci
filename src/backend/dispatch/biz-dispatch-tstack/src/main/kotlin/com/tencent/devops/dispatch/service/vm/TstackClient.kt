/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.service.vm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.Profile
import com.tencent.devops.dispatch.cron.TstackSystemConfig
import com.tencent.devops.dispatch.pojo.RawFloatingIp
import com.tencent.devops.dispatch.pojo.RawTstackVm
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TstackClient @Autowired constructor(
    private val tstackSystemConfig: TstackSystemConfig,
    private val profile: Profile
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TstackClient::class.java)

        private val GET_TOKEN_REQUEST = "{\"auth\":{\"scope\":{\"project\":{\"domain\":{\"name\":\"Default\"},\"name\":\"ALL_DEFAULT\"}},\"identity\":{\"password\":{\"user\":{\"domain\":{\"name\":\"Default\"},\"password\":\"###password\",\"name\":\"###username\"}},\"methods\":[\"password\"]}}}"
        private val USERNAME_PLACE_HOLDER = "###username"
        private val PASSWORD_PLACE_HOLDER = "###password"

        private val VOLUME_STATUS_AVAILABLE = "available"
        // private val VOLUME_STATUS_IN_USE = "in-use"
        private val VOLUME_STATUS_CREATING = "creating"

        private val VM_STATUS_ACTIVE = "ACTIVE"
        private val VM_STATUS_BUILD = "BUILD"

        private val FLOATING_IP_STATUS_DOWN = "DOWN"
        private val FLOATING_IP_STATUS_ACTIVE = "ACTIVE"
    }

    @Value("\${dispatch.tstack.username:#{null}}")
    private val tstackUsername: String? = null

    @Value("\${dispatch.tstack.password:#{null}}")
    private val tstackPassword: String? = null

    @Value("\${dispatch.tstack.server:#{null}}")
    private val tstackServer: String? = null

//    private val okHttpClient = HttpUtil.getHttpClient()

    fun getToken(): String {
        val url = "$tstackServer/v3/auth/tokens"
        val requestBody = GET_TOKEN_REQUEST
                .replace(USERNAME_PLACE_HOLDER, tstackUsername!!)
                .replace(PASSWORD_PLACE_HOLDER, tstackPassword!!)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")
        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("Tstack-Service", "token")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to get tstack token")
                throw RuntimeException("failed to get tstack token")
            }
            val token = response.header("X-Subject-Token")!!
            logger.info("token: $token")
            return token
        }
    }

    fun syncCreateVolume(pipelineId: String, vmSetId: String): String {
        val token = getToken()
        val volumeName = "${getProfileStr()}_volume_${pipelineId}_$vmSetId"
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
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/volumes"
        val requestData = mapOf("volume" to mapOf(
                "name" to volumeName,
                "size" to systemConfig.defaultVolumeSize,
                "snapshot_id" to systemConfig.volumeSnapshot,
                "description" to "volume for pipeline $pipelineId",
                "metadata" to emptyMap<String, Any>()
        ))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "volumn")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to create tstack volume")
                throw RuntimeException("failed to create tstack volume")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val volume = responseData["volume"] as Map<String, Any>
            return volume["id"] as String
        }
    }

    fun getVolumeStatus(token: String, volumeId: String): String {
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/volumes/$volumeId"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "volumn")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to get tstack volume status")
                throw RuntimeException("failed to get tstack volume status")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val volume = responseData["volume"] as Map<String, Any>
            return volume["status"] as String
        }
    }

    fun attachVolume(tstackVmId: String, volumeId: String) {
        val token = getToken()
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/servers/$tstackVmId/os-volume_attachments"
        val requestData = mapOf(
                "volumeAttachment" to mapOf("volumeId" to volumeId))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "compute")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to attach volume")
                throw RuntimeException("failed to attach volume")
            }
        }
    }

    fun detachVolume(tstackVmId: String, volumeId: String) {
        val token = getToken()
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/servers/$tstackVmId/os-volume_attachments/$volumeId"
        logger.info("DELETE url: $url")

        val request = Request.Builder().url(url).delete()
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "compute")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful && response.code() != 404) {
                val responseBody = response.body()!!.string()
                logger.error("failed to detach volume, responseBody: $responseBody")
                throw RuntimeException("failed to detach volume")
            } else {
                logger.info("detach volume success, response code: ${response.code()}")
            }
        }
    }

    fun createVncToken(tstackVmId: String): String {
        val token = getToken()
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/servers/$tstackVmId/action"

        val requestData = mapOf("os-getVNCConsole" to mapOf("type" to "novnc"))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "compute")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to get VNC token")
                throw RuntimeException("failed to get VNC token")
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

    fun syncBindFloatingIpToVm(token: String, floatingIpId: String, vmId: String) {
        bindFloatingIpToVm(token, floatingIpId, vmId)
        val pollingStart = System.currentTimeMillis()
        loop@ while (true) {
            Thread.sleep(3 * 1000)
            if ((System.currentTimeMillis() - pollingStart) > 20 * 1000) {
                throw RuntimeException("polling create volume timeout(20s)")
            }
            val floatingIp = getFloatingIpInfo(floatingIpId)
            when {
                floatingIp.status == FLOATING_IP_STATUS_DOWN -> continue@loop
                (floatingIp.status == FLOATING_IP_STATUS_ACTIVE && !floatingIp.fixedIp.isNullOrBlank()) -> return
                floatingIp.status == "" -> return
                else -> throw RuntimeException("unexpected floating ip status")
            }
        }
    }

    fun getOneFreeFloatingIp(token: String): RawFloatingIp {
        val freeFloatingIps = listFloatingIp().filter { it.fixedIp.isNullOrBlank() }
        val usableFloatingIps = tstackSystemConfig.getTstackFloatingIps()
        freeFloatingIps.forEach {
            if (usableFloatingIps.contains(it.floatingIp)) {
                return it
            }
        }
        logger.error("no free floating IP, abort")
        throw RuntimeException("no free floating IP")
    }

    fun syncCreateBuildVm(imageId: String, flavorId: String): RawTstackVm {
        val token = getToken()
        val floatingIp = getOneFreeFloatingIp(token)
        val vmName = buildVmName(floatingIp.floatingIp!!)
        val tstackVmId = createVm(token, vmName, imageId, flavorId)
        val pollingStart = System.currentTimeMillis()
        loop@ while (true) {
            Thread.sleep(3 * 1000)
            if ((System.currentTimeMillis() - pollingStart) > 900 * 1000) {
                tryDeleteVm(token, tstackVmId)
                throw RuntimeException("polling create vm timeout(900s)")
            }
            val vmStatus = getVmStatus(token, tstackVmId)
            when (vmStatus) {
                VM_STATUS_BUILD -> continue@loop
                VM_STATUS_ACTIVE -> break@loop
                else -> throw RuntimeException("unexpected volume status")
            }
        }
        syncBindFloatingIpToVm(token, floatingIp.id!!, tstackVmId)
        return RawTstackVm(tstackVmId,
                floatingIp.floatingIp!!,
                vmName,
                "Windows",
                "Win7",
                "8",
                "16G"
        )
    }

    fun getProfileStr(): String {
        return when {
            profile.isProd() -> "prod"
            profile.isTest() -> "test"
            else -> "dev"
        }
    }

    fun buildVmName(ip: String): String {
        val ipSplits = ip.split(".")
        if (ipSplits.size != 4) {
            throw RuntimeException("invalid IP")
        }
        return "${getProfileStr()}_vm_${ipSplits[2]}-${ipSplits[3]}"
    }

    fun createVm(token: String, serverName: String, imageId: String, flavorId: String): String {
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
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/servers"

        val requestData = mapOf("server" to mapOf(
                "name" to serverName,
                "imageRef" to imageId,
                "flavorRef" to flavorId,
                "networks" to listOf(mapOf("uuid" to systemConfig.networkId))
        ))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "compute")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to create tstack vm")
                throw RuntimeException("failed to create tstack vm")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val server = responseData["server"] as Map<String, Any>
            return server["id"] as String
        }
    }

    fun getVmStatus(token: String, vmId: String): String {
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/servers/$vmId"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "compute")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to get tstack vm status")
                throw RuntimeException("failed to get tstack vm status")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            val server = responseData["server"] as Map<String, Any>
            return server["status"] as String
        }
    }

    private fun tryDeleteVm(token: String, vmId: String) {
        logger.info("try delete vm $vmId")
        try {
            deleteVm(token, vmId)
        } catch (e: Exception) {
            // do nothing
        }
    }

    fun deleteVm(token: String, vmId: String) {
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/servers/$vmId"
        logger.info("DELETE url: $url")

        val request = Request.Builder().url(url).delete()
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "compute")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val success = response.isSuccessful || response.code() == 404
            if (!success) {
                val responseBody = response.body()!!.string()
                logger.error("failed to delete vm, responseBody: $responseBody")
                throw RuntimeException("failed to delete vm")
            } else {
                logger.info("delete vm success, response code: ${response.code()}")
            }
        }
    }

    fun listFloatingIp(): List<RawFloatingIp> {
        val token = getToken()
        val url = "$tstackServer/v2.0/floatingips"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "network")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to list floating ips")
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
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2.0/floatingips"
        /*{
            "floatingip": {
            "floating_network_id": "6723629e-196a-4f70-884e-0b2724daa188"
            }
        }*/
        val requestData = mapOf("floatingip" to mapOf(
                "floating_network_id" to systemConfig.networkId
        ))
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "network")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
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
                logger.error("failed to create floating ip")
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
        val url = "$tstackServer/v2.0/floatingips/$floatingIpId"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "network")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.warn("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to get floating ip info")
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
        val url = "$tstackServer/v2.0/floatingips/$floatingIpId"
        logger.info("DELETE url: $url")

        val request = Request.Builder().url(url).delete()
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "network")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val success = response.isSuccessful || response.code() == 404
            if (!success) {
                val responseBody = response.body()!!.string()
                logger.error("failed to delete floating ip, responseBody: $responseBody")
                throw RuntimeException("failed to delete floating ip")
            } else {
                logger.info("delete floating ip success, response code: ${response.code()}")
            }
        }
    }

    fun getVmPortId(vmId: String): String {
        val token = getToken()
        val systemConfig = tstackSystemConfig.getTstackSystemConfig()
        val url = "$tstackServer/v2/${systemConfig.tstackProjectId}/servers/$vmId/os-interface"
        logger.info("Get url: $url")

        val request = Request.Builder().url(url).get()
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "compute")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to get tstack vm port ID")
                throw RuntimeException("failed to get tstack vm port ID")
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

    fun bindFloatingIpToVm(token: String, floatingIpId: String, statskVmId: String?): RawFloatingIp {
        val url = "$tstackServer/v2.0/floatingips/$floatingIpId"
        val requestData: Map<String, Any>?
        /*{
            "floatingip": {
                "port_id": "305b84bd-897e-49c5-bb19-b41c0ca59c2a"
                }
        }*/
        if (statskVmId.isNullOrBlank()) {
            requestData = mapOf("floatingip" to mapOf(
                    "port_id" to null
            ))
        } else {
            val vmPortId = getVmPortId(statskVmId!!)
            requestData = mapOf("floatingip" to mapOf(
                    "port_id" to vmPortId
            ))
        }
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("PUT url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url)
                .put(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .addHeader("X-Auth-Token", token)
                .addHeader("Tstack-Service", "network")
                .build()
//        val client = okHttpClient.newBuilder().build()
//        client.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to bind floating IP to tstack vm")
                throw RuntimeException("failed to bind floating IP to tstack vm")
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

    fun unBindFloatingIpToVm(token: String, floatingIpId: String): RawFloatingIp {
        return bindFloatingIpToVm(token, floatingIpId, null)
    }
}