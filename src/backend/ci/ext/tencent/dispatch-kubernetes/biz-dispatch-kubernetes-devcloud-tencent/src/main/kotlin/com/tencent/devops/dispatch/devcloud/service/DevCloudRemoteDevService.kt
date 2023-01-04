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
import com.tencent.devops.dispatch.devcloud.pojo.ImagePullCertificate
import com.tencent.devops.dispatch.devcloud.pojo.ObjectMeta
import com.tencent.devops.dispatch.devcloud.pojo.ResourceRequirements
import com.tencent.devops.dispatch.devcloud.pojo.Volume
import com.tencent.devops.dispatch.devcloud.pojo.VolumeMount
import com.tencent.devops.dispatch.devcloud.pojo.VolumeSource
import com.tencent.devops.dispatch.devcloud.utils.DevcloudWorkspaceRedisUtils
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.interfaces.RemoteDevInterface
import com.tencent.devops.dispatch.kubernetes.pojo.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.devcloud.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("devcloudRemoteDevService")
class DevCloudRemoteDevService @Autowired constructor(
    private val dslContext: DSLContext,
    private val devcloudWorkspaceRedisUtils: DevcloudWorkspaceRedisUtils,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val workspaceDevCloudClient: WorkspaceDevCloudClient
) : RemoteDevInterface {

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

        val environmentOpRsp = workspaceDevCloudClient.createWorkspace(
            userId,
                Environment(
            kind = "evn/v1",
            APIVersion = "",
            metadata = ObjectMeta(
                labels = mapOf(
                    "bkci.dispatch.kubenetes.remoting/workspaceID" to event.workspaceName,
                    "bkci.dispatch.kubenetes.remoting/core" to "workspace",
                    "bkci.dispatch.kubenetes.remoting/owner" to userId
                ),
                annotations = mapOf(
                    "bkci.dispatch.kubenetes.remoting/sshPublicKeys" to event.sshKeys
                )
            ),
            spec = EnvironmentSpec(
                containers = listOf(
                    Container(
                        name = event.workspaceName,
                        image = event.devFile.image?.publicImage ?: "",
                        resource = ResourceRequirements(2000, 4096),
                        volumeMounts = listOf(
                            VolumeMount(
                                name = "workspace",
                                mountPath = WORKSPACE_PATH
                            )
                        ),
                        env = listOf(
                            EnvVar("DEVOPS_REMOTING_IDE_PORT", "23000"),
                            EnvVar("DEVOPS_REMOTING_WORKSPACE_ROOT_PATH", WORKSPACE_PATH),
                            EnvVar(
                                "DEVOPS_REMOTING_GIT_REPO_ROOT_PATH",
                                WORKSPACE_PATH + "/" +
                                    GitUtils.getDomainAndRepoName(event.repositoryUrl).second.split("/").last()
                            ),
                            EnvVar("DEVOPS_REMOTING_GIT_USERNAME", userId),
                            EnvVar("DEVOPS_REMOTING_GIT_EMAIL", event.devFile.gitEmail ?: ""),
                            EnvVar("DEVOPS_REMOTING_YAML_NAME", event.devFilePath),
                            EnvVar("DEVOPS_REMOTING_DEBUG_ENABLE", "true"),
                            EnvVar("DEVOPS_REMOTING_WORKSPACE_FIRST_CREATE", "true"),
                        )
                    )
                ),
                initContainers = listOf(
                    Container(
                        name = event.workspaceName + "-init",
                        image = "mirrors.tencent.com/sawyertest/workspace-init:v1.0.1",
                        resource = ResourceRequirements(2000, 4096),
                        volumeMounts = listOf(
                            VolumeMount(
                                name = "workspace",
                                mountPath = WORKSPACE_PATH
                            )
                        ),
                        env = listOf(
                            EnvVar("GIT_TOKEN", event.gitOAuth),
                            EnvVar("GIT_URL", event.repositoryUrl)
                        )
                    )
                ),
                imagePullCertificate = imagePullCertificateList,
                volumes = listOf(
                    Volume(
                        name = "workspace",
                        volumeSource = VolumeSource(
                            dataDisk = DataDiskSource()
                        )
                    )
                )
            )
        )
        )

        return Pair(environmentOpRsp.environmentUid ?: "", environmentOpRsp.taskUid)
    }

    override fun startWorkspace(userId: String, workspaceName: String): Boolean {
        val environmentUid = getEnvironmentUid(workspaceName)
        workspaceDevCloudClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.START
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = workspaceName,
            status = EnvStatusEnum.Running,
            dslContext = dslContext
        )

        return true
    }

    override fun stopWorkspace(userId: String, workspaceName: String): Boolean {
        val environmentUid = getEnvironmentUid(workspaceName)
        workspaceDevCloudClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.STOP
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = workspaceName,
            status = EnvStatusEnum.Stopped,
            dslContext = dslContext
        )

        return true
    }

    override fun deleteWorkspace(userId: String, workspaceName: String): Boolean {
        val environmentUid = getEnvironmentUid(workspaceName)
        workspaceDevCloudClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.DELETE
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = workspaceName,
            status = EnvStatusEnum.Deleted,
            dslContext = dslContext
        )

        return true
    }

    override fun getWorkspaceUrl(userId: String, workspaceName: String): String {
        TODO("Not yet implemented")
    }

    override fun workspaceHeartbeat(userId: String, workspaceName: String): Boolean {
        devcloudWorkspaceRedisUtils.refreshHeartbeat(userId, workspaceName)
        return true
    }

    override fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        devcloudWorkspaceRedisUtils.refreshTaskStatus("devcloud", taskStatus.uid, taskStatus)
        return true
    }

    private fun getEnvironmentUid(workspaceName: String): String {
        val workspaceRecord = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
        return workspaceRecord?.environmentUid ?: throw RuntimeException("No devcloud environment with $workspaceName")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudRemoteDevService::class.java)

        private const val WORKSPACE_PATH = "/data/landun/workspace"
    }
}
