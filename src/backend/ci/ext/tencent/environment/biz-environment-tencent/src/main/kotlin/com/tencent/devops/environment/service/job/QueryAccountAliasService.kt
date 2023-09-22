package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.JobCloudQueryAccountAliasReq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryAccountAliasService")
class QueryAccountAliasService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    fun queryAccountAlias(jobCloudAccountAliasQueryReq: JobCloudQueryAccountAliasReq): String {
        // TODO：get Result<QueryAccountAliasResult> from 上云job接口
        return jobCloudAccountAliasQueryReq.account
    }
}