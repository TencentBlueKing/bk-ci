/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.bcs.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.bcs.client.WorkspaceBcsClient
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentOpPatch
import com.tencent.devops.dispatch.kubernetes.pojo.PatchOp
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.interfaces.RemoteDevInterface
import com.tencent.devops.dispatch.kubernetes.pojo.BK_DEVCLOUD_TASK_TIMED_OUT
import com.tencent.devops.dispatch.kubernetes.pojo.Container
import com.tencent.devops.dispatch.kubernetes.pojo.EnvVar
import com.tencent.devops.dispatch.kubernetes.pojo.Environment
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentSpec
import com.tencent.devops.dispatch.kubernetes.pojo.HTTPGetAction
import com.tencent.devops.dispatch.kubernetes.pojo.ImagePullCertificate
import com.tencent.devops.dispatch.kubernetes.pojo.ObjectMeta
import com.tencent.devops.dispatch.kubernetes.pojo.Probe
import com.tencent.devops.dispatch.kubernetes.pojo.ProbeHandler
import com.tencent.devops.dispatch.kubernetes.pojo.ResourceRequirements
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.dispatch.kubernetes.utils.WorkspaceRedisUtils
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.Base64Utils

@Service
class BcsRemoteDevService @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceRedisUtils: WorkspaceRedisUtils,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val workspaceBcsClient: WorkspaceBcsClient,
    private val profile: Profile
) : RemoteDevInterface {

    @Value("\${bcsCloud.workspace.environment.cpu:8000}")
    var workspaceCpu: Int = 8000

    @Value("\${bcsCloud.workspace.environment.memory:16096}")
    var workspaceMemory: Int = 16096 // 单位: MB

    @Value("\${bcsCloud.workspace.environment.disk:100}")
    var workspaceDisk: Int = 100 // 单位: G

    @Value("\${bcsCloud.workspace.preCIGateWayUrl:}")
    val preCIGateWayUrl: String = ""

    @Value("\${bcsCloud.workspace.preCIDownUrl:}")
    val preCIDownUrl: String = ""

    @Value("\${bcsCloud.workspace.backendHost:}")
    val backendHost: String = ""

    @Value("\${bcsCloud.workspace.turboInstallUrl:}")
    val turboInstallUrl: String = ""

    @Value("\${bcsCloud.appId}")
    val bcsCloudAppId: String = ""

    @Value("\${remotedev.idePort}")
    val idePort: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(BcsRemoteDevService::class.java)
        private const val BYTES_IN_GIGABYTE = 1024L * 1024 * 1024
        private const val WORKSPACE_PATH = "/data/landun/workspace"
        private const val VOLUME_MOUNT_NAME = "workspace"

        private const val DEVOPS_REMOTING_IDE_PORT = "DEVOPS_REMOTING_IDE_PORT"
        private const val DEVOPS_REMOTING_WORKSPACE_ROOT_PATH = "DEVOPS_REMOTING_WORKSPACE_ROOT_PATH"
        private const val DEVOPS_REMOTING_GIT_REPO_ROOT_PATH = "DEVOPS_REMOTING_GIT_REPO_ROOT_PATH"
        private const val DEVOPS_REMOTING_GIT_USERNAME = "DEVOPS_REMOTING_GIT_USERNAME"
        private const val DEVOPS_REMOTING_GIT_EMAIL = "DEVOPS_REMOTING_GIT_EMAIL"
        private const val DEVOPS_REMOTING_YAML_NAME = "DEVOPS_REMOTING_YAML_NAME"
        private const val DEVOPS_REMOTING_DEBUG_ENABLE = "DEVOPS_REMOTING_DEBUG_ENABLE"
        private const val DEVOPS_REMOTING_WORKSPACE_FIRST_CREATE = "DEVOPS_REMOTING_WORKSPACE_FIRST_CREATE"
        private const val DEVOPS_REMOTING_WORKSPACE_ID = "DEVOPS_REMOTING_WORKSPACE_ID"
        private const val DEVOPS_REMOTING_PRECI_DOWN_URL = "DEVOPS_REMOTING_PRECI_DOWN_URL"
        private const val DEVOPS_REMOTING_TURBO_DOWN_URL = "DEVOPS_REMOTING_TURBO_DOWN_URL"
        private const val DEVOPS_REMOTING_PRECI_GATEWAY_URL = "DEVOPS_REMOTING_PRECI_GATEWAY_URL"
        private const val DEVOPS_REMOTING_BACKEND_HOST = "DEVOPS_REMOTING_BACKEND_HOST"
        private const val BK_PRE_BUILD_GATEWAY = "BK_PRE_BUILD_GATEWAY"
        private const val DEVOPS_REMOTING_GIT_REMOTE_REPO_URL = "DEVOPS_REMOTING_GIT_REMOTE_REPO_URL"
        private const val DEVOPS_REMOTING_GIT_REMOTE_REPO_BRANCH = "DEVOPS_REMOTING_GIT_REMOTE_REPO_BRANCH"
        private const val DEVOPS_REMOTING_DOTFILE_REPO = "DEVOPS_REMOTING_DOTFILE_REPO"
        private const val INIT_CONTAINER_GIT_TOKEN = "GIT_TOKEN"
        private const val INIT_CONTAINER_GIT_URL = "GIT_URL"
        private const val INIT_CONTAINER_GIT_BRANCH = "GIT_BRANCH"
    }

    override fun createWorkspace(userId: String, event: WorkspaceCreateEvent): Pair<String, String> {
        logger.info("User $userId create workspace: ${JsonUtil.toJson(event)}")
        val imagePullCertificateList = if (event.devFile.runsOn?.container?.credentials != null) {
            listOf(
                ImagePullCertificate(
                    host = event.devFile.runsOn?.container?.host,
                    username = event.devFile.runsOn?.container?.credentials?.username,
                    password = event.devFile.runsOn?.container?.credentials?.password
                )
            )
        } else {
            emptyList()
        }

        val gitRepoRootPath = WORKSPACE_PATH + "/" +
            GitUtils.getDomainAndRepoName(event.repositoryUrl).second.split("/").last()

        val environmentOpRsp = workspaceBcsClient.createWorkspace(
            userId,
            Environment(
                kind = "StatefulSet",
                APIVersion = "apps/v1",
                metadata = ObjectMeta(
                    labels = mapOf(),
                    annotations = mapOf(
                        "bkbcs.tencent.com/advanced-container-type" to "advanced",
                        "bkbcs.tencent.com/advanced-disk-size-bytes" to "${workspaceDisk.toLong() * BYTES_IN_GIGABYTE}"
                    )
                ),
                spec = EnvironmentSpec(
                    containers = listOf(
                        Container(
                            name = event.workspaceName,
                            image = event.devFile.runsOn?.container?.image ?: "",
                            resource = ResourceRequirements(workspaceCpu, workspaceMemory),
                            workingDir = gitRepoRootPath,
                            readinessProbe = Probe(
                                handler = ProbeHandler(
                                    httpGet = HTTPGetAction(
                                        path = "/_remoting/api/remoting/status",
                                        port = 22999,
                                        host = "",
                                        httpHeaders = emptyList()
                                    )
                                )
                            ),
                            command = listOf("/.devopsRemoting/devopsRemoting", "init"),
                            env = generateContainerEnvVar(userId, gitRepoRootPath, event)
                        )
                    ),
                    initContainers = emptyList(),
                    imagePullCertificate = imagePullCertificateList
                )
            )
        )

        return Pair(environmentOpRsp.environmentUid ?: "", environmentOpRsp.taskUid)
    }

    override fun startWorkspace(userId: String, workspaceName: String): String {
        val environmentUid = getEnvironmentUid(workspaceName)
        val environment = workspaceBcsClient.getWorkspaceDetail(userId, environmentUid)
        val envPatchStr = getWorkspaceEnvPatchStr(DEVOPS_REMOTING_WORKSPACE_FIRST_CREATE, "false", environment)
        val resp = workspaceBcsClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.START,
            envPatchStr = envPatchStr
        )

        return resp.taskUid
    }

    override fun stopWorkspace(userId: String, workspaceName: String): String {
        val environmentUid = getEnvironmentUid(workspaceName)
        val resp = workspaceBcsClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.STOP
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = workspaceName,
            status = EnvStatusEnum.stopped,
            dslContext = dslContext
        )

        return resp.taskUid
    }

    override fun deleteWorkspace(userId: String, event: WorkspaceOperateEvent): String {
        val environmentUid = getEnvironmentUid(event.workspaceName)
        val resp = workspaceBcsClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = event.workspaceName,
            environmentAction = EnvironmentAction.DELETE
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = event.workspaceName,
            status = EnvStatusEnum.deleted,
            dslContext = dslContext
        )

        return resp.taskUid
    }

    override fun getWorkspaceUrl(userId: String, workspaceName: String): String {
        TODO("Not yet implemented")
    }

    override fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        logger.info("workspaceTaskCallback|${taskStatus.uid}|$taskStatus")
        workspaceRedisUtils.refreshTaskStatus("bcs", taskStatus.uid, taskStatus)
        return true
    }

    override fun getWorkspaceInfo(userId: String, workspaceName: String): WorkspaceInfo {
        val environmentStatus = workspaceBcsClient.getWorkspaceStatus(userId, getEnvironmentUid(workspaceName))
        val podInfo = environmentStatus.containerStatuses.firstOrNull { it.name == workspaceName }
        return WorkspaceInfo(
            status = environmentStatus.status,
            hostIP = environmentStatus.hostIP,
            environmentIP = environmentStatus.environmentIP,
            clusterId = environmentStatus.clusterId,
            namespace = environmentStatus.namespace,
            environmentHost = getEnvironmentHost(environmentStatus.clusterId, workspaceName),
            ready = podInfo?.ready,
            started = podInfo?.started
        )
    }
    override fun waitTaskFinish(userId: String, taskId: String): DispatchBuildTaskStatus {

        logger.info("BcsRemoteDevService|start to do waitTaskFinish")
        // 将task放入缓存，等待回调
        workspaceRedisUtils.refreshTaskStatus(
            userId = userId,
            taskUid = taskId,
            taskStatus = TaskStatus(taskId)
        )
        // 轮训十分钟
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("Wait task: $taskId finish timeout(10min)")
                return DispatchBuildTaskStatus(
                    DispatchBuildTaskStatusEnum.FAILED,
                    I18nUtil.getCodeLanMessage(BK_DEVCLOUD_TASK_TIMED_OUT)
                )
            }
            Thread.sleep(1 * 1000)
            val taskStatus = workspaceRedisUtils.getTaskStatus(taskId)
            logger.info("BcsRemoteDevService|taskStatus|$taskStatus")
            if (taskStatus?.status != null) {
                logger.info("Loop task status: ${JsonUtil.toJson(taskStatus)}")
                return if (taskStatus.status == TaskStatusEnum.successed) {
                    DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.SUCCEEDED, null)
                } else {
                    DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.FAILED, taskStatus.logs.toString())
                }
            }
        }
    }

    private fun getEnvironmentHost(clusterId: String, workspaceName: String): String {
        return "$workspaceName.${workspaceRedisUtils.getDevcloudClusterIdHost(clusterId, "")}"
    }
    private fun generateContainerEnvVar(
        userId: String,
        gitRepoRootPath: String,
        event: WorkspaceCreateEvent
    ): List<EnvVar> {
        val envVarList = mutableListOf<EnvVar>()
        envVarList.addAll(
            listOf(
                // 此env环境变量顺序不能变更，需保持在第一位，pod patch根据env index更新
                EnvVar(DEVOPS_REMOTING_WORKSPACE_FIRST_CREATE, "true"),
                EnvVar(DEVOPS_REMOTING_IDE_PORT, idePort),
                EnvVar(DEVOPS_REMOTING_WORKSPACE_ROOT_PATH, WORKSPACE_PATH),
                EnvVar(DEVOPS_REMOTING_GIT_REPO_ROOT_PATH, gitRepoRootPath),
                EnvVar(DEVOPS_REMOTING_GIT_USERNAME, userId),
                EnvVar(DEVOPS_REMOTING_GIT_EMAIL, event.devFile.gitEmail ?: ""),
                EnvVar(DEVOPS_REMOTING_DOTFILE_REPO, event.devFile.dotfileRepo ?: ""),
                EnvVar(DEVOPS_REMOTING_YAML_NAME, event.devFilePath),
                EnvVar(DEVOPS_REMOTING_DEBUG_ENABLE, if (profile.isDebug()) "true" else "false"),
                EnvVar(DEVOPS_REMOTING_WORKSPACE_ID, event.workspaceName),
                EnvVar(DEVOPS_REMOTING_PRECI_DOWN_URL, preCIDownUrl),
                EnvVar(DEVOPS_REMOTING_TURBO_DOWN_URL, turboInstallUrl),
                EnvVar(DEVOPS_REMOTING_PRECI_GATEWAY_URL, preCIGateWayUrl),
                EnvVar(DEVOPS_REMOTING_BACKEND_HOST, backendHost),
                EnvVar(BK_PRE_BUILD_GATEWAY, preCIGateWayUrl),
                EnvVar(DEVOPS_REMOTING_GIT_REMOTE_REPO_URL, event.repositoryUrl),
                EnvVar(DEVOPS_REMOTING_GIT_REMOTE_REPO_BRANCH, event.branch)

            )
        )

        val allCustomizedEnvs = event.settingEnvs.toMutableMap().plus(event.devFile.envs ?: emptyMap())
        allCustomizedEnvs.forEach { (t, u) ->
            envVarList.add(EnvVar(t, u))
        }

        return envVarList
    }
    private fun getEnvironmentUid(workspaceName: String): String {
        val workspaceRecord = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
        return workspaceRecord?.environmentUid ?: throw RuntimeException("No devcloud environment with $workspaceName")
    }
    private fun getWorkspaceEnvPatchStr(
        envName: String,
        patchValue: String,
        environment: Environment
    ): String {
        val envList = environment.spec.containers[0].env
        if (envList.isEmpty()) return ""

        val envNameIndex = envList.indexOfFirst { it.name == envName }
        if (envNameIndex < 0) return ""

        val environmentPatch = EnvironmentOpPatch(
            op = PatchOp.ADD.value,
            path = "/spec/containers/0/env/$envNameIndex/value",
            value = patchValue
        )

        val patchJson = JsonUtil.toJson(listOf(environmentPatch)).toByteArray()
        return Base64Utils.encodeToString(patchJson)
    }
}
