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

package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.dispatch.kubernetes.client.KubernetesJobClient
import com.tencent.devops.dispatch.kubernetes.interfaces.JobService
import com.tencent.devops.dispatch.kubernetes.pojo.Job
import com.tencent.devops.dispatch.kubernetes.pojo.JobStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesDockerRegistry
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResource
import com.tencent.devops.dispatch.kubernetes.pojo.NfsConfig
import com.tencent.devops.dispatch.kubernetes.pojo.PodNameSelector
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobLogResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.isFailed
import com.tencent.devops.dispatch.kubernetes.pojo.isRunning
import com.tencent.devops.dispatch.kubernetes.pojo.isSucceeded
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("kubernetesJobService")
class KubernetesJobService @Autowired constructor(
    private val kubernetesJobClient: KubernetesJobClient
) : JobService {

    @Value("\${kubernetes.resources.job.cpu}")
    var cpu: Double = 32.0

    @Value("\${kubernetes.resources.job.memory}")
    var memory: Int = 65535

    @Value("\${kubernetes.resources.job.disk}")
    var disk: Int = 500

    override val slaveEnv = "Kubernetes"

    companion object {
        // kubernetes构建Job默认request配置
        private const val DEFAULT_JOB_REQUEST_CPU = 1
        private const val DEFAULT_JOB_REQUEST_MEM = 1024
        private const val DEFAULT_JOB_REQUEST_DISK = 100
    }

    override fun createJob(userId: String, jobReq: DispatchJobReq): DispatchTaskResp {
        val job = with(jobReq) {
            Job(
                name = alias,
                activeDeadlineSeconds = activeDeadlineSeconds,
                image = image,
                registry = if (registry.host.isBlank() || registry.username.isNullOrBlank() ||
                    registry.password.isNullOrBlank()
                ) {
                    null
                } else {
                    KubernetesDockerRegistry(registry.host, registry.username!!, registry.password!!)
                },
                resource = KubernetesResource(
                    requestCPU = DEFAULT_JOB_REQUEST_CPU.toString(),
                    requestDisk = "${DEFAULT_JOB_REQUEST_DISK}G",
                    requestDiskIO = "0",
                    requestMem = "${DEFAULT_JOB_REQUEST_MEM}Mi",
                    limitCpu = cpu.toString(),
                    limitDisk = "${disk}G",
                    limitDiskIO = "1",
                    limitMem = "${memory}Mi"
                ),
                env = params?.env,
                command = params?.command,
                nfs = params?.nfsVolume?.map { nfsVo ->
                    NfsConfig(
                        server = nfsVo.server,
                        path = nfsVo.path,
                        mountPath = nfsVo.mountPath
                    )
                },
                podNameSelector = PodNameSelector(
                    selector = podNameSelector,
                    usePodData = true
                )
            )
        }

        val result = kubernetesJobClient.createJob(userId, job)
        if (result.isNotOk() || result.data == null) {
            return DispatchTaskResp(
                result.data?.taskId,
                result.message
            )
        }
        return DispatchTaskResp(result.data.taskId)
    }

    override fun getJobStatus(userId: String, jobName: String): DispatchBuildStatusResp {
        val result = kubernetesJobClient.getJobStatus(userId, jobName)
        if (result.isNotOk()) {
            return DispatchBuildStatusResp(
                status = DispatchBuildStatusEnum.failed.name,
                errorMsg = result.message
            )
        }
        val status = JobStatusEnum.realNameOf(result.data?.state)
        if (status == null || status.isFailed()) {
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status?.message)
        }
        return when {
            status.isSucceeded() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.succeeded.name)
            status.isRunning() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.running.name)
            else -> DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status.message)
        }
    }

    override fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): DispatchJobLogResp {
        val result = kubernetesJobClient.getJobLogs(userId, jobName, sinceTime)
        if (result.isNotOk()) {
            return DispatchJobLogResp(
                log = result.data?.split("\n"),
                errorMsg = result.message
            )
        }
        return DispatchJobLogResp(log = result.data?.split("\n"))
    }
}
