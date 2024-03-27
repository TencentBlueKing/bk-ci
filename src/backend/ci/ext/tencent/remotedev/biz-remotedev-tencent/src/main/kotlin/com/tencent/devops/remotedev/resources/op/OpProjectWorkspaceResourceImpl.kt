package com.tencent.devops.remotedev.resources.op

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.remotedev.api.op.OpProjectWorkspaceResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceCreate
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceFetchData
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.OpUpdateCCHostData
import com.tencent.devops.remotedev.pojo.op.WindowsSpecResInfo
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyListData
import com.tencent.devops.remotedev.pojo.windows.FetchOwnerAndAdminData
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceXlsxExportService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors
import javax.ws.rs.core.Response

@Suppress("ALL")
@RestResource
class OpProjectWorkspaceResourceImpl @Autowired constructor(
    private val workspaceCommon: WorkspaceCommon,
    private val createControl: CreateControl,
    private val workspaceService: WorkspaceService,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val desktopWorkspaceService: DesktopWorkspaceService,
    private val gitProxyService: GitProxyService,
    private val xlsxExportService: WorkspaceXlsxExportService,
    private val client: Client,
    private val notifyControl: NotifyControl,
    private val redisOperation: RedisOperation
) : OpProjectWorkspaceResource {
    private val executor = Executors.newCachedThreadPool()

    @AuditEntry(
        actionId = ActionId.CGS_ASSIGN,
        subActionIds = [ActionId.CGS_CREATE]
    )
    @ActionAuditRecord(
        actionId = ActionId.CGS_ASSIGN,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS
        ),
        content = ActionAuditContent.CGS_ASSIGN_PROJECT_CONTENT
    )
    override fun assignWorkspace(
        userId: String,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        // 分配之前先同步下最新的数据
        workspaceCommon.syncStartCloudResourceList()
        val cgsData = workspaceCommon.getCgsData(data.cgsIds, data.ips) ?: return Result(false)
        // 增加可以分配的配额
        if (!data.ips.isNullOrEmpty() || !data.cgsIds.isNullOrEmpty()) {
            client.get(ServiceTxProjectResource::class).updateRemotedev(
                userId = userId,
                projectCode = data.projectId,
                addcloudDesktopNum = (data.ips?.size ?: 0) + (data.cgsIds?.size ?: 0),
                enable = null
            )
        }
        // 判断是不是特殊机型，如果是特殊机型增加特殊机型份额
        val allSpecSize = windowsResourceConfigService.getAllType(true, true).map { it.size }.toSet()
        val allowSpecSize = windowsResourceConfigService.fetchSpec(
            projectId = data.projectId,
            machineType = null,
            page = 1,
            pageSize = 1000
        ).records.associate { it.size to it.quota }
        cgsData.forEach { cgs ->
            // 判断是不是特殊机型，如果是特殊机型增加特殊机型份额
            if (cgs.machineType in allSpecSize) {
                windowsResourceConfigService.createOrUpdateSpec(
                    WindowsSpecResInfo(
                        projectId = data.projectId,
                        size = cgs.machineType.trim(),
                        quota = ((allowSpecSize[cgs.machineType.trim()] ?: 0) + 1)
                    )
                )
            }
            if (cgs.status != Constansts.CGS_AVAIABLE_STATUS) return@forEach
            // 先校验该cgsId是否已被申领分配并运行中
            if (workspaceCommon.checkCgsRunning(cgs.cgsId)) return@forEach
            // 审计
            ActionAuditContext.current()
                .addInstanceInfo(
                    cgs.cgsId,
                    cgs.cgsId,
                    null,
                    null
                )
                .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, data.projectId)
                .scopeId = data.projectId
            // 再根据机型和地域获取硬件资源配置
            val windowsResourceConfigId = windowsResourceConfigService.getTypeConfig(
                machineType = cgs.machineType
            ) ?: return Result(false)
            // 调用CreateControl.asyncCreateWorkspace发起创建
            createControl.asyncCreateWorkspace(
                pmUserId = userId,
                projectId = data.projectId,
                cgsId = cgs.cgsId,
                autoAssign = false,
                workspaceCreate = ProjectWorkspaceCreate(
                    windowsType = windowsResourceConfigId.size,
                    windowsZone = cgs.zoneId.replace(Regex("\\d+"), ""),
                    baseImageId = 0,
                    count = 1
                )
            )
            Thread.sleep(200)
        }

        // 启动流水线完成剩下的分配工作
        if (data.repoId.isNullOrBlank() || data.localDriver.isNullOrBlank()) {
            return Result(true)
        }
        executor.execute {
            try {
                val infoS = redisOperation.get(PIPELINE_CONFIG_INFO) ?: return@execute
                val info = JsonUtil.to(infoS, AssignWorkspacePipelineInfo::class.java)

                val cgsIps = data.cgsIds?.map {
                    val hostIdSub = it.split(".")
                    hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")
                }?.toSet()
                val resIps = mutableSetOf<String>()
                resIps.addAll(cgsIps ?: emptySet())
                resIps.addAll(data.ips ?: emptySet())

                val newParam = mutableMapOf<String, String>()
                info.buildParam.forEach { (k, v) ->
                    when (v) {
                        "job_ip_list" -> newParam[k] = resIps.joinToString(separator = " ")
                        "repoId" -> newParam[k] = data.repoId ?: ""
                        "localDriver" -> newParam[k] = data.localDriver ?: ""
                        else -> newParam[k] = v
                    }
                }
                client.get(ServiceBuildResource::class).manualStartupNew(
                    userId = info.userId ?: userId,
                    projectId = info.projectId,
                    pipelineId = info.pipelineId,
                    values = newParam,
                    channelCode = ChannelCode.BS,
                    buildNo = null,
                    startType = StartType.SERVICE
                )
            } catch (e: Exception) {
                logger.warn("execute assignWorkspace pipeline error", e)
            }
        }

        return Result(true)
    }

    override fun getProjectWorkspaceList(
        userId: String,
        data: ProjectWorkspaceFetchData
    ): Result<Page<ProjectWorkspace>> {
        return Result(workspaceService.getProjectWorkspaceList4Op(userId, data))
    }

    override fun fetchOwnerAndAdmin(
        userId: String,
        data: FetchOwnerAndAdminData
    ): Result<Set<String>> {
        return Result(desktopWorkspaceService.fetchOwnerAndAdmin(data))
    }

    override fun updateCCHost(userId: String, data: OpUpdateCCHostData): Result<Boolean> {
        return Result(desktopWorkspaceService.updateCCHost(data))
    }

    override fun refreshCodeProxy(userId: String, projectId: String) {
        gitProxyService.refreshCodeProxy(projectId)
    }

    override fun exportProjectWorkspaceList(userId: String, data: ProjectWorkspaceFetchData): Response {
        return xlsxExportService.exportProjectWorkspaceListOp(userId, data)
    }

    override fun notify(userId: String, notifyData: WorkspaceNotifyData): Result<Boolean> {
        notifyControl.notifyWorkspaceInfo(
            userId = userId,
            notifyData = notifyData
        )
        return Result(true)
    }

    override fun fetchNotifyList(userId: String, page: Int, pageSize: Int): Result<List<WorkspaceNotifyListData>> {
        return Result(notifyControl.fetchNotifyList(page, pageSize))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpProjectWorkspaceResourceImpl::class.java)
        private const val PIPELINE_CONFIG_INFO = "remotedev:assignWorkspace.pipelineinfo"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssignWorkspacePipelineInfo(
    val userId: String?,
    val projectId: String,
    val pipelineId: String,
    val buildParam: Map<String, String>
)
