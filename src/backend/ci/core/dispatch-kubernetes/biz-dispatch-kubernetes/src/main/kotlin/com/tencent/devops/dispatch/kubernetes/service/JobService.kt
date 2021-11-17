package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.kubernetes.client.JobClient
import com.tencent.devops.dispatch.kubernetes.kubernetes.client.PodsClient
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobResp
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobStatusResp
import com.tencent.devops.dispatch.kubernetes.utils.CommonUtils
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil.isSuccessful
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class JobService @Autowired constructor(
    private val podsClient: PodsClient,
    private val jobClient: JobClient
) {

    fun createJob(userId: String, jobReq: KubernetesJobReq): KubernetesJobResp {
        val jobName = "${userId}${System.currentTimeMillis()}"
        // 获取创建调用方所在的节点
        val nodeName = podsClient.list(jobReq.podNameSelector!!)
            .items?.ifEmpty { null }?.get(0)?.spec?.nodeName
            ?: CommonUtils.onFailure(
                ErrorCodeEnum.CREATE_JOB_ERROR.errorType,
                ErrorCodeEnum.CREATE_JOB_ERROR.errorCode,
                ErrorCodeEnum.CREATE_JOB_ERROR.formatErrorMessage,
                KubernetesClientUtil.getClientFailInfo("获取Pod节点信息失败: 节点 ${jobReq.podNameSelector} 不存在")
            )
        val result = jobClient.create(jobName, jobReq, nodeName.toString())
        if (!result.isSuccessful()) {
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
        TODO()
    }

    fun getJobLogs(jobName: String, sinceTime: String): String {
        TODO()
    }
}
