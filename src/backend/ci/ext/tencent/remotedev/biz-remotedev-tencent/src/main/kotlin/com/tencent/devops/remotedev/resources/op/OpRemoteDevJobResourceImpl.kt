package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevJobResource
import com.tencent.devops.remotedev.pojo.job.JobActionType
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaConstValResp
import com.tencent.devops.remotedev.pojo.job.JobType
import com.tencent.devops.remotedev.pojo.job.OpJobSchemaCreateData
import com.tencent.devops.remotedev.service.job.RemoteDevJobService
import com.tencent.devops.remotedev.service.job.RemoteDevSchemaService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpRemoteDevJobResourceImpl @Autowired constructor(
    private val remoteDevSchemaService: RemoteDevSchemaService,
    private val remoteDevJobService: RemoteDevJobService
) : OpRemoteDevJobResource {
    override fun getJobSchemaConstval(userId: String): Result<JobSchemaConstValResp> {
        return Result(
            JobSchemaConstValResp(
                jobType = JobType.values().toSet(),
                jobActionType = JobActionType.values().toSet()
            )
        )
    }

    override fun createJobSchema(userId: String, data: OpJobSchemaCreateData): Result<Boolean> {
        remoteDevSchemaService.opCreateOrUpdateSchema(data)
        return Result(true)
    }

    override fun getSchemaList(userId: String): Result<List<JobSchema>> {
        return Result(remoteDevSchemaService.getSchemaList())
    }

    override fun getSchema(userId: String, schemaId: String): Result<JobSchema?> {
        return Result(remoteDevSchemaService.getSchema(schemaId, true))
    }

    override fun callBackUpdateJobStatus(userId: String, jobId: Long): Result<Boolean> {
        remoteDevJobService.pipelineJobEnd(jobId)
        return Result(true)
    }

    override fun deleteSchema(userId: String, schemaId: String): Result<Boolean> {
        remoteDevSchemaService.deleteSchema(schemaId)
        return Result(true)
    }
}
