package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentStatus
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentStatusRsp
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient.Companion.APP_NOT_BIND_CGS
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient.Companion.NO_CGS_CHOOSE
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentCreate
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentCreateRsp
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentOperate
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentOperateRsp
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.ListCgsResp
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.ListCgsRespData
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceDispatchException
import com.tencent.devops.remotedev.pojo.image.ListImagesData
import com.tencent.devops.remotedev.pojo.image.ListImagesResp
import com.tencent.devops.remotedev.pojo.image.ListVmImagesResp
import com.tencent.devops.remotedev.pojo.image.StandardVmImage
import com.tencent.devops.remotedev.pojo.remotedev.BcsResp
import com.tencent.devops.remotedev.pojo.remotedev.BcsTaskData
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskData
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmReq
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmResp
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmRespData
import java.net.SocketTimeoutException
import java.util.UUID
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Suppress("NestedBlockDepth")
@Component
class WorkspaceBcsClient @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao
) {
    @Value("\${bcsCloud.apiUrl}")
    val bcsCloudUrl: String = ""

    @Value("\${bcsCloud.token}")
    val bcsToken: String = ""

    @Value("\${apigw.appCode}")
    val appCode: String = ""

    @Value("\${apigw.appToken}")
    val appToken: String = ""

    /**
     * TODO: 函数带有 start 的都是之前是放到 start client 下但是操作的是 bcs 的接口，先平移过来，未来看整合到一起
     */

    fun startCreateWorkspace(
        userId: String,
        environment: EnvironmentCreate
    ): EnvironmentCreateRsp.EnvironmentCreateRspData {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/createvm"
        val id = UUID.randomUUID()
        val body = JsonUtil.toJson(environment, false)
        logger.info("$id|User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info(
                    "$id|User $userId create environment response: " +
                            "${response.rid()}|${response.code}|$responseContent"
                )
                if (!response.isSuccessful) {
                    throw WorkspaceDispatchException(
                        "创建环境接口异常: ${response.code}"
                    )
                }

                val environmentRsp: EnvironmentCreateRsp = jacksonObjectMapper().readValue(responseContent)
                logger.info("$id|createWorkspace rsp: $environmentRsp")
                when {
                    WorkspaceStartCloudClient.OK == environmentRsp.code && environmentRsp.data != null
                        -> return environmentRsp.data

                    APP_NOT_BIND_CGS == environmentRsp.code || NO_CGS_CHOOSE == environmentRsp.code
                        -> throw WorkspaceDispatchException(
                        "创建环境接口返回失败: ${environment.basicBody.zoneId}地区${environment.basicBody.machineType}" +
                                "型云桌面资源不足(${environmentRsp.code})"
                    )

                    else -> throw WorkspaceDispatchException(
                        "创建环境接口返回失败: (${environmentRsp.code}-${environmentRsp.message})"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("User $userId create environment get SocketTimeoutException", e)
            throw WorkspaceDispatchException(
                errorMessage = "创建环境接口返回失败: 接口超时, url: $url"
            )
        }
    }

    fun startOperateWorkspace(
        userId: String,
        action: EnvironmentAction,
        workspaceName: String,
        environmentOperate: EnvironmentOperate,
        actionMsg: String = ""
    ): EnvironmentOperateRsp.EnvironmentOperateRspData {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/${action.action}"
        val body = JsonUtil.toJson(environmentOperate, false)
        logger.info("$userId ${action.action} workspace url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$userId ${action.action} workspace response: ${response.rid()}|$responseContent")
                if (!response.isSuccessful) {
                    throw WorkspaceDispatchException(
                        "操作环境接口返回失败: ${response.code}-$responseContent"
                    )
                }

                val environmentOpRsp: EnvironmentOperateRsp = jacksonObjectMapper().readValue(responseContent)
                if (WorkspaceStartCloudClient.OK == environmentOpRsp.code) {
                    // 记录操作历史
                    dispatchWorkspaceOpHisDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        environmentUid = environmentOperate.uid,
                        operator = userId,
                        uid = environmentOpRsp.data!!.taskUid,
                        action = action,
                        actionMsg = actionMsg
                    )

                    return environmentOpRsp.data!!
                } else {
                    throw WorkspaceDispatchException(
                        "操作环境接口返回失败: ${environmentOpRsp.code}-${environmentOpRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("$userId ${action.action} workspace get SocketTimeoutException.", e)
            throw WorkspaceDispatchException(
                errorMessage = "操作环境接口返回失败: 接口超时, url: $url"
            )
        }
    }

    fun startGetWorkspaceInfo(
        userId: String,
        environmentOperate: EnvironmentOperate
    ): EnvironmentStatus {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/status"
        val body = JsonUtil.toJson(environmentOperate, false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$userId get workspace info body: $body response: ${response.rid()}|$responseContent")
                if (!response.isSuccessful) {
                    throw WorkspaceDispatchException(
                        "获取工作空间信息接口异常: ${response.code}"
                    )
                }

                val environmentInfoRsp: EnvironmentStatusRsp = jacksonObjectMapper().readValue(responseContent)
                if (WorkspaceStartCloudClient.OK == environmentInfoRsp.code) {
                    return environmentInfoRsp.data!!
                } else {
                    throw WorkspaceDispatchException(
                        "获取工作空间信息接口异常: ${environmentInfoRsp.code}-${environmentInfoRsp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("$userId get workspace info SocketTimeoutException.", e)
            throw WorkspaceDispatchException(
                errorMessage = "获取工作空间信息接口异常:  接口超时, url: $url"
            )
        }
    }

    fun startGetResourceVm(
        data: ResourceVmReq
    ): List<ResourceVmRespData>? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/resource/vm/list"
        val body = JsonUtil.toJson(data, false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("get resource vm body: $body response: ${response.rid()}|$responseContent")
                if (!response.isSuccessful) {
                    throw WorkspaceDispatchException(
                        "获取机器资源接口异常: ${response.code}"
                    )
                }
                val resp: ResourceVmResp = jacksonObjectMapper().readValue(responseContent)
                if (WorkspaceStartCloudClient.OK == resp.code) {
                    return resp.data?.zoneResources
                } else {
                    throw WorkspaceDispatchException(
                        "获取机器资源接口异常: ${resp.code}-${resp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("get resource vm SocketTimeoutException.", e)
            throw WorkspaceDispatchException(
                errorMessage = "获取机器资源接口异常: 接口超时, url: $url"
            )
        }
    }

    fun startListCgs(): List<ListCgsRespData> {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/listcgs"
        val body = JsonUtil.toJson("", false)
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("get cgs list response: ${response.rid()}|${response.code}|$responseContent")
                if (!response.isSuccessful) {
                    throw WorkspaceDispatchException(
                        " 获取listcgs接口异常: ${response.code}"
                    )
                }

                val resp: ListCgsResp = jacksonObjectMapper().readValue(responseContent)
                when (resp.code) {
                    WorkspaceStartCloudClient.OK -> {
                        if (resp.data == null) {
                            throw WorkspaceDispatchException(
                                " 获取listcgs接口异常: data is null"
                            )
                        }
                        return resp.data
                    }

                    else -> throw WorkspaceDispatchException(
                        " 获取listcgs接口异常: ${resp.code}-${resp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("get listcgs SocketTimeoutException", e)
            throw WorkspaceDispatchException(
                errorMessage = " 获取listcgs接口超时, url: $url"
            )
        }
    }

    // 获取基础镜像列表
    fun startGetVmStandardImages(): List<StandardVmImage>? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/list/image"
        val body = JsonUtil.toJson("", false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("list vm image body: $body response: ${response.rid()}|$responseContent")
                if (!response.isSuccessful) {
                    throw WorkspaceDispatchException(
                        "获取机器资源接口异常: ${response.code}"
                    )
                }

                val resp: ListVmImagesResp = jacksonObjectMapper().readValue(responseContent)
                if (WorkspaceStartCloudClient.OK == resp.code) {
                    return resp.data
                } else {
                    throw WorkspaceDispatchException(
                        "获取标准镜像接口异常:${resp.code}-${resp.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("get resource vm SocketTimeoutException.", e)
            throw WorkspaceDispatchException(
                errorMessage = "获取标准镜像接口异常: 接口超时, url: $url"
            )
        }
    }

    fun expandDiskValidate(
        data: EnvironmentOperate
    ): ExpandDiskValidateResp? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/expanddisk/validate"
        val body = JsonUtil.toJson(data, false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        return OkhttpUtils.doHttp(request).resolveResponse<BcsResp<ExpandDiskValidateResp>>().data
    }

    fun expandDisk(
        data: ExpandDiskData
    ): BcsTaskData? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/expanddisk"
        val body = JsonUtil.toJson(data, false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        return OkhttpUtils.doHttp(request).resolveResponse<BcsResp<BcsTaskData>>().data
    }

    fun fetchImages(
        data: ListImagesData
    ): ListImagesResp? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/images".addQuery(
            "architecture" to data.architecture,
            "envId" to data.envId,
            "imageName" to data.imageName,
            "imageType" to data.imageType,
            "machineType" to data.machineType,
            "platform" to data.platform,
            "projectId" to data.projectId,
            "provider" to data.provider,
            "zoneId" to data.zoneId
        )
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .get()
            .build()
        return OkhttpUtils.doHttp(request).resolveResponse<BcsResp<ListImagesResp>>().data
    }

    fun deleteImage(
        imageId: String,
        delaySeconds: Int?
    ): EnvironmentOperateRsp.EnvironmentOperateRspData? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/images/$imageId".addQuery(
            "delaySeconds" to delaySeconds
        )
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .delete()
            .build()
        return OkhttpUtils.doHttp(request)
            .resolveResponse<EnvironmentOperateRsp>().data
    }

    private inline fun <reified T> okhttp3.Response.resolveResponse(): T {
        this.use {
            val responseContent = this.body!!.string()
            logger.info("request bcs ${this.request.url} resp ${this.rid()}|$responseContent}")
            if (this.isSuccessful) {
                return objectMapper.readValue(responseContent, jacksonTypeRef<T>())
            }

            val responseData = try {
                objectMapper.readValue<BcsResp<Void>>(responseContent)
            } catch (e: JacksonException) {
                throw RemoteServiceException(responseContent, this.code)
            }
            throw RemoteServiceException(responseData.message ?: responseData.code.toString(), this.code)
        }
    }

    private fun makeHeaders(): Map<String, String> {
        val headerMap = mapOf("bk_app_code" to appCode, "bk_app_secret" to appToken)
        val headerStr = objectMapper.writeValueAsString(headerMap).replace("\\s".toRegex(), "")
        return mapOf("X-Bkapi-Authorization" to headerStr, "BK-Devops-Token" to bcsToken)
    }

    private fun String.addQuery(vararg pairs: Pair<String, Any?>): String {
        val sb = StringBuilder(this)
        var flag = 0
        pairs.forEach { (name, value) ->
            if (value == null) {
                return@forEach
            }
            flag += 1
            if (flag == 1) {
                sb.append("?$name=$value")
            } else {
                sb.append("&$name=$value")
            }
        }
        return sb.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceBcsClient::class.java)
        const val OK = 0

        private fun Response.rid(): String? {
            return this.headers["x-request-id"]
        }
    }
}
