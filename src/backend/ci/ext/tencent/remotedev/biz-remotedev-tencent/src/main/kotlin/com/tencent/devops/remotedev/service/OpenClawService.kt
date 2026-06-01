package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.auth.api.service.ServiceResourceGroupResource
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.CreateOpenClawData
import com.tencent.devops.remotedev.pojo.CreateOpenClawDataResp
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.TaskStatusResp
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.bk.BkSopRequestBody
import com.tencent.devops.remotedev.pojo.bk.BkSopResponse
import com.tencent.devops.remotedev.pojo.bk.BkSopStatusResp
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64


/**
 * 自建龙虾云桌面相关能力
 */
@Service
class OpenClawService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val bkConfig: BkConfig,
    private val workspaceDao: WorkspaceDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val deliverControl: DeliverControl,
    private val tokenService: ClientTokenService,
    private val permissionService: PermissionService
) {
    fun createOpenClaw(userId: String, data: CreateOpenClawData): CreateOpenClawDataResp {
        // 1、添加用户到项目用户组(AnyDev云桌面用户组)：先判断用户是否有项目权限，
        // 没有才创建用户组 + 加人，有“AnyDev云桌面用户组”直接加人，没有才创建；
        if (!permissionService.checkUserVisitPermission(data.params.userName, data.projectId)) {
            val gpId = client.get(ServiceResourceGroupResource::class).createCustomGroupAndPermissions(
                projectCode = data.projectId,
                customGroupCreateReq = CustomGroupCreateReq(
                    groupName = "AnyDev云桌面用户组",
                    groupDesc = "AnyDev云桌面用户组，用于控制AnyDev的权限",
                    actions = listOf("project_visit")
                )
            ).data ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.OPEN_CLAW_WORKSPACE_CREATE_ERROR.errorCode,
                params = arrayOf("create AnyDev云桌面用户组 error no id")
            )
            val res = client.get(ServiceResourceMemberResource::class).batchAddResourceGroupMembers(
                tokenService.getSystemToken(),
                data.projectId,
                ProjectCreateUserInfo(
                    createUserId = userId,
                    groupId = gpId,
                    userIds = listOf(data.params.userName),
                    roleName = null,
                    roleId = null,
                    deptIds = null,
                    resourceType = null,
                    resourceCode = null
                )
            ).data
            if (res != true) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.OPEN_CLAW_WORKSPACE_CREATE_ERROR.errorCode,
                    params = arrayOf("add ${data.params.userName} AnyDev云桌面用户组 error")
                )
            }
        }

        // 2、分配实例给用户；传入的ip为空：需要从该项目的待分配的 + 开启coffee_ia的实例中选择一台，分配给用户
        var ip = data.ip
        var workspaceName: String? = null
        if (ip.isNullOrBlank()) {
            val record =
                workspaceDao.fetchOldDistributingAIWorkspace(dslContext, data.projectId) ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.OPEN_CLAW_WORKSPACE_CREATE_ERROR.errorCode,
                    params = arrayOf("no distributing workspace")
                )
            ip = record.ip ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.OPEN_CLAW_WORKSPACE_CREATE_ERROR.errorCode,
                params = arrayOf("distributing workspace ${record.workspaceName} no ip")
            )
            workspaceName = record.workspaceName
        }
        if (workspaceName == null) {
            workspaceName = workspaceDao.fetchAIWorkspaceByIp(dslContext, data.projectId, ip)?.workspaceName
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.OPEN_CLAW_WORKSPACE_CREATE_ERROR.errorCode,
                    params = arrayOf("find workspace by $ip null")
                )
        }
        deliverControl.assignUser2Workspace(
            userId = userId,
            workspaceName = workspaceName,
            assigns = listOf(
                ProjectWorkspaceAssign(
                    userId = data.params.userName,
                    type = WorkspaceShared.AssignType.OWNER,
                    expiration = null
                )
            ),
            checkPermission = false
        )

        // 3、执行openclaw初始化脚本：作为脚本命令作为入参传入，发起标准运维执行
        val windowsInfo = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)
        val req = BkSopRequestBody(
            name = "【云桌面】安装龙虾云桌面 ${data.params.userName}:${windowsInfo?.regionId}:${ip}",
            constants = mapOf(
                "\${desktop_ip}" to "${windowsInfo?.regionId}:${ip}",
                "\${envs_base64}" to Base64.getEncoder()
                    .encodeToString(JsonUtil.toJson(data.params.envs, false).toByteArray())
            )
        )
        val body = JsonUtil.toJson(req, false)
        logger.info("createOpenClaw CreateTask|request url: ${bkConfig.bksopsCreateTask}, body: $body")
        val request = Request.Builder()
            .url(bkConfig.bksopsOpenClawCreateTask)
            .headers(bkConfig.sopHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = OkhttpUtils.doHttp(request).resolveResponse<BkSopResponse>()
        logger.info("createOpenClaw CreateTask|task status: ${resp.result}|task url: ${resp.data?.taskUrl}")
        if (resp.data?.taskId != null) {
            val url = bkConfig.bksopsStartTask.replace("{taskId}", "${resp.data.taskId}")
            logger.info("createOpenClaw startTask|request url: $url")
            val request = Request.Builder()
                .url(url)
                .headers(bkConfig.sopHeaders())
                .post("{\"scope\":\"project\"}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
            OkhttpUtils.doHttp(request).resolveResponse<Any>()
        } else {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.OPEN_CLAW_WORKSPACE_CREATE_ERROR.errorCode,
                params = arrayOf("create task no taskId $resp")
            )
        }
        return CreateOpenClawDataResp(
            taskId = resp.data.taskId.toString(),
            workspaceName = workspaceName,
            ip = ip
        )
    }

    fun getTaskStatus(taskId: String): TaskStatusResp {
        val url = bkConfig.bksopsStartTask.replace("{taskId}", taskId)
        logger.info("getTaskStatus|request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(bkConfig.sopHeaders())
            .get()
            .build()
        val resp = OkhttpUtils.doHttp(request).resolveResponse<BkSopStatusResp>()
        return TaskStatusResp(resp.state)
    }

    private inline fun <reified T> okhttp3.Response.resolveResponse(): T {
        this.use {
            val responseContent = this.body!!.string()
            logger.info("request api[${this.request.url.toUrl()}] code: ${this.code}, body: $responseContent")
            if (!this.isSuccessful) {
                throw RemoteServiceException("request api[${this.request.url.toUrl()}] error", this.code)
            }

            val responseData = try {
                objectMapper.readValue(responseContent, jacksonTypeRef<T>())
            } catch (e: Exception) {
                logger.error("request api[${this.request.url.toUrl()}] error: ${this.code}, body: $responseContent")
                throw RemoteServiceException("parse api[${this.request.url.toUrl()}] resp $responseContent", this.code)
            }

            return responseData
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenClawService::class.java)
    }
}