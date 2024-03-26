package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserRemoteDevJobResource
import com.tencent.devops.remotedev.pojo.job.CronJob
import com.tencent.devops.remotedev.pojo.job.CronJobSearchParam
import com.tencent.devops.remotedev.pojo.job.JobCreateData
import com.tencent.devops.remotedev.pojo.job.JobRecord
import com.tencent.devops.remotedev.pojo.job.JobRecordSearchParam
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobType
import com.tencent.devops.remotedev.service.job.RemoteDevJobService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserRemoteDevJobResourceImpl @Autowired constructor(
    private val remoteDevJobService: RemoteDevJobService
) : UserRemoteDevJobResource {
    override fun fetchJobSchemaList(userId: String, type: JobType): Result<List<JobSchema>> {
        // TODO: 是否需要鉴权
        return Result(remoteDevJobService.getJobIdAndNames(type))
    }

    override fun getJobSchema(userId: String, schemaId: String): Result<JobSchema?> {
        return Result(remoteDevJobService.getSchema(schemaId))
    }

    override fun createJob(userId: String, data: JobCreateData): Result<Boolean> {
        remoteDevJobService.createJob(userId, data)
        return Result(true)
    }

    override fun fetchJobRecord(userId: String, search: JobRecordSearchParam): Result<Page<JobRecord>> {
        return Result(remoteDevJobService.fetchJobRecord(search))
    }

    override fun fetchCronList(userId: String, search: CronJobSearchParam): Result<Page<CronJob>> {
        return Result(remoteDevJobService.fetchCronJob(search))
    }

    override fun recordRerun(userId: String, id: Long): Result<Boolean> {
        remoteDevJobService.recordRerun(id)
        return Result(true)
    }
}
