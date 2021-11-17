package com.tencent.devops.dispatch.kubernetes.kubernetes.client

import com.tencent.devops.dispatch.kubernetes.config.DispatchBuildConfig
import com.tencent.devops.dispatch.kubernetes.config.KubernetesClientConfig
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.job.Job
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.job.JobData
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.ContainerData
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.NodeSelector
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.PodData
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobReq
import io.kubernetes.client.openapi.ApiResponse
import io.kubernetes.client.openapi.apis.BatchV1Api
import io.kubernetes.client.openapi.models.V1Job
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class JobClient @Autowired constructor(
    private val k8sConfig: KubernetesClientConfig,
    private val dispatchBuildConfig: DispatchBuildConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobClient::class.java)
    }

    fun create(
        jobName: String,
        jobReq: KubernetesJobReq,
        nodeName: String?
    ): ApiResponse<V1Job> {
        val labels = jobReq.params?.labels?.toMutableMap()?.let {
            it.putAll(getCoreLabels(jobName))
            it
        } ?: getCoreLabels(jobName)
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
                            image = image,
                            cpu = cpu?.toString(),
                            memory = memory,
                            disk = null,
                            ports = null,
                            env = params?.env,
                            commends = params?.command
                        ),
                        nodeSelector = if (nodeName != null) {
                            NodeSelector(
                                nodeName = podNameSelector!!
                            )
                        }else{
                            null
                        }
                    )
                )
            )
        }
        return BatchV1Api().createNamespacedJobWithHttpInfo(
            k8sConfig.nameSpace,
            job,
            null, null, null
        )
    }

    private fun getCoreLabels(jobName: String) = mapOf(dispatchBuildConfig.jobLabel!! to jobName)
}
