package com.tencent.devops.dispatch.macos.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import com.tencent.devops.dispatch.macos.dao.DevcloudVirtualMachineDao
import com.tencent.devops.dispatch.macos.enums.DevCloudCreateMacVMStatus
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreate
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreateInfo
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDelete
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmInfo
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class DevCloudMacosService @Autowired constructor(
    private val dslContext: DSLContext,
    private val devcloudVirtualMachineDao: DevcloudVirtualMachineDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudMacosService::class.java)
    }

    @Value("\${macos.devCloud.appId:}")
    private lateinit var devCloudAppId: String

    @Value("\${macos.devCloud.token:}")
    private lateinit var devCloudToken: String

    @Value("\${macos.devCloud.url:}")
    private lateinit var devCloudUrl: String

    @Value("\${devopsGateway.idcProxy:}")
    private lateinit var devopsIdcProxyGateway: String

    fun creatVM(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        creator: String,
        macosVersion: String?,
        xcodeVersion: String?,
        source: String = ""
    ): DevCloudMacosVmCreateInfo? {

        val url = "$devCloudUrl/api/mac/vm/create"
        val macosVmCreate = DevCloudMacosVmCreate(
            project = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            source = source,
            os = macosVersion,
            xcode = xcodeVersion
        )
        var taskId = ""
        val body = ObjectMapper().writeValueAsString(macosVmCreate)
        logger.info("$buildId DevCloud creatVM request body: $body")
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body.toString()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("$buildId DevCloud creatVM http code is ${response.code}, $responseContent")
            if (!response.isSuccessful) {
                logger.error(
                    "$buildId Fail to request to DevCloud creatVM, http response code: ${response.code}, " +
                        "msg: $responseContent"
                )
                return null
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            val message = responseData["actionMessage"] as String
            if (code != 200) {
                logger.info("$buildId DevCloud fail to create MacOS,actionCode is $code ,actionMessage is $message")
                return null
            }
            try {
                val temp = responseData["data"] as Map<String, Any>
                taskId = temp["taskId"] as String
            } catch (e: Exception) {
                try {
                    val dataList = responseData["data"] as List<Any>
                    var vm = dataList[0] as Map<String, Any>
                    logger.info("success create VM")
                    return DevCloudMacosVmCreateInfo(
                        creator = vm["creator"] as String,
                        name = vm["name"] as String,
                        memory = vm["memory"] as String,
                        assetId = vm["assetId"] as String,
                        ip = vm["ip"] as String,
                        disk = vm["disk"] as String,
                        os = vm["os"] as String,
                        id = vm["id"] as Int ?: 0,
                        createdAt = vm["createdAt"] as String,
                        cpu = vm["cpu"] as String,
                        user = vm["user"] as String ?: "",
                        password = vm["password"] as String ?: ""
                    )
                } catch (e: Exception) {
                    logger.info("$buildId DevCloud return wrong info, Exception is ${e.message}")
                    return null
                }
            }
            logger.info("$buildId success send creating VM request,enters the query process,taskId is $taskId")
        }

        var times = 0
        logger.info("start query")
        while (times < 200) {
            var temp = queryTaskStatus(taskId = taskId, creator = creator)

            var staus = temp.first
            if (staus == DevCloudCreateMacVMStatus.failed.title) {
                logger.info("fail to query task status, actionMessage is ${temp.third}")
                return null
            } else if (staus == DevCloudCreateMacVMStatus.canceled.title) {
                logger.info("user cancel task")
                return null
            } else if (staus == DevCloudCreateMacVMStatus.succeeded.title) {
                logger.info("success create MacOS VM")
                return temp.second
            }

            if (times % 50 == 0) {
                logger.info("query times is ${times + 1}")
            }
            times++
            Thread.sleep(3000)
        }
        logger.info("create failed. creation time Over limit")
        return null
    }

    // 返回值为三元组,分别代表devcloud构建状态,构建成功时返回的数据,构建失败时的错误信息
    fun queryTaskStatus(taskId: String, creator: String): Triple<String, DevCloudMacosVmCreateInfo?, String> {
        val url = "$devCloudUrl/api/mac/task/result/$taskId"
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            // 如果网络波动导致的失败,就不需要返回failed状态,而是返回running状态,过几秒再来轮询
            if (!response.isSuccessful) {
                logger.info("$taskId request fail,retry later, $responseContent")
                return Triple(DevCloudCreateMacVMStatus.running.title, null, "")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            val message = responseData["actionMessage"] as String
            // 如果actionCode不是200,就是devcloud出问题了,返回错误状态
            if (code != 200) {
                logger.info("$taskId response code not 200, $responseContent")
                return Triple(DevCloudCreateMacVMStatus.failed.title, null, message)
            }
            val vm = responseData["data"] as Map<String, Any>
            val status = vm["status"] as String
            // 如果返回状态为成功,就从response里取出数据并返回
            if (status == DevCloudCreateMacVMStatus.succeeded.title) {
                logger.info("$taskId request success. $responseContent")
                return Triple(status,
                    DevCloudMacosVmCreateInfo(
                        creator = vm["creator"] as String,
                        name = vm["name"] as String,
                        memory = vm["memory"] as String,
                        assetId = vm["assetId"] as String,
                        ip = vm["ip"] as String,
                        disk = vm["disk"] as String,
                        os = vm["os"] as String,
                        id = vm["id"] as Int ?: 0,
                        createdAt = vm["createdAt"] as String,
                        cpu = vm["cpu"] as String,
                        user = vm["user"] as String ?: "",
                        password = vm["password"] as String ?: ""
                    ), "")
            }
            if (status == DevCloudCreateMacVMStatus.failed.title) {
                logger.info("$taskId response status failed. $responseContent")
                return Triple(DevCloudCreateMacVMStatus.failed.title, null, message)
            }

            if (status == DevCloudCreateMacVMStatus.canceled.title) {
                logger.info("$taskId response status canceled. $responseContent")
                return Triple(DevCloudCreateMacVMStatus.canceled.title, null, message)
            }

            // 否则返回运行中状态
            return Triple(DevCloudCreateMacVMStatus.running.title, null, "")
        }
    }

    fun saveVM(vmCreateInfo: DevCloudMacosVmCreateInfo): Boolean {
        return devcloudVirtualMachineDao.create(dslContext, vmCreateInfo)
    }

    fun deleteVM(
        creator: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        vmId: Int
    ): Boolean {
        val url = "$devCloudUrl/api/mac/vm/delete"
        val macosVmDelete = DevCloudMacosVmDelete(
            project = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            id = vmId.toString()
        )
        val body = ObjectMapper().writeValueAsString(macosVmDelete)
        logger.info("DevCloud deleteVM body:$body")
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body.toString()))
            .build()
        var result: Boolean = true
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("DevCloud deleteVM http code is ${response.code}, $responseContent")
            if (!response.isSuccessful) {
                logger.error(
                    "Fail to request to DevCloud deleteVM, http response code: ${response.code}, msg: $responseContent"
                )
                result = false
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            result = 200 == code
        }
        return result
    }

    fun getVmList(creator: String): List<DevCloudMacosVmInfo> {
        val url = "$devCloudUrl/api/mac/pool/list?page=1&size=9999"
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .get()
            .build()
        val vmInfoList = mutableListOf<DevCloudMacosVmInfo>()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("DevCloud getVmList http code is ${response.code}, $responseContent")
            if (!response.isSuccessful) {
                logger.error("Fail to request to DevCloud getVmList, http response code: ${response.code}, " +
                                 "msg: $responseContent")
                return vmInfoList
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                if (dataMap.containsKey("items")) {
                    val itemsList = dataMap["items"] as List<Any>
                    itemsList.forEach { item ->
                        var itemTmp = item as Map<String, Any>
                        if (itemTmp["ip"] != null) {
                            vmInfoList.add(
                                DevCloudMacosVmInfo(
                                    name = itemTmp["name"] as String ?: "",
                                    memory = itemTmp["memory"] as String ?: "",
                                    assetId = itemTmp["assetId"] as String ?: "",
                                    ip = itemTmp["ip"] as String ?: "",
                                    disk = itemTmp["disk"] as String ?: "",
                                    os = itemTmp["os"] as String ?: "",
                                    id = itemTmp["id"] as Int ?: 0,
                                    cpu = itemTmp["cpu"] as String ?: ""
                                )
                            )
                        }
                    }
                } else {
                    logger.error("Fail to request to DevCloud getVmList, http response code: ${response.code}, " +
                                     "msg: $responseContent")
                    return vmInfoList
                }
            } else {
                logger.error("Fail to request to DevCloud getVmList, http response code: ${response.code}, " +
                                 "msg: $responseContent")
                return vmInfoList
            }
            return vmInfoList
        }
    }

    fun toIdcUrl(realUrl: String) = "$devopsIdcProxyGateway/proxy-devnet?" +
        "url=${URLEncoder.encode(realUrl, "UTF-8")}"
}
