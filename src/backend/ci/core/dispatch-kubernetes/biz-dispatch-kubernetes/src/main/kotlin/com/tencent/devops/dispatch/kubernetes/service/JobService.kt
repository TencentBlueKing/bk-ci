package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.kubernetes.client.JobClient
import com.tencent.devops.dispatch.kubernetes.kubernetes.client.PodsClient
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobResp
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.PodStatus
import com.tencent.devops.dispatch.kubernetes.utils.CommonUtils
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil.getFirstContainer
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil.getFirstPod
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class JobService @Autowired constructor(
    private val podsClient: PodsClient,
    private val jobClient: JobClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobService::class.java)
    }

    fun createJob(buildId: String, jobReq: KubernetesJobReq): KubernetesJobResp {
        val jobName = KubernetesClientUtil.getKubernetesWorkloadOnlyLabelValue(buildId)
        // 获取创建调用方所在的节点
        val nodeName = podsClient.read(jobReq.podNameSelector!!)?.spec?.nodeName ?: CommonUtils.onFailure(
            ErrorCodeEnum.CREATE_JOB_ERROR.errorType,
            ErrorCodeEnum.CREATE_JOB_ERROR.errorCode,
            ErrorCodeEnum.CREATE_JOB_ERROR.formatErrorMessage,
            KubernetesClientUtil.getClientFailInfo("获取Pod节点信息失败: pod ${jobReq.podNameSelector} 不存在")
        )
        val result = jobClient.create(jobName, jobReq, nodeName.toString())
        if (result.isNotOk()) {
            CommonUtils.onFailure(
                ErrorCodeEnum.CREATE_JOB_ERROR.errorType,
                ErrorCodeEnum.CREATE_JOB_ERROR.errorCode,
                ErrorCodeEnum.CREATE_JOB_ERROR.formatErrorMessage,
                KubernetesClientUtil.getClientFailInfo("创建job容器失败: $result")
            )
        }
        return KubernetesJobResp(jobName)
    }

    fun getJobStatus(jobName: String): KubernetesJobStatusResp {
        val result = podsClient.listWithHttpInfo(workloadOnlyLabel = jobName)
        if (result.isNotOk()) {
            logger.warn("getJobStatus error: ${result.message}")
            return KubernetesJobStatusResp(
                deleted = false,
                status = PodStatus.FAILED.value,
                podResult = null
            )
        }

        val podStatus = result.data?.getFirstPod()?.status
        val podEvents = podStatus?.conditions
            ?.filter { !it.message.isNullOrBlank() && !it.reason.isNullOrBlank() }
            ?.map { condition ->
                KubernetesJobStatusResp.PodResultEvent(
                    message = condition.message!!,
                    reason = condition.reason!!,
                    type = condition.type
                )
            }
        return KubernetesJobStatusResp(
            deleted = false,
            status = PodStatus.getStatusFromK8s(podStatus?.phase).value,
            podResult = listOf(
                KubernetesJobStatusResp.PodResult(
                    ip = podStatus?.podIP,
                    events = podEvents
                )
            )
        )
    }

    fun getJobLogs(jobName: String, sinceTime: Int?): String? {
        val pod = podsClient.list(workloadOnlyLabel = jobName)?.getFirstPod() ?: return null
        val container = pod.getFirstContainer() ?: return null
        return podsClient.logs(
            podName = pod.metadata?.name ?: return null,
            containerName = container.name,
            since = sinceTime
        )
    }
}
