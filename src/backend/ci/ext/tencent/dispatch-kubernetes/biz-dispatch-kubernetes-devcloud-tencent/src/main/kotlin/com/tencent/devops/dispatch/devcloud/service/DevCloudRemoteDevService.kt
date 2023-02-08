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

package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dispatch.devcloud.client.WorkspaceDevCloudClient
import com.tencent.devops.dispatch.devcloud.pojo.Container
import com.tencent.devops.dispatch.devcloud.pojo.DataDiskSource
import com.tencent.devops.dispatch.devcloud.pojo.EnvVar
import com.tencent.devops.dispatch.devcloud.pojo.Environment
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentSpec
import com.tencent.devops.dispatch.devcloud.pojo.HTTPGetAction
import com.tencent.devops.dispatch.devcloud.pojo.ImagePullCertificate
import com.tencent.devops.dispatch.devcloud.pojo.Probe
import com.tencent.devops.dispatch.devcloud.pojo.ProbeHandler
import com.tencent.devops.dispatch.devcloud.pojo.ResourceRequirements
import com.tencent.devops.dispatch.devcloud.pojo.Volume
import com.tencent.devops.dispatch.devcloud.pojo.VolumeMount
import com.tencent.devops.dispatch.devcloud.pojo.VolumeSource
import com.tencent.devops.dispatch.devcloud.utils.DevcloudWorkspaceRedisUtils
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.interfaces.RemoteDevInterface
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("devcloudRemoteDevService")
class DevCloudRemoteDevService @Autowired constructor(
    private val dslContext: DSLContext,
    private val devcloudWorkspaceRedisUtils: DevcloudWorkspaceRedisUtils,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val workspaceDevCloudClient: WorkspaceDevCloudClient
) : RemoteDevInterface {

    @Value("\${devCloud.initContainer.image:mirrors.tencent.com/ci/workspace-init:v1.0.3}")
    var initContainerImage: String = "mirrors.tencent.com/ci/workspace-init:v1.0.3"

    @Value("\${devCloud.initContainer.preCIGateWayUrl:")
    val preCIGateWayUrl: String = ""

    @Value("\${devCloud.initContainer.preCIDownUrl:")
    val preCIDownUrl: String = ""

    @Value("\${devCloud.initContainer.backendHost:")
    val backendHost: String = ""

    override fun createWorkspace(userId: String, event: WorkspaceCreateEvent): Pair<String, String> {
        logger.info("User $userId create workspace: ${JsonUtil.toJson(event)}")
        val imagePullCertificateList = if (event.devFile.image?.imagePullCertificate != null) {
            listOf(
                ImagePullCertificate(
                    host = event.devFile.image?.imagePullCertificate?.host,
                    username = event.devFile.image?.imagePullCertificate?.username,
                    password = event.devFile.image?.imagePullCertificate?.password
                )
            )
        } else {
            emptyList()
        }

        val gitRepoRootPath = WORKSPACE_PATH + "/" +
            GitUtils.getDomainAndRepoName(event.repositoryUrl).second.split("/").last()

        val environmentOpRsp = workspaceDevCloudClient.createWorkspace(
            userId,
            Environment(
                kind = "evn/v1",
                APIVersion = "",
                spec = EnvironmentSpec(
                    containers = listOf(
                        Container(
                            name = event.workspaceName,
                            image = event.devFile.image?.publicImage ?: "",
                            resource = ResourceRequirements(2000, 16096),
                            workingDir = gitRepoRootPath,
                            volumeMounts = listOf(
                                VolumeMount(
                                    name = VOLUME_MOUNT_NAME,
                                    mountPath = WORKSPACE_PATH
                                )
                            ),
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
                    initContainers = listOf(
                        Container(
                            name = event.workspaceName + "-init",
                            image = initContainerImage,
                            resource = ResourceRequirements(2000, 4096),
                            volumeMounts = listOf(
                                VolumeMount(
                                    name = VOLUME_MOUNT_NAME,
                                    mountPath = WORKSPACE_PATH
                                )
                            ),
                            env = listOf(
                                EnvVar(INIT_CONTAINER_GIT_TOKEN, event.gitOAuth),
                                EnvVar(INIT_CONTAINER_GIT_URL, event.repositoryUrl),
                                EnvVar(DEVOPS_REMOTING_GIT_REPO_ROOT_PATH, gitRepoRootPath),
                                EnvVar(INIT_CONTAINER_GIT_BRANCH, event.branch)
                            )
                        )
                    ),
                    imagePullCertificate = imagePullCertificateList,
                    volumes = listOf(
                        Volume(
                            name = VOLUME_MOUNT_NAME,
                            volumeSource = VolumeSource(
                                dataDisk = DataDiskSource(
                                    type = "pvc",
                                    sizeLimit = 100
                                )
                            )
                        )
                    )
                )
            )
        )

        return Pair(environmentOpRsp.environmentUid ?: "", environmentOpRsp.taskUid)
    }

    override fun startWorkspace(userId: String, workspaceName: String): String {
        val environmentUid = getEnvironmentUid(workspaceName)
        val resp = workspaceDevCloudClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.START
        )

        return resp.taskUid
    }

    override fun stopWorkspace(userId: String, workspaceName: String): String {
        val environmentUid = getEnvironmentUid(workspaceName)
        val resp = workspaceDevCloudClient.operatorWorkspace(
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

    override fun deleteWorkspace(userId: String, workspaceName: String): String {
        val environmentUid = getEnvironmentUid(workspaceName)
        val resp = workspaceDevCloudClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.DELETE
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = workspaceName,
            status = EnvStatusEnum.deleted,
            dslContext = dslContext
        )

        return resp.taskUid
    }

    override fun getWorkspaceUrl(userId: String, workspaceName: String): String {
        TODO("Not yet implemented")
    }

    override fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        devcloudWorkspaceRedisUtils.refreshTaskStatus("devcloud", taskStatus.uid, taskStatus)
        return true
    }

    override fun getWorkspaceInfo(userId: String, workspaceName: String): WorkspaceInfo {
        val environmentStatus = workspaceDevCloudClient.getWorkspaceStatus(userId, getEnvironmentUid(workspaceName))
        return WorkspaceInfo(
            status = environmentStatus.status,
            hostIP = environmentStatus.hostIP,
            environmentIP = environmentStatus.EnvironmentIP,
            clusterId = environmentStatus.clusterId,
            namespace = environmentStatus.namespace,
            environmentHost = getEnvironmentHost(environmentStatus.clusterId, workspaceName)
        )
    }

    private fun getEnvironmentUid(workspaceName: String): String {
        val workspaceRecord = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
        return workspaceRecord?.environmentUid ?: throw RuntimeException("No devcloud environment with $workspaceName")
    }

    private fun getEnvironmentHost(clusterId: String, workspaceName: String): String {
        return "$workspaceName.${devcloudWorkspaceRedisUtils.getDevcloudClusterIdHost(clusterId)}"
    }

    private fun generateContainerEnvVar(
        userId: String,
        gitRepoRootPath: String,
        event: WorkspaceCreateEvent
    ): List<EnvVar> {
        val envVarList = mutableListOf<EnvVar>()
        val allCustomizedEnvs = event.settingEnvs.toMutableMap().plus(event.devFile.envs ?: emptyMap())
        allCustomizedEnvs.forEach { (t, u) ->
            envVarList.add(EnvVar(t, u))
        }

        envVarList.addAll(
            listOf(
                EnvVar(DEVOPS_REMOTING_IDE_PORT, "23000"),
                EnvVar(DEVOPS_REMOTING_WORKSPACE_ROOT_PATH, WORKSPACE_PATH),
                EnvVar(DEVOPS_REMOTING_GIT_REPO_ROOT_PATH, gitRepoRootPath),
                EnvVar(DEVOPS_REMOTING_GIT_USERNAME, userId),
                EnvVar(DEVOPS_REMOTING_GIT_EMAIL, event.devFile.gitEmail ?: ""),
                EnvVar(DEVOPS_REMOTING_YAML_NAME, event.devFilePath),
                EnvVar(DEVOPS_REMOTING_DEBUG_ENABLE, "true"),
                EnvVar(DEVOPS_REMOTING_WORKSPACE_FIRST_CREATE, "true"),
                EnvVar(DEVOPS_REMOTING_WORKSPACE_ID, event.workspaceName),
                EnvVar(DEVOPS_REMOTING_PRECI_DOWN_URL, preCIDownUrl),
                EnvVar(DEVOPS_REMOTING_PRECI_GATEWAY_URL, preCIGateWayUrl),
                EnvVar(DEVOPS_REMOTING_BACKEND_HOST, backendHost),
                EnvVar(BK_PRE_BUILD_GATEWAY, backendHost),
                EnvVar(DEVOPS_REMOTING_GIT_REMOTE_REPO_URL, event.repositoryUrl)

            )
        )

        return envVarList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudRemoteDevService::class.java)

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
        private const val DEVOPS_REMOTING_PRECI_GATEWAY_URL = "DEVOPS_REMOTING_PRECI_GATEWAY_URL"
        private const val DEVOPS_REMOTING_BACKEND_HOST = "DEVOPS_REMOTING_BACKEND_HOST"
        private const val BK_PRE_BUILD_GATEWAY = "BK_PRE_BUILD_GATEWAY"
        private const val DEVOPS_REMOTING_GIT_REMOTE_REPO_URL = "DEVOPS_REMOTING_GIT_REMOTE_REPO_URL"

        private const val INIT_CONTAINER_GIT_TOKEN = "GIT_TOKEN"
        private const val INIT_CONTAINER_GIT_URL = "GIT_URL"
        private const val INIT_CONTAINER_GIT_BRANCH = "GIT_BRANCH"
    }
}
