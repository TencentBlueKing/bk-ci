package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.JobCloudQueryAccountAliasReq
import com.tencent.devops.environment.pojo.job.QueryAccountAliasResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryAccountAliasService")
class QueryAccountAliasService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    fun queryAccountAlias(jobCloudAccountAliasQueryReq: JobCloudQueryAccountAliasReq): String {
        // get Result<QueryAccountAliasResult> from 上云job接口
        return jobCloudAccountAliasQueryReq.account
    }
}