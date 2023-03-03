package com.tencent.devops.dispatch.macos.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatcher.macos.dao.DevcloudVirtualMachineDao
import com.tencent.devops.dispatcher.macos.dao.BuildTaskDao
import com.tencent.devops.dispatch.macos.enums.DevCloudCreateMacVMStatus
import com.tencent.devops.dispatcher.macos.pojo.devcloud.DevCloudMacosVmCreate
import com.tencent.devops.dispatcher.macos.pojo.devcloud.DevCloudMacosVmDelete
import com.tencent.devops.dispatcher.macos.pojo.devcloud.DevCloudMacosVmCreateInfo
import com.tencent.devops.dispatcher.macos.pojo.devcloud.DevCloudMacosVmInfo
import com.tencent.devops.dispatch.macos.util.SmartProxyUtil
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class DevCloudMacosService @Autowired constructor(
    private val dslContext: DSLContext,
    private val devcloudVirtualMachineDao: DevcloudVirtualMachineDao,
    private val buildTaskDao: BuildTaskDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudMacosService::class.java)
    }

    @Value("\${devCloud.appId:}")
    private lateinit var devCloudAppId: String

    @Value("\${devCloud.token:}")
    private lateinit var devCloudToken: String

    @Value("\${devCloud.url:}")
    private lateinit var devCloudUrl: String

    @Value("\${devCloud.smartProxyToken:}")
    private lateinit var smartProxyToken: String

    @Value("\${devCloud.rsaPrivateKey:}")
    private lateinit var rsaPrivateKey: String

    @Value("\${credential.aes-key:C/R%3{?OS}IeGT21}")
    private lateinit var aesKey: String

    private val devCloudProjectCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String/*projectId*/, Boolean/*Boolean*/>(
            object : CacheLoader<String, Boolean>() {
                override fun load(projectId: String): Boolean {
                    return try {
                        val projectList = getDevCloudProjectList("")
                        logger.info("devcloud project list:$projectList")
                        if (projectList.contains(projectId)) {
                            logger.info("projectId[$projectId] is in  devcloud project.")
                            true
                        } else {
                            logger.info("projectId[$projectId] is not in  devcloud project")
                            false
                        }
                    } catch (t: Throwable) {
                        logger.info("projectId[$projectId] failed to get devcloud gray project： $t")
                        false
                    }
                }
            }
        )

    private fun getDevCloudProjectList(creator: String): List<String> {
        val url = "$devCloudUrl/api/mac/devops_project_list?page=1&size=9999"
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, smartProxyToken, creator)))
            .get()
            .build()
        val projectList = mutableListOf<String>()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("DevCloud getDevCloudProjectList http code is ${response.code()}, $responseContent")
            if (!response.isSuccessful) {
                logger.error("Fail to request to DevCloud getDevCloudProjectList, http response code: ${response.code()}, msg: $responseContent")
                return projectList
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                if (dataMap.containsKey("items")) {
                    val itemsList = dataMap["items"] as List<Any>
                    itemsList.forEach { item ->
                        var itemTmp = item as Map<String, Any>
                        if (itemTmp["project_name"] != null) {
                            projectList.add(itemTmp["project_name"] as String ?: "")
                        }
                    }
                } else {
                    logger.error("Fail to request to DevCloud getDevCloudProjectList, http response code: ${response.code()}, msg: $responseContent")
                    return projectList
                }
            } else {
                logger.error("Fail to request to DevCloud getDevCloudProjectList, http response code: ${response.code()}, msg: $responseContent")
                return projectList
            }
            return projectList
        }
    }

    fun getVmList(creator: String): List<DevCloudMacosVmInfo> {
        val url = "$devCloudUrl/api/mac/pool/list?page=1&size=9999"
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, smartProxyToken, creator)))
            .get()
            .build()
        val vmInfoList = mutableListOf<DevCloudMacosVmInfo>()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("DevCloud getVmList http code is ${response.code()}, $responseContent")
            if (!response.isSuccessful) {
                logger.error("Fail to request to DevCloud getVmList, http response code: ${response.code()}, msg: $responseContent")
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
                    logger.error("Fail to request to DevCloud getVmList, http response code: ${response.code()}, msg: $responseContent")
                    return vmInfoList
                }
            } else {
                logger.error("Fail to request to DevCloud getVmList, http response code: ${response.code()}, msg: $responseContent")
                return vmInfoList
            }
            return vmInfoList
        }
    }

    fun isDevCloudGrayProject(projectId: String): Boolean {
        return true
//         return devCloudProjectCache.get(projectId)
    }

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
        //  val url ="http:// localhost:9797/create"
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
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, smartProxyToken, creator)))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("$buildId DevCloud creatVM http code is ${response.code()}, $responseContent")
            if (!response.isSuccessful) {
                logger.error("$buildId Fail to request to DevCloud creatVM, http response code: ${response.code()}, msg: $responseContent")
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
            var temp = queryTaskStaus(taskId = taskId, creator = creator)

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
    fun queryTaskStaus(taskId: String, creator: String): Triple<String, DevCloudMacosVmCreateInfo?, String> {
        val url = "$devCloudUrl/api/mac/task/result/$taskId"
        // val url ="http:// localhost:9797/task"
        // val bodyMap=mutableMapOf("taskId" to taskId)
        // var body= jacksonObjectMapper().writeValueAsString(bodyMap)
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, smartProxyToken, creator)))
            .get()
            // .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()
        //  logger.info("start request")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("request is $request")
            logger.info("responseContent is $responseContent")
            // 如果网络波动导致的失败,就不需要返回failed状态,而是返回running状态,过几秒再来轮询
            if (!response.isSuccessful) {
                logger.info("request fail,retry later")
                return Triple(DevCloudCreateMacVMStatus.running.title, null, "")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            logger.info("responseData is $responseData")
            val code = responseData["actionCode"] as Int
            val message = responseData["actionMessage"] as String
            // 如果actionCode不是200,就是devcloud出问题了,返回错误状态
            if (code != 200) {
                return Triple(DevCloudCreateMacVMStatus.failed.title, null, message)
            }
            val vm = responseData["data"] as Map<String, Any>
            val status = vm["status"] as String
            // 如果返回状态为成功,就从response里取出数据并返回
            if (status == DevCloudCreateMacVMStatus.succeeded.title) {
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
                return Triple(DevCloudCreateMacVMStatus.failed.title, null, message)
            }

            if (status == DevCloudCreateMacVMStatus.canceled.title) {
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
            .url(url)
            .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, smartProxyToken, creator)))
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
            .build()
        var result: Boolean = true
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("DevCloud deleteVM http code is ${response.code()}, $responseContent")
            if (!response.isSuccessful) {
                logger.error("Fail to request to DevCloud deleteVM, http response code: ${response.code()}, msg: $responseContent")
                result = false
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                result = true
            } else {
                result = false
            }
        }
        return result
    }
}
