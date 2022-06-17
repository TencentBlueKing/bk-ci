package com.tencent.devops.dispatch.kubernetes.kubernetes.client

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.kubernetes.common.NFS_VOLUME_NAME_PREFIX
import com.tencent.devops.dispatch.kubernetes.config.DispatchBuildConfig
import com.tencent.devops.dispatch.kubernetes.config.DispatchBuildConfiguration
import com.tencent.devops.dispatch.kubernetes.config.KubernetesClientConfig
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.job.Job
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.job.JobData
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.ContainerData
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.NfsVolume
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.NodeSelector
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.PodData
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.VolumeMount
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobReq
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesDataUtils
import io.kubernetes.client.openapi.models.V1Job
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class JobClient @Autowired constructor(
    private val k8sConfig: KubernetesClientConfig,
    private val dispatchBuildConfig: DispatchBuildConfig,
    private val kubernetesDataUtils: KubernetesDataUtils,
    private val dispatchBuildConfiguration: DispatchBuildConfiguration,
    private val v1ApiSet: V1ApiSet
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobClient::class.java)
    }

    fun create(
        jobName: String,
        jobReq: KubernetesJobReq,
        nodeName: String?
    ): Result<V1Job> {
        val labels = jobReq.params?.labels?.toMutableMap()?.apply {
            putAll(getCoreLabels(jobName))
        } ?: getCoreLabels(jobName)

        val nfsVolumeNames = mutableListOf<String>()
        val volumes = kubernetesDataUtils.getPodVolume().toMutableList().apply {
            addAll(
                jobReq.params?.nfsVolume?.map { nfs ->
                    val name = "${NFS_VOLUME_NAME_PREFIX}_${System.currentTimeMillis()}"
                    nfsVolumeNames.add(name)
                    NfsVolume(
                        name = name,
                        path = nfs.path,
                        server = nfs.server
                    )
                } ?: emptyList()
            )
        }
        val volumeMounts = kubernetesDataUtils.getPodVolumeMount().toMutableList().apply {
            var index = -1
            addAll(
                jobReq.params?.nfsVolume?.map { nfs ->
                    index++
                    VolumeMount(
                        name = nfsVolumeNames[index],
                        mountPath = nfs.mountPath
                    )
                } ?: emptyList()
            )
        }

        val job = with(jobReq) {
            Job.job(
                JobData(
                    apiVersion = "batch/v1",
                    name = alias,
                    nameSpace = k8sConfig.nameSpace!!,
                    backoffLimit = 0,
                    activeDeadlineSeconds = activeDeadlineSeconds,
                    pod = PodData(
                        labels = labels,
                        container = ContainerData(
                            imageName = jobName,
                            image = "${registry?.host ?: ""}$image",
                            cpu = cpu?.toString(),
                            memory = memory,
                            disk = null,
                            ports = null,
                            env = params?.env,
                            commends = params?.command,
                            volumeMounts = volumeMounts
                        ),
                        volumes = volumes,
                        nodeSelector = if (nodeName != null) {
                            NodeSelector(
                                nodeName = nodeName
                            )
                        } else {
                            null
                        },
                        restartPolicy = "Never",
                        tolerations = dispatchBuildConfiguration.tolerations
                    )
                )
            )
        }
        return KubernetesClientUtil.apiHandle {
            v1ApiSet.batchV1Api.createNamespacedJobWithHttpInfo(
                k8sConfig.nameSpace,
                job,
                null, null, null
            )
        }
    }

    private fun getCoreLabels(jobName: String) = mapOf(dispatchBuildConfig.workloadLabel!! to jobName)
}
