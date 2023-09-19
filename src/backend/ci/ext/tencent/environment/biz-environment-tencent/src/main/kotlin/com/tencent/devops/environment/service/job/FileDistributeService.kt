package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.JobCloudFileDistributeReq
import com.tencent.devops.environment.pojo.job.FileDistributeResult
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("FileDistributeService")
class FileDistributeService {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""
    fun distributeFile(jobCloudFileDistributeReq: JobCloudFileDistributeReq): Result<FileDistributeResult> {
        val bkAppCode = appCode
        val bkAppSecret = appSecret
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"userId\": \"${jobCloudFileDistributeReq.bkUsername}\"}"
        val fileDistributeUrl = "https://jobv3-cloud.apigw.o.woa.com/stag/api/v3/fast_transfer_file/" // 预发布 TODO：改为配置项
        jobCloudFileDistributeReq.bkAppCode = appCode!!
        jobCloudFileDistributeReq.bkAppSecret = appSecret!!
//        fileDistributeJobCloudReq.bk_scope_type = bkScopeType!! // TODO：改为配置项
//        fileDistributeJobCloudReq.bk_scope_id = bkScopeId!! // TODO：改为配置项

        val request = NetworkUtil.createPostRequest(
            url = fileDistributeUrl,
            bkAuthorization = bkAuthorization,
            jobCloudReq = jobCloudFileDistributeReq::class.java
        )
        val jobCloudResp: JobCloudResp<FileDistributeResult> = NetworkUtil.executeHttpRequest("distributeFile", request)
        val fileDistributeResult = FileDistributeResult(
            jobInstanceId = jobCloudResp.data?.jobInstanceId ?: 0,
            jobInstanceName = jobCloudResp.data?.jobInstanceName ?: "",
            stepInstanceId = jobCloudResp.data?.stepInstanceId ?: 0
        )
        return Result(
            status = jobCloudResp.code,
            message = jobCloudResp.message,
            data = fileDistributeResult
        )
    }
}